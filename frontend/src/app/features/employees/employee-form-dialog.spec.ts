import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, of, throwError } from 'rxjs';
import { EmployeeFormDialog, EmployeeFormData } from './employee-form-dialog';
import { EmployeeApi } from '../../core/employee-api';
import { TODAY } from '../../core/today';
import { Employee, EmployeeRequest, ReferenceData } from '../../core/models';

const REFERENCE_DATA: ReferenceData = {
  countries: [
    { code: 'IN', name: 'India', currency: 'INR' },
    { code: 'US', name: 'United States', currency: 'USD' },
  ],
  departments: ['Engineering', 'Finance'],
  jobTitles: ['Data Analyst', 'Software Engineer'],
  employmentTypes: ['FULL_TIME', 'PART_TIME', 'CONTRACTOR'],
};

const PRIYA: Employee = {
  id: 7,
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

class FakeEmployeeApi {
  created: EmployeeRequest[] = [];
  updated: { id: number; request: EmployeeRequest }[] = [];
  respond: () => Observable<Employee> = () => of(PRIYA);

  create(request: EmployeeRequest): Observable<Employee> {
    this.created.push(request);
    return this.respond();
  }

  update(id: number, request: EmployeeRequest): Observable<Employee> {
    this.updated.push({ id, request });
    return this.respond();
  }
}

function problem(status: number, errors?: { field: string; message: string }[]): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { status, detail: 'Problem', errors } });
}

describe('EmployeeFormDialog', () => {
  let api: FakeEmployeeApi;
  let close: ReturnType<typeof vi.fn>;
  let fixture: ComponentFixture<EmployeeFormDialog>;
  let loader: HarnessLoader;
  let element: HTMLElement;

  async function render(employee?: Employee): Promise<void> {
    api = new FakeEmployeeApi();
    close = vi.fn();
    const data: EmployeeFormData = { employee, referenceData: REFERENCE_DATA };
    TestBed.configureTestingModule({
      imports: [EmployeeFormDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: { close } },
        { provide: EmployeeApi, useValue: api },
        { provide: TODAY, useValue: () => '2026-01-15' },
      ],
    });
    fixture = TestBed.createComponent(EmployeeFormDialog);
    loader = TestbedHarnessEnvironment.loader(fixture);
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  function input(testId: string): HTMLInputElement {
    return element.querySelector(`[data-testid="${testId}"]`) as HTMLInputElement;
  }

  async function type(testId: string, value: string): Promise<void> {
    const field = input(testId);
    field.value = value;
    field.dispatchEvent(new Event('input'));
    field.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  async function choose(testId: string, option: string): Promise<void> {
    const select = await loader.getHarness(
      MatSelectHarness.with({ selector: `[data-testid="${testId}"]` }),
    );
    await select.open();
    await select.clickOptions({ text: option });
  }

  async function save(): Promise<void> {
    (element.querySelector('[data-testid="save"]') as HTMLButtonElement).click();
    await fixture.whenStable();
  }

  async function fillValidForm(): Promise<void> {
    await type('employeeCode', 'EMP-00001');
    await type('fullName', 'Priya Sharma');
    await type('email', 'priya.sharma@acme.com');
    await type('jobTitle', 'Software Engineer');
    await type('department', 'Engineering');
    await choose('country', 'India');
    await type('salary', '2400000');
    await type('hireDate', '2022-04-01');
  }

  function errorFor(testId: string): string {
    return (
      input(testId).closest('mat-form-field')?.querySelector('mat-error')?.textContent?.trim() ?? ''
    );
  }

  it('lists_what_is_missing_when_saving_an_empty_form', async () => {
    await render();

    await save();

    expect(errorFor('employeeCode')).toBe('Required');
    expect(errorFor('fullName')).toBe('Required');
    expect(errorFor('salary')).toBe('Required');
    expect(api.created).toHaveLength(0);
  });

  it('checks_the_email_format', async () => {
    await render();

    await type('email', 'not-an-email');

    expect(errorFor('email')).toBe('Enter a valid email address');
  });

  it('limits_text_to_the_lengths_the_server_accepts', async () => {
    await render();

    await type('employeeCode', 'X'.repeat(21));

    expect(errorFor('employeeCode')).toBe('At most 20 characters');
  });

  it('rejects_a_salary_that_is_not_positive', async () => {
    await render();

    await type('salary', '0');

    expect(errorFor('salary')).toBe('Must be greater than zero');
  });

  it('rejects_a_salary_with_more_than_two_decimals', async () => {
    await render();

    await type('salary', '1000.123');

    expect(errorFor('salary')).toBe('At most 2 decimal places');
  });

  it('rejects_a_hire_date_in_the_future', async () => {
    await render();

    await type('hireDate', '2026-01-16');

    expect(errorFor('hireDate')).toBe('Cannot be in the future');
  });

  it('shows_the_currency_of_the_selected_country_next_to_the_salary', async () => {
    await render();

    await choose('country', 'India');

    expect(element.textContent).toContain('Annual salary (INR)');
  });

  it('creates_the_employee_and_closes_with_it', async () => {
    await render();
    await fillValidForm();

    await save();

    expect(api.created).toEqual([
      {
        employeeCode: 'EMP-00001',
        fullName: 'Priya Sharma',
        email: 'priya.sharma@acme.com',
        jobTitle: 'Software Engineer',
        department: 'Engineering',
        country: 'IN',
        salary: 2400000,
        employmentType: 'FULL_TIME',
        hireDate: '2022-04-01',
      },
    ]);
    expect(close).toHaveBeenCalledWith(PRIYA);
  });

  it('opens_an_existing_employee_filled_in_and_saves_changes', async () => {
    await render(PRIYA);
    expect(input('fullName').value).toBe('Priya Sharma');
    expect(input('salary').value).toBe('2400000');

    await type('fullName', 'Priya Sharma-Rao');
    await save();

    expect(api.updated).toEqual([
      { id: 7, request: expect.objectContaining({ fullName: 'Priya Sharma-Rao' }) },
    ]);
    expect(close).toHaveBeenCalledWith(PRIYA);
  });

  it('shows_server_field_errors_on_the_matching_fields', async () => {
    await render();
    await fillValidForm();
    api.respond = () =>
      throwError(() =>
        problem(409, [
          { field: 'email', message: 'Email priya.sharma@acme.com is already in use' },
        ]),
      );

    await save();

    expect(errorFor('email')).toBe('Email priya.sharma@acme.com is already in use');
    expect(close).not.toHaveBeenCalled();
  });

  it('shows_a_general_message_when_saving_fails_without_field_details', async () => {
    await render();
    await fillValidForm();
    api.respond = () => throwError(() => problem(500));

    await save();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Could not save');
    expect(close).not.toHaveBeenCalled();
  });

  it('closes_without_saving_when_cancelled', async () => {
    await render();

    (element.querySelector('[data-testid="cancel"]') as HTMLButtonElement).click();

    expect(close).toHaveBeenCalledWith();
    expect(api.created).toHaveLength(0);
  });
});
