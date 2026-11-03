import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type IconTileTone = 'income' | 'accent' | 'violet' | 'warning' | 'neutral';

// Full class names are listed so Tailwind can detect them.
const TONES: Record<IconTileTone, string> = {
  income: 'bg-income/15 text-income',
  accent: 'bg-accent/15 text-accent',
  violet: 'bg-violet/15 text-violet',
  warning: 'bg-warning/15 text-warning',
  neutral: 'bg-surface-overlay text-content-muted',
};

/** Rounded icon tile (ledger kind, feature, quick action). Decorative: the label next to it carries the meaning. */
@Component({
  selector: 'app-icon-tile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span aria-hidden="true" class="grid shrink-0 place-items-center rounded-control" [class]="classes()">
      <i [class]="icon()"></i>
    </span>
  `,
})
export class IconTileComponent {
  readonly icon = input.required<string>();
  readonly tone = input<IconTileTone>('neutral');
  readonly size = input<'sm' | 'md' | 'lg'>('md');

  protected readonly classes = computed(
    () => `${TONES[this.tone()]} ${this.size() === 'sm' ? 'size-9 text-base' : this.size() === 'lg' ? 'size-14 text-2xl' : 'size-[42px] text-xl'}`,
  );
}
