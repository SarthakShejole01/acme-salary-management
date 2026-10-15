import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmployeeApi } from './employee-api';
import { EmployeeQuery, Page, Employee, ReferenceData } from './models';

describe('EmployeeApi', () => {
  let api: EmployeeApi;
  let http: HttpTestingController;

  const query: EmployeeQuery = {
    page: 2,
    size: 50,
    sort: { active: 'salary', direction: 'desc' },
    search: 'sharma',
    country: 'IN',
    department: 'Engineering',
    jobTitle: 'Software Engineer',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(EmployeeApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests_a_page_of_employees_with_paging_sort_and_filters', () => {
    api.search(query).subscribe();

    const request = http.expectOne((r) => r.url === '/api/employees');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('50');
    expect(request.request.params.get('sort')).toBe('salary,desc');
    expect(request.request.params.get('search')).toBe('sharma');
    expect(request.request.params.get('country')).toBe('IN');
    expect(request.request.params.get('department')).toBe('Engineering');
    expect(request.request.params.get('jobTitle')).toBe('Software Engineer');
    request.flush(emptyPage());
  });

  it('leaves_out_blank_filters_and_an_unset_sort', () => {
    api
      .search({ ...query, sort: { active: '', direction: '' }, search: '  ', country: '', department: '', jobTitle: '' })
      .subscribe();

    const request = http.expectOne((r) => r.url === '/api/employees');
    expect(request.request.params.keys().sort()).toEqual(['page', 'size']);
    request.flush(emptyPage());
  });

  it('returns_the_page_sent_by_the_server', () => {
    let received: Page<Employee> | undefined;
    api.search(query).subscribe((page) => (received = page));

    http.expectOne((r) => r.url === '/api/employees').flush({ ...emptyPage(), totalElements: 7 });

    expect(received?.totalElements).toBe(7);
  });

  it('loads_reference_data_for_the_filters', () => {
    let received: ReferenceData | undefined;
    api.referenceData().subscribe((data) => (received = data));

    http.expectOne('/api/reference-data').flush({
      countries: [{ code: 'IN', name: 'India', currency: 'INR' }],
      departments: ['Engineering'],
      jobTitles: ['Software Engineer'],
      employmentTypes: ['FULL_TIME'],
    });

    expect(received?.countries[0].name).toBe('India');
  });

  function emptyPage(): Page<Employee> {
    return { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
  }
});
