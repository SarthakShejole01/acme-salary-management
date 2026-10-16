import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CountryInsight, JobTitleInsight, OrgSummary, SalaryDistribution } from './models';

/** All HTTP access to the insights endpoints. */
@Injectable({ providedIn: 'root' })
export class InsightsApi {
  private readonly http = inject(HttpClient);

  summary(): Observable<OrgSummary> {
    return this.http.get<OrgSummary>('/api/insights/summary');
  }

  countries(): Observable<CountryInsight[]> {
    return this.http.get<CountryInsight[]>('/api/insights/countries');
  }

  jobTitles(countryCode: string): Observable<JobTitleInsight[]> {
    return this.http.get<JobTitleInsight[]>(`/api/insights/countries/${countryCode}/job-titles`);
  }

  distribution(countryCode: string): Observable<SalaryDistribution> {
    return this.http.get<SalaryDistribution>(`/api/insights/countries/${countryCode}/distribution`);
  }
}
