import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { InsightsApi } from './insights-api';

describe('InsightsApi', () => {
  let api: InsightsApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(InsightsApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads_the_organisation_summary', () => {
    api.summary().subscribe();

    const call = http.expectOne('/api/insights/summary');
    expect(call.request.method).toBe('GET');
    call.flush({});
  });

  it('loads_pay_statistics_for_every_country', () => {
    api.countries().subscribe();

    http.expectOne('/api/insights/countries').flush([]);
  });

  it('loads_job_title_statistics_for_one_country', () => {
    api.jobTitles('IN').subscribe();

    http.expectOne('/api/insights/countries/IN/job-titles').flush([]);
  });

  it('loads_the_salary_distribution_of_one_country', () => {
    api.distribution('IN').subscribe();

    http.expectOne('/api/insights/countries/IN/distribution').flush({ buckets: [] });
  });
});
