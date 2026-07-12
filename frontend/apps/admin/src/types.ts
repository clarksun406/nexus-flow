export type UserInfo = {
  userId: string;
  email?: string;
  displayName?: string;
  activeMerchantId?: string;
};

export type Role = {
  id: string;
  code: string;
  name?: string;
  scope?: "SYSTEM" | "MERCHANT" | string;
  permissions?: Permission[];
};

export type Permission = {
  id?: string;
  code: string;
  name?: string;
  scope?: "SYSTEM" | "MERCHANT" | string;
};

export type UserRole = {
  roleId: string;
  roleCode?: string;
  scopeType?: "SYSTEM" | "MERCHANT" | string;
  scopeId?: string;
};

export type CreateRoleRequest = {
  code: string;
  name?: string;
  scope: "SYSTEM" | "MERCHANT";
};

export type GrantRoleRequest = {
  userId: string;
  roleId: string;
  scopeType: "SYSTEM" | "MERCHANT";
  scopeId?: string;
};
