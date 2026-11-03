import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SessionService } from '../../core/auth/session.service';
import { ButtonDirective } from '../../shared/ui/button.directive';
import { IconTileComponent, IconTileTone } from '../../shared/ui/icon-tile.component';

interface Feature {
  readonly icon: string;
  readonly tone: IconTileTone;
  readonly title: string;
  readonly text: string;
}

const FEATURES: readonly Feature[] = [
  { icon: 'ph-fill ph-chart-bar', tone: 'accent', title: 'Total control', text: 'Follow your spending and income in one place.' },
  { icon: 'ph-fill ph-shield-check', tone: 'violet', title: 'Secure transactions', text: 'Sign-in handled by a dedicated identity provider.' },
  { icon: 'ph-fill ph-lightning', tone: 'income', title: 'Fast payments', text: 'Pix, deposits and mobile recharge, with clear results.' },
];

/**
 * Public landing page. It is also where the identity provider sends the browser back to (`/?code=…&state=…`):
 * while that callback is being handled it shows "Signing you in…", then moves a signed-in customer to Home.
 */
@Component({
  selector: 'app-landing-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonDirective, IconTileComponent],
  template: `
    @if (finishingSignIn) {
      <section class="grid min-h-[50vh] place-items-center" aria-live="polite" role="status">
        <p class="flex items-center gap-3 font-display text-xl font-semibold">
          <i class="ph-bold ph-circle-notch animate-spin motion-reduce:animate-none" aria-hidden="true"></i>
          Signing you in…
        </p>
      </section>
    } @else {
      <section class="relative overflow-hidden rounded-panel border border-white/25 bg-hero px-6 py-12 text-on-primary shadow-overlay sm:px-12 sm:py-16" aria-labelledby="landing-title">
        <i class="ph-bold ph-bank pointer-events-none absolute -right-10 -bottom-16 text-[320px] text-white/10" aria-hidden="true"></i>
        <div class="relative flex max-w-xl flex-col gap-5">
          <p class="text-xs font-bold tracking-[0.14em] uppercase text-on-primary/80">Maia Bank · demo</p>
          <h1 id="landing-title" class="font-display text-4xl leading-tight font-extrabold sm:text-5xl">Your financial life, simpler and safer.</h1>
          <p class="text-lg text-on-primary/85">Manage your money, make transactions and keep track of everything in one place.</p>
          <div>
            <a appButton routerLink="/signin" class="bg-white! from-white! to-white! text-primary-strong! shadow-none!">
              Access my account
              <i class="ph-bold ph-arrow-right" aria-hidden="true"></i>
            </a>
          </div>
        </div>
      </section>

      <ul class="mt-6 grid gap-4 sm:grid-cols-3" aria-label="What you can do">
        @for (feature of features; track feature.title) {
          <li class="flex items-start gap-3.5 rounded-card border border-line bg-glass p-5 shadow-card">
            <app-icon-tile [icon]="feature.icon" [tone]="feature.tone" />
            <div>
              <h2 class="font-display text-base font-semibold">{{ feature.title }}</h2>
              <p class="mt-1 text-sm text-content-muted">{{ feature.text }}</p>
            </div>
          </li>
        }
      </ul>

      <p class="mt-6 flex items-center gap-2 text-sm text-content-muted">
        <i class="ph ph-flask" aria-hidden="true"></i>
        Demo environment · test data only. Maia Bank is fictitious, not a real bank.
      </p>
    }
  `,
})
export class LandingPage {
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly query = inject(ActivatedRoute).snapshot.queryParamMap;

  protected readonly features = FEATURES;
  /** The identity provider has just redirected back with an authorization code. */
  protected readonly finishingSignIn = this.query.has('code') && this.query.has('state');

  constructor() {
    if (this.query.has('error')) void this.router.navigate(['/signin'], { queryParams: { failed: 1 }, replaceUrl: true });
    effect(() => {
      if (this.session.status() === 'authenticated') void this.router.navigateByUrl('/home', { replaceUrl: true });
    });
  }
}
