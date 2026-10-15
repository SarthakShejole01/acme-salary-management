import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Employee, EmployeeQuery, Page, ReferenceData } from './models';

/** All HTTP access to the employee endpoints. */
@Injectable({ providedIn: 'root' })
export class EmployeeApi {
  private readonly http = inject(HttpClient);

  search(query: EmployeeQuery): Observable<Page<Employee>> {
    return this.http.get<Page<Employee>>('/api/employees', { params: toParams(query) });
  }

  referenceData(): Observable<ReferenceData> {
    return this.http.get<ReferenceData>('/api/reference-data');
  }
}

function toParams(query: EmployeeQuery): HttpParams {
  let params = new HttpParams().set('page', query.page).set('size', query.size);
  if (query.sort.active && query.sort.direction) {
    params = params.set('sort', `${query.sort.active},${query.sort.direction}`);
  }
  for (const key of ['search', 'country', 'department', 'jobTitle'] as const) {
    const value = query[key].trim();
    if (value) {
      params = params.set(key, value);
    }
  }
  return params;
}
