-- tend together: each profile carries this week's number of activities (and whether its bridge is
-- built), so friends can add them up for the shared weekly goal.
-- Run once in the Supabase SQL editor of the "Bo & Mia" project, after tend_notes_2.sql.

alter table tend.profiles add column if not exists week text not null default '' check (char_length(week) <= 10);
alter table tend.profiles add column if not exists week_count int not null default 0 check (week_count between 0 and 500);
alter table tend.profiles add column if not exists bridge boolean not null default false;

drop function if exists public.tend_save_profile(text, text, int, text);
create or replace function public.tend_save_profile(
  p_name text, p_companion text, p_level int, p_placed text default '',
  p_week text default '', p_week_count int default 0, p_bridge boolean default false
)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  insert into tend.profiles (user_id, name, companion, level, placed, week, week_count, bridge, updated_at)
  values (auth.uid(), left(p_name, 40), p_companion, greatest(1, least(p_level, 1000)), left(coalesce(p_placed, ''), 400),
          left(coalesce(p_week, ''), 10), greatest(0, least(coalesce(p_week_count, 0), 500)), coalesce(p_bridge, false), now())
  on conflict (user_id) do update set name = excluded.name, companion = excluded.companion, level = excluded.level,
    placed = excluded.placed, week = excluded.week, week_count = excluded.week_count, bridge = excluded.bridge, updated_at = now();
end $$;

drop function if exists public.tend_friends();
create function public.tend_friends()
returns table (user_id uuid, name text, companion text, level int, placed text, since timestamptz, week text, week_count int, bridge boolean)
language sql security definer set search_path = '' stable as $$
  select p.user_id, p.name, p.companion, p.level, p.placed, f.created_at, p.week, p.week_count, p.bridge
  from tend.friendships f
  join tend.profiles p on p.user_id = case when f.a = auth.uid() then f.b else f.a end
  where auth.uid() in (f.a, f.b)
  order by f.created_at;
$$;

revoke all on function public.tend_save_profile(text, text, int, text, text, int, boolean) from public, anon;
revoke all on function public.tend_friends() from public, anon;
grant execute on function public.tend_save_profile(text, text, int, text, text, int, boolean) to authenticated;
grant execute on function public.tend_friends() to authenticated;
