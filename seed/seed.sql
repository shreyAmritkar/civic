-- ============================================================================
-- CivicTrack demo / test data
--
-- NOT a Flyway migration: keep this file outside src/main/resources/db/migration
-- so it never runs automatically. Load it by hand after the app has started once
-- (Flyway must have created the tables first). See TESTING.md.
--
-- Every account uses the password:  Civic@123
-- (BCrypt hash below. To use another password, generate a BCrypt hash for it
--  and replace the hash string everywhere in this file.)
--
-- Safe to run more than once: rows are inserted with fixed IDs and
-- ON CONFLICT DO NOTHING. To refresh the timestamps, run reset.sql first.
--
-- Fixed IDs are deliberate: the admin screen asks you to type raw department and
-- official UUIDs when assigning a complaint, so these are easy to copy from
-- TESTING.md.
--   users:        a0.. admin | b0.. officials | c0.. citizens
--   departments:  d0..        assignments: e0..        complaints: f0..
-- ============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- Users
-- ---------------------------------------------------------------------------
INSERT INTO users (id, name, email, password_hash, role) VALUES
 ('a0000000-0000-0000-0000-000000000001', 'Admin User',   'admin@civictrack.test',       '$2b$10$A5.WN0iKo68fWMozmJd7vejcaCh.jth1BU6C9eLOO44yx1QdN.HWe', 'ADMIN'),
 ('b0000000-0000-0000-0000-000000000001', 'Ravi Kumar',   'ravi.roads@civictrack.test',  '$2b$10$A5.WN0iKo68fWMozmJd7vejcaCh.jth1BU6C9eLOO44yx1QdN.HWe', 'OFFICIAL'),
 ('b0000000-0000-0000-0000-000000000002', 'Priya Nair',   'priya.water@civictrack.test', '$2b$10$A5.WN0iKo68fWMozmJd7vejcaCh.jth1BU6C9eLOO44yx1QdN.HWe', 'OFFICIAL'),
 ('b0000000-0000-0000-0000-000000000003', 'Dev Malhotra', 'dev.water@civictrack.test',   '$2b$10$A5.WN0iKo68fWMozmJd7vejcaCh.jth1BU6C9eLOO44yx1QdN.HWe', 'OFFICIAL'),
 ('b0000000-0000-0000-0000-000000000004', 'Sunil Patil',  'sunil.waste@civictrack.test', '$2b$10$A5.WN0iKo68fWMozmJd7vejcaCh.jth1BU6C9eLOO44yx1QdN.HWe', 'OFFICIAL'),
 ('b0000000-0000-0000-0000-000000000005', 'Anita Rao',    'anita.power@civictrack.test', '$2b$10$A5.WN0iKo68fWMozmJd7vejcaCh.jth1BU6C9eLOO44yx1QdN.HWe', 'OFFICIAL'),
 ('b0000000-0000-0000-0000-000000000006', 'Kiran Shah',   'kiran.power@civictrack.test', '$2b$10$A5.WN0iKo68fWMozmJd7vejcaCh.jth1BU6C9eLOO44yx1QdN.HWe', 'OFFICIAL'),
 ('c0000000-0000-0000-0000-000000000001', 'Asha Verma',   'asha@civictrack.test',        '$2b$10$A5.WN0iKo68fWMozmJd7vejcaCh.jth1BU6C9eLOO44yx1QdN.HWe', 'CITIZEN'),
 ('c0000000-0000-0000-0000-000000000002', 'Rohan Mehta',  'rohan@civictrack.test',       '$2b$10$A5.WN0iKo68fWMozmJd7vejcaCh.jth1BU6C9eLOO44yx1QdN.HWe', 'CITIZEN'),
 ('c0000000-0000-0000-0000-000000000003', 'Meera Iyer',   'meera@civictrack.test',       '$2b$10$A5.WN0iKo68fWMozmJd7vejcaCh.jth1BU6C9eLOO44yx1QdN.HWe', 'CITIZEN')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Departments. Categories must not overlap, otherwise auto-assignment sees more
-- than one match and sends the complaint to the admin queue.
-- ---------------------------------------------------------------------------
INSERT INTO departments (id, name, categories) VALUES
 ('d0000000-0000-0000-0000-000000000001', 'Roads & Drainage',        'ROADS,DRAINAGE'),
 ('d0000000-0000-0000-0000-000000000002', 'Water Supply',            'WATER'),
 ('d0000000-0000-0000-0000-000000000003', 'Sanitation & Waste',      'WASTE'),
 ('d0000000-0000-0000-0000-000000000004', 'Electrical (Streetlights)', 'STREETLIGHTS')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Official assignments. These drive automatic routing:
--   Roads, Sanitation  -> one official, any ward         => always auto-assigned
--   Water              -> Priya = North, Dev = South     => routed by ward text
--   Electrical         -> two officials, any ward        => ambiguous => admin queue
--   OTHER              -> no department                  => admin queue
-- ---------------------------------------------------------------------------
INSERT INTO official_assignments (id, official_id, department_id, ward) VALUES
 ('e0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', NULL),
 ('e0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'North'),
 ('e0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000002', 'South'),
 ('e0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000003', NULL),
 ('e0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000004', NULL),
 ('e0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000004', NULL)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Complaints. Times are relative to the moment this script runs.
-- SLA = created + 24h URGENT / 48h HIGH / 72h MEDIUM / 120h LOW (same as the app).
-- ---------------------------------------------------------------------------
INSERT INTO complaints
 (id, citizen_id, department_id, assigned_official_id, title, description, category, priority,
  location, ward, status, assignment_status, sla_deadline, overdue, resolution_notes,
  created_at, updated_at)
VALUES
-- F01 Asha: freshly auto-assigned to Ravi
 ('f0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001',
  'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
  'Large pothole on MG Road', 'A deep pothole near City Mall is damaging two-wheelers. It has been getting wider after the rain.',
  'ROADS', 'HIGH', 'MG Road, near City Mall', 'North', 'ASSIGNED', 'AUTO_ASSIGNED',
  now() + interval '45 hours', false, NULL, now() - interval '3 hours', now() - interval '3 hours'),

-- F02 Asha: in progress with Priya (North water)
 ('f0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001',
  'd0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002',
  'Water leakage near Sector 4 market', 'A main pipe is leaking onto the road and the water pressure in nearby homes has dropped.',
  'WATER', 'MEDIUM', 'Sector 4 market entrance', 'North', 'IN_PROGRESS', 'AUTO_ASSIGNED',
  now() + interval '46 hours', false, NULL, now() - interval '26 hours', now() - interval '20 hours'),

-- F03 Asha: RESOLVED, waiting for Asha to confirm or dispute  <-- citizen verification test
 ('f0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001',
  'd0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000004',
  'Garbage not collected for a week', 'The bins on Lane 7 have overflowed and the smell is spreading. No collection vehicle has come this week.',
  'WASTE', 'MEDIUM', 'Lane 7, Green Park', 'East', 'RESOLVED', 'AUTO_ASSIGNED',
  now() + interval '42 hours', false, 'Backlog cleared and a daily pickup slot added for Lane 7.',
  now() - interval '30 hours', now() - interval '2 hours'),

-- F04 Rohan: UNASSIGNED (two streetlight officials => ambiguous)  <-- admin queue
 ('f0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000002',
  NULL, NULL,
  'Streetlight out on 5th Cross', 'The streetlight outside house 42 has not worked for four nights. The stretch is very dark.',
  'STREETLIGHTS', 'MEDIUM', '5th Cross, Lake View', 'West', 'SUBMITTED', 'PENDING_REVIEW',
  now() + interval '70 hours', false, NULL, now() - interval '2 hours', now() - interval '2 hours'),

-- F05 Rohan: SLA already marked overdue
 ('f0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002',
  'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
  'Open drain overflowing onto the road', 'The drain beside the bus stop is blocked and sewage is flowing across the footpath.',
  'DRAINAGE', 'HIGH', 'Bus stop, Station Road', 'South', 'ASSIGNED', 'AUTO_ASSIGNED',
  now() - interval '12 hours', true, NULL, now() - interval '60 hours', now() - interval '60 hours'),

-- F06 Meera: category OTHER => no department  <-- admin queue
 ('f0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000003',
  NULL, NULL,
  'Fallen tree blocking the lane', 'A large branch has fallen across Lane 3 after last night''s storm. Cars cannot pass.',
  'OTHER', 'URGENT', 'Lane 3, Lake View', 'North', 'SUBMITTED', 'PENDING_REVIEW',
  now() + interval '23 hours', false, NULL, now() - interval '1 hour', now() - interval '1 hour'),

-- F07 Meera: urgent water issue in South ward => Dev
 ('f0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000003',
  'd0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000003',
  'No water supply since morning', 'The whole block has had no tap water since 6 AM and the tanker has not arrived.',
  'WATER', 'URGENT', 'Block C, Hill View Colony', 'South', 'ASSIGNED', 'AUTO_ASSIGNED',
  now() + interval '19 hours', false, NULL, now() - interval '5 hours', now() - interval '5 hours'),

-- F08 Meera: full lifecycle finished (VERIFIED)  <-- nice history timeline
 ('f0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000003',
  'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
  'Pothole near school gate', 'A pothole at the school gate is a hazard for children getting dropped off.',
  'ROADS', 'LOW', 'Government School, Park Street', 'North', 'VERIFIED', 'AUTO_ASSIGNED',
  now() - interval '24 hours', false, 'Pothole filled and levelled.',
  now() - interval '6 days', now() - interval '4 days'),

-- F09 Rohan: citizen disputed the fix => REOPENED
 ('f0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000002',
  'd0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000004',
  'Garbage dumped at park entrance', 'Construction waste and household garbage are piled at the north gate of the park.',
  'WASTE', 'MEDIUM', 'Central Park, north gate', 'East', 'REOPENED', 'AUTO_ASSIGNED',
  now() + interval '22 hours', false, NULL, now() - interval '50 hours', now() - interval '3 hours'),

-- F10 Asha: deadline passed but overdue flag NOT yet set  <-- SLA job test
 ('f0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000001',
  'd0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
  'Broken footpath tiles near bus stand', 'Several footpath tiles are broken and uneven; an elderly neighbour tripped here.',
  'ROADS', 'MEDIUM', 'Bus stand, Market Road', 'North', 'IN_PROGRESS', 'AUTO_ASSIGNED',
  now() - interval '8 hours', false, NULL, now() - interval '80 hours', now() - interval '70 hours')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Status history (old status -> new status, note, actor)
-- ---------------------------------------------------------------------------
INSERT INTO complaint_status_history (id, complaint_id, from_status, to_status, note, actor_id, created_at) VALUES
 -- F01
 ('10000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', NULL, 'ASSIGNED', 'Complaint submitted', 'c0000000-0000-0000-0000-000000000001', now() - interval '3 hours'),
 -- F02
 ('10000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000002', NULL, 'ASSIGNED', 'Complaint submitted', 'c0000000-0000-0000-0000-000000000001', now() - interval '26 hours'),
 ('10000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000002', 'ASSIGNED', 'IN_PROGRESS', 'Crew dispatched to inspect the pipe', 'b0000000-0000-0000-0000-000000000002', now() - interval '20 hours'),
 -- F03
 ('10000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000003', NULL, 'ASSIGNED', 'Complaint submitted', 'c0000000-0000-0000-0000-000000000001', now() - interval '30 hours'),
 ('10000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000003', 'ASSIGNED', 'IN_PROGRESS', 'Collection vehicle scheduled', 'b0000000-0000-0000-0000-000000000004', now() - interval '12 hours'),
 ('10000000-0000-0000-0000-000000000006', 'f0000000-0000-0000-0000-000000000003', 'IN_PROGRESS', 'RESOLVED', 'Backlog cleared and a daily pickup slot added for Lane 7.', 'b0000000-0000-0000-0000-000000000004', now() - interval '2 hours'),
 -- F04
 ('10000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000004', NULL, 'SUBMITTED', 'Complaint submitted', 'c0000000-0000-0000-0000-000000000002', now() - interval '2 hours'),
 -- F05
 ('10000000-0000-0000-0000-000000000008', 'f0000000-0000-0000-0000-000000000005', NULL, 'ASSIGNED', 'Complaint submitted', 'c0000000-0000-0000-0000-000000000002', now() - interval '60 hours'),
 -- F06
 ('10000000-0000-0000-0000-000000000009', 'f0000000-0000-0000-0000-000000000006', NULL, 'SUBMITTED', 'Complaint submitted', 'c0000000-0000-0000-0000-000000000003', now() - interval '1 hour'),
 -- F07
 ('10000000-0000-0000-0000-000000000010', 'f0000000-0000-0000-0000-000000000007', NULL, 'ASSIGNED', 'Complaint submitted', 'c0000000-0000-0000-0000-000000000003', now() - interval '5 hours'),
 -- F08 (complete lifecycle)
 ('10000000-0000-0000-0000-000000000011', 'f0000000-0000-0000-0000-000000000008', NULL, 'ASSIGNED', 'Complaint submitted', 'c0000000-0000-0000-0000-000000000003', now() - interval '6 days'),
 ('10000000-0000-0000-0000-000000000012', 'f0000000-0000-0000-0000-000000000008', 'ASSIGNED', 'IN_PROGRESS', 'Material ordered, work starts tomorrow', 'b0000000-0000-0000-0000-000000000001', now() - interval '5 days'),
 ('10000000-0000-0000-0000-000000000013', 'f0000000-0000-0000-0000-000000000008', 'IN_PROGRESS', 'RESOLVED', 'Pothole filled and levelled.', 'b0000000-0000-0000-0000-000000000001', now() - interval '4 days 6 hours'),
 ('10000000-0000-0000-0000-000000000014', 'f0000000-0000-0000-0000-000000000008', 'RESOLVED', 'VERIFIED', 'Confirmed. The road is smooth now, thank you.', 'c0000000-0000-0000-0000-000000000003', now() - interval '4 days'),
 -- F09 (resolved, then disputed)
 ('10000000-0000-0000-0000-000000000015', 'f0000000-0000-0000-0000-000000000009', NULL, 'ASSIGNED', 'Complaint submitted', 'c0000000-0000-0000-0000-000000000002', now() - interval '50 hours'),
 ('10000000-0000-0000-0000-000000000016', 'f0000000-0000-0000-0000-000000000009', 'ASSIGNED', 'IN_PROGRESS', 'Team assigned', 'b0000000-0000-0000-0000-000000000004', now() - interval '40 hours'),
 ('10000000-0000-0000-0000-000000000017', 'f0000000-0000-0000-0000-000000000009', 'IN_PROGRESS', 'RESOLVED', 'Area cleaned.', 'b0000000-0000-0000-0000-000000000004', now() - interval '20 hours'),
 ('10000000-0000-0000-0000-000000000018', 'f0000000-0000-0000-0000-000000000009', 'RESOLVED', 'REOPENED', 'Only the surface litter was removed. The construction waste is still there.', 'c0000000-0000-0000-0000-000000000002', now() - interval '3 hours'),
 -- F10
 ('10000000-0000-0000-0000-000000000019', 'f0000000-0000-0000-0000-000000000010', NULL, 'ASSIGNED', 'Complaint submitted', 'c0000000-0000-0000-0000-000000000001', now() - interval '80 hours'),
 ('10000000-0000-0000-0000-000000000020', 'f0000000-0000-0000-0000-000000000010', 'ASSIGNED', 'IN_PROGRESS', 'Inspection done, repair quoted', 'b0000000-0000-0000-0000-000000000001', now() - interval '70 hours')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Notifications
-- ---------------------------------------------------------------------------
INSERT INTO notifications (id, user_id, message, "read", created_at) VALUES
 ('20000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'New complaint assigned: Large pothole on MG Road', false, now() - interval '3 hours'),
 ('20000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'SLA overdue: Open drain overflowing onto the road', false, now() - interval '12 hours'),
 ('20000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'New complaint assigned: No water supply since morning', false, now() - interval '5 hours'),
 ('20000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', 'Complaint ''Water leakage near Sector 4 market'' is now IN_PROGRESS', true,  now() - interval '20 hours'),
 ('20000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000001', 'Complaint ''Garbage not collected for a week'' is now RESOLVED', false, now() - interval '2 hours'),
 ('20000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000002', 'Complaint ''Garbage dumped at park entrance'' is now REOPENED', false, now() - interval '3 hours'),
 ('20000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000003', 'Complaint ''Pothole near school gate'' is now VERIFIED', true, now() - interval '4 days')
ON CONFLICT DO NOTHING;

COMMIT;