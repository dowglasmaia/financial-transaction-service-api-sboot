import { computed, signal } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { ApiError } from '../../core/http/api-error';

/** idle → pending → one of the three outcomes the result panel knows. */
export type OperationPhase = 'idle' | 'pending' | 'success' | 'business' | 'uncertain';

export interface OperationOptions {
  /** Makes the `Idempotency-Key` of a user attempt. */
  readonly newKey?: () => string;
  /** How long to wait for an answer before the outcome becomes "uncertain" (the request is not repeated). */
  readonly timeoutMs?: number;
}

const DEFAULT_TIMEOUT_MS = 30_000;

const isApiError = (error: unknown): error is ApiError =>
  typeof error === 'object' && error !== null && typeof (error as ApiError).status === 'number';

/**
 * The operation shell shared by every money flow (withdrawal, deposit, recharge, Pix).
 *
 * Guarantees: at most ONE request in flight (a double click is ignored), and NO automatic retry of any kind. Each user
 * attempt gets its own `Idempotency-Key`; the same key is sent again only when the user presses "Try again" after an
 * uncertain outcome ({@link retry}), so the server can recognise it and never move the money twice.
 *
 * Outcomes: 2xx → success; an answer in the 4xx range → business failure (the server's message is shown, nothing moved);
 * no answer, a timeout or a 5xx → uncertain (the customer must check the statement before trying again).
 */
export class OperationController {
  readonly phase = signal<OperationPhase>('idle');
  readonly error = signal<ApiError | null>(null);
  readonly pending = computed(() => this.phase() === 'pending');

  private readonly newKey: () => string;
  private readonly timeoutMs: number;
  private attemptKey: string | null = null;
  private lastSend: ((key: string) => Observable<unknown>) | null = null;
  private inFlight = false;

  constructor(options: OperationOptions = {}) {
    this.newKey = options.newKey ?? (() => crypto.randomUUID());
    this.timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  }

  /** A fresh user attempt with a NEW key. Returns false (and sends nothing) while another request is in flight. */
  run(send: (key: string) => Observable<unknown>): boolean {
    if (this.inFlight) return false;
    this.attemptKey = this.newKey();
    this.lastSend = send;
    return this.dispatch();
  }

  /** The explicit "Try again" after an uncertain outcome: the same key, the same request. */
  retry(): boolean {
    if (this.inFlight || this.phase() !== 'uncertain' || !this.lastSend || !this.attemptKey) return false;
    return this.dispatch();
  }

  /** Back to the form: the next attempt starts clean. */
  reset(): void {
    if (this.inFlight) return;
    this.phase.set('idle');
    this.error.set(null);
    this.attemptKey = null;
    this.lastSend = null;
  }

  private dispatch(): boolean {
    const send = this.lastSend!;
    const key = this.attemptKey!;
    this.inFlight = true;
    this.error.set(null);
    this.phase.set('pending');
    send(key)
      .pipe(timeout(this.timeoutMs))
      .subscribe({
        next: () => undefined,
        complete: () => this.finish('success'),
        error: (error: unknown) => {
          if (isApiError(error)) {
            this.error.set(error);
            this.finish(error.status === 0 || error.status >= 500 ? 'uncertain' : 'business');
          } else {
            this.finish('uncertain');
          }
        },
      });
    return true;
  }

  private finish(phase: OperationPhase): void {
    this.inFlight = false;
    this.phase.set(phase);
  }
}
