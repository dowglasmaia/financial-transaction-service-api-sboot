import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, Injector, afterNextRender, effect, inject, input, model, untracked } from '@angular/core';

let nextId = 0;

const FOCUSABLE =
  'button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Modal dialog (sign out, review before confirming, session expired). Modal semantics, focus moves inside on open,
 * Tab is trapped, Escape and the backdrop close it and the focus returns to whatever opened it.
 * Put the action buttons in the default slot.
 */
@Component({
  selector: 'app-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (open()) {
      <div class="fixed inset-0 z-50 flex items-end justify-center p-4 sm:items-center">
        <div data-dialog-backdrop class="absolute inset-0 bg-black/60 backdrop-blur-sm" aria-hidden="true" (click)="dismissible() && close()"></div>
        <div
          role="dialog"
          aria-modal="true"
          tabindex="-1"
          class="relative w-full max-w-md animate-[fade-up_0.25s_ease-out] rounded-panel border border-line bg-surface-overlay p-6 shadow-overlay motion-reduce:animate-none"
          [attr.aria-labelledby]="titleId"
          [attr.aria-describedby]="description() ? descriptionId : null"
          (keydown)="onKeydown($event)"
        >
          <h2 [id]="titleId" class="font-display text-xl font-semibold">{{ title() }}</h2>
          @if (description()) {
            <p [id]="descriptionId" class="mt-2 text-content-muted">{{ description() }}</p>
          }
          <div class="mt-5 flex flex-wrap justify-end gap-3"><ng-content /></div>
        </div>
      </div>
    }
  `,
})
export class DialogComponent {
  readonly open = model(false);
  readonly title = input.required<string>();
  readonly description = input('');
  /** False while something must finish first (a request in flight): Escape and the backdrop then do nothing. */
  readonly dismissible = input(true);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly document = inject(DOCUMENT);
  private readonly injector = inject(Injector);

  private readonly uid = nextId++;
  protected readonly titleId = `dialog-${this.uid}-title`;
  protected readonly descriptionId = `dialog-${this.uid}-description`;

  private opener: HTMLElement | null = null;
  private wasOpen = false;

  constructor() {
    effect(() => {
      const isOpen = this.open();
      untracked(() => {
        if (isOpen && !this.wasOpen) {
          this.opener = this.document.activeElement as HTMLElement | null;
          afterNextRender(() => this.focusFirst(), { injector: this.injector });
        } else if (!isOpen && this.wasOpen) {
          this.opener?.focus();
          this.opener = null;
        }
        this.wasOpen = isOpen;
      });
    });
  }

  protected close(): void {
    this.open.set(false);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.stopPropagation();
      if (this.dismissible()) this.close();
      return;
    }
    if (event.key !== 'Tab') return;

    const items = this.focusables();
    if (items.length === 0) {
      event.preventDefault();
      return;
    }
    const first = items[0];
    const last = items[items.length - 1];
    const active = this.document.activeElement;
    if (event.shiftKey && (active === first || active === this.panel())) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  private panel(): HTMLElement | null {
    return this.host.nativeElement.querySelector<HTMLElement>('[role="dialog"]');
  }

  private focusables(): HTMLElement[] {
    return Array.from(this.panel()?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? []);
  }

  private focusFirst(): void {
    (this.focusables()[0] ?? this.panel())?.focus();
  }
}
