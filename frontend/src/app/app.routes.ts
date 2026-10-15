import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'employees' },
  {
    path: 'employees',
    title: 'Employees · ACME Salary Management',
    loadComponent: () => import('./features/employees/employees-page').then((m) => m.EmployeesPage),
  },
  { path: '**', redirectTo: 'employees' },
];
