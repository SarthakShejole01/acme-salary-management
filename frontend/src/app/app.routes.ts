import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'employees' },
  {
    path: 'employees',
    title: 'Employees · ACME Salary Management',
    loadComponent: () => import('./features/employees/employees-page').then((m) => m.EmployeesPage),
  },
  {
    path: 'insights',
    title: 'Pay insights · ACME Salary Management',
    loadComponent: () => import('./features/insights/insights-page').then((m) => m.InsightsPage),
  },
  { path: '**', redirectTo: 'employees' },
];
