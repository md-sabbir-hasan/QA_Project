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
