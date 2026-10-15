import { Pipe, PipeTransform } from '@angular/core';
import { Money } from '../core/models';

/** "₹2,400,000" — an amount in its own currency; whole units by default, as salaries are usually read. */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  private readonly formats = new Map<string, Intl.NumberFormat>();

  transform(money: Money | null | undefined, fractionDigits = 0): string {
    if (!money) {
      return '';
    }
    return this.format(money.currency, fractionDigits).format(money.amount);
  }

  private format(currency: string, fractionDigits: number): Intl.NumberFormat {
    const key = `${currency}:${fractionDigits}`;
    let format = this.formats.get(key);
    if (!format) {
      format = new Intl.NumberFormat('en', {
        style: 'currency',
        currency,
        minimumFractionDigits: fractionDigits,
        maximumFractionDigits: fractionDigits,
      });
      this.formats.set(key, format);
    }
    return format;
  }
}
