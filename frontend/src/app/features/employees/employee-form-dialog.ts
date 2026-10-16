import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Observable } from 'rxjs';
import { EmployeeApi } from '../../core/employee-api';
import { TODAY } from '../../core/today';
import {
  Employee,
  EmployeeRequest,
  EmploymentType,
  Problem,
  ReferenceData,
} from '../../core/models';

export interface EmployeeFormData {
  /** Present when editing. */
  employee?: Employee;
  referenceData: ReferenceData;
}

const EMPLOYMENT_TYPES: { value: EmploymentType; label: string }[] = [
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'PART_TIME', label: 'Part-time' },
  { value: 'CONTRACTOR', label: 'Contractor' },
];

/** Salary as typed: a plain decimal, at most two decimals, above zero (the server's rules). */
const salaryValidator: ValidatorFn = (
  control: AbstractControl<string>,
): ValidationErrors | null => {
  const value = control.value.trim();
  if (value === '') {
    return null;
  }
  if (!/^\d+(\.\d+)?$/.test(value)) {
    return { number: true };
  }
  if (!/^\d+(\.\d{1,2})?$/.test(value)) {
    return { decimals: true };
  }
  return Number(value) > 0 ? null : { positive: true };
};

function notAfter(today: () => string): ValidatorFn {
  return (control: AbstractControl<string>) =>
    control.value && control.value > today() ? { future: true } : null;
}

/** Add or edit one employee. Validates like the server; server field errors are shown on their fields. */
@Component({
  selector: 'app-employee-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatProgressBarModule,
  ],
  templateUrl: './employee-form-dialog.html',
  styleUrl: './employee-form-dialog.scss',
})
export class EmployeeFormDialog {
  private readonly api = inject(EmployeeApi);

  private readonly dialogRef = inject<MatDialogRef<EmployeeFormDialog, Employee>>(MatDialogRef);

  protected readonly data = inject<EmployeeFormData>(MAT_DIALOG_DATA);

  protected readonly today = inject(TODAY);

  protected readonly employmentTypes = EMPLOYMENT_TYPES;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    employeeCode: ['', [Validators.required, Validators.maxLength(20)]],
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    jobTitle: ['', [Validators.required, Validators.maxLength(100)]],
    department: ['', [Validators.required, Validators.maxLength(100)]],
    country: ['', Validators.required],
    salary: ['', [Validators.required, salaryValidator]],
    employmentType: ['FULL_TIME' as EmploymentType, Validators.required],
    hireDate: ['', [Validators.required, notAfter(this.today)]],
  });

  protected readonly saving = signal(false);

  protected readonly generalError = signal<string | null>(null);

  private readonly country = toSignal(this.form.controls.country.valueChanges, {
    initialValue: this.form.controls.country.value,
  });

  protected readonly salaryLabel = computed(() => {
    const currency = this.data.referenceData.countries.find(
      (c) => c.code === this.country(),
    )?.currency;
    return currency ? `Annual salary (${currency})` : 'Annual salary';
  });

  private readonly jobTitleText = toSignal(this.form.controls.jobTitle.valueChanges, {
    initialValue: '',
  });

  private readonly departmentText = toSignal(this.form.controls.department.valueChanges, {
    initialValue: '',
  });

  protected readonly jobTitleOptions = computed(() =>
    matching(this.data.referenceData.jobTitles, this.jobTitleText()),
  );

  protected readonly departmentOptions = computed(() =>
    matching(this.data.referenceData.departments, this.departmentText()),
  );

  constructor() {
    const employee = this.data.employee;
    if (employee) {
      this.form.setValue({
        employeeCode: employee.employeeCode,
        fullName: employee.fullName,
        email: employee.email,
        jobTitle: employee.jobTitle,
        department: employee.department,
        country: employee.country,
        salary: String(employee.salary.amount),
        employmentType: employee.employmentType,
        hireDate: employee.hireDate,
      });
    }
  }

  protected title(): string {
    return this.data.employee ? 'Edit employee' : 'Add employee';
  }

  protected message(field: keyof typeof this.form.controls): string {
    const errors = this.form.controls[field].errors;
    if (!errors) {
      return '';
    }
    if (errors['server']) return errors['server'];
    if (errors['required']) return 'Required';
    if (errors['email']) return 'Enter a valid email address';
    if (errors['maxlength']) return `At most ${errors['maxlength'].requiredLength} characters`;
    if (errors['number']) return 'Enter a number, e.g. 85000 or 85000.50';
    if (errors['decimals']) return 'At most 2 decimal places';
    if (errors['positive']) return 'Must be greater than zero';
    if (errors['future']) return 'Cannot be in the future';
    return 'Invalid value';
  }

  protected save(): void {
    this.generalError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const request = this.toRequest();
    const call: Observable<Employee> = this.data.employee
      ? this.api.update(this.data.employee.id, request)
      : this.api.create(request);
    call.subscribe({
      next: (saved) => this.dialogRef.close(saved),
      error: (error: unknown) => {
        this.saving.set(false);
        this.showServerErrors(error);
      },
    });
  }

  protected cancel(): void {
    this.dialogRef.close();
  }

  private toRequest(): EmployeeRequest {
    const value = this.form.getRawValue();
    return {
      employeeCode: value.employeeCode.trim(),
      fullName: value.fullName.trim(),
      email: value.email.trim(),
      jobTitle: value.jobTitle.trim(),
      department: value.department.trim(),
      country: value.country,
      salary: Number(value.salary.trim()),
      employmentType: value.employmentType,
      hireDate: value.hireDate,
    };
  }

  /** Puts each server field error on its control; anything else becomes a general message. */
  private showServerErrors(error: unknown): void {
    const problem = error instanceof HttpErrorResponse ? (error.error as Problem | null) : null;
    let shownOnField = false;
    for (const fieldError of problem?.errors ?? []) {
      const control = this.form.get(fieldError.field);
      if (control) {
        control.setErrors({ server: fieldError.message });
        control.markAsTouched();
        shownOnField = true;
      }
    }
    if (!shownOnField) {
      this.generalError.set(
        problem?.detail
          ? `Could not save the employee: ${problem.detail}`
          : 'Could not save the employee. Please try again.',
      );
    }
  }
}

function matching(options: string[], text: string): string[] {
  const needle = text.trim().toLowerCase();
  return needle ? options.filter((option) => option.toLowerCase().includes(needle)) : options;
}
