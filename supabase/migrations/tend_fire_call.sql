-- "Come to the fire": a friend calls you to put the phones down together. Once an hour per friend.
alter table tend.notes drop constraint notes_kind_check;
alter table tend.notes add constraint notes_kind_check check (kind = any (array['wave', 'visit', 'tp', 'cheer', 'gift', 'fire']));

create or replace function public.tend_send_note(p_to uuid, p_kind text)
returns boolean language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if not exists (select 1 from tend.friendships where a = least(p_to, auth.uid()) and b = greatest(p_to, auth.uid())) then return false; end if;
  if exists (
    select 1 from tend.notes where from_id = auth.uid() and to_id = p_to and kind = p_kind
      and (created_at > now() - (case when p_kind = 'fire' then interval '1 hour' else interval '24 hours' end) or (kind = 'tp' and cleared_at is null))
  ) then return false; end if;
  insert into tend.notes (from_id, to_id, kind) values (auth.uid(), p_to, p_kind);
  delete from tend.notes where created_at < now() - interval '30 days' and not (kind = 'tp' and cleared_at is null);
  return true;
end $$;
