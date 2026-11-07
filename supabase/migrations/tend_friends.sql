-- tend friends: invite codes, friendships, and what friends may see of each other.
-- Run once in the Supabase SQL editor of the "Bo & Mia" project, after tend_accounts.sql.

-- What friends see of you: the camp's layout ("spot:thing,...") next to name, companion and level.
alter table tend.profiles add column if not exists placed text not null default '' check (char_length(placed) <= 400);

-- One invite code per person; a new one replaces the old (old links stop working).
create table if not exists tend.invites (
  code text primary key check (code ~ '^[A-Z2-9]{6}$'),
  owner uuid not null unique references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);
alter table tend.invites enable row level security;

-- A friendship is one row, smaller id first.
create table if not exists tend.friendships (
  a uuid not null references auth.users(id) on delete cascade,
  b uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (a, b),
  check (a < b)
);
alter table tend.friendships enable row level security;

-- Profile with the camp (replaces the 3-argument version).
drop function if exists public.tend_save_profile(text, text, int);
create or replace function public.tend_save_profile(p_name text, p_companion text, p_level int, p_placed text default '')
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  insert into tend.profiles (user_id, name, companion, level, placed, updated_at)
  values (auth.uid(), left(p_name, 40), p_companion, greatest(1, least(p_level, 1000)), left(coalesce(p_placed, ''), 400), now())
  on conflict (user_id) do update set name = excluded.name, companion = excluded.companion, level = excluded.level,
    placed = excluded.placed, updated_at = now();
end $$;

-- A random 6-character code without look-alike letters (no 0/O, 1/I).
create or replace function tend.random_code() returns text language sql volatile set search_path = '' as $$
  select string_agg(substr('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + floor(random() * 32)::int, 1), '')
  from generate_series(1, 6);
$$;

-- The caller's invite code; made on first use, or replaced when p_new is true.
create or replace function public.tend_my_invite(p_new boolean default false)
returns text language plpgsql security definer set search_path = '' as $$
declare c text;
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if not p_new then
    select code into c from tend.invites where owner = auth.uid();
    if c is not null then return c; end if;
  end if;
  delete from tend.invites where owner = auth.uid();
  loop
    c := tend.random_code();
    begin
      insert into tend.invites (code, owner) values (c, auth.uid());
      return c;
    exception when unique_violation then
      -- taken: try another
    end;
  end loop;
end $$;

-- Who an invite code belongs to (name, companion, level), to ask "Add Peti?". Empty when unknown or your own.
create or replace function public.tend_invite_owner(p_code text)
returns table (user_id uuid, name text, companion text, level int)
language sql security definer set search_path = '' stable as $$
  select p.user_id, p.name, p.companion, p.level
  from tend.invites i join tend.profiles p on p.user_id = i.owner
  where i.code = upper(trim(p_code)) and i.owner <> auth.uid();
$$;

-- Accepts an invite: the two become friends. Returns the friend's id, or null for an unknown code.
create or replace function public.tend_accept_invite(p_code text)
returns uuid language plpgsql security definer set search_path = '' as $$
declare o uuid;
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  select owner into o from tend.invites where code = upper(trim(p_code));
  if o is null or o = auth.uid() then return null; end if;
  insert into tend.friendships (a, b) values (least(o, auth.uid()), greatest(o, auth.uid())) on conflict do nothing;
  return o;
end $$;

-- The caller's friends with what they share: name, companion, level and camp.
create or replace function public.tend_friends()
returns table (user_id uuid, name text, companion text, level int, placed text, since timestamptz)
language sql security definer set search_path = '' stable as $$
  select p.user_id, p.name, p.companion, p.level, p.placed, f.created_at
  from tend.friendships f
  join tend.profiles p on p.user_id = case when f.a = auth.uid() then f.b else f.a end
  where auth.uid() in (f.a, f.b)
  order by f.created_at;
$$;

-- Removes a friend, quietly, and replaces the caller's invite code so an old link can't add them back.
create or replace function public.tend_remove_friend(p_friend uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  delete from tend.friendships where a = least(p_friend, auth.uid()) and b = greatest(p_friend, auth.uid());
  delete from tend.invites where owner = auth.uid();
end $$;

revoke all on function tend.random_code() from public, anon, authenticated;
revoke all on function public.tend_save_profile(text, text, int, text) from public, anon;
revoke all on function public.tend_my_invite(boolean) from public, anon;
revoke all on function public.tend_invite_owner(text) from public, anon;
revoke all on function public.tend_accept_invite(text) from public, anon;
revoke all on function public.tend_friends() from public, anon;
revoke all on function public.tend_remove_friend(uuid) from public, anon;
grant execute on function public.tend_save_profile(text, text, int, text) to authenticated;
grant execute on function public.tend_my_invite(boolean) to authenticated;
grant execute on function public.tend_invite_owner(text) to authenticated;
grant execute on function public.tend_accept_invite(text) to authenticated;
grant execute on function public.tend_friends() to authenticated;
grant execute on function public.tend_remove_friend(uuid) to authenticated;
