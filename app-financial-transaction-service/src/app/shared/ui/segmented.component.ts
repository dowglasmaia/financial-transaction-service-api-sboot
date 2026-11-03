import { ChangeDetectionStrategy, Component, ElementRef, inject, input, model } from '@angular/core';

export interface SegmentedOption {
  readonly value: string;
  readonly label: string;
  readonly icon?: string;
}

/**
 * Single-choice segmented control (key type, period). A radio group: one tab stop, arrow keys move and wrap,
 * the checked state is exposed with `aria-checked`.
 */
@Component({
  selector: 'app-segmented',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div role="radiogroup" [attr.aria-label]="label()" class="flex gap-1 rounded-control border border-line bg-surface-sunken p-1">
      @for (option of options(); track option.value) {
        <button
          type="button"
          role="radio"
          class="flex h-10 flex-1 items-center justify-center gap-1.5 rounded-[9px] px-3.5 text-sm font-semibold transition duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent motion-reduce:transition-none"
          [class]="option.value === value() ? 'bg-linear-to-br from-primary to-primary-strong text-on-primary shadow-glow' : 'text-content-muted hover:text-content'"
          [attr.aria-checked]="option.value === value()"
          [tabindex]="option.value === value() ? 0 : -1"
          (click)="select(option.value)"
          (keydown)="onKeydown($event)"
        >
          @if (option.icon) {
            <i [class]="option.icon" aria-hidden="true"></i>
          }
          {{ option.label }}
        </button>
      }
    </div>
  `,
})
export class SegmentedComponent {
  readonly label = input.required<string>();
  readonly options = input.required<readonly SegmentedOption[]>();
  readonly value = model<string>('');

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected select(value: string): void {
    this.value.set(value);
  }

  protected onKeydown(event: KeyboardEvent): void {
    const options = this.options();
    const index = options.findIndex((option) => option.value === this.value());
    let next = index;
    switch (event.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        next = (index + 1) % options.length;
        break;
      case 'ArrowLeft':
      case 'ArrowUp':
        next = (index - 1 + options.length) % options.length;
        break;
      case 'Home':
        next = 0;
        break;
      case 'End':
        next = options.length - 1;
        break;
      default:
        return;
    }
    event.preventDefault();
    this.value.set(options[next].value);
    queueMicrotask(() => this.host.nativeElement.querySelectorAll<HTMLElement>('[role="radio"]')[next]?.focus());
  }
}
