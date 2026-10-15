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

/** Body for create and update. Currency is not sent: the server derives it from the country. */
export interface EmployeeRequest {
  employeeCode: string;
  fullName: string;
  email: string;
  jobTitle: string;
  department: string;
  country: string;
  salary: number;
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

/** RFC 7807 body returned by the API for errors; field problems are listed in {@code errors}. */
export interface Problem {
  status: number;
  title?: string;
  detail?: string;
  errors?: { field: string; message: string }[];
}

export interface CountryInsight {
  countryCode: string;
  countryName: string;
  headcount: number;
  min: Money;
  max: Money;
  average: Money;
  median: Money;
}

export interface JobTitleInsight {
  jobTitle: string;
  headcount: number;
  min: Money;
  max: Money;
  average: Money;
  median: Money;
}

export interface DistributionBucket {
  from: Money;
  to: Money;
  headcount: number;
}

export interface SalaryDistribution {
  countryCode: string;
  currency: string;
  buckets: DistributionBucket[];
}

/** Organisation totals in USD at fixed rates: always approximate, valid on {@code ratesAsOf}. */
export interface OrgSummary {
  headcount: number;
  totalAnnualCost: Money;
  averageSalary: Money | null;
  approximate: boolean;
  ratesAsOf: string;
}
