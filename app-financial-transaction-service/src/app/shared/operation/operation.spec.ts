import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../core/http/api-error';
import { OperationController } from './operation';

const error = (status: number, message = 'x'): ApiError => ({ status, code: `HTTP_${status}`, message });

function controller() {
  let n = 0;
  return new OperationController({ newKey: () => `key-${++n}` });
}

describe('OperationController (the operation shell)', () => {
  it('sends once however many times it is triggered while a request is in flight (double click)', () => {
    const op = controller();
    const response = new Subject<void>();
    const send = vi.fn(() => response);

    expect(op.run(send)).toBe(true);
    expect(op.run(send)).toBe(false);
    expect(op.run(send)).toBe(false);

    expect(send).toHaveBeenCalledTimes(1);
    expect(op.phase()).toBe('pending');
    expect(op.pending()).toBe(true);
  });

  it('ends in success on a 2xx and is free for a new attempt with a NEW key', () => {
    const op = controller();
    const keys: string[] = [];
    op.run((key) => (keys.push(key), of(undefined)));
    expect(op.phase()).toBe('success');

    op.run((key) => (keys.push(key), of(undefined)));
    expect(keys).toEqual(['key-1', 'key-2']);
  });

  it.each([400, 403, 404, 422])('a %s is a business failure that keeps the server message', (status) => {
    const op = controller();
    op.run(() => throwError(() => error(status, 'Operation not performed')));
    expect(op.phase()).toBe('business');
    expect(op.error()?.message).toBe('Operation not performed');
  });

  it.each([0, 500, 502, 503, 504])('a %s (no answer or a server fault) is an uncertain outcome', (status) => {
    const op = controller();
    op.run(() => throwError(() => error(status)));
    expect(op.phase()).toBe('uncertain');
  });

  it('treats a non-API failure (a client timeout) as uncertain', () => {
    const op = controller();
    op.run(() => throwError(() => new Error('timeout')));
    expect(op.phase()).toBe('uncertain');
  });

  it('never retries by itself: one failure means one call', () => {
    const op = controller();
    const send = vi.fn(() => throwError(() => error(0)));
    op.run(send);
    expect(send).toHaveBeenCalledTimes(1);
  });

  it('"Try again" after an uncertain outcome reuses the SAME key and the same request', () => {
    const op = controller();
    const keys: string[] = [];
    let attempt = 0;
    const send = (key: string) => {
      keys.push(key);
      return ++attempt === 1 ? throwError(() => error(0)) : of(undefined);
    };
    op.run(send);
    expect(op.phase()).toBe('uncertain');

    expect(op.retry()).toBe(true);
    expect(keys).toEqual(['key-1', 'key-1']);
    expect(op.phase()).toBe('success');
  });

  it('a fresh run after an uncertain outcome is a new attempt with a new key', () => {
    const op = controller();
    const keys: string[] = [];
    op.run((key) => (keys.push(key), throwError(() => error(0))));
    op.run((key) => (keys.push(key), of(undefined)));
    expect(keys).toEqual(['key-1', 'key-2']);
  });

  it('retry does nothing unless the last outcome was uncertain', () => {
    const op = controller();
    expect(op.retry()).toBe(false);
    op.run(() => of(undefined));
    expect(op.retry()).toBe(false);
  });

  it('reset forgets the outcome and the key', () => {
    const op = controller();
    op.run(() => throwError(() => error(0)));
    op.reset();
    expect(op.phase()).toBe('idle');
    expect(op.retry()).toBe(false);
  });

  it('gives up waiting after the timeout without retrying, as an uncertain outcome', () => {
    vi.useFakeTimers();
    try {
      const op = new OperationController({ newKey: () => 'k', timeoutMs: 50 });
      const send = vi.fn(() => new Subject<void>());
      op.run(send);
      vi.advanceTimersByTime(60);
      expect(op.phase()).toBe('uncertain');
      expect(send).toHaveBeenCalledTimes(1);
    } finally {
      vi.useRealTimers();
    }
  });
});
