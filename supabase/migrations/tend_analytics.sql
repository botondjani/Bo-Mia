-- Where people leave onboarding, and whether they get going after it. Read-only views over tend.events.

-- Each onboarding step: how many installs reached it, and the share of those who started.
create or replace view tend.onboarding_funnel with (security_invoker = true) as
with reached as (
  select install_id, (props ->> 'n')::int as n, props ->> 'step' as step
  from tend.events where name = 'onboarding_step'
), starts as (select count(distinct install_id) as total from reached where n = 1)
select n, step, count(distinct install_id) as installs,
       round(100.0 * count(distinct install_id) / nullif((select total from starts), 0)) as pct_of_start
from reached group by n, step order by n;

-- After onboarding: per day-since-install cohort, how many did an activity, bought something, came back.
create or replace view tend.activation with (security_invoker = true) as
with done as (select distinct install_id from tend.events where name = 'onboarding_done')
select count(*) as onboarded,
       count(*) filter (where exists (select 1 from tend.events e where e.install_id = d.install_id and e.name = 'activity_done' and e.day <= 1)) as activity_by_day1,
       count(*) filter (where exists (select 1 from tend.events e where e.install_id = d.install_id and e.name = 'bought' and e.day <= 1)) as bought_by_day1,
       count(*) filter (where exists (select 1 from tend.events e where e.install_id = d.install_id and e.name = 'app_open' and e.day between 1 and 3)) as back_day1_to_3,
       count(*) filter (where exists (select 1 from tend.events e where e.install_id = d.install_id and e.name = 'paused')) as took_a_break,
       count(*) filter (where exists (select 1 from tend.events e where e.install_id = d.install_id and e.name = 'borrowed')) as borrowed
from done d;
