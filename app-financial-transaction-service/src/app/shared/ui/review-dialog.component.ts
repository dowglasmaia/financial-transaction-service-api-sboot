import { ChangeDetectionStrategy, Component, input, model, output } from '@angular/core';
import { ButtonDirective } from './button.directive';
import { DialogComponent } from './dialog.component';

export interface ReceiptLine {
  readonly label: string;
  readonly value: string;
}

/**
 * "Review before you confirm" for every money flow: the receipt lines, Back and Confirm. While the request is pending
 * Confirm shows the spinner and is inert and the dialog cannot be dismissed, so one click means one request.
 *
 * The page owns the request (see `OperationController`); this component only asks: `confirmed` is emitted on Confirm.
 */
@Component({
  selector: 'app-review-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DialogComponent, ButtonDirective],
  template: `
    <app-dialog [(open)]="open" [title]="title()" [dismissible]="!pending()">
      <dl class="mb-1 w-full divide-y divide-line rounded-control border border-line bg-surface-sunken px-4">
        @for (line of lines(); track line.label) {
          <div class="flex justify-between gap-3 py-2.5 text-[15px]">
            <dt class="text-content-muted">{{ line.label }}</dt>
            <dd class="text-right font-bold tabular-nums">{{ line.value }}</dd>
          </div>
        }
      </dl>
      <button appButton variant="secondary" type="button" [disabled]="pending()" (click)="open.set(false)">Back</button>
      <button appButton type="button" [loading]="pending()" (click)="confirmed.emit()">{{ confirmLabel() }}</button>
    </app-dialog>
  `,
})
export class ReviewDialogComponent {
  readonly open = model(false);
  readonly title = input.required<string>();
  readonly lines = input.required<readonly ReceiptLine[]>();
  readonly pending = input(false);
  readonly confirmLabel = input('Confirm');
  readonly confirmed = output<void>();
}
