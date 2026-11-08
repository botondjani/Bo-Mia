-- The shared campfire: who is phone-free right now, and until when, for their friends to see.
alter table tend.profiles add column if not exists focus_started timestamptz;
alter table tend.profiles add column if not exists focus_until timestamptz;
alter table tend.profiles add column if not exists focus_task text;

-- Sets (or with a null p_until, clears) your own phone-free time; at most 4 hours ahead.
create or replace function public.tend_set_focus(p_until timestamptz, p_task text default null)
returns void language plpgsql security definer set search_path to '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  update tend.profiles set
    focus_started = case when p_until is null then null else now() end,
    focus_until = case when p_until is null then null else least(p_until, now() + interval '4 hours') end,
    focus_task = case when p_until is null then null else left(coalesce(p_task, ''), 40) end
  where user_id = auth.uid();
end $$;
grant execute on function public.tend_set_focus(timestamptz, text) to authenticated, service_role;
revoke execute on function public.tend_set_focus(timestamptz, text) from anon, public;

drop function if exists public.tend_friends();
create or replace function public.tend_friends()
returns table(user_id uuid, name text, companion text, level integer, placed text, since timestamptz, week text, week_count integer, bridge boolean, layout text,
              focus_started timestamptz, focus_until timestamptz, focus_task text)
language sql stable security definer set search_path to '' as $$
  select p.user_id, p.name, p.companion, p.level, p.placed, f.created_at, p.week, p.week_count, p.bridge, p.layout,
         p.focus_started, case when p.focus_until > now() then p.focus_until end, p.focus_task
  from tend.friendships f
  join tend.profiles p on p.user_id = case when f.a = auth.uid() then f.b else f.a end
  where auth.uid() in (f.a, f.b)
  order by f.created_at;
$$;
grant execute on function public.tend_friends() to authenticated, service_role;
revoke execute on function public.tend_friends() from anon, public;
