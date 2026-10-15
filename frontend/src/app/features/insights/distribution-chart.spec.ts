import { TestBed } from '@angular/core/testing';
import { DistributionChart } from './distribution-chart';
import { DistributionBucket } from '../../core/models';

function bucket(from: number, to: number, headcount: number): DistributionBucket {
  return { from: { amount: from, currency: 'GBP' }, to: { amount: to, currency: 'GBP' }, headcount };
}

describe('DistributionChart', () => {
  async function render(buckets: DistributionBucket[]): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(DistributionChart);
    fixture.componentRef.setInput('buckets', buckets);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const buckets = [bucket(10000, 20000, 5), bucket(20000, 30000, 87), bucket(30000, 40000, 0)];

  it('draws_one_bar_per_bucket', async () => {
    const element = await render(buckets);

    expect(element.querySelectorAll('rect.bar')).toHaveLength(3);
  });

  it('scales_bar_heights_to_the_largest_bucket', async () => {
    const element = await render(buckets);
    const heights = Array.from(element.querySelectorAll('rect.bar')).map((bar) => Number(bar.getAttribute('height')));

    expect(heights[1]).toBeGreaterThan(heights[0]);
    expect(heights[0] / heights[1]).toBeCloseTo(5 / 87, 2);
    expect(heights[2]).toBe(0);
  });

  it('names_each_bar_with_its_range_and_headcount_for_hover_and_screen_readers', async () => {
    const element = await render(buckets);

    const label = element.querySelectorAll('rect.bar')[1].querySelector('title')?.textContent;

    expect(label).toBe('£20,000 – £30,000: 87 employees');
  });

  it('uses_the_singular_for_one_employee', async () => {
    const element = await render([bucket(10000, 20000, 1)]);

    expect(element.querySelector('rect.bar title')?.textContent).toBe('£10,000 – £20,000: 1 employee');
  });

  it('labels_the_axis_with_the_bucket_edges', async () => {
    const element = await render(buckets);
    const labels = Array.from(element.querySelectorAll('text.edge')).map((text) => text.textContent?.trim());

    expect(labels).toEqual(['£10,000', '£20,000', '£30,000', '£40,000']);
  });

  it('describes_the_chart_for_screen_readers', async () => {
    const element = await render(buckets);

    expect(element.querySelector('svg')?.getAttribute('role')).toBe('img');
    expect(element.querySelector('svg')?.getAttribute('aria-label')).toBe(
      'Salary distribution: 92 employees in 3 salary ranges',
    );
  });

  it('draws_nothing_without_buckets', async () => {
    const element = await render([]);

    expect(element.querySelector('svg')).toBeNull();
  });
});
