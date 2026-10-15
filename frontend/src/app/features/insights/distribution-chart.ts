import { Component, computed, input } from '@angular/core';
import { DistributionBucket } from '../../core/models';
import { MoneyPipe } from '../../shared/money.pipe';

const WIDTH = 640;
const HEIGHT = 240;
const PLOT_TOP = 12;
const PLOT_BOTTOM = HEIGHT - 28;
const GAP = 4;

interface Bar {
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
}

interface EdgeLabel {
  x: number;
  text: string;
}

/** Salary histogram as plain SVG: one bar per bucket, scaled to the largest, with a text alternative. */
@Component({
  selector: 'app-distribution-chart',
  template: `
    @if (buckets().length > 0) {
      <svg
        [attr.viewBox]="'0 0 ' + width + ' ' + height"
        role="img"
        [attr.aria-label]="description()"
        preserveAspectRatio="xMidYMid meet"
      >
        @for (bar of bars(); track $index) {
          <rect
            class="bar"
            [attr.x]="bar.x"
            [attr.y]="bar.y"
            [attr.width]="bar.width"
            [attr.height]="bar.height"
          >
            <title>{{ bar.label }}</title>
          </rect>
        }
        @for (edge of edges(); track $index) {
          <text class="edge" [attr.x]="edge.x" [attr.y]="height - 8" text-anchor="middle">
            {{ edge.text }}
          </text>
        }
      </svg>
    }
  `,
  styles: `
    :host {
      display: block;
    }

    svg {
      width: 100%;
      height: auto;
      max-height: 300px;
    }

    .bar {
      fill: var(--mat-sys-primary);
    }

    .bar:hover {
      fill: var(--mat-sys-tertiary);
    }

    .edge {
      font-size: 11px;
      fill: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class DistributionChart {
  readonly buckets = input.required<DistributionBucket[]>();

  protected readonly width = WIDTH;

  protected readonly height = HEIGHT;

  private readonly money = new MoneyPipe();

  protected readonly bars = computed<Bar[]>(() => {
    const buckets = this.buckets();
    const slot = WIDTH / buckets.length;
    const largest = Math.max(...buckets.map((bucket) => bucket.headcount), 1);
    const plotHeight = PLOT_BOTTOM - PLOT_TOP;
    return buckets.map((bucket, index) => {
      const height = (bucket.headcount / largest) * plotHeight;
      return {
        x: index * slot + GAP / 2,
        y: PLOT_BOTTOM - height,
        width: slot - GAP,
        height,
        label: `${this.money.transform(bucket.from)} – ${this.money.transform(bucket.to)}: ${bucket.headcount} ${bucket.headcount === 1 ? 'employee' : 'employees'}`,
      };
    });
  });

  /** Bucket edges: every lower edge plus the last upper edge. */
  protected readonly edges = computed<EdgeLabel[]>(() => {
    const buckets = this.buckets();
    const slot = WIDTH / buckets.length;
    const labels = buckets.map((bucket, index) => ({
      x: index * slot,
      text: this.money.transform(bucket.from),
    }));
    labels.push({ x: WIDTH, text: this.money.transform(buckets[buckets.length - 1].to) });
    return labels;
  });

  protected readonly description = computed(() => {
    const buckets = this.buckets();
    const total = buckets.reduce((sum, bucket) => sum + bucket.headcount, 0);
    return `Salary distribution: ${total} employees in ${buckets.length} salary ranges`;
  });
}
