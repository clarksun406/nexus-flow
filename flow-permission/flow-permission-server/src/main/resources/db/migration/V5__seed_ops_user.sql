-- Seed global ops service account with OPS_ADMIN role in SYSTEM scope.
-- The UUID matches UserIdMapper.OPS_USER_ID in flow-api.
INSERT INTO user_roles (id, user_id, role_id, scope_type, scope_id, granted_by, created_at)
SELECT gen_random_uuid(), '00000000-0000-0000-0000-000000000001', id, 'SYSTEM', NULL, NULL, now()
FROM roles WHERE code = 'OPS_ADMIN'
ON CONFLICT DO NOTHING;
