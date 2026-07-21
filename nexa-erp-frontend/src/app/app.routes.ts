import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { ShellComponent } from './core/layout/shell/shell.component';
import { LoginComponent } from './features/auth/pages/login/login.component';
import { DashboardComponent } from './features/dashboard/pages/dashboard/dashboard.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard],
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/pages/forgot-password/forgot-password').then((m) => m.ForgotPassword),
    canActivate: [guestGuard],
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/pages/reset-password/reset-password').then((m) => m.ResetPassword),
    canActivate: [guestGuard],
  },
  {
    path: 'set-password',
    loadComponent: () =>
      import('./features/auth/pages/set-password/set-password').then((m) => m.SetPassword),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        component: DashboardComponent,
      },

      {
        path: 'users',
        loadComponent: () =>
          import('./features/users/pages/user-list/user-list').then((m) => m.UserList),
      },
      {
        path: 'roles',
        loadComponent: () =>
          import('./features/roles/pages/role-list/role-list').then((m) => m.RoleList),
      },
      {
        path: 'permissions',
        loadComponent: () =>
          import('./features/permissions/pages/permission-list/permission-list').then(
            (m) => m.PermissionList,
          ),
      },

      {
        path: 'audit',
        loadComponent: () =>
          import('./features/audit/pages/audit-log-list/audit-log-list').then(
            (m) => m.AuditLogList,
          ),
      },

      {
        path: 'fiscal-years',
        loadComponent: () =>
          import('./features/fiscal-year/pages/fiscal-year-list/fiscal-year-list').then(
            (m) => m.FiscalYearList,
          ),
      },
      {
        path: 'accounting-periods',
        loadComponent: () =>
          import('./features/accounting-period/pages/accounting-period-list/accounting-period-list').then(
            (m) => m.AccountingPeriodList,
          ),
      },

      {
        path: 'accounts',
        loadComponent: () =>
          import('./features/accounts/pages/account-list/account-list').then((m) => m.AccountList),
      },

      {
        path: 'journals',
        loadComponent: () =>
          import('./features/journal/pages/journal-list/journal-list').then((m) => m.JournalList),
      },
      {
        path: 'journals/new',
        loadComponent: () =>
          import('./features/journal/pages/journal-form/journal-form').then((m) => m.JournalForm),
      },
      {
        path: 'journals/:id/edit',
        loadComponent: () =>
          import('./features/journal/pages/journal-form/journal-form').then((m) => m.JournalForm),
      },
      {
        path: 'reports/ledger',
        loadComponent: () => import('./features/reports/pages/ledger/ledger').then((m) => m.Ledger),
      },
      {
        path: 'reports/trial-balance',
        loadComponent: () =>
          import('./features/reports/pages/trial-balance/trial-balance').then(
            (m) => m.TrialBalance,
          ),
      },
      {
        path: 'invoice',
        loadComponent: () =>
          import('./features/invoice/pages/invoice-list/invoice-list').then((m) => m.InvoiceList),
      },
      {
        path: 'invoice/new',
        loadComponent: () =>
          import('./features/invoice/pages/invoice-form/invoice-form').then((m) => m.InvoiceForm),
      },
      {
        path: 'invoice/:id/edit',
        loadComponent: () =>
          import('./features/invoice/pages/invoice-form/invoice-form').then((m) => m.InvoiceForm),
      },
      {
        path: 'invoice/:id',
        loadComponent: () =>
          import('./features/invoice/pages/invoice-details/invoice-details').then(
            (m) => m.InvoiceDetails,
          ),
      },

      {
        path: 'vendor-bill',
        loadComponent: () =>
          import('./features/vendor-bill/pages/vendor-bill-list/vendor-bill-list').then(
            (m) => m.VendorBillList,
          ),
      },
      {
        path: 'vendor-bill/new',
        loadComponent: () =>
          import('./features/vendor-bill/pages/vendor-bill-form/vendor-bill-form').then(
            (m) => m.VendorBillForm,
          ),
      },
      {
        path: 'vendor-bill/:id',
        loadComponent: () =>
          import('./features/vendor-bill/pages/vendor-bill-details/vendor-bill-details').then(
            (m) => m.VendorBillDetails,
          ),
      },
      {
        path: 'vendor-bill/:id/edit',
        loadComponent: () =>
          import('./features/vendor-bill/pages/vendor-bill-form/vendor-bill-form').then(
            (m) => m.VendorBillForm,
          ),
      },

      {
        path: 'expense',
        loadComponent: () =>
          import('./features/expense/pages/expense-list/expense-list').then((m) => m.ExpenseList),
      },
      {
        path: 'expense/new',
        loadComponent: () =>
          import('./features/expense/pages/expense-form/expense-form').then((m) => m.ExpenseForm),
      },
      {
        path: 'expense/:id',
        loadComponent: () =>
          import('./features/expense/pages/expense-detail/expense-detail').then((m) => m.ExpenseDetail),
      },

      // Budget
      {
        path: 'budget',
        loadComponent: () =>
          import('./features/budget/pages/budget-list/budget-list').then((m) => m.BudgetList),
      },
      {
        path: 'budget/new',
        loadComponent: () =>
          import('./features/budget/pages/budget-form/budget-form').then((m) => m.BudgetForm),
      },
      {
        path: 'budget/:id',
        loadComponent: () =>
          import('./features/budget/pages/budget-detail/budget-detail').then((m) => m.BudgetDetail),
      },
      {
        path: 'budget/:id/variance',
        loadComponent: () =>
          import('./features/budget/pages/budget-variance/budget-variance').then((m) => m.BudgetVariance),
      },

      // Payment
      {
        path: 'payment',
        loadComponent: () =>
          import('./features/payment/pages/payment-list/payment-list').then((m) => m.PaymentList),
      },

      {
        path: 'payment/new',
        loadComponent: () =>
          import('./features/payment/pages/payment-form/payment-form').then((m) => m.PaymentForm),
      },

      {
        path: 'payment/:id',
        loadComponent: () =>
          import('./features/payment/pages/payment-details/payment-details').then(
            (m) => m.PaymentDetails,
          ),
      },
      //banking
      {
        path: 'banking',
        loadComponent: () =>
          import('./features/banking/pages/bank-account-list/bank-account-list').then(
            (m) => m.BankAccountList,
          ),
      },
      {
        path: 'banking/transactions',
        loadComponent: () =>
          import('./features/banking/pages/bank-transaction-list/bank-transaction-list').then(
            (m) => m.BankTransactionList,
          ),
      },
      {
        path: 'banking/reconciliation',
        loadComponent: () =>
          import('./features/banking/pages/bank-reconciliation-list/bank-reconciliation-list').then(
            (m) => m.BankReconciliationList,
          ),
      },
      {
        path: 'banking/reconciliation/:id',
        loadComponent: () =>
          import('./features/banking/pages/bank-reconciliation-detail/bank-reconciliation-detail').then(
            (m) => m.BankReconciliationDetail,
          ),
      },
      {
        path: 'banking/reconciliation',
        loadComponent: () =>
          import('./features/banking/pages/bank-reconciliation-list/bank-reconciliation-list').then(
            (m) => m.BankReconciliationList,
          ),
      },
      {
        path: 'banking/reconciliation/:id',
        loadComponent: () =>
          import('./features/banking/pages/bank-reconciliation-detail/bank-reconciliation-detail').then(
            (m) => m.BankReconciliationDetail,
          ),
      },

      {
        path: 'fixed-assets',
        loadComponent: () =>
          import('./features/fixed-assets/pages/fixed-asset-list/fixed-asset-list').then(
            (m) => m.FixedAssetList,
          ),
      },
      {
        path: 'fixed-assets/:id',
        loadComponent: () =>
          import('./features/fixed-assets/pages/fixed-asset-detail/fixed-asset-detail').then(
            (m) => m.FixedAssetDetail,
          ),
      },

      {
        path: 'party',
        loadComponent: () =>
          import('./features/party/pages/party-list/party-list').then((m) => m.PartyList),
      },
      {
        path: 'party/new',
        loadComponent: () =>
          import('./features/party/pages/party-form/party-form').then((m) => m.PartyForm),
      },
      {
        path: 'party/:id',
        loadComponent: () =>
          import('./features/party/pages/party-details/party-details').then((m) => m.PartyDetails),
      },
      {
        path: 'party/:id/edit',
        loadComponent: () =>
          import('./features/party/pages/party-form/party-form').then((m) => m.PartyForm),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('./features/reports/pages/reports-dashboard/reports-dashboard').then(
            (m) => m.ReportsDashboard,
          ),
      },

      {
        path: 'reports/party-statement',
        loadComponent: () =>
          import('./features/reports/pages/party-statement-report/party-statement-report').then(
            (m) => m.PartyStatementReport,
          ),
      },
      {
        path: 'reports/profit-loss',
        loadComponent: () =>
          import('./features/reports/pages/profit-loss-report/profit-loss-report').then(
            (m) => m.ProfitLossReport,
          ),
      },
      {
        path: 'reports/balance-sheet',
        loadComponent: () =>
          import('./features/reports/pages/balance-sheet-report/balance-sheet-report').then(
            (m) => m.BalanceSheetReport,
          ),
      },
      {
        path: 'reports/aging',
        loadComponent: () =>
          import('./features/reports/pages/aging-report/aging-report').then((m) => m.AgingReport),
      },

      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/pages/settings-list/settings-list').then(
            (m) => m.SettingsList,
          ),
      },

      {
        path: 'credit-notes',
        loadComponent: () =>
          import('./features/credit-note/pages/credit-note-list/credit-note-list').then(
            (m) => m.CreditNoteList,
          ),
      },
      {
        path: 'credit-notes/new',
        loadComponent: () =>
          import('./features/credit-note/pages/credit-note-form/credit-note-form').then(
            (m) => m.CreditNoteForm,
          ),
      },
      {
        path: 'credit-notes/:id/edit',
        loadComponent: () =>
          import('./features/credit-note/pages/credit-note-form/credit-note-form').then(
            (m) => m.CreditNoteForm,
          ),
      },

      {
        path: 'debit-notes',
        loadComponent: () =>
          import('./features/debit-note/pages/debit-note-list/debit-note-list').then(
            (m) => m.DebitNoteList,
          ),
      },
      {
        path: 'debit-notes/new',
        loadComponent: () =>
          import('./features/debit-note/pages/debit-note-form/debit-note-form').then(
            (m) => m.DebitNoteForm,
          ),
      },
      {
        path: 'debit-notes/:id/edit',
        loadComponent: () =>
          import('./features/debit-note/pages/debit-note-form/debit-note-form').then(
            (m) => m.DebitNoteForm,
          ),
      },

      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
