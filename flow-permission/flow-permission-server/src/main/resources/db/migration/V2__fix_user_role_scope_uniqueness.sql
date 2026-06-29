ALTER TABLE user_roles
    DROP CONSTRAINT IF EXISTS user_roles_user_id_role_id_scope_type_scope_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_roles_scoped
    ON user_roles(user_id, role_id, scope_type, scope_id)
    WHERE scope_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_roles_global
    ON user_roles(user_id, role_id, scope_type)
    WHERE scope_id IS NULL;
