import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

/** Mount once in the shell. The live region exists even when empty so screen readers announce the next message. */
@Component({
  selector: 'app-toast-host',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pointer-events-none fixed inset-x-0 bottom-24 z-50 flex justify-center px-4 md:bottom-8" role="status" aria-live="polite">
      @if (toast.message(); as message) {
        <p class="animate-[fade-up_0.2s_ease-out] rounded-control border border-line bg-surface-overlay px-4 py-3 text-sm font-semibold shadow-overlay motion-reduce:animate-none">
          {{ message }}
        </p>
      }
    </div>
  `,
})
export class ToastHostComponent {
  protected readonly toast = inject(ToastService);
}
