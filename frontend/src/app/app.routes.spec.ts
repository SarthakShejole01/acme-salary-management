import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from './app.routes';
import { EmployeeApi } from './core/employee-api';
import { InsightsApi } from './core/insights-api';

describe('routes', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {
          provide: InsightsApi,
          useValue: {
            summary: () =>
              of({
                headcount: 0,
                totalAnnualCost: { amount: 0, currency: 'USD' },
                averageSalary: null,
                approximate: true,
                ratesAsOf: '2026-10-14',
              }),
            countries: () => of([]),
          },
        },
        {
          provide: EmployeeApi,
          useValue: {
            search: () => of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
            referenceData: () => of({ countries: [], departments: [], jobTitles: [], employmentTypes: [] }),
            exportUrl: () => '/api/employees/export.csv',
          },
        },
      ],
    });
  });

  it('opens_the_employees_page_from_the_root_url', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/');

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Employees');
  });

  it('opens_the_insights_page_at_insights', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/insights');

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Pay insights');
  });
});
