-- Cheers: what a friend did last (in their words: "went for a walk"), and a one-tap cheer back.
-- Run after tend_focus.sql.
alter table tend.profiles add column if not exists last_done text;
alter table tend.profiles add column if not exists last_done_at timestamptz;

alter table tend.notes drop constraint if exists notes_kind_check;
alter table tend.notes add constraint notes_kind_check check (kind = any (array['wave', 'visit', 'tp', 'cheer']));

-- What you just did, for friends to cheer ("went for a walk"); own activities arrive without their name.
create or replace function public.tend_set_done(p_done text)
returns void language plpgsql security definer set search_path to '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  update tend.profiles set last_done = left(coalesce(p_done, ''), 60), last_done_at = now() where user_id = auth.uid();
end $$;
grant execute on function public.tend_set_done(text) to authenticated, service_role;
revoke execute on function public.tend_set_done(text) from anon, public;

drop function if exists public.tend_friends();
create or replace function public.tend_friends()
returns table(user_id uuid, name text, companion text, level integer, placed text, since timestamptz, week text, week_count integer, bridge boolean, layout text,
              focus_started timestamptz, focus_until timestamptz, focus_task text, last_done text, last_done_at timestamptz)
language sql stable security definer set search_path to '' as $$
  select p.user_id, p.name, p.companion, p.level, p.placed, f.created_at, p.week, p.week_count, p.bridge, p.layout,
         p.focus_started, case when p.focus_until > now() then p.focus_until end, p.focus_task,
         case when p.last_done_at > now() - interval '2 days' then p.last_done end,
         case when p.last_done_at > now() - interval '2 days' then p.last_done_at end
  from tend.friendships f
  join tend.profiles p on p.user_id = case when f.a = auth.uid() then f.b else f.a end
  where auth.uid() in (f.a, f.b)
  order by f.created_at;
$$;
grant execute on function public.tend_friends() to authenticated, service_role;
revoke execute on function public.tend_friends() from anon, public;
