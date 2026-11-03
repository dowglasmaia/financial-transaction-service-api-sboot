import { ChangeDetectionStrategy, Component, ElementRef, HostListener, computed, inject, input, output, signal } from '@angular/core';
import { SessionUser } from '../auth/session.service';

/** Account button in the top bar: who is signed in, the account, and "Sign out" (the shell asks for confirmation). */
@Component({
  selector: 'app-account-menu',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative">
      <button
        type="button"
        class="flex h-11 items-center gap-2.5 rounded-control border border-line bg-surface-raised/70 pr-3 pl-1.5 text-left hover:bg-surface-overlay focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
        aria-haspopup="true"
        [attr.aria-expanded]="open()"
        aria-controls="account-menu-panel"
        (click)="open.set(!open())"
      >
        <span class="grid size-8 shrink-0 place-items-center rounded-full bg-linear-to-br from-primary to-primary-strong text-xs font-bold text-on-primary" aria-hidden="true">{{ initials() }}</span>
        <span class="hidden max-w-[10rem] truncate text-sm font-semibold sm:inline">{{ user().name }}</span>
        <span class="sr-only sm:hidden">Account menu for {{ user().name }}</span>
        <i class="ph-bold ph-caret-down text-xs text-content-muted" aria-hidden="true"></i>
      </button>

      @if (open()) {
        <div id="account-menu-panel" class="absolute right-0 z-40 mt-2 w-72 rounded-card border border-line bg-surface-overlay p-2 shadow-overlay" role="group" aria-label="Account">
          <div class="px-3 py-2">
            <p class="truncate font-semibold">{{ user().name }}</p>
            @if (user().email) {
              <p class="truncate text-sm text-content-muted">{{ user().email }}</p>
            }
            @if (account(); as account) {
              <p class="mt-1 text-sm text-content-muted">Agency {{ account.agency }} · Account {{ account.number }}</p>
            }
          </div>
          <button
            type="button"
            class="flex h-11 w-full items-center gap-3 rounded-control px-3 text-left text-sm font-semibold hover:bg-surface-raised focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-accent"
            (click)="requestSignOut()"
          >
            <i class="ph-bold ph-sign-out text-lg text-accent" aria-hidden="true"></i>
            Sign out
          </button>
        </div>
      }
    </div>
  `,
})
export class AccountMenuComponent {
  readonly user = input.required<SessionUser>();
  readonly account = input<{ agency: string; number: string } | null>(null);
  readonly signOut = output<void>();

  protected readonly open = signal(false);
  protected readonly initials = computed(() =>
    this.user()
      .name.split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join('') || '?',
  );

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected requestSignOut(): void {
    this.open.set(false);
    this.signOut.emit();
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: Event): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) this.open.set(false);
  }

  @HostListener('keydown.escape')
  protected onEscape(): void {
    if (this.open()) {
      this.open.set(false);
      this.host.nativeElement.querySelector<HTMLButtonElement>('button')?.focus();
    }
  }
}
