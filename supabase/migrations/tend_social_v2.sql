-- tend social, second version: a cheer instead of a wave, pranks that can be turned off, what friends
-- see (last activity, level, at the fire), blocking, a buddy who's told when you slip, a reward for
-- both sides of an invite after the first activity, and an anonymous count of people at the fire.

alter table tend.profiles add column if not exists pranks boolean not null default true;
alter table tend.profiles add column if not exists share_done boolean not null default true;
alter table tend.profiles add column if not exists share_level boolean not null default true;
alter table tend.profiles add column if not exists share_fire boolean not null default true;

-- Who invited whom (the code's owner), and whether the reward for it has been given.
alter table tend.friendships add column if not exists invited_by uuid;
alter table tend.friendships add column if not exists invite_rewarded boolean not null default false;

-- Blocked: no friendship, no new invite between the two.
create table if not exists tend.blocks (
  blocker uuid not null references auth.users(id) on delete cascade,
  blocked uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (blocker, blocked)
);
alter table tend.blocks enable row level security;

-- A buddy: [buddy] is told when [owner] slips. Asked by the owner, accepted by the buddy.
create table if not exists tend.buddies (
  owner uuid not null references auth.users(id) on delete cascade,
  buddy uuid not null references auth.users(id) on delete cascade,
  accepted boolean not null default false,
  created_at timestamptz not null default now(),
  primary key (owner, buddy),
  check (owner <> buddy)
);
alter table tend.buddies enable row level security;

alter table tend.notes drop constraint if exists notes_kind_check;
alter table tend.notes add constraint notes_kind_check check (kind in ('wave', 'visit', 'tp', 'cheer', 'gift', 'fire', 'thing', 'buddy_ask', 'buddy_alert', 'invite_bonus'));

create or replace function tend.blocked_between(x uuid, y uuid) returns boolean language sql stable set search_path = '' as $$
  select exists (select 1 from tend.blocks where (blocker = x and blocked = y) or (blocker = y and blocked = x));
$$;

create or replace function public.tend_accept_invite(p_code text)
returns uuid language plpgsql security definer set search_path = '' as $$
declare o uuid;
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  select owner into o from tend.invites where code = upper(trim(p_code));
  if o is null or o = auth.uid() or tend.blocked_between(o, auth.uid()) then return null; end if;
  insert into tend.friendships (a, b, invited_by) values (least(o, auth.uid()), greatest(o, auth.uid()), o) on conflict do nothing;
  return o;
end $$;

-- Who an invite code belongs to; nobody when one of you blocked the other.
create or replace function public.tend_invite_owner(p_code text)
returns table (user_id uuid, name text, companion text, level int)
language sql security definer set search_path = '' stable as $$
  select p.user_id, p.name, p.companion, p.level
  from tend.invites i join tend.profiles p on p.user_id = i.owner
  where i.code = upper(trim(p_code)) and i.owner <> auth.uid() and not tend.blocked_between(i.owner, auth.uid());
$$;

-- Friends, with only what each one shares; whether they take pranks; and the buddy asks both ways.
drop function if exists public.tend_friends();
create function public.tend_friends()
returns table (user_id uuid, name text, companion text, level int, placed text, since timestamptz, week text, week_count int, bridge boolean, layout text,
  focus_started timestamptz, focus_until timestamptz, focus_task text, last_done text, last_done_at timestamptz,
  pranks boolean, buddy_out text, buddy_in text)
language sql stable security definer set search_path = '' as $$
  select p.user_id, p.name, p.companion, case when p.share_level then p.level else 0 end, p.placed, f.created_at, p.week, p.week_count, p.bridge, p.layout,
    case when p.share_fire then p.focus_started end, case when p.share_fire and p.focus_until > now() then p.focus_until end, case when p.share_fire then p.focus_task end,
    case when p.share_done and p.last_done_at > now() - interval '2 days' then p.last_done end,
    case when p.share_done and p.last_done_at > now() - interval '2 days' then p.last_done_at end,
    p.pranks,
    (select case when b.accepted then 'yes' else 'asked' end from tend.buddies b where b.owner = auth.uid() and b.buddy = p.user_id),
    (select case when b.accepted then 'yes' else 'asked' end from tend.buddies b where b.owner = p.user_id and b.buddy = auth.uid())
  from tend.friendships f
  join tend.profiles p on p.user_id = case when f.a = auth.uid() then f.b else f.a end
  where auth.uid() in (f.a, f.b)
  order by f.created_at;
$$;

-- The caller's sharing choices and whether pranks may land in their camp.
create or replace function public.tend_save_settings(p_pranks boolean, p_share_done boolean, p_share_level boolean, p_share_fire boolean)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  update tend.profiles set pranks = p_pranks, share_done = p_share_done, share_level = p_share_level, share_fire = p_share_fire where user_id = auth.uid();
  -- Pranks turned off: any paper up comes down.
  if not p_pranks then update tend.notes set cleared_at = now() where to_id = auth.uid() and kind = 'tp' and cleared_at is null; end if;
end $$;

-- Notes sent by hand: no pranks to someone who turned them off; buddy and invite notes only come from their own functions.
create or replace function public.tend_send_note(p_to uuid, p_kind text)
returns boolean language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if p_kind not in ('wave', 'visit', 'tp', 'cheer', 'fire') then return false; end if;
  if not exists (select 1 from tend.friendships where a = least(p_to, auth.uid()) and b = greatest(p_to, auth.uid())) then return false; end if;
  if p_kind = 'tp' and not coalesce((select pranks from tend.profiles where user_id = p_to), true) then return false; end if;
  if exists (
    select 1 from tend.notes where from_id = auth.uid() and to_id = p_to and kind = p_kind
      and (created_at > now() - (case when p_kind = 'fire' then interval '1 hour' else interval '24 hours' end) or (kind = 'tp' and cleared_at is null))
  ) then return false; end if;
  insert into tend.notes (from_id, to_id, kind) values (auth.uid(), p_to, p_kind);
  delete from tend.notes where created_at < now() - interval '30 days' and not (kind = 'tp' and cleared_at is null);
  return true;
end $$;

-- Removes a friend for good: no friendship, no buddy, no new invite either way.
create or replace function public.tend_block(p_friend uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  delete from tend.friendships where a = least(p_friend, auth.uid()) and b = greatest(p_friend, auth.uid());
  delete from tend.buddies where (owner = auth.uid() and buddy = p_friend) or (owner = p_friend and buddy = auth.uid());
  insert into tend.blocks (blocker, blocked) values (auth.uid(), p_friend) on conflict do nothing;
  delete from tend.invites where owner = auth.uid();
end $$;

-- Removing a friend ends a buddy pact between you too.
create or replace function public.tend_remove_friend(p_friend uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  delete from tend.friendships where a = least(p_friend, auth.uid()) and b = greatest(p_friend, auth.uid());
  delete from tend.buddies where (owner = auth.uid() and buddy = p_friend) or (owner = p_friend and buddy = auth.uid());
  delete from tend.invites where owner = auth.uid();
end $$;

-- Asks a friend to be your buddy; at most p_max (1, or 3 with Plus; never more than 3). True when asked.
create or replace function public.tend_buddy_ask(p_to uuid, p_max int)
returns boolean language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if not exists (select 1 from tend.friendships where a = least(p_to, auth.uid()) and b = greatest(p_to, auth.uid())) then return false; end if;
  if (select count(*) from tend.buddies where owner = auth.uid()) >= least(greatest(p_max, 1), 3) then return false; end if;
  insert into tend.buddies (owner, buddy) values (auth.uid(), p_to) on conflict do nothing;
  if not found then return false; end if;
  insert into tend.notes (from_id, to_id, kind) values (auth.uid(), p_to, 'buddy_ask');
  return true;
end $$;

-- Says yes or no to a friend who asked you to be their buddy.
create or replace function public.tend_buddy_answer(p_from uuid, p_yes boolean)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if p_yes then update tend.buddies set accepted = true where owner = p_from and buddy = auth.uid();
  else delete from tend.buddies where owner = p_from and buddy = auth.uid(); end if;
end $$;

-- Ends the pact with [p_other], whichever side you're on.
create or replace function public.tend_buddy_leave(p_other uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  delete from tend.buddies where (owner = auth.uid() and buddy = p_other) or (owner = p_other and buddy = auth.uid());
end $$;

-- Tells the caller's buddies that they slipped ('strict_off' or 'over_3'); once a day per reason. How many were told.
create or replace function public.tend_buddy_alert(p_reason text)
returns int language plpgsql security definer set search_path = '' as $$
declare n int;
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if p_reason not in ('strict_off', 'over_3') then return 0; end if;
  if exists (select 1 from tend.notes where from_id = auth.uid() and kind = 'buddy_alert' and thing = p_reason and created_at > now() - interval '24 hours') then return 0; end if;
  insert into tend.notes (from_id, to_id, kind, thing)
    select auth.uid(), b.buddy, 'buddy_alert', p_reason from tend.buddies b where b.owner = auth.uid() and b.accepted;
  get diagnostics n = row_count;
  return n;
end $$;

-- The invited one did a first activity: the inviter gets a note (and their acorns with it). How many invites this rewarded.
create or replace function public.tend_invite_reward()
returns int language plpgsql security definer set search_path = '' as $$
declare n int;
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  with done as (
    update tend.friendships set invite_rewarded = true
    where auth.uid() in (a, b) and invited_by is not null and invited_by <> auth.uid() and not invite_rewarded
    returning invited_by
  )
  insert into tend.notes (from_id, to_id, kind) select auth.uid(), invited_by, 'invite_bonus' from done;
  get diagnostics n = row_count;
  return n;
end $$;

-- How many people are phone-free at a fire right now; null until tend has 20 people a day (no one can be picked out).
create or replace function public.tend_fire_count()
returns int language sql stable security definer set search_path = '' as $$
  select case when coalesce((select max(users) from tend.daily_active where day >= current_date - 1), 0) >= 20
    then (select count(*)::int from tend.profiles where share_fire and focus_until > now()) end;
$$;

revoke all on function tend.blocked_between(uuid, uuid) from public, anon, authenticated;
revoke all on function public.tend_save_settings(boolean, boolean, boolean, boolean) from public, anon;
revoke all on function public.tend_block(uuid) from public, anon;
revoke all on function public.tend_buddy_ask(uuid, int) from public, anon;
revoke all on function public.tend_buddy_answer(uuid, boolean) from public, anon;
revoke all on function public.tend_buddy_leave(uuid) from public, anon;
revoke all on function public.tend_buddy_alert(text) from public, anon;
revoke all on function public.tend_invite_reward() from public, anon;
revoke all on function public.tend_fire_count() from public, anon;
revoke all on function public.tend_friends() from public, anon;
grant execute on function public.tend_save_settings(boolean, boolean, boolean, boolean) to authenticated;
grant execute on function public.tend_block(uuid) to authenticated;
grant execute on function public.tend_buddy_ask(uuid, int) to authenticated;
grant execute on function public.tend_buddy_answer(uuid, boolean) to authenticated;
grant execute on function public.tend_buddy_leave(uuid) to authenticated;
grant execute on function public.tend_buddy_alert(text) to authenticated;
grant execute on function public.tend_invite_reward() to authenticated;
grant execute on function public.tend_fire_count() to authenticated;
grant execute on function public.tend_friends() to authenticated;
