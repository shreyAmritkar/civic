-- ============================================================================
-- Removes the demo data created by seed.sql, plus anything the demo citizens
-- created while you were testing (their complaints, history and notifications).
-- Any other account whose email ends in @civictrack.test is removed too.
--
-- Run seed.sql afterwards to get a fresh copy with up-to-date timestamps.
--
-- Safe by design: it runs in one transaction and only deletes demo rows. If real
-- data still points at a demo user or department, PostgreSQL raises a foreign-key
-- error and NOTHING is deleted.
-- Departments you created yourself in the admin screen are left alone.
-- ============================================================================

BEGIN;

DELETE FROM notifications
 WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@civictrack.test');

-- complaint_status_history rows go with their complaints (ON DELETE CASCADE)
DELETE FROM complaints
 WHERE citizen_id IN (SELECT id FROM users WHERE email LIKE '%@civictrack.test');

DELETE FROM official_assignments
 WHERE official_id IN (SELECT id FROM users WHERE email LIKE '%@civictrack.test');

DELETE FROM departments
 WHERE id IN ('d0000000-0000-0000-0000-000000000001',
              'd0000000-0000-0000-0000-000000000002',
              'd0000000-0000-0000-0000-000000000003',
              'd0000000-0000-0000-0000-000000000004');

DELETE FROM users WHERE email LIKE '%@civictrack.test';

COMMIT;