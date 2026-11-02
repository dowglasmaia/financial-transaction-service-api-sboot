import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/** Toggle chip (filters, amount presets). Exposes its state with `aria-pressed`. */
@Component({
  selector: 'app-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      class="inline-flex h-11 items-center gap-1.5 rounded-control border px-4 text-sm font-medium transition duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-not-allowed disabled:opacity-45"
      [class]="selected() ? 'border-accent bg-surface-overlay text-accent' : 'border-line bg-transparent text-content-muted hover:bg-surface-sunken hover:text-content'"
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
