import { Injectable, Signal } from '@angular/core';

export type SessionStatus = 'loading' | 'authenticated' | 'anonymous';

/** What the app knows about the signed-in person: only what the screens show (never the subject or any token). */
export interface SessionUser {
  readonly name: string;
  readonly email: string;
  readonly picture: string;
}

/** Optional inputs of {@link SessionService.login}. */
export interface LoginOptions {
  /** The e-mail typed on the sign-in page: it only pre-fills the identity provider's form (never a credential). */
  readonly loginHint?: string;
}

/**
 * The app's view of the sign-in session. The rest of the app depends on this class, never on the OIDC library, so the
 * identity provider (or the library) can change in one place. Tokens never leave this boundary except through
 * {@link accessToken}, which the HTTP interceptor uses for requests to the API.
 */
@Injectable()
export abstract class SessionService {
  abstract readonly status: Signal<SessionStatus>;
  abstract readonly user: Signal<SessionUser | null>;
  /** True after the API or the identity provider said the session is no longer valid (shows the "session expired" dialog). */
  abstract readonly expired: Signal<boolean>;

  /** Resolves when the library finished handling a redirect callback / the first session check. */
  abstract whenSettled(): Promise<SessionStatus>;
  abstract login(returnTo?: string, options?: LoginOptions): Promise<void>;
  abstract logout(): Promise<void>;
  /** The access token for the API, or null when there is none. `renew` forces a silent refresh. */
  abstract accessToken(options?: { renew?: boolean }): Promise<string | null>;
  abstract markExpired(): void;
  abstract dismissExpired(): void;
}
