-- Things given between friends: a note of kind 'thing' carries which one (a thing id from the shop).
-- It lands in the friend's chest. Run after tend_fire_call.sql.
alter table tend.notes add column if not exists thing text;
alter table tend.notes drop constraint if exists notes_kind_check;
alter table tend.notes add constraint notes_kind_check check (kind = any (array['wave', 'visit', 'tp', 'cheer', 'gift', 'fire', 'thing']));

-- The inbox, now with the thing; things are kept a month, so one isn't missed after a week away.
drop function if exists public.tend_inbox();
create function public.tend_inbox()
returns table (id bigint, from_id uuid, name text, companion text, kind text, created_at timestamptz, cleared boolean, thing text)
language sql security definer set search_path = '' stable as $$
  select n.id, n.from_id, p.name, p.companion, n.kind, n.created_at, n.cleared_at is not null, n.thing
  from tend.notes n
  join tend.profiles p on p.user_id = n.from_id
  join tend.friendships f on f.a = least(n.from_id, n.to_id) and f.b = greatest(n.from_id, n.to_id)
  where n.to_id = auth.uid()
    and (n.created_at > now() - interval '7 days' or (n.kind = 'tp' and n.cleared_at is null) or (n.kind = 'thing' and n.created_at > now() - interval '30 days'))
  order by n.created_at desc
  limit 50;
$$;
revoke all on function public.tend_inbox() from public, anon;
grant execute on function public.tend_inbox() to authenticated;

-- Gives a friend a thing: at most 5 a day in all.
create or replace function public.tend_give_thing(p_to uuid, p_thing text)
returns boolean language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if p_thing is null or p_thing !~ '^[a-z_]{1,40}$' then return false; end if;
  if not exists (select 1 from tend.friendships where a = least(p_to, auth.uid()) and b = greatest(p_to, auth.uid())) then return false; end if;
  if (select count(*) from tend.notes where from_id = auth.uid() and kind = 'thing' and created_at > now() - interval '24 hours') >= 5 then return false; end if;
  insert into tend.notes (from_id, to_id, kind, thing) values (auth.uid(), p_to, 'thing', p_thing);
  return true;
end $$;
revoke all on function public.tend_give_thing(uuid, text) from public, anon;
grant execute on function public.tend_give_thing(uuid, text) to authenticated;
