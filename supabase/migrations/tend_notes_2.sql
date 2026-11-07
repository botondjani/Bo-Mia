-- tend notes, second version: a wave, a visit (a rubber duck in the friend's river) and toilet paper
-- on their oak, which stays until they unroll it. No more gifts or other pranks.
-- Run once in the Supabase SQL editor of the "Bo & Mia" project, after tend_notes.sql.

delete from tend.notes where kind not in ('wave', 'visit', 'tp');
alter table tend.notes drop constraint if exists notes_kind_check;
alter table tend.notes add constraint notes_kind_check check (kind in ('wave', 'visit', 'tp'));
alter table tend.notes add column if not exists cleared_at timestamptz;

-- Notes to the caller: the last 7 days, and toilet paper still up however old; newest first.
drop function if exists public.tend_inbox();
create function public.tend_inbox()
returns table (id bigint, from_id uuid, name text, companion text, kind text, created_at timestamptz, cleared boolean)
language sql security definer set search_path = '' stable as $$
  select n.id, n.from_id, p.name, p.companion, n.kind, n.created_at, n.cleared_at is not null
  from tend.notes n
  join tend.profiles p on p.user_id = n.from_id
  join tend.friendships f on f.a = least(n.from_id, n.to_id) and f.b = greatest(n.from_id, n.to_id)
  where n.to_id = auth.uid()
    and (n.created_at > now() - interval '7 days' or (n.kind = 'tp' and n.cleared_at is null))
  order by n.created_at desc
  limit 50;
$$;

-- What the caller sent in the last day, plus their toilet paper still up in friends' camps.
drop function if exists public.tend_sent();
create function public.tend_sent()
returns table (to_id uuid, kind text, created_at timestamptz, cleared boolean)
language sql security definer set search_path = '' stable as $$
  select n.to_id, n.kind, n.created_at, n.cleared_at is not null from tend.notes n
  where n.from_id = auth.uid()
    and (n.created_at > now() - interval '24 hours' or (n.kind = 'tp' and n.cleared_at is null));
$$;

-- The caller unrolls the toilet paper in their camp.
create or replace function public.tend_clear_paper()
returns void language sql security definer set search_path = '' as $$
  update tend.notes set cleared_at = now() where to_id = auth.uid() and kind = 'tp' and cleared_at is null;
$$;

-- Toilet paper goes up once per friend until it's taken down; waves and visits once a day.
create or replace function public.tend_send_note(p_to uuid, p_kind text)
returns boolean language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if not exists (select 1 from tend.friendships where a = least(p_to, auth.uid()) and b = greatest(p_to, auth.uid())) then return false; end if;
  if exists (
    select 1 from tend.notes where from_id = auth.uid() and to_id = p_to and kind = p_kind
      and (created_at > now() - interval '24 hours' or (kind = 'tp' and cleared_at is null))
  ) then return false; end if;
  insert into tend.notes (from_id, to_id, kind) values (auth.uid(), p_to, p_kind);
  delete from tend.notes where created_at < now() - interval '30 days' and not (kind = 'tp' and cleared_at is null);
  return true;
end $$;

revoke all on function public.tend_inbox() from public, anon;
revoke all on function public.tend_sent() from public, anon;
revoke all on function public.tend_clear_paper() from public, anon;
grant execute on function public.tend_inbox() to authenticated;
grant execute on function public.tend_sent() to authenticated;
grant execute on function public.tend_clear_paper() to authenticated;
