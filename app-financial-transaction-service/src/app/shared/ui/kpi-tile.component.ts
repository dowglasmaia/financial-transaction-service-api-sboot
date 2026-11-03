import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { IconTileComponent, IconTileTone } from './icon-tile.component';

export type KpiTone = 'neutral' | 'income' | 'expense';

const TILE: Record<KpiTone, IconTileTone> = { neutral: 'accent', income: 'income', expense: 'neutral' };

/** Labelled figure (money in / money out / net). The value comes from the backend; the tile only displays it. */
@Component({
  selector: 'app-kpi-tile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconTileComponent],
  template: `
    <div class="flex flex-col gap-2.5 rounded-card border border-line bg-glass px-[18px] py-4 shadow-card">
      <div class="flex items-center gap-2.5">
        <app-icon-tile [icon]="icon()" [tone]="tile()" size="sm" />
        <p class="min-w-0 text-sm text-content-muted">{{ label() }}</p>
      </div>
      <p class="font-display text-xl font-semibold whitespace-nowrap sm:text-2xl" [class]="valueClass()">{{ value() }}</p>
    </div>
  `,
})
export class KpiTileComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly icon = input('ph-bold ph-chart-line-up');
  readonly tone = input<KpiTone>('neutral');

  protected readonly tile = computed(() => TILE[this.tone()]);
  protected readonly valueClass = computed(() => (this.tone() === 'income' ? 'text-income' : this.tone() === 'expense' ? 'text-expense' : 'text-content'));
}
