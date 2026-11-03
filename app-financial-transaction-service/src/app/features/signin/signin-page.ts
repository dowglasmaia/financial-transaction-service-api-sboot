import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SessionService } from '../../core/auth/session.service';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonDirective } from '../../shared/ui/button.directive';

/** Starts the Auth0 sign-in (Authorization Code + PKCE). The password is typed on Auth0's page, never here. */
@Component({
  selector: 'app-signin-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonDirective, AlertComponent],
  template: `
    <section class="mx-auto grid max-w-md gap-5 rounded-panel border border-line bg-glass p-8 text-center shadow-card" aria-labelledby="signin-title">
      <span class="mx-auto grid size-[60px] place-items-center rounded-[18px] bg-linear-to-br from-accent via-primary to-violet text-3xl text-on-primary shadow-glow-accent" aria-hidden="true">
        <i class="ph-bold ph-lock-simple"></i>
      </span>
      <div>
        <h1 id="signin-title" class="font-display text-2xl font-bold">Welcome back</h1>
        <p class="mt-1 text-content-muted">Sign in to see your balance and move your money.</p>
      </div>

      @if (failed) {
        <app-alert kind="error" title="We couldn't sign you in">Something went wrong on the way back. Try again.</app-alert>
      }

      @if (redirecting()) {
        <p class="flex items-center justify-center gap-2 font-semibold" role="status">
          <i class="ph-bold ph-circle-notch animate-spin motion-reduce:animate-none" aria-hidden="true"></i>
          Redirecting…
        </p>
      } @else {
        <button appButton type="button" class="w-full" (click)="signIn()">{{ failed ? 'Try again' : 'Sign in' }}</button>
      }

      <p class="flex items-center justify-center gap-2 text-sm text-content-muted">
        <i class="ph-bold ph-shield-check text-accent" aria-hidden="true"></i>
        Secure sign-in with Auth0
      </p>
      <a routerLink="/" class="text-sm font-semibold text-accent">Back to the start page</a>
    </section>
  `,
})
export class SignInPage {
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  protected readonly failed = inject(ActivatedRoute).snapshot.queryParamMap.has('failed');
  protected readonly redirecting = signal(false);

  constructor() {
    effect(() => {
      if (this.session.status() === 'authenticated') void this.router.navigateByUrl('/home', { replaceUrl: true });
    });
  }

  protected signIn(): void {
    this.redirecting.set(true);
    this.session.login('/home').catch(() => this.redirecting.set(false));
  }
}
