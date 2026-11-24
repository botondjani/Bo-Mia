-- The invite reward, checked on the server: only once the invited one has done an activity
-- (tend_set_done has recorded one). Returns how many invites were rewarded; 0 when there's one
-- to reward but no activity yet (ask again later); -1 when there's nothing to reward (stop asking).
create or replace function public.tend_invite_reward()
returns int language plpgsql security definer set search_path = '' as $$
declare n int;
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  if not exists (
    select 1 from tend.friendships
    where auth.uid() in (a, b) and invited_by is not null and invited_by <> auth.uid() and not invite_rewarded
  ) then return -1; end if;
  if not exists (select 1 from tend.profiles where user_id = auth.uid() and (last_done_at is not null or week_count > 0)) then return 0; end if;
  with done as (
    update tend.friendships set invite_rewarded = true
    where auth.uid() in (a, b) and invited_by is not null and invited_by <> auth.uid() and not invite_rewarded
    returning invited_by
  )
  insert into tend.notes (from_id, to_id, kind) select auth.uid(), invited_by, 'invite_bonus' from done;
  get diagnostics n = row_count;
  return n;
end $$;

revoke all on function public.tend_invite_reward() from public, anon;
grant execute on function public.tend_invite_reward() to authenticated;
