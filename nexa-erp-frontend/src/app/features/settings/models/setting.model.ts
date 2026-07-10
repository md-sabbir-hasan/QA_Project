export type SettingKey =
  | 'DEFAULT_RECEIVABLE_ACCOUNT'
  | 'DEFAULT_PAYABLE_ACCOUNT'
  | 'DEFAULT_SALES_REVENUE'
  | 'DEFAULT_VAT_PAYABLE'
  | 'DEFAULT_INPUT_VAT'
  | 'DEFAULT_TDS_PAYABLE'
  | 'DEFAULT_OPENING_EQUITY'
  | 'COMPANY_NAME'
  | 'DEFAULT_CURRENCY'
  | 'FINANCIAL_YEAR'
  | 'DECIMAL_PRECISION'
  | 'TIMEZONE'
  | 'DATE_FORMAT'
  | 'AUTO_POST_INVOICE'
  | 'ALLOW_NEGATIVE_STOCK'
  | 'DEFAULT_WAREHOUSE';

export interface SystemSetting {
  id: number;
  key: SettingKey;
  value: string;
  description: string | null;
  updatedAt: string;
}

// Backend's PUT /api/settings/{key} currently only accepts { accountId },
// so only these keys can actually be saved today. Everything else in the
// SettingKey enum is defined but not yet wired up on the backend.
export const ACCOUNT_MAPPED_KEYS: SettingKey[] = [
  'DEFAULT_RECEIVABLE_ACCOUNT',
  'DEFAULT_PAYABLE_ACCOUNT',
  'DEFAULT_SALES_REVENUE',
  'DEFAULT_VAT_PAYABLE',
  'DEFAULT_INPUT_VAT',
  'DEFAULT_TDS_PAYABLE',
  'DEFAULT_OPENING_EQUITY',
];

export const SETTING_LABELS: Record<SettingKey, string> = {
  DEFAULT_RECEIVABLE_ACCOUNT: 'Default Accounts Receivable',
  DEFAULT_PAYABLE_ACCOUNT: 'Default Accounts Payable',
  DEFAULT_SALES_REVENUE: 'Default Sales Revenue',
  DEFAULT_VAT_PAYABLE: 'Default VAT Payable',
  DEFAULT_INPUT_VAT: 'Default Input VAT',
  DEFAULT_TDS_PAYABLE: 'Default TDS Payable',
  DEFAULT_OPENING_EQUITY: 'Default Opening Balance Equity',
  COMPANY_NAME: 'Company Name',
  DEFAULT_CURRENCY: 'Default Currency',
  FINANCIAL_YEAR: 'Financial Year',
  DECIMAL_PRECISION: 'Decimal Precision',
  TIMEZONE: 'Timezone',
  DATE_FORMAT: 'Date Format',
  AUTO_POST_INVOICE: 'Auto-post Invoices',
  ALLOW_NEGATIVE_STOCK: 'Allow Negative Stock',
  DEFAULT_WAREHOUSE: 'Default Warehouse',
};
