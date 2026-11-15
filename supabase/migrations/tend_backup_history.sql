-- Earlier backups are kept: each time a backup is replaced, the one before goes here (the last 10
-- per account), so a camp overwritten by mistake can still be brought back. Run after tend_accounts.sql.
create table if not exists tend.backup_history (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  data jsonb not null,
  app_version text,
  saved_at timestamptz not null
);
create index if not exists backup_history_user on tend.backup_history (user_id, saved_at desc);
alter table tend.backup_history enable row level security; -- no policies: only through the functions below

create or replace function tend.keep_old_backup() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  if old.data is distinct from new.data then
    insert into tend.backup_history (user_id, data, app_version, saved_at) values (old.user_id, old.data, old.app_version, old.updated_at);
    delete from tend.backup_history h where h.user_id = old.user_id
      and h.id not in (select id from tend.backup_history where user_id = old.user_id order by saved_at desc limit 10);
  end if;
  return new;
end $$;

drop trigger if exists keep_old_backup on tend.backups;
create trigger keep_old_backup before update on tend.backups for each row execute function tend.keep_old_backup();
