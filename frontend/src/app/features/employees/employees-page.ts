import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  BehaviorSubject,
  Subject,
  catchError,
  combineLatest,
  debounceTime,
  filter,
  map,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { EmployeeApi } from '../../core/employee-api';
import { Employee, EmployeeQuery, EmploymentType, Page, ReferenceData } from '../../core/models';
import { MoneyPipe } from '../../shared/money.pipe';
import { ConfirmDialog, ConfirmDialogData } from '../../shared/confirm-dialog';
import { EmployeeFormData, EmployeeFormDialog } from './employee-form-dialog';

type Filter = 'country' | 'department' | 'jobTitle';

const INITIAL_QUERY: EmployeeQuery = {
  page: 0,
  size: 20,
  sort: { active: '', direction: '' },
  search: '',
  country: '',
  department: '',
  jobTitle: '',
};

const NO_REFERENCE_DATA: ReferenceData = {
  countries: [],
  departments: [],
  jobTitles: [],
  employmentTypes: [],
};

const EMPLOYMENT_TYPE_LABELS: Record<EmploymentType, string> = {
  FULL_TIME: 'Full-time',
  PART_TIME: 'Part-time',
  CONTRACTOR: 'Contractor',
};

/** Searchable, filterable employee list. Paging, sorting and filtering all happen on the server. */
@Component({
  selector: 'app-employees-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MoneyPipe,
  ],
  templateUrl: './employees-page.html',
  styleUrl: './employees-page.scss',
})
export class EmployeesPage {
  private readonly api = inject(EmployeeApi);

  private readonly dialog = inject(MatDialog);

  private readonly snackBar = inject(MatSnackBar);

  protected readonly columns = [
    'employeeCode',
    'fullName',
    'email',
    'jobTitle',
    'department',
    'country',
    'salary',
    'employmentType',
    'hireDate',
    'actions',
  ];

  protected readonly pageSizes = [20, 50, 100];

  /** The query is a stream so that each change goes straight to the HTTP pipeline. */
  private readonly query$ = new BehaviorSubject<EmployeeQuery>(INITIAL_QUERY);

  protected readonly query = toSignal(this.query$, { requireSync: true });

  protected readonly searchControl = new FormControl('', { nonNullable: true });

  protected readonly status = signal<'loading' | 'loaded' | 'error'>('loading');

  protected readonly result = signal<Page<Employee> | null>(null);

  /** Loaded at start, again after a save (a new department or job title may now exist) and by Retry. */
  private readonly loadReferenceData$ = new Subject<void>();

  protected readonly referenceData = signal<ReferenceData>(NO_REFERENCE_DATA);

  /** Add and edit need the options, so they stay disabled until they have loaded. */
  protected readonly referenceStatus = signal<'loading' | 'loaded' | 'error'>('loading');

  protected readonly exportUrl = computed(() => this.api.exportUrl(this.query()));

  private readonly retry$ = new Subject<void>();

  constructor() {
    this.loadReferenceData$
      .pipe(
        startWith(undefined),
        tap(() => {
          if (this.referenceStatus() !== 'loaded') {
            this.referenceStatus.set('loading');
          }
        }),
        switchMap(() =>
          this.api.referenceData().pipe(
            map((data): ReferenceData | null => data),
            catchError(() => of(null)),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((data) => {
        if (data) {
          this.referenceData.set(data);
          this.referenceStatus.set('loaded');
        } else if (this.referenceStatus() !== 'loaded') {
          // A failed refresh keeps the options already known; only a failed first load is an error.
          this.referenceStatus.set('error');
        }
      });

    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        map((text) => text.trim()),
        // Compare with the current query rather than the previous keystroke, so that retyping a search
        // after "Clear filters" still runs it.
        filter((search) => search !== this.query$.value.search),
        takeUntilDestroyed(),
      )
      .subscribe((search) => this.update({ search }));

    combineLatest([this.query$, this.retry$.pipe(startWith(undefined))])
      .pipe(
        tap(() => this.status.set('loading')),
        switchMap(([query]) =>
          this.api.search(query).pipe(
            map((page): Page<Employee> | null => page),
            catchError(() => of(null)),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((page) => {
        if (page) {
          this.result.set(page);
          this.status.set('loaded');
        } else {
          this.status.set('error');
        }
      });
  }

  protected filterBy(filter: Filter, value: string): void {
    this.update({ [filter]: value });
  }

  protected onSort(sort: Sort): void {
    this.update({ sort: { active: sort.direction ? sort.active : '', direction: sort.direction } });
  }

  protected onPage(event: PageEvent): void {
    this.query$.next({ ...this.query$.value, page: event.pageIndex, size: event.pageSize });
  }

  protected clearFilters(): void {
    this.searchControl.setValue('', { emitEvent: false });
    this.update({ search: '', country: '', department: '', jobTitle: '' });
  }

  protected retry(): void {
    this.retry$.next();
  }

  protected retryReferenceData(): void {
    this.loadReferenceData$.next();
  }

  protected add(): void {
    this.openForm(undefined);
  }

  protected edit(employee: Employee): void {
    this.openForm(employee);
  }

  protected remove(employee: Employee): void {
    const data: ConfirmDialogData = {
      title: 'Delete employee',
      message: `Delete ${employee.fullName} (${employee.employeeCode})? This cannot be undone.`,
      confirmLabel: 'Delete',
    };
    this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, { data })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed === true),
        switchMap(() => this.api.delete(employee.id)),
      )
      .subscribe({
        next: () => {
          this.snackBar.open(`Deleted ${employee.fullName}`, undefined, { duration: 4000 });
          this.retry$.next();
        },
        error: () =>
          this.snackBar.open(`Could not delete ${employee.fullName}. Please try again.`, 'Dismiss'),
      });
  }

  private openForm(employee: Employee | undefined): void {
    const data: EmployeeFormData = { employee, referenceData: this.referenceData() };
    this.dialog
      .open<EmployeeFormDialog, EmployeeFormData, Employee>(EmployeeFormDialog, {
        data,
        width: '640px',
      })
      .afterClosed()
      .pipe(filter((saved): saved is Employee => saved !== undefined))
      .subscribe((saved) => {
        this.snackBar.open(`Saved ${saved.fullName}`, undefined, { duration: 4000 });
        this.retry$.next();
        this.loadReferenceData$.next();
      });
  }

  protected employmentTypeLabel(type: EmploymentType): string {
    return EMPLOYMENT_TYPE_LABELS[type];
  }

  protected isEmpty(): boolean {
    return this.status() === 'loaded' && this.result()?.totalElements === 0;
  }

  /** Any change other than paging starts again from the first page. */
  private update(changes: Partial<EmployeeQuery>): void {
    this.query$.next({ ...this.query$.value, ...changes, page: 0 });
  }
}
