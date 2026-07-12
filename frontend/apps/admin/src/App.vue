<template>
  <main class="admin-app">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand__mark" aria-hidden="true"></span>
        <div>
          <strong>NexusFlow</strong>
          <span>Admin Console</span>
        </div>
      </div>

      <section class="side-section">
        <label for="apiBase">API Base</label>
        <input id="apiBase" v-model="apiBaseInput" placeholder="/api" @blur="saveSettings" />
      </section>

      <section v-if="user" class="side-section">
        <span class="label">Signed In</span>
        <strong class="user-line">{{ user.email || user.displayName || user.userId }}</strong>
        <button class="btn btn--ghost" type="button" @click="signOut">Sign Out</button>
      </section>

      <nav v-if="user" class="nav" aria-label="Admin sections">
        <button :class="{ active: activeView === 'roles' }" type="button" @click="activeView = 'roles'">
          Roles
        </button>
        <button :class="{ active: activeView === 'users' }" type="button" @click="activeView = 'users'">
          User Roles
        </button>
      </nav>
    </aside>

    <section v-if="!user" class="login-view">
      <form class="login-panel" @submit.prevent="submitLogin">
        <div>
          <p class="eyebrow">Platform Access</p>
          <h1>Sign in</h1>
        </div>
        <label>
          Email
          <input v-model="loginForm.email" type="email" autocomplete="email" required />
        </label>
        <label>
          Password
          <input v-model="loginForm.password" type="password" autocomplete="current-password" required />
        </label>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <button class="btn btn--primary" type="submit" :disabled="busy">
          {{ busy ? "Signing in" : "Sign In" }}
        </button>
      </form>
    </section>

    <section v-else class="workspace">
      <header class="workspace__header">
        <div>
          <p class="eyebrow">RBAC Administration</p>
          <h1>{{ activeView === "roles" ? "Roles and permissions" : "User role assignments" }}</h1>
        </div>
        <div class="header-actions">
          <button class="btn" type="button" @click="refreshAll" :disabled="busy">Refresh</button>
        </div>
      </header>

      <section class="metrics" aria-label="admin metrics">
        <div class="metric">
          <span>Roles</span>
          <strong>{{ roles.length }}</strong>
        </div>
        <div class="metric">
          <span>Permissions</span>
          <strong>{{ permissions.length }}</strong>
        </div>
        <div class="metric">
          <span>Selected Role</span>
          <strong>{{ selectedRole?.code || "None" }}</strong>
        </div>
        <div class="metric">
          <span>User Grants</span>
          <strong>{{ userRoles.length }}</strong>
        </div>
      </section>

      <div v-if="activeView === 'roles'" class="content-grid">
        <section class="panel">
          <div class="panel__head">
            <h2>Roles</h2>
            <select v-model="roleScopeFilter" @change="loadRoles">
              <option value="">All scopes</option>
              <option value="SYSTEM">SYSTEM</option>
              <option value="MERCHANT">MERCHANT</option>
            </select>
          </div>

          <form class="form-grid" @submit.prevent="submitCreateRole">
            <label>
              Role Code
              <input v-model="roleForm.code" placeholder="OPS_ADMIN" required />
            </label>
            <label>
              Display Name
              <input v-model="roleForm.name" placeholder="Ops Admin" />
            </label>
            <label>
              Scope
              <select v-model="roleForm.scope">
                <option value="SYSTEM">SYSTEM</option>
                <option value="MERCHANT">MERCHANT</option>
              </select>
            </label>
            <button class="btn btn--primary" type="submit" :disabled="busy">Create Role</button>
          </form>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Scope</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="role in roles" :key="role.id" :class="{ selected: selectedRoleId === role.id }">
                  <td class="mono">{{ role.code }}</td>
                  <td>{{ role.name || "-" }}</td>
                  <td><span class="badge" :class="scopeClass(role.scope)">{{ role.scope || "-" }}</span></td>
                  <td class="actions">
                    <button class="btn" type="button" @click="selectRole(role.id)">Open</button>
                    <button class="btn btn--danger" type="button" @click="removeRole(role)">Delete</button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="roles.length === 0" class="empty">No roles loaded</div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>Role permissions</h2>
            <select v-model="selectedRoleId" @change="loadSelectedRole">
              <option value="">Choose role</option>
              <option v-for="role in roles" :key="role.id" :value="role.id">
                {{ role.code }}
              </option>
            </select>
          </div>

          <div v-if="selectedRole" class="permission-grid">
            <div class="assigned-list">
              <div v-for="permission in selectedPermissions" :key="permission.code" class="permission-row">
                <div>
                  <strong class="mono">{{ permission.code }}</strong>
                  <span>{{ permission.name || "-" }}</span>
                </div>
                <button class="btn btn--danger" type="button" @click="removePermission(permission.code)">
                  Remove
                </button>
              </div>
              <div v-if="selectedPermissions.length === 0" class="empty empty--compact">No permissions assigned</div>
            </div>

            <form class="inline-form" @submit.prevent="addPermission">
              <select v-model="permissionToAdd">
                <option value="">Choose permission</option>
                <option v-for="permission in availablePermissions" :key="permission.code" :value="permission.code">
                  {{ permission.code }}
                </option>
              </select>
              <button class="btn btn--primary" type="submit" :disabled="!permissionToAdd || busy">Add</button>
            </form>
          </div>
          <div v-else class="empty">Select a role</div>
        </section>

        <section class="panel panel--wide">
          <div class="panel__head">
            <h2>Permission catalog</h2>
            <select v-model="permissionScopeFilter" @change="loadPermissions">
              <option value="">All scopes</option>
              <option value="SYSTEM">SYSTEM</option>
              <option value="MERCHANT">MERCHANT</option>
            </select>
          </div>
          <div class="permission-catalog">
            <div v-for="permission in permissions" :key="permission.code" class="catalog-item">
              <strong class="mono">{{ permission.code }}</strong>
              <span>{{ permission.name || "-" }}</span>
              <small :class="scopeClass(permission.scope)">{{ permission.scope || "-" }}</small>
            </div>
          </div>
          <div v-if="permissions.length === 0" class="empty">No permissions loaded</div>
        </section>
      </div>

      <div v-else class="content-grid content-grid--users">
        <section class="panel">
          <div class="panel__head">
            <h2>Lookup user roles</h2>
            <button class="btn btn--primary" type="button" @click="lookupUserRoles" :disabled="busy">Lookup</button>
          </div>

          <form class="form-grid" @submit.prevent="lookupUserRoles">
            <label class="span-2">
              User ID
              <input v-model="userLookup.userId" placeholder="00000000-0000-0000-0000-000000000001" required />
            </label>
            <label>
              Scope Type
              <select v-model="userLookup.scopeType">
                <option value="">All</option>
                <option value="SYSTEM">SYSTEM</option>
                <option value="MERCHANT">MERCHANT</option>
              </select>
            </label>
          </form>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Role</th>
                  <th>Scope Type</th>
                  <th>Scope ID</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="role in userRoles" :key="`${role.roleId}-${role.scopeType}-${role.scopeId}`">
                  <td class="mono">{{ role.roleCode || role.roleId }}</td>
                  <td>{{ role.scopeType || "-" }}</td>
                  <td class="mono">{{ role.scopeId || "-" }}</td>
                  <td>
                    <button class="btn btn--danger" type="button" @click="revokeUserRole(role)">Revoke</button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="userRoles.length === 0" class="empty">No roles loaded</div>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>Grant role</h2>
            <button class="btn btn--primary" type="button" @click="submitGrantRole" :disabled="busy">Grant</button>
          </div>

          <form class="form-grid" @submit.prevent="submitGrantRole">
            <label class="span-2">
              User ID
              <input v-model="grantForm.userId" placeholder="User UUID" required />
            </label>
            <label>
              Role
              <select v-model="grantForm.roleId" required>
                <option value="">Choose role</option>
                <option v-for="role in roles" :key="role.id" :value="role.id">{{ role.code }}</option>
              </select>
            </label>
            <label>
              Scope Type
              <select v-model="grantForm.scopeType">
                <option value="MERCHANT">MERCHANT</option>
                <option value="SYSTEM">SYSTEM</option>
              </select>
            </label>
            <label class="span-2">
              Scope ID
              <input v-model="grantForm.scopeId" placeholder="Merchant UUID for MERCHANT scope" />
            </label>
          </form>
        </section>
      </div>

      <p v-if="errorMessage" class="toast toast--error">{{ errorMessage }}</p>
      <p v-if="noticeMessage" class="toast">{{ noticeMessage }}</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { configureApiClient } from "@nexusflow/api-client";
import {
  createRole,
  deleteRole,
  getRole,
  grantRole,
  listPermissions,
  listRoles,
  listUserRoles,
  loadCurrentUser,
  login,
  logout,
  revokeRole,
  setRolePermissions
} from "./adminApi";
import type { Permission, Role, UserInfo, UserRole } from "./types";

type AdminView = "roles" | "users";
type ScopeType = "SYSTEM" | "MERCHANT";

const settingsKey = "nexusflow.admin.config";

const apiBaseInput = ref("");
const user = ref<UserInfo | null>(null);
const activeView = ref<AdminView>("roles");
const busy = ref(false);
const errorMessage = ref("");
const noticeMessage = ref("");
const roles = ref<Role[]>([]);
const permissions = ref<Permission[]>([]);
const selectedRole = ref<Role | null>(null);
const selectedRoleId = ref("");
const roleScopeFilter = ref("");
const permissionScopeFilter = ref("");
const permissionToAdd = ref("");
const userRoles = ref<UserRole[]>([]);

const loginForm = ref({
  email: "",
  password: ""
});

const roleForm = ref<{ code: string; name: string; scope: ScopeType }>({
  code: "",
  name: "",
  scope: "SYSTEM"
});

const userLookup = ref({
  userId: "",
  scopeType: ""
});

const grantForm = ref<{ userId: string; roleId: string; scopeType: ScopeType; scopeId: string }>({
  userId: "",
  roleId: "",
  scopeType: "MERCHANT",
  scopeId: ""
});

const selectedPermissions = computed(() => selectedRole.value?.permissions ?? []);
const selectedPermissionCodes = computed(() => new Set(selectedPermissions.value.map((permission) => permission.code)));
const availablePermissions = computed(() => permissions.value.filter(
  (permission) => !selectedPermissionCodes.value.has(permission.code)
));

onMounted(() => {
  loadSettings();
  void refreshSession();
});

async function submitLogin() {
  await run(async () => {
    saveSettings();
    user.value = await login(loginForm.value.email, loginForm.value.password);
    loginForm.value.password = "";
    await refreshAll();
    notice("Signed in");
  });
}

async function refreshSession() {
  await run(async () => {
    user.value = await loadCurrentUser();
    await refreshAll();
  }, { quietUnauthorized: true });
}

async function signOut() {
  await run(async () => {
    await logout();
    user.value = null;
    roles.value = [];
    permissions.value = [];
    selectedRole.value = null;
    userRoles.value = [];
  });
}

async function refreshAll() {
  await Promise.all([loadRoles(), loadPermissions()]);
  if (selectedRoleId.value) {
    await loadSelectedRole();
  }
}

async function loadRoles() {
  roles.value = await listRoles(roleScopeFilter.value || undefined);
}

async function loadPermissions() {
  permissions.value = await listPermissions(permissionScopeFilter.value || undefined);
}

async function submitCreateRole() {
  await run(async () => {
    await createRole({
      code: roleForm.value.code.trim(),
      name: roleForm.value.name.trim(),
      scope: roleForm.value.scope
    });
    roleForm.value.code = "";
    roleForm.value.name = "";
    await loadRoles();
    notice("Role created");
  });
}

async function selectRole(roleId: string) {
  selectedRoleId.value = roleId;
  await loadSelectedRole();
}

async function loadSelectedRole() {
  if (!selectedRoleId.value) {
    selectedRole.value = null;
    return;
  }
  selectedRole.value = await getRole(selectedRoleId.value);
  permissionToAdd.value = "";
}

async function addPermission() {
  if (!selectedRole.value || !permissionToAdd.value) {
    return;
  }
  await run(async () => {
    const nextCodes = [...selectedPermissionCodes.value, permissionToAdd.value];
    await setRolePermissions(selectedRole.value!.id, nextCodes);
    await loadSelectedRole();
    notice("Permission added");
  });
}

async function removePermission(permissionCode: string) {
  if (!selectedRole.value) {
    return;
  }
  await run(async () => {
    const nextCodes = [...selectedPermissionCodes.value].filter((code) => code !== permissionCode);
    await setRolePermissions(selectedRole.value!.id, nextCodes);
    await loadSelectedRole();
    notice("Permission removed");
  });
}

async function removeRole(role: Role) {
  if (!window.confirm(`Delete role ${role.code}?`)) {
    return;
  }
  await run(async () => {
    await deleteRole(role.id);
    if (selectedRoleId.value === role.id) {
      selectedRoleId.value = "";
      selectedRole.value = null;
    }
    await loadRoles();
    notice("Role deleted");
  });
}

async function lookupUserRoles() {
  await run(async () => {
    userRoles.value = await listUserRoles(userLookup.value.userId.trim(), userLookup.value.scopeType || undefined);
    grantForm.value.userId = userLookup.value.userId.trim();
  });
}

async function submitGrantRole() {
  await run(async () => {
    await grantRole({
      userId: grantForm.value.userId.trim(),
      roleId: grantForm.value.roleId,
      scopeType: grantForm.value.scopeType,
      scopeId: emptyToUndefined(grantForm.value.scopeId)
    });
    userLookup.value.userId = grantForm.value.userId.trim();
    userLookup.value.scopeType = grantForm.value.scopeType;
    await lookupUserRoles();
    notice("Role granted");
  });
}

async function revokeUserRole(role: UserRole) {
  if (!userLookup.value.userId || !role.roleId) {
    return;
  }
  if (!window.confirm(`Revoke ${role.roleCode || role.roleId}?`)) {
    return;
  }
  await run(async () => {
    await revokeRole(userLookup.value.userId, role.roleId, role.scopeType || "MERCHANT", role.scopeId);
    await lookupUserRoles();
    notice("Role revoked");
  });
}

function loadSettings() {
  const settings = readJson<{ apiBase?: string }>(settingsKey, {});
  apiBaseInput.value = settings.apiBase ?? "";
  configureApiClient({ apiBase: apiBaseInput.value });
}

function saveSettings() {
  const apiBase = apiBaseInput.value.trim().replace(/\/$/, "");
  apiBaseInput.value = apiBase;
  configureApiClient({ apiBase });
  localStorage.setItem(settingsKey, JSON.stringify({ apiBase }));
}

async function run(action: () => Promise<void>, options: { quietUnauthorized?: boolean } = {}) {
  busy.value = true;
  errorMessage.value = "";
  try {
    await action();
  } catch (error) {
    const message = error instanceof Error ? error.message : "Request failed";
    if (!options.quietUnauthorized || !message.toLowerCase().includes("authenticated")) {
      errorMessage.value = message;
    }
  } finally {
    busy.value = false;
  }
}

function notice(message: string) {
  noticeMessage.value = message;
  window.setTimeout(() => {
    if (noticeMessage.value === message) {
      noticeMessage.value = "";
    }
  }, 2200);
}

function scopeClass(scope?: string) {
  return scope === "SYSTEM" ? "badge--system" : "badge--merchant";
}

function emptyToUndefined(value: string) {
  const trimmed = value.trim();
  return trimmed || undefined;
}

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) as T : fallback;
  } catch {
    return fallback;
  }
}
</script>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  margin: 0;
  background: #f4f6f8;
  color: #17202a;
  font-family: "Aptos", "Segoe UI", sans-serif;
}

button,
input,
select {
  font: inherit;
}

.admin-app {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
}

.sidebar {
  min-height: 100vh;
  padding: 22px;
  border-right: 1px solid #dde5ed;
  background: #ffffff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.brand__mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0f766e, #334155);
}

.brand strong,
.brand span,
.metric span,
.label {
  display: block;
}

.brand strong {
  font-size: 15px;
}

.brand span,
.label,
label {
  color: #617080;
  font-size: 12px;
  font-weight: 700;
}

.side-section {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
}

.nav {
  display: grid;
  gap: 8px;
}

.nav button {
  min-height: 38px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #475569;
  padding: 8px 10px;
  text-align: left;
  font-weight: 800;
  cursor: pointer;
}

.nav button.active {
  border-color: #cbd5df;
  background: #f7fafc;
  color: #17202a;
}

input,
select {
  width: 100%;
  min-height: 40px;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  color: #17202a;
  padding: 9px 10px;
  outline: none;
}

input:focus,
select:focus {
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.12);
}

.user-line {
  overflow-wrap: anywhere;
  font-size: 13px;
}

.login-view {
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-panel {
  width: min(420px, 100%);
  display: grid;
  gap: 16px;
  padding: 28px;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(23, 32, 42, 0.07);
}

.workspace {
  min-width: 0;
  padding: 24px;
}

.workspace__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #617080;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  letter-spacing: 0;
}

h1 {
  font-size: 28px;
}

h2 {
  font-size: 15px;
}

.header-actions,
.panel__head,
.actions,
.inline-form {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn {
  min-height: 38px;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  color: #17202a;
  padding: 8px 12px;
  font-weight: 800;
  cursor: pointer;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.btn--primary {
  border-color: #0f766e;
  background: #0f766e;
  color: #ffffff;
}

.btn--danger {
  border-color: #efcaca;
  color: #b91c1c;
}

.btn--ghost {
  background: #f7fafc;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.metric,
.panel {
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(23, 32, 42, 0.05);
}

.metric {
  min-height: 82px;
  padding: 14px;
}

.metric strong {
  display: block;
  margin-top: 8px;
  overflow-wrap: anywhere;
  font-size: 20px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(430px, 0.95fr) minmax(420px, 1fr);
  gap: 16px;
  align-items: start;
}

.content-grid--users {
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
}

.panel {
  overflow: hidden;
}

.panel--wide {
  grid-column: span 2;
}

.panel__head {
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #edf1f5;
}

.panel__head select {
  max-width: 220px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
}

label {
  display: grid;
  gap: 6px;
}

.span-2 {
  grid-column: span 2;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 660px;
  border-collapse: collapse;
}

th,
td {
  padding: 11px 14px;
  border-bottom: 1px solid #edf1f5;
  text-align: left;
  font-size: 13px;
}

tr.selected td {
  background: #f0fdfa;
}

th {
  color: #617080;
  font-size: 12px;
}

.mono {
  font-family: "Cascadia Mono", Consolas, monospace;
}

.badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 12px;
  font-weight: 800;
}

.badge--system {
  background: #eef2ff;
  color: #3730a3;
}

.badge--merchant {
  background: #e9f7ef;
  color: #15803d;
}

.permission-grid {
  display: grid;
  gap: 12px;
  padding: 16px;
}

.assigned-list {
  display: grid;
  gap: 8px;
}

.permission-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  background: #fbfcfe;
  padding: 10px;
}

.permission-row strong,
.permission-row span {
  display: block;
  overflow-wrap: anywhere;
}

.permission-row span {
  color: #617080;
  font-size: 12px;
}

.inline-form {
  align-items: stretch;
}

.permission-catalog {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 16px;
}

.catalog-item {
  display: grid;
  gap: 6px;
  min-height: 86px;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  background: #fbfcfe;
  padding: 11px;
}

.catalog-item strong,
.catalog-item span {
  overflow-wrap: anywhere;
}

.catalog-item span {
  color: #617080;
  font-size: 12px;
}

.catalog-item small {
  width: fit-content;
  border-radius: 999px;
  padding: 2px 7px;
  font-size: 11px;
  font-weight: 800;
}

.empty {
  padding: 34px 16px;
  color: #617080;
  text-align: center;
}

.empty--compact {
  padding: 16px;
  border: 1px solid #edf1f5;
  border-radius: 8px;
}

.error {
  margin: 0;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
  color: #b91c1c;
  padding: 10px;
  font-size: 13px;
}

.toast {
  position: fixed;
  right: 18px;
  bottom: 18px;
  max-width: min(420px, calc(100vw - 36px));
  margin: 0;
  border: 1px solid #d9e1e8;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(23, 32, 42, 0.12);
  padding: 12px 14px;
  font-size: 13px;
  font-weight: 800;
}

.toast--error {
  border-color: #fecaca;
  color: #b91c1c;
}

@media (max-width: 1180px) {
  .admin-app {
    grid-template-columns: 1fr;
  }

  .sidebar {
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid #dde5ed;
  }

  .content-grid,
  .content-grid--users {
    grid-template-columns: 1fr;
  }

  .panel--wide {
    grid-column: span 1;
  }

  .permission-catalog {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .workspace,
  .login-view,
  .sidebar {
    padding: 16px;
  }

  .workspace__header,
  .panel__head,
  .permission-row {
    align-items: stretch;
    flex-direction: column;
  }

  .metrics,
  .form-grid,
  .permission-catalog {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }
}
</style>
