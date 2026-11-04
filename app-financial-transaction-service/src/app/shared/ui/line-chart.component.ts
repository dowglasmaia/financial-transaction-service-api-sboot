import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { DayPoint, buildChart } from '../util/chart';
import { formatBrl } from '../util/money';

const WIDTH = 600;
const HEIGHT = 160;

/**
 * Hand-made SVG line chart of money in and out per day, with a "View as table" fallback that carries the same numbers
 * (the chart alone is not accessible). It only draws what the backend sent.
 */
@Component({
  selector: 'app-line-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  template: `
    <div class="grid gap-3 rounded-card border border-line bg-glass p-4 shadow-card">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <h3 class="font-display text-lg font-semibold">Money in and out</h3>
        <button type="button" class="text-sm font-semibold text-accent underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent" [attr.aria-pressed]="asTable()" (click)="asTable.set(!asTable())">
          {{ asTable() ? 'View as chart' : 'View as table' }}
        </button>
      </div>

      @if (asTable()) {
        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm">
            <caption class="sr-only">Incoming and outgoing money per day</caption>
            <thead class="text-content-muted">
              <tr>
                <th scope="col" class="py-2 pr-3 font-semibold">Day</th>
                <th scope="col" class="py-2 pr-3 text-right font-semibold">Incoming</th>
                <th scope="col" class="py-2 text-right font-semibold">Outgoing</th>
              </tr>
            </thead>
            <tbody>
              @for (day of days(); track day.date) {
                <tr class="border-t border-line">
                  <th scope="row" class="py-2 pr-3 font-normal">{{ day.date | date: 'MMM d' }}</th>
                  <td class="py-2 pr-3 text-right">{{ money(day.incoming) }}</td>
                  <td class="py-2 text-right">{{ money(day.outgoing) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      } @else {
        <svg [attr.viewBox]="'0 0 ' + width + ' ' + height" class="h-40 w-full overflow-visible" role="img" [attr.aria-label]="summary()">
          <line x1="0" [attr.y1]="height" [attr.x2]="width" [attr.y2]="height" class="stroke-line" stroke-width="1" />
          <path [attr.d]="chart().incomingPath" fill="none" class="stroke-income" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round" />
          <path [attr.d]="chart().outgoingPath" fill="none" class="stroke-expense" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round" stroke-dasharray="6 4" />
        </svg>
        <ul class="flex flex-wrap gap-4 text-sm text-content-muted" aria-label="Legend">
          <li class="flex items-center gap-2"><span class="inline-block h-0.5 w-6 bg-income" aria-hidden="true"></span> Incoming (solid)</li>
          <li class="flex items-center gap-2"><span class="inline-block h-0.5 w-6 border-t-2 border-dashed border-expense" aria-hidden="true"></span> Outgoing (dashed)</li>
        </ul>
      }
    </div>
  `,
})
export class LineChartComponent {
  readonly days = input.required<readonly DayPoint[]>();

  protected readonly width = WIDTH;
  protected readonly height = HEIGHT;
  protected readonly asTable = signal(false);
  protected readonly chart = computed(() => buildChart(this.days(), WIDTH, HEIGHT));
  protected readonly summary = computed(() => `Line chart of incoming and outgoing money for ${this.days().length} days. Use "View as table" for the values.`);
  protected readonly money = (value: number) => formatBrl(value ?? 0);
}
