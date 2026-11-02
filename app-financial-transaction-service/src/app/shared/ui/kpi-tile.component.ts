import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type KpiTone = 'neutral' | 'income' | 'expense';

/** Labelled figure (money in / money out / net). The value comes from the backend; the tile only displays it. */
@Component({
  selector: 'app-kpi-tile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="rounded-card border border-line bg-surface-raised p-4 shadow-card">
      <p class="flex items-center gap-1.5 text-sm text-content-muted">
        <i [class]="icon()" aria-hidden="true" [class.text-income]="tone() === 'income'" [class.text-expense]="tone() === 'expense'"></i>
        {{ label() }}
      </p>
      <p class="mt-1 font-display text-2xl font-semibold" [class]="valueClass()">{{ value() }}</p>
    </div>
  `,
})
export class KpiTileComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly icon = input('ph-bold ph-chart-line-up');
  readonly tone = input<KpiTone>('neutral');

  protected readonly valueClass = computed(() => (this.tone() === 'income' ? 'text-income' : this.tone() === 'expense' ? 'text-expense' : 'text-content'));
}
