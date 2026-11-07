-- tend accounts: a profile per signed-in user and one backup of the app's data.
-- Run once in the Supabase SQL editor of the "Bo & Mia" project.
-- Nothing here is readable through the API directly; signed-in users go through the functions below,
-- which only ever touch their own row.
create table if not exists tend.profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  name text check (char_length(name) <= 40),
  companion text check (companion in ('BO', 'MIA')),
  level int not null default 1 check (level between 1 and 1000),
  updated_at timestamptz not null default now()
);
alter table tend.profiles enable row level security;

create table if not exists tend.backups (
  user_id uuid primary key references auth.users(id) on delete cascade,
  data jsonb not null check (pg_column_size(data) < 2000000),
  app_version text check (char_length(app_version) <= 20),
  updated_at timestamptz not null default now()
);
alter table tend.backups enable row level security;

-- Save or update the caller's profile.
create or replace function public.tend_save_profile(p_name text, p_companion text, p_level int)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  insert into tend.profiles (user_id, name, companion, level, updated_at)
  values (auth.uid(), left(p_name, 40), p_companion, greatest(1, least(p_level, 1000)), now())
  on conflict (user_id) do update set name = excluded.name, companion = excluded.companion, level = excluded.level, updated_at = now();
end $$;

-- Save the caller's backup (replaces the previous one).
create or replace function public.tend_save_backup(p_data jsonb, p_version text)
returns timestamptz language plpgsql security definer set search_path = '' as $$
declare t timestamptz := now();
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  insert into tend.backups (user_id, data, app_version, updated_at) values (auth.uid(), p_data, left(p_version, 20), t)
  on conflict (user_id) do update set data = excluded.data, app_version = excluded.app_version, updated_at = t;
  return t;
end $$;

-- The caller's backup, or nothing.
create or replace function public.tend_load_backup()
returns table (data jsonb, updated_at timestamptz) language sql security definer set search_path = '' stable as $$
  select b.data, b.updated_at from tend.backups b where b.user_id = auth.uid();
$$;

-- Deletes the caller's account and everything tied to it (profile and backup cascade).
create or replace function public.tend_delete_account()
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  delete from auth.users where id = auth.uid();
end $$;

revoke all on function public.tend_save_profile(text, text, int) from public, anon;
revoke all on function public.tend_save_backup(jsonb, text) from public, anon;
revoke all on function public.tend_load_backup() from public, anon;
revoke all on function public.tend_delete_account() from public, anon;
grant execute on function public.tend_save_profile(text, text, int) to authenticated;
grant execute on function public.tend_save_backup(jsonb, text) to authenticated;
grant execute on function public.tend_load_backup() to authenticated;
grant execute on function public.tend_delete_account() to authenticated;
