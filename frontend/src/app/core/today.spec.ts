import { TestBed } from '@angular/core/testing';
import { TODAY } from './today';

describe('TODAY', () => {
  afterEach(() => vi.useRealTimers());

  it('gives_the_local_date_as_year_month_day', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 0, 5, 23, 59));

    expect(TestBed.inject(TODAY)()).toBe('2026-01-05');
  });

  it('follows_the_clock_each_time_it_is_called', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 11, 31, 23, 59));
    const today = TestBed.inject(TODAY);
    expect(today()).toBe('2026-12-31');

    vi.setSystemTime(new Date(2027, 0, 1, 0, 1));

    expect(today()).toBe('2027-01-01');
  });
});
