import { Injectable, signal } from '@angular/core';

/** One transient, non-blocking message at a time (e.g. "Transaction id copied"). Never carries anything the user must act on. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly current = signal<string | null>(null);
  private timer: ReturnType<typeof setTimeout> | undefined;

  readonly message = this.current.asReadonly();

  show(message: string, durationMs = 2400): void {
    clearTimeout(this.timer);
    this.current.set(message);
    this.timer = setTimeout(() => this.current.set(null), durationMs);
  }

  dismiss(): void {
    clearTimeout(this.timer);
    this.current.set(null);
  }
}
