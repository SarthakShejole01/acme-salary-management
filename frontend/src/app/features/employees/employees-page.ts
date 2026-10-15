import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import {
  BehaviorSubject,
  Subject,
  catchError,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { EmployeeApi } from '../../core/employee-api';
import { Employee, EmployeeQuery, EmploymentType, Page, ReferenceData } from '../../core/models';
import { MoneyPipe } from '../../shared/money.pipe';

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

const NO_REFERENCE_DATA: ReferenceData = { countries: [], departments: [], jobTitles: [], employmentTypes: [] };

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
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule,
    MatButtonModule,
    MoneyPipe,
  ],
  templateUrl: './employees-page.html',
  styleUrl: './employees-page.scss',
})
export class EmployeesPage {
  private readonly api = inject(EmployeeApi);

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
  ];

  protected readonly pageSizes = [20, 50, 100];

  /** The query is a stream so that each change goes straight to the HTTP pipeline. */
  private readonly query$ = new BehaviorSubject<EmployeeQuery>(INITIAL_QUERY);

  protected readonly query = toSignal(this.query$, { requireSync: true });

  protected readonly searchText = signal('');

  protected readonly status = signal<'loading' | 'loaded' | 'error'>('loading');

  protected readonly result = signal<Page<Employee> | null>(null);

  protected readonly referenceData = toSignal(
    this.api.referenceData().pipe(catchError(() => of(NO_REFERENCE_DATA))),
    { initialValue: NO_REFERENCE_DATA },
  );

  private readonly searchInput$ = new Subject<string>();

  private readonly retry$ = new Subject<void>();

  constructor() {
    this.searchInput$
      .pipe(
        debounceTime(300),
        map((text) => text.trim()),
        distinctUntilChanged(),
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

  protected onSearchInput(text: string): void {
    this.searchText.set(text);
    this.searchInput$.next(text);
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
    this.searchText.set('');
    this.update({ search: '', country: '', department: '', jobTitle: '' });
  }

  protected retry(): void {
    this.retry$.next();
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
