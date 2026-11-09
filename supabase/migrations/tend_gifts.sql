-- Gifts: a friend sends 10 acorns; they arrive as a present in the camp. Run after tend_cheers.sql.
-- (One a day to each friend, like the other notes: tend_send_note already sees to that.)
alter table tend.notes drop constraint if exists notes_kind_check;
alter table tend.notes add constraint notes_kind_check check (kind = any (array['wave', 'visit', 'tp', 'cheer', 'gift']));
