import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Progress through a short flow (Pix: Key → Amount → Review). State is in text and `aria-current`, never colour alone. */
@Component({
  selector: 'app-stepper',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p class="sr-only">Step {{ current() }} of {{ steps().length }}</p>
    <ol class="flex min-w-0 items-center gap-2 sm:gap-3">
      @for (step of steps(); track step; let i = $index; let last = $last) {
        <li
          class="flex min-w-0 items-center gap-2 sm:gap-3"
          [class.flex-1]="!last"
          [attr.aria-current]="i + 1 === current() ? 'step' : null"
        >
          <span
            class="grid size-[34px] shrink-0 place-items-center rounded-full font-display text-sm font-semibold"
            [class]="
              i + 1 < current()
                ? 'bg-income text-on-accent'
                : i + 1 === current()
                  ? 'bg-linear-to-br from-primary to-accent text-on-primary shadow-glow'
                  : 'bg-surface-overlay text-content-muted'
            "
          >
            @if (i + 1 < current()) {
              <i class="ph-bold ph-check" aria-hidden="true"></i>
              <span class="sr-only">completed: </span>
            } @else {
              {{ i + 1 }}
            }
          </span>
          <span
            class="truncate text-sm font-semibold"
            [class]="
              (i + 1 <= current() ? 'text-content' : 'text-content-muted') +
              (i + 1 === current() ? '' : ' max-sm:sr-only')
            "
            >{{ step }}</span
          >
          @if (!last) {
            <span
              class="h-0.5 min-w-4 flex-1 rounded-full"
              [class]="i + 1 < current() ? 'bg-income' : 'bg-line'"
              aria-hidden="true"
            ></span>
          }
        </li>
      }
    </ol>
  `,
})
export class StepperComponent {
  readonly steps = input.required<readonly string[]>();
  /** 1-based index of the current step. */
  readonly current = input.required<number>();
}
