/** Shapes of the backend's JSON. Amounts are numbers with at most two decimals; currency is an ISO code. */

export interface Money {
  amount: number;
  currency: string;
}

export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACTOR';

export interface Employee {
  id: number;
  employeeCode: string;
  fullName: string;
  email: string;
  jobTitle: string;
  department: string;
  country: string;
  salary: Money;
  employmentType: EmploymentType;
  hireDate: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CountryOption {
  code: string;
  name: string;
  currency: string;
}

export interface ReferenceData {
  countries: CountryOption[];
  departments: string[];
  jobTitles: string[];
  employmentTypes: EmploymentType[];
}

export interface SortState {
  /** Property name, or '' for the server's default order. */
  active: string;
  direction: 'asc' | 'desc' | '';
}

export interface EmployeeQuery {
  page: number;
  size: number;
  sort: SortState;
  search: string;
  country: string;
  department: string;
  jobTitle: string;
}
