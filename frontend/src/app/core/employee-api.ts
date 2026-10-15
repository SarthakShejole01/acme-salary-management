import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Employee, EmployeeQuery, EmployeeRequest, Page, ReferenceData } from './models';

/** All HTTP access to the employee endpoints. */
@Injectable({ providedIn: 'root' })
export class EmployeeApi {
  private readonly http = inject(HttpClient);

  search(query: EmployeeQuery): Observable<Page<Employee>> {
    return this.http.get<Page<Employee>>('/api/employees', {
      params: filterParams(query).set('page', query.page).set('size', query.size),
    });
  }

  create(request: EmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>('/api/employees', request);
  }

  update(id: number, request: EmployeeRequest): Observable<Employee> {
    return this.http.put<Employee>(`/api/employees/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/employees/${id}`);
  }

  /** Link for downloading every employee matching the query (filters and sort; no paging) as CSV. */
  exportUrl(query: EmployeeQuery): string {
    const params = filterParams(query).toString();
    return params ? `/api/employees/export.csv?${params}` : '/api/employees/export.csv';
  }

  referenceData(): Observable<ReferenceData> {
    return this.http.get<ReferenceData>('/api/reference-data');
  }
}

/** Sort and non-blank filters, as the list and export endpoints expect them. */
function filterParams(query: EmployeeQuery): HttpParams {
  let params = new HttpParams();
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
