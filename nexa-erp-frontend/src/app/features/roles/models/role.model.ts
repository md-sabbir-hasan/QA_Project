export interface RolePermission {
  id: number;
  code: string;
  name: string;
  module: string;
}

export interface Role {
  id: number;
  name: string;
  description: string;
  permissions: RolePermission[];
  userCount: number;
}
