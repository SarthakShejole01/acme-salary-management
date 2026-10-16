import { DecimalPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import {
  BehaviorSubject,
  Observable,
  Subject,
  catchError,
  combineLatest,
  filter,
  forkJoin,
  map,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { InsightsApi } from '../../core/insights-api';
import { CountryInsight, JobTitleInsight, OrgSummary, SalaryDistribution } from '../../core/models';
import { MoneyPipe } from '../../shared/money.pipe';
import { DistributionChart } from './distribution-chart';

interface Load<T> {
  status: 'loading' | 'loaded' | 'error';
  data: T | null;
}

interface Details {
  jobTitles: JobTitleInsight[];
  distribution: SalaryDistribution;
}

const LOADING = { status: 'loading', data: null } as const;
const FAILED = { status: 'error', data: null } as const;

/** Pay insights: org totals in USD (approximate), country comparison, and a per-country drill-down. */
@Component({
  selector: 'app-insights-page',
  imports: [
    DecimalPipe,
    DatePipe,
    MatButtonModule,
    MatCardModule,
    MatProgressBarModule,
    MatTableModule,
    DistributionChart,
    MoneyPipe,
  ],
  templateUrl: './insights-page.html',
  styleUrl: './insights-page.scss',
})
export class InsightsPage {
  private readonly api = inject(InsightsApi);

  /** Statistics shown for each country and job title, all in local currency. */
  protected readonly statColumns = [
    { key: 'min', label: 'Lowest' },
    { key: 'median', label: 'Median' },
    { key: 'average', label: 'Average' },
    { key: 'max', label: 'Highest' },
  ] as const;

  protected readonly countryColumns = [
    'country',
    'headcount',
    ...this.statColumns.map((c) => c.key),
  ];

  protected readonly jobTitleColumns = [
    'jobTitle',
    'headcount',
    ...this.statColumns.map((c) => c.key),
  ];

  protected readonly summary = signal<Load<OrgSummary>>(LOADING);

  protected readonly countries = signal<Load<CountryInsight[]>>(LOADING);

  protected readonly details = signal<Load<Details>>(LOADING);

  private readonly selected$ = new BehaviorSubject<CountryInsight | null>(null);

  protected readonly selected = toSignal(this.selected$, { requireSync: true });

  private readonly retrySummary$ = new Subject<void>();

  private readonly retryCountries$ = new Subject<void>();

  private readonly retryDetails$ = new Subject<void>();

  constructor() {
    this.load(
      this.retrySummary$.pipe(startWith(undefined)),
      () => this.api.summary(),
      this.summary,
    );
    this.load(
      this.retryCountries$.pipe(startWith(undefined)),
      () => this.api.countries(),
      this.countries,
    );

    combineLatest([
      this.selected$.pipe(filter((country) => country !== null)),
      this.retryDetails$.pipe(startWith(undefined)),
    ])
      .pipe(
        tap(() => this.details.set(LOADING)),
        switchMap(([country]) =>
          forkJoin({
            jobTitles: this.api.jobTitles(country.countryCode),
            distribution: this.api.distribution(country.countryCode),
          }).pipe(
            map((details): Load<Details> => ({ status: 'loaded', data: details })),
            catchError(() => of<Load<Details>>(FAILED)),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((state) => this.details.set(state));
  }

  protected select(country: CountryInsight): void {
    this.selected$.next(country);
  }

  protected retrySummary(): void {
    this.retrySummary$.next();
  }

  protected retryCountries(): void {
    this.retryCountries$.next();
  }

  protected retryDetails(): void {
    this.retryDetails$.next();
  }

  /** Each trigger starts a load, replacing any still running; the result lands in the target signal. */
  private load<T>(
    trigger$: Observable<unknown>,
    source: () => Observable<T>,
    target: ReturnType<typeof signal<Load<T>>>,
  ): void {
    trigger$
      .pipe(
        tap(() => target.set(LOADING)),
        switchMap(() =>
          source().pipe(
            map((data): Load<T> => ({ status: 'loaded', data })),
            catchError(() => of<Load<T>>(FAILED)),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((state) => target.set(state));
  }
}
