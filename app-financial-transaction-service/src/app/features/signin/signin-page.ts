import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SessionService } from '../../core/auth/session.service';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonDirective } from '../../shared/ui/button.directive';
import { FieldControlDirective, FormFieldComponent } from '../../shared/ui/form-field';

/**
 * Starts the Auth0 sign-in (Authorization Code + PKCE). The optional e-mail only pre-fills Auth0's form; the password is
 * typed on Auth0's page, never here. The provider is always asked to show its form (`prompt=login`).
 */
/** Shape check only (feedback); Auth0 is the one that decides who may sign in. */
const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-signin-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonDirective, AlertComponent, FormFieldComponent, FieldControlDirective],
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
        <form class="grid gap-4 text-left" novalidate (submit)="submit($event)">
          <app-form-field label="E-mail" [optional]="true" hint="It only pre-fills the next step. You type your password on the Auth0 page." [error]="emailError()">
            <input appFieldControl type="email" name="email" autocomplete="username" inputmode="email" [value]="email()" (input)="onEmail($event)" />
          </app-form-field>
          <button appButton type="submit" class="w-full">{{ failed ? 'Try again' : 'Sign in' }}</button>
        </form>
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
  protected readonly email = signal('');
  protected readonly emailError = signal('');

  constructor() {
    effect(() => {
      if (this.session.status() === 'authenticated') void this.router.navigateByUrl('/home', { replaceUrl: true });
    });
  }

  protected onEmail(event: Event): void {
    this.email.set((event.target as HTMLInputElement).value);
    this.emailError.set('');
  }

  protected submit(event: Event): void {
    event.preventDefault();
    const email = this.email().trim();
    if (email !== '' && !EMAIL_SHAPE.test(email)) {
      this.emailError.set('Enter a valid e-mail address, or leave the field empty.');
      return;
    }
    this.redirecting.set(true);
    this.session.login('/home', email === '' ? undefined : { loginHint: email }).catch(() => this.redirecting.set(false));
  }
}
