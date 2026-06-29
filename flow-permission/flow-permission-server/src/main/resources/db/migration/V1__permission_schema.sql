-- NexusFlow Permission Service Schema

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    scope VARCHAR(20) NOT NULL DEFAULT 'MERCHANT',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_permissions_scope ON permissions(scope);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    scope VARCHAR(20) NOT NULL DEFAULT 'MERCHANT',
    is_system BOOLEAN NOT NULL DEFAULT false,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(role_id, permission_id)
);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    scope_type VARCHAR(20) NOT NULL,
    scope_id UUID,
    granted_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_scope ON user_roles(scope_type, scope_id);
CREATE UNIQUE INDEX uq_user_roles_scoped
    ON user_roles(user_id, role_id, scope_type, scope_id)
    WHERE scope_id IS NOT NULL;
CREATE UNIQUE INDEX uq_user_roles_global
    ON user_roles(user_id, role_id, scope_type)
    WHERE scope_id IS NULL;

INSERT INTO roles (code, name, scope, is_system, description) VALUES
('ADMIN', 'Administrator', 'MERCHANT', true, 'Full access'),
('MEMBER', 'Member', 'MERCHANT', true, 'Standard access'),
('VIEWER', 'Viewer', 'MERCHANT', true, 'Read-only access');

INSERT INTO permissions (code, name, scope, description) VALUES
('READ', 'Read', 'MERCHANT', 'View resources'),
('WRITE', 'Write', 'MERCHANT', 'Create/update resources'),
('DELETE', 'Delete', 'MERCHANT', 'Delete resources'),
('MANAGE', 'Manage', 'MERCHANT', 'Full management access');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.code = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'MEMBER' AND p.code IN ('READ', 'WRITE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.code = 'READ';
