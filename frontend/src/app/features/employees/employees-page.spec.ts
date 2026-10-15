import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { MatSortHarness } from '@angular/material/sort/testing';
import { NEVER, Observable, of, throwError } from 'rxjs';
import { EmployeesPage } from './employees-page';
import { EmployeeApi } from '../../core/employee-api';
import { Employee, EmployeeQuery, Page, ReferenceData } from '../../core/models';

class FakeEmployeeApi {
  readonly searches: EmployeeQuery[] = [];
  respond: (query: EmployeeQuery) => Observable<Page<Employee>> = () => of(page([PRIYA, EMILY], 45));

  search(query: EmployeeQuery): Observable<Page<Employee>> {
    this.searches.push(query);
    return this.respond(query);
  }

  referenceData(): Observable<ReferenceData> {
    return of(REFERENCE_DATA);
  }

  last(): EmployeeQuery {
    return this.searches[this.searches.length - 1];
  }
}

const PRIYA: Employee = {
  id: 1,
  employeeCode: 'EMP-00001',
  fullName: 'Priya Sharma',
  email: 'priya.sharma@acme.com',
  jobTitle: 'Software Engineer',
  department: 'Engineering',
  country: 'IN',
  salary: { amount: 2400000, currency: 'INR' },
  employmentType: 'FULL_TIME',
  hireDate: '2022-04-01',
};

const EMILY: Employee = {
  ...PRIYA,
  id: 2,
  employeeCode: 'EMP-00003',
  fullName: 'Emily Clark',
  email: 'emily.clark@acme.com',
  country: 'US',
  salary: { amount: 130000, currency: 'USD' },
  employmentType: 'PART_TIME',
};

const REFERENCE_DATA: ReferenceData = {
  countries: [
    { code: 'IN', name: 'India', currency: 'INR' },
    { code: 'US', name: 'United States', currency: 'USD' },
  ],
  departments: ['Engineering', 'Finance'],
  jobTitles: ['Data Analyst', 'Software Engineer'],
  employmentTypes: ['FULL_TIME', 'PART_TIME', 'CONTRACTOR'],
};

function page(content: Employee[], totalElements = content.length): Page<Employee> {
  return { content, page: 0, size: 20, totalElements, totalPages: Math.ceil(totalElements / 20) };
}

describe('EmployeesPage', () => {
  let api: FakeEmployeeApi;
  let fixture: ComponentFixture<EmployeesPage>;
  let loader: HarnessLoader;

  beforeEach(() => {
    api = new FakeEmployeeApi();
    TestBed.configureTestingModule({
      imports: [EmployeesPage],
      providers: [{ provide: EmployeeApi, useValue: api }],
    });
  });

  afterEach(() => vi.useRealTimers());

  async function render(): Promise<HTMLElement> {
    fixture = TestBed.createComponent(EmployeesPage);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function rows(element: HTMLElement): string[] {
    return Array.from(element.querySelectorAll('tr.mat-mdc-row')).map((row) => row.textContent ?? '');
  }

  it('shows_the_first_page_sorted_by_the_server_on_load', async () => {
    const element = await render();

    expect(api.searches[0]).toEqual({
      page: 0,
      size: 20,
      sort: { active: '', direction: '' },
      search: '',
      country: '',
      department: '',
      jobTitle: '',
    });
    expect(rows(element)).toHaveLength(2);
    expect(rows(element)[0]).toContain('Priya Sharma');
    expect(rows(element)[0]).toContain('₹2,400,000');
    expect(rows(element)[0]).toContain('Full-time');
    expect(rows(element)[1]).toContain('Part-time');
  });

  it('shows_a_progress_bar_while_employees_load', async () => {
    api.respond = () => NEVER;

    const element = await render();

    expect(element.querySelector('mat-progress-bar')).not.toBeNull();
    expect(rows(element)).toHaveLength(0);
  });

  it('shows_an_empty_state_when_no_employee_matches', async () => {
    api.respond = () => of(page([]));

    const element = await render();

    expect(element.textContent).toContain('No employees match these filters');
  });

  it('shows_an_error_with_a_retry_that_loads_again', async () => {
    let attempt = 0;
    api.respond = () => (++attempt === 1 ? throwError(() => new Error('down')) : of(page([PRIYA])));
    const element = await render();
    expect(element.textContent).toContain('Could not load employees');

    (element.querySelector('[data-testid="retry"]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(api.searches).toHaveLength(2);
    expect(rows(element)).toHaveLength(1);
  });

  it('filters_by_country_and_returns_to_the_first_page', async () => {
    await render();
    const country = await loader.getHarness(MatSelectHarness.with({ selector: '[data-testid="country-filter"]' }));

    await country.open();
    await country.clickOptions({ text: 'India' });

    expect(api.last()).toMatchObject({ country: 'IN', page: 0 });
  });

  it('filters_by_department_and_job_title', async () => {
    await render();
    const department = await loader.getHarness(MatSelectHarness.with({ selector: '[data-testid="department-filter"]' }));
    const jobTitle = await loader.getHarness(MatSelectHarness.with({ selector: '[data-testid="job-title-filter"]' }));

    await department.open();
    await department.clickOptions({ text: 'Finance' });
    await jobTitle.open();
    await jobTitle.clickOptions({ text: 'Data Analyst' });

    expect(api.last()).toMatchObject({ department: 'Finance', jobTitle: 'Data Analyst', page: 0 });
  });

  it('requests_the_next_page_from_the_paginator', async () => {
    await render();
    const paginator = await loader.getHarness(MatPaginatorHarness);

    await paginator.goToNextPage();

    expect(api.last()).toMatchObject({ page: 1, size: 20 });
  });

  it('sorts_on_the_server_when_a_column_header_is_clicked', async () => {
    await render();
    const sort = await loader.getHarness(MatSortHarness);
    const [salary] = await sort.getSortHeaders({ label: 'Salary' });

    await salary.click();

    expect(api.last()).toMatchObject({ sort: { active: 'salary', direction: 'asc' }, page: 0 });
  });

  it('searches_once_typing_pauses', async () => {
    vi.useFakeTimers();
    const element = await render();
    const input = element.querySelector('[data-testid="search"]') as HTMLInputElement;

    input.value = 'sharma';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(299);
    expect(api.searches).toHaveLength(1);

    vi.advanceTimersByTime(1);
    expect(api.last()).toMatchObject({ search: 'sharma', page: 0 });
  });

  it('clears_every_filter_from_the_empty_state', async () => {
    api.respond = () => of(page([]));
    const element = await render();
    const country = await loader.getHarness(MatSelectHarness.with({ selector: '[data-testid="country-filter"]' }));
    await country.open();
    await country.clickOptions({ text: 'India' });

    (element.querySelector('[data-testid="clear-filters"]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(api.last()).toMatchObject({ search: '', country: '', department: '', jobTitle: '', page: 0 });
  });
});
