-- How well the pause works, by phone maker: from the daily "pause_health" counts (with consent).
-- missed_pct: opens of a picked app the pause never covered; gap_days: days the monitor was stopped for a while.
create or replace view tend.pause_health with (security_invoker = true) as
select props ->> 'maker' as maker,
       count(distinct install_id) as installs,
       sum((props ->> 'shown')::int) as shown,
       sum((props ->> 'retry')::int) as retried,
       sum((props ->> 'missed')::int) as missed,
       round(100.0 * sum((props ->> 'missed')::int) / nullif(sum((props ->> 'shown')::int) + sum((props ->> 'missed')::int), 0), 1) as missed_pct,
       count(*) filter (where (props ->> 'gap')::int > 0) as gap_days,
       sum((props ->> 'stopped')::int) as stopped
from tend.events where name = 'pause_health'
group by 1 order by installs desc;
