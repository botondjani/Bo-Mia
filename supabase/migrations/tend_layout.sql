-- Where friends put things in their camp, so the river shows each camp as its owner arranged it.
alter table tend.profiles add column if not exists layout text not null default '';

-- Same as before, plus p_layout (older apps leave it out and keep what was saved).
drop function if exists public.tend_save_profile(text, text, integer, text, text, integer, boolean);
create or replace function public.tend_save_profile(
  p_name text, p_companion text, p_level integer, p_placed text default '', p_week text default '',
  p_week_count integer default 0, p_bridge boolean default false, p_layout text default null
) returns void language plpgsql security definer set search_path to '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  insert into tend.profiles (user_id, name, companion, level, placed, week, week_count, bridge, layout, updated_at)
    values (auth.uid(), left(p_name, 40), p_companion, greatest(1, least(p_level, 1000)), left(coalesce(p_placed, ''), 400),
            left(coalesce(p_week, ''), 10), greatest(0, least(coalesce(p_week_count, 0), 500)), coalesce(p_bridge, false),
            left(coalesce(p_layout, ''), 400), now())
    on conflict (user_id) do update set name = excluded.name, companion = excluded.companion, level = excluded.level,
      placed = excluded.placed, week = excluded.week, week_count = excluded.week_count, bridge = excluded.bridge,
      layout = case when p_layout is null then tend.profiles.layout else excluded.layout end, updated_at = now();
end $$;
grant execute on function public.tend_save_profile(text, text, integer, text, text, integer, boolean, text) to authenticated, service_role;
revoke execute on function public.tend_save_profile(text, text, integer, text, text, integer, boolean, text) from anon, public;

drop function if exists public.tend_friends();
create or replace function public.tend_friends()
returns table(user_id uuid, name text, companion text, level integer, placed text, since timestamptz, week text, week_count integer, bridge boolean, layout text)
language sql stable security definer set search_path to '' as $$
  select p.user_id, p.name, p.companion, p.level, p.placed, f.created_at, p.week, p.week_count, p.bridge, p.layout
  from tend.friendships f
  join tend.profiles p on p.user_id = case when f.a = auth.uid() then f.b else f.a end
  where auth.uid() in (f.a, f.b)
  order by f.created_at;
$$;
grant execute on function public.tend_friends() to authenticated, service_role;
revoke execute on function public.tend_friends() from anon, public;
