-- Size limits on what clients can store: a backup stays a backup, a stats batch stays small.

create or replace function public.tend_save_backup(p_data jsonb, p_version text)
returns timestamptz language plpgsql security definer set search_path = '' as $$
declare t timestamptz := now();
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  -- Years of history fit in a fraction of this.
  if pg_column_size(p_data) > 4 * 1024 * 1024 then raise exception 'backup too large'; end if;
  insert into tend.backups (user_id, data, app_version, updated_at) values (auth.uid(), p_data, left(p_version, 20), t)
    on conflict (user_id) do update set data = excluded.data, app_version = excluded.app_version, updated_at = t;
  return t;
end $$;

create or replace function public.tend_log(events jsonb)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if jsonb_typeof(events) <> 'array' or jsonb_array_length(events) > 200 or pg_column_size(events) > 200 * 1024 then
    raise exception 'bad batch';
  end if;
  insert into tend.events (install_id, name, day, props, app_version, flavor, sdk)
  select (e->>'install_id')::uuid, left(e->>'name', 40), (e->>'day')::int,
         -- A few small props only: anything bigger isn't one of tend's events.
         case when pg_column_size(coalesce(e->'props', '{}'::jsonb)) <= 1024 then coalesce(e->'props', '{}'::jsonb) else '{}'::jsonb end,
         left(e->>'v', 20), left(e->>'flavor', 20), (e->>'sdk')::int
  from jsonb_array_elements(events) as e;
end $$;
