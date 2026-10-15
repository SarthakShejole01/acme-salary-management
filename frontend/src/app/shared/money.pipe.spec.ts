import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('formats_an_amount_in_its_own_currency_without_decimals', () => {
    expect(pipe.transform({ amount: 98000, currency: 'USD' })).toBe('$98,000');
    expect(pipe.transform({ amount: 2400000, currency: 'INR' })).toBe('₹2,400,000');
    expect(pipe.transform({ amount: 60100, currency: 'EUR' })).toBe('€60,100');
  });

  it('rounds_to_the_nearest_whole_unit', () => {
    expect(pipe.transform({ amount: 66195.66, currency: 'USD' })).toBe('$66,196');
  });

  it('shows_cents_when_asked', () => {
    expect(pipe.transform({ amount: 66195.66, currency: 'USD' }, 2)).toBe('$66,195.66');
  });

  it('shows_nothing_for_a_missing_amount', () => {
    expect(pipe.transform(null)).toBe('');
  });
});
