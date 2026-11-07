-- tend notes: little things friends send each other (a wave, a gift of acorns, a prank in their camp).
-- Run once in the Supabase SQL editor of the "Bo & Mia" project, after tend_friends.sql.

create table if not exists tend.notes (
  id bigint generated always as identity primary key,
  from_id uuid not null references auth.users(id) on delete cascade,
  to_id uuid not null references auth.users(id) on delete cascade,
  kind text not null check (kind in ('wave', 'gift', 'duck', 'gnome', 'flamingo', 'tp')),
  created_at timestamptz not null default now()
);
create index if not exists notes_to on tend.notes (to_id, created_at desc);
create index if not exists notes_from on tend.notes (from_id, created_at desc);
alter table tend.notes enable row level security;

-- Sends a note to a friend. Returns false when they aren't friends, or this kind went to them in the last 24 hours.
create or replace function public.tend_send_note(p_to uuid, p_kind text)
returns boolean language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if not exists (select 1 from tend.friendships where a = least(p_to, auth.uid()) and b = greatest(p_to, auth.uid())) then return false; end if;
  if exists (select 1 from tend.notes where from_id = auth.uid() and to_id = p_to and kind = p_kind and created_at > now() - interval '24 hours') then return false; end if;
  insert into tend.notes (from_id, to_id, kind) values (auth.uid(), p_to, p_kind);
  delete from tend.notes where created_at < now() - interval '30 days';
  return true;
end $$;

-- Notes to the caller from the last 7 days, from people who are still friends; newest first.
create or replace function public.tend_inbox()
returns table (id bigint, from_id uuid, name text, companion text, kind text, created_at timestamptz)
language sql security definer set search_path = '' stable as $$
  select n.id, n.from_id, p.name, p.companion, n.kind, n.created_at
  from tend.notes n
  join tend.profiles p on p.user_id = n.from_id
  join tend.friendships f on f.a = least(n.from_id, n.to_id) and f.b = greatest(n.from_id, n.to_id)
  where n.to_id = auth.uid() and n.created_at > now() - interval '7 days'
  order by n.created_at desc
  limit 50;
$$;

-- What the caller sent in the last 24 hours (to show their pranks in friends' camps, and what's used up today).
create or replace function public.tend_sent()
returns table (to_id uuid, kind text, created_at timestamptz)
language sql security definer set search_path = '' stable as $$
  select n.to_id, n.kind, n.created_at from tend.notes n
  where n.from_id = auth.uid() and n.created_at > now() - interval '24 hours';
$$;

revoke all on function public.tend_send_note(uuid, text) from public, anon;
revoke all on function public.tend_inbox() from public, anon;
revoke all on function public.tend_sent() from public, anon;
grant execute on function public.tend_send_note(uuid, text) to authenticated;
grant execute on function public.tend_inbox() to authenticated;
grant execute on function public.tend_sent() to authenticated;
