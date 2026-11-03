import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/** Toggle chip (filters, amount presets). Exposes its state with `aria-pressed`. */
@Component({
  selector: 'app-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      class="inline-flex h-11 items-center gap-1.5 rounded-control border-[1.5px] px-4 text-sm font-bold transition duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-not-allowed disabled:opacity-45 motion-reduce:transition-none"
      [class]="selected() ? 'border-accent bg-accent/10 text-content shadow-glow-accent' : 'border-line bg-surface-raised/40 text-content hover:bg-surface-overlay'"
      [attr.aria-pressed]="selected()"
      [disabled]="disabled()"
      (click)="toggled.emit()"
    >
      <ng-content />
    </button>
  `,
})
export class ChipComponent {
  readonly selected = input(false);
  readonly disabled = input(false);
  readonly toggled = output<void>();
}
