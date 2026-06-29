INSERT INTO permissions (code, name, scope, description) VALUES
('merchant:read', 'Read merchant profile', 'MERCHANT', 'View merchant profile and status'),
('merchant:create', 'Create merchant', 'SYSTEM', 'Create merchant profiles'),
('merchant:update', 'Update merchant profile', 'MERCHANT', 'Update merchant profile fields'),
('merchant:activate', 'Activate merchant', 'SYSTEM', 'Activate approved merchants'),
('merchant:suspend', 'Suspend merchant', 'SYSTEM', 'Suspend merchants'),

('merchant_user:read', 'Read merchant users', 'MERCHANT', 'View merchant users and memberships'),
('merchant_user:invite', 'Invite merchant users', 'MERCHANT', 'Invite users to a merchant'),
('merchant_user:update', 'Update merchant users', 'MERCHANT', 'Update merchant user membership or profile state'),
('merchant_user:disable', 'Disable merchant users', 'MERCHANT', 'Disable merchant users'),

('api_key:read', 'Read API keys', 'MERCHANT', 'View merchant API key metadata'),
('api_key:create', 'Create API keys', 'MERCHANT', 'Create merchant API keys'),
('api_key:rotate', 'Rotate API keys', 'MERCHANT', 'Rotate merchant API keys'),
('api_key:revoke', 'Revoke API keys', 'MERCHANT', 'Revoke merchant API keys'),

('webhook:read', 'Read webhooks', 'MERCHANT', 'View merchant webhook configuration'),
('webhook:update', 'Update webhooks', 'MERCHANT', 'Update merchant webhook configuration'),
('webhook:test', 'Test webhooks', 'MERCHANT', 'Send merchant webhook test events'),
('webhook:replay', 'Replay merchant webhooks', 'MERCHANT', 'Replay merchant-owned webhook deliveries'),

('payment_order:read', 'Read payment orders', 'MERCHANT', 'View merchant payment orders'),
('payment_order:create', 'Create payment orders', 'MERCHANT', 'Create merchant payment orders'),
('payment_order:export', 'Export payment orders', 'MERCHANT', 'Export merchant payment orders'),

('crypto_payment:read', 'Read crypto payments', 'MERCHANT', 'View execution-layer crypto payments'),
('crypto_payment:create', 'Create crypto payments', 'MERCHANT', 'Create execution-layer crypto payments'),
('crypto_payment:confirm', 'Confirm crypto payments', 'SYSTEM', 'Manually confirm crypto payments'),
('crypto_payment:fail', 'Fail crypto payments', 'SYSTEM', 'Manually fail crypto payments'),

('refund:read', 'Read refunds', 'MERCHANT', 'View refund records'),
('refund:create', 'Create refunds', 'MERCHANT', 'Create refund requests'),
('refund:approve', 'Approve refunds', 'SYSTEM', 'Approve sensitive refund operations'),
('refund:retry', 'Retry refunds', 'SYSTEM', 'Retry failed refund operations'),

('fiat_ramp:quote', 'Quote fiat ramp orders', 'MERCHANT', 'Request fiat ramp quotes'),
('fiat_ramp:create', 'Create fiat ramp orders', 'MERCHANT', 'Create fiat ramp orders'),
('fiat_ramp:read', 'Read fiat ramp orders', 'MERCHANT', 'View fiat ramp orders'),
('fiat_ramp:operate', 'Operate fiat ramp orders', 'SYSTEM', 'Operate or reconcile fiat ramp orders'),

('orphan:read', 'Read orphan transactions', 'SYSTEM', 'View orphan blockchain transactions'),
('orphan:resolve', 'Resolve orphan transactions', 'SYSTEM', 'Mark orphan transactions as resolved'),
('orphan:ignore', 'Ignore orphan transactions', 'SYSTEM', 'Ignore orphan transactions'),
('orphan:compensate', 'Compensate orphan transactions', 'SYSTEM', 'Create compensation payments for orphan transactions'),

('webhook_dlq:read', 'Read webhook dead letters', 'SYSTEM', 'View webhook dead-letter records'),
('webhook_dlq:replay', 'Replay webhook dead letters', 'SYSTEM', 'Replay failed webhook deliveries'),
('webhook_dlq:ignore', 'Ignore webhook dead letters', 'SYSTEM', 'Ignore webhook dead-letter records'),

('ops_dashboard:read', 'Read ops dashboard', 'SYSTEM', 'View operational dashboard'),

('provider:read', 'Read providers', 'SYSTEM', 'View provider and channel configuration'),
('provider:update', 'Update providers', 'SYSTEM', 'Update provider and channel configuration'),
('provider:disable', 'Disable providers', 'SYSTEM', 'Disable providers or channels'),
('provider:enable', 'Enable providers', 'SYSTEM', 'Enable providers or channels'),

('permission:read', 'Read permissions', 'SYSTEM', 'View permission catalog'),
('permission:manage', 'Manage permissions', 'SYSTEM', 'Create, update, or delete permissions'),
('role:read', 'Read roles', 'SYSTEM', 'View roles and role permissions'),
('role:manage', 'Manage roles', 'SYSTEM', 'Create, update, delete, or assign role permissions'),
('user_role:read', 'Read user roles', 'SYSTEM', 'View user role assignments'),
('user_role:grant', 'Grant user roles', 'SYSTEM', 'Grant roles to users'),
('user_role:revoke', 'Revoke user roles', 'SYSTEM', 'Revoke roles from users'),

('audit:read', 'Read audit logs', 'SYSTEM', 'View audit logs')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    scope = EXCLUDED.scope,
    description = EXCLUDED.description;
