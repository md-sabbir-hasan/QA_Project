export interface DashboardSummary {
  users: UserSummary;
  security: SecuritySummary;
  finance: FinanceSummary;
  system: SystemSummary;
  health: HealthSummary;
  recentActivities: RecentActivity[];
}

export interface UserSummary {
  total: number;
  active: number;
  pending: number;
  inactive: number;
  locked: number;
}

export interface SecuritySummary {
  totalRoles: number;
  totalPermissions: number;
}

export interface FinanceSummary {
  totalAccounts: number;
  totalJournalEntries: number;
  postedJournalEntries: number;
  draftJournalEntries: number;
  reversedJournalEntries: number;
}

export interface SystemSummary {
  applicationVersion: string;
  serverTime: string;
  serverTimezone: string;
  environment: string;
  javaVersion: string;
}

export interface HealthSummary {
  application: string;
  database: string;
  mail: string;
}

export interface RecentActivity {
  action: string;
  entityName: string;
  entityId: number;
  userName: string;
  createdAt: string;
  description?: string;
}