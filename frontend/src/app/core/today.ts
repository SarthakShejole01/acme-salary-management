import { InjectionToken } from '@angular/core';

/**
 * Today's date as yyyy-MM-dd in the user's time zone. Injected (not read directly) so tests can fix the date;
 * the server still has the final say on "not in the future".
 */
export const TODAY = new InjectionToken<() => string>('TODAY', {
  providedIn: 'root',
  factory: () => () => {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
  },
});
