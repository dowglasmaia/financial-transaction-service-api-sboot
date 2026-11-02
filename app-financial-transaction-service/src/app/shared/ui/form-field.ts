import { ChangeDetectionStrategy, Component, Directive, InjectionToken, computed, inject, input } from '@angular/core';

export interface FormFieldContext {
  readonly controlId: string;
  readonly describedBy: () => string | null;
  readonly invalid: () => boolean;
}

export const FORM_FIELD = new InjectionToken<FormFieldContext>('FORM_FIELD');

let nextId = 0;

/** Label + control + hint/error with the accessibility wiring (`for`, `aria-describedby`, `aria-invalid`). */
@Component({
  selector: 'app-form-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: FORM_FIELD, useExisting: FormFieldComponent }],
  template: `
    <div class="flex flex-col gap-1.5">
      <label [attr.for]="controlId" class="text-sm font-medium text-content">
        {{ label() }}
        @if (optional()) {
          <span class="font-normal text-content-muted"> (optional)</span>
        }
      </label>
      <ng-content />
      @if (error()) {
        <p [id]="errorId" role="alert" class="flex items-start gap-1.5 text-sm text-expense">
          <i class="ph-bold ph-warning-circle mt-0.5" aria-hidden="true"></i>
          <span>{{ error() }}</span>
        </p>
      } @else if (hint()) {
        <p [id]="hintId" class="text-sm text-content-muted">{{ hint() }}</p>
      }
    </div>
  `,
})
export class FormFieldComponent implements FormFieldContext {
  readonly label = input.required<string>();
  readonly hint = input('');
  readonly error = input('');
  readonly optional = input(false);

  private readonly uid = nextId++;
  readonly controlId = `field-${this.uid}`;
  protected readonly hintId = `field-${this.uid}-hint`;
  protected readonly errorId = `field-${this.uid}-error`;

  readonly invalid = computed(() => this.error() !== '');
  readonly describedBy = computed(() => (this.error() ? this.errorId : this.hint() ? this.hintId : null));
}

/** Styles a native input/textarea/select and connects it to the surrounding `app-form-field`. */
@Directive({
  selector: 'input[appFieldControl], textarea[appFieldControl], select[appFieldControl]',
  host: {
    '[id]': 'field?.controlId',
    '[attr.aria-describedby]': 'field?.describedBy()',
    '[attr.aria-invalid]': 'field?.invalid() ? true : null',
    class:
      'h-12 w-full rounded-control border border-line-input bg-surface-sunken px-3.5 text-[15px] text-content ' +
      'placeholder:text-content-muted focus:border-accent focus-visible:outline-2 focus-visible:outline-offset-2 ' +
      'focus-visible:outline-accent aria-invalid:border-expense disabled:cursor-not-allowed disabled:opacity-45',
  },
})
export class FieldControlDirective {
  protected readonly field = inject(FORM_FIELD, { optional: true });
}
