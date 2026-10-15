import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NEVER, Observable, of, throwError } from 'rxjs';
import { InsightsPage } from './insights-page';
import { InsightsApi } from '../../core/insights-api';
import {
  CountryInsight,
  JobTitleInsight,
  Money,
  OrgSummary,
  SalaryDistribution,
} from '../../core/models';

const inr = (amount: number): Money => ({ amount, currency: 'INR' });
const usd = (amount: number): Money => ({ amount, currency: 'USD' });

const INDIA: CountryInsight = {
  countryCode: 'IN',
  countryName: 'India',
  headcount: 2512,
  min: inr(508000),
  max: inr(3621000),
  average: inr(1789824.44),
  median: inr(1764000),
};

const US: CountryInsight = {
  countryCode: 'US',
  countryName: 'United States',
  headcount: 3004,
  min: usd(28600),
  max: usd(200800),
  average: usd(100281.96),
  median: usd(98000),
};

const SUMMARY: OrgSummary = {
  headcount: 10000,
  totalAnnualCost: usd(661956638.75),
  averageSalary: usd(66195.66),
  approximate: true,
  ratesAsOf: '2026-10-14',
};

const ENGINEERS: JobTitleInsight = {
  jobTitle: 'Software Engineer',
  headcount: 400,
  min: inr(1500000),
  max: inr(2500000),
  average: inr(1950000),
  median: inr(1940000),
};

const DISTRIBUTION: SalaryDistribution = {
  countryCode: 'IN',
  currency: 'INR',
  buckets: [
    { from: inr(500000), to: inr(1000000), headcount: 206 },
    { from: inr(1000000), to: inr(1500000), headcount: 616 },
  ],
};

class FakeInsightsApi {
  readonly jobTitleRequests: string[] = [];
  readonly distributionRequests: string[] = [];
  summaryResult: () => Observable<OrgSummary> = () => of(SUMMARY);
  countriesResult: () => Observable<CountryInsight[]> = () => of([INDIA, US]);
  jobTitlesResult: (code: string) => Observable<JobTitleInsight[]> = () => of([ENGINEERS]);
  distributionResult: (code: string) => Observable<SalaryDistribution> = () => of(DISTRIBUTION);
  summaryCalls = 0;
  countriesCalls = 0;

  summary(): Observable<OrgSummary> {
    this.summaryCalls++;
    return this.summaryResult();
  }

  countries(): Observable<CountryInsight[]> {
    this.countriesCalls++;
    return this.countriesResult();
  }

  jobTitles(code: string): Observable<JobTitleInsight[]> {
    this.jobTitleRequests.push(code);
    return this.jobTitlesResult(code);
  }

  distribution(code: string): Observable<SalaryDistribution> {
    this.distributionRequests.push(code);
    return this.distributionResult(code);
  }
}

describe('InsightsPage', () => {
  let api: FakeInsightsApi;
  let fixture: ComponentFixture<InsightsPage>;
  let element: HTMLElement;

  beforeEach(() => {
    api = new FakeInsightsApi();
    TestBed.configureTestingModule({
      imports: [InsightsPage],
      providers: [{ provide: InsightsApi, useValue: api }],
    });
  });

  async function render(): Promise<void> {
    fixture = TestBed.createComponent(InsightsPage);
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  }

  async function click(selector: string): Promise<void> {
    (element.querySelector(selector) as HTMLElement).click();
    await fixture.whenStable();
  }

  function text(selector: string): string {
    return element.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  }

  it('shows_org_totals_marked_approximate_with_the_rate_date', async () => {
    await render();

    expect(text('[data-testid="total-cost"]')).toContain('≈ $661,956,639');
    expect(text('[data-testid="average-salary"]')).toContain('≈ $66,196');
    expect(text('[data-testid="headcount"]')).toContain('10,000');
    expect(text('[data-testid="rates-note"]')).toBe(
      'Approximate: each salary converted to USD at fixed exchange rates as of Oct 14, 2026.',
    );
  });

  it('shows_a_dash_for_the_average_when_there_are_no_employees', async () => {
    api.summaryResult = () =>
      of({ ...SUMMARY, headcount: 0, totalAnnualCost: usd(0), averageSalary: null });

    await render();

    expect(text('[data-testid="average-salary"]')).toContain('—');
  });

  it('compares_countries_in_their_own_currencies', async () => {
    await render();

    const rows = Array.from(element.querySelectorAll('tr.mat-mdc-row')).map(
      (row) => row.textContent ?? '',
    );
    expect(rows).toHaveLength(2);
    expect(rows[0]).toContain('India');
    expect(rows[0]).toContain('2,512');
    expect(rows[0]).toContain('₹1,764,000');
    expect(rows[1]).toContain('$98,000');
  });

  it('invites_a_choice_of_country_before_one_is_selected', async () => {
    await render();

    expect(element.textContent).toContain(
      'Select a country to see its job titles and salary distribution',
    );
    expect(api.jobTitleRequests).toEqual([]);
  });

  it('loads_job_titles_and_the_distribution_for_the_selected_country', async () => {
    await render();

    await click('[data-testid="select-IN"]');

    expect(api.jobTitleRequests).toEqual(['IN']);
    expect(api.distributionRequests).toEqual(['IN']);
    expect(text('[data-testid="details-heading"]')).toContain('India');
    expect(text('[data-testid="job-titles"]')).toContain('Software Engineer');
    expect(text('[data-testid="job-titles"]')).toContain('₹1,940,000');
    expect(element.querySelector('app-distribution-chart svg')).not.toBeNull();
  });

  it('marks_the_selected_country', async () => {
    await render();

    await click('[data-testid="select-IN"]');

    expect(element.querySelector('[data-testid="select-IN"]')?.getAttribute('aria-pressed')).toBe(
      'true',
    );
    expect(element.querySelector('[data-testid="select-US"]')?.getAttribute('aria-pressed')).toBe(
      'false',
    );
  });

  it('switches_to_another_country', async () => {
    await render();
    await click('[data-testid="select-IN"]');

    await click('[data-testid="select-US"]');

    expect(api.jobTitleRequests).toEqual(['IN', 'US']);
    expect(text('[data-testid="details-heading"]')).toContain('United States');
  });

  it('shows_progress_while_the_overview_loads', async () => {
    api.summaryResult = () => NEVER;
    api.countriesResult = () => NEVER;

    await render();

    expect(element.querySelectorAll('mat-progress-bar').length).toBeGreaterThanOrEqual(1);
    expect(element.querySelector('[data-testid="total-cost"]')).toBeNull();
  });

  it('offers_a_retry_when_the_summary_fails', async () => {
    let attempt = 0;
    api.summaryResult = () => (++attempt === 1 ? throwError(() => new Error('down')) : of(SUMMARY));
    await render();
    expect(element.textContent).toContain('Could not load the summary');

    await click('[data-testid="retry-summary"]');

    expect(text('[data-testid="total-cost"]')).toContain('≈ $661,956,639');
  });

  it('offers_a_retry_when_the_country_table_fails', async () => {
    let attempt = 0;
    api.countriesResult = () =>
      ++attempt === 1 ? throwError(() => new Error('down')) : of([INDIA]);
    await render();
    expect(element.textContent).toContain('Could not load the country comparison');

    await click('[data-testid="retry-countries"]');

    expect(element.querySelectorAll('tr.mat-mdc-row')).toHaveLength(1);
  });

  it('says_when_there_is_no_data_yet', async () => {
    api.countriesResult = () => of([]);

    await render();

    expect(element.textContent).toContain('No employees yet. Add employees to see pay insights.');
  });

  it('shows_progress_while_a_countrys_details_load', async () => {
    api.jobTitlesResult = () => NEVER;
    api.distributionResult = () => NEVER;
    await render();

    await click('[data-testid="select-IN"]');

    expect(
      element.querySelectorAll('[data-testid="details"] mat-progress-bar').length,
    ).toBeGreaterThanOrEqual(1);
  });

  it('offers_a_retry_when_the_details_fail', async () => {
    let attempt = 0;
    api.jobTitlesResult = () =>
      ++attempt === 1 ? throwError(() => new Error('down')) : of([ENGINEERS]);
    await render();
    await click('[data-testid="select-IN"]');
    expect(element.textContent).toContain('Could not load details for India');

    await click('[data-testid="retry-details"]');

    expect(text('[data-testid="job-titles"]')).toContain('Software Engineer');
  });
});
