import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { MatSortHarness } from '@angular/material/sort/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NEVER, Observable, of, throwError } from 'rxjs';
import { EmployeesPage } from './employees-page';
import { EmployeeApi } from '../../core/employee-api';
import { EmployeeFormDialog } from './employee-form-dialog';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Employee, EmployeeQuery, Page, ReferenceData } from '../../core/models';

class FakeEmployeeApi {
  readonly searches: EmployeeQuery[] = [];
  respond: (query: EmployeeQuery) => Observable<Page<Employee>> = () =>
    of(page([PRIYA, EMILY], 45));

  search(query: EmployeeQuery): Observable<Page<Employee>> {
    this.searches.push(query);
    return this.respond(query);
  }

  readonly deleted: number[] = [];
  deleteResult: () => Observable<void> = () => of(undefined);

  referenceResult: () => Observable<ReferenceData> = () => of(REFERENCE_DATA);
  referenceCalls = 0;

  referenceData(): Observable<ReferenceData> {
    this.referenceCalls++;
    return this.referenceResult();
  }

  delete(id: number): Observable<void> {
    this.deleted.push(id);
    return this.deleteResult();
  }

  exportUrl(query: EmployeeQuery): string {
    return `/api/employees/export.csv?country=${query.country}`;
  }

  last(): EmployeeQuery {
    return this.searches[this.searches.length - 1];
  }
}

class FakeDialog {
  readonly opened: { component: unknown; data: unknown }[] = [];
  result: unknown;

  open(component: unknown, config: { data: unknown }) {
    this.opened.push({ component, data: config.data });
    return { afterClosed: () => of(this.result) };
  }
}

class FakeSnackBar {
  readonly messages: string[] = [];

  open(message: string) {
    this.messages.push(message);
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
  let dialog: FakeDialog;
  let snackBar: FakeSnackBar;
  let fixture: ComponentFixture<EmployeesPage>;
  let loader: HarnessLoader;

  beforeEach(() => {
    api = new FakeEmployeeApi();
    dialog = new FakeDialog();
    snackBar = new FakeSnackBar();
    TestBed.configureTestingModule({
      imports: [EmployeesPage],
      providers: [
        { provide: EmployeeApi, useValue: api },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snackBar },
      ],
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
    return Array.from(element.querySelectorAll('tr.mat-mdc-row')).map(
      (row) => row.textContent ?? '',
    );
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

  it('hides_a_stale_employee_count_when_a_later_load_fails', async () => {
    const element = await render();
    expect(element.querySelector('.count')?.textContent).toContain('45 employees');
    api.respond = () => throwError(() => new Error('down'));
    const country = await loader.getHarness(
      MatSelectHarness.with({ selector: '[data-testid="country-filter"]' }),
    );

    await country.open();
    await country.clickOptions({ text: 'India' });

    expect(element.textContent).toContain('Could not load employees');
    expect(element.querySelector('.count')).toBeNull();
  });

  it('filters_by_country_and_returns_to_the_first_page', async () => {
    await render();
    const country = await loader.getHarness(
      MatSelectHarness.with({ selector: '[data-testid="country-filter"]' }),
    );

    await country.open();
    await country.clickOptions({ text: 'India' });

    expect(api.last()).toMatchObject({ country: 'IN', page: 0 });
  });

  it('filters_by_department_and_job_title', async () => {
    await render();
    const department = await loader.getHarness(
      MatSelectHarness.with({ selector: '[data-testid="department-filter"]' }),
    );
    const jobTitle = await loader.getHarness(
      MatSelectHarness.with({ selector: '[data-testid="job-title-filter"]' }),
    );

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
    const element = await render();
    vi.useFakeTimers();
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
    const country = await loader.getHarness(
      MatSelectHarness.with({ selector: '[data-testid="country-filter"]' }),
    );
    await country.open();
    await country.clickOptions({ text: 'India' });

    (element.querySelector('[data-testid="clear-filters"]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(api.last()).toMatchObject({
      search: '',
      country: '',
      department: '',
      jobTitle: '',
      page: 0,
    });
    expect((element.querySelector('[data-testid="search"]') as HTMLInputElement).value).toBe('');
  });

  it('empties_the_search_box_when_filters_are_cleared', async () => {
    api.respond = () => of(page([]));
    const element = await render();
    const input = element.querySelector('[data-testid="search"]') as HTMLInputElement;
    input.value = 'zzzz';
    input.dispatchEvent(new Event('input'));

    (element.querySelector('[data-testid="clear-filters"]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(input.value).toBe('');
  });

  async function click(element: HTMLElement, selector: string): Promise<void> {
    (element.querySelector(selector) as HTMLElement).click();
    await fixture.whenStable();
  }

  it('adds_an_employee_through_the_form_then_reloads', async () => {
    const element = await render();
    dialog.result = PRIYA;

    await click(element, '[data-testid="add"]');

    expect(dialog.opened[0]).toEqual({
      component: EmployeeFormDialog,
      data: { employee: undefined, referenceData: REFERENCE_DATA },
    });
    expect(snackBar.messages).toEqual(['Saved Priya Sharma']);
    expect(api.searches).toHaveLength(2);
  });

  it('does_not_reload_when_the_form_is_cancelled', async () => {
    const element = await render();
    dialog.result = undefined;

    await click(element, '[data-testid="add"]');

    expect(api.searches).toHaveLength(1);
    expect(snackBar.messages).toEqual([]);
  });

  it('opens_a_row_in_the_form_for_editing', async () => {
    const element = await render();

    await click(element, 'tr.mat-mdc-row [data-testid="edit"]');

    expect(dialog.opened[0].data).toEqual({ employee: PRIYA, referenceData: REFERENCE_DATA });
  });

  it('deletes_an_employee_after_confirmation_then_reloads', async () => {
    const element = await render();
    dialog.result = true;

    await click(element, 'tr.mat-mdc-row [data-testid="delete"]');

    expect(dialog.opened[0].component).toBe(ConfirmDialog);
    expect(dialog.opened[0].data).toMatchObject({
      message: expect.stringContaining('Priya Sharma (EMP-00001)'),
    });
    expect(api.deleted).toEqual([1]);
    expect(snackBar.messages).toEqual(['Deleted Priya Sharma']);
    expect(api.searches).toHaveLength(2);
  });

  it('keeps_the_employee_when_delete_is_not_confirmed', async () => {
    const element = await render();
    dialog.result = false;

    await click(element, 'tr.mat-mdc-row [data-testid="delete"]');

    expect(api.deleted).toEqual([]);
  });

  it('says_so_when_a_delete_fails', async () => {
    const element = await render();
    dialog.result = true;
    api.deleteResult = () => throwError(() => new Error('down'));

    await click(element, 'tr.mat-mdc-row [data-testid="delete"]');

    expect(snackBar.messages).toEqual(['Could not delete Priya Sharma. Please try again.']);
  });

  it('exports_the_list_with_the_current_filters', async () => {
    const element = await render();
    const country = await loader.getHarness(
      MatSelectHarness.with({ selector: '[data-testid="country-filter"]' }),
    );

    await country.open();
    await country.clickOptions({ text: 'India' });

    expect(element.querySelector('[data-testid="export"]')?.getAttribute('href')).toBe(
      '/api/employees/export.csv?country=IN',
    );
  });

  function addButton(element: HTMLElement): HTMLButtonElement {
    return element.querySelector('[data-testid="add"]') as HTMLButtonElement;
  }

  it('warns_and_blocks_add_and_edit_when_the_form_options_cannot_be_loaded', async () => {
    api.referenceResult = () => throwError(() => new Error('down'));

    const element = await render();

    expect(element.querySelector('[data-testid="reference-warning"]')?.textContent).toContain(
      'Could not load the filter and form options',
    );
    expect(addButton(element).disabled).toBe(true);
    expect(
      (element.querySelector('tr.mat-mdc-row [data-testid="edit"]') as HTMLButtonElement).disabled,
    ).toBe(true);
  });

  it('loads_the_form_options_again_on_retry_and_enables_add', async () => {
    let attempt = 0;
    api.referenceResult = () =>
      ++attempt === 1 ? throwError(() => new Error('down')) : of(REFERENCE_DATA);
    const element = await render();

    await click(element, '[data-testid="retry-reference-data"]');

    expect(element.querySelector('[data-testid="reference-warning"]')).toBeNull();
    expect(addButton(element).disabled).toBe(false);
  });

  it('keeps_add_disabled_without_a_warning_while_the_form_options_are_loading', async () => {
    api.referenceResult = () => NEVER;

    const element = await render();

    expect(addButton(element).disabled).toBe(true);
    expect(element.querySelector('[data-testid="reference-warning"]')).toBeNull();
  });

  it('keeps_the_known_options_when_refreshing_them_after_a_save_fails', async () => {
    const element = await render();
    dialog.result = PRIYA;
    api.referenceResult = () => throwError(() => new Error('down'));

    await click(element, '[data-testid="add"]');

    expect(element.querySelector('[data-testid="reference-warning"]')).toBeNull();
    expect(addButton(element).disabled).toBe(false);
  });

  it('names_the_table_for_screen_readers', async () => {
    const element = await render();

    expect(element.querySelector('table')?.getAttribute('aria-label')).toBe('Employees');
  });
});
