/** Typed error produced by the HTTP boundary. `code`/`message` mirror the backend body `{ code, message }`. */
export interface ApiError {
  readonly status: number;
  readonly code: string;
  readonly message: string;
  readonly requestId?: string;
}

export const GENERIC_ERROR_MESSAGE = 'Something went wrong. Try again.';

export function toApiError(status: number, body: unknown, requestId?: string): ApiError {
  if (status === 0) {
    return { status, code: 'NETWORK', message: 'We could not reach the server. Check your connection.', requestId };
  }
  const record = typeof body === 'object' && body !== null ? (body as Record<string, unknown>) : {};
  const code = typeof record['code'] === 'string' ? record['code'] : `HTTP_${status}`;
  const message = typeof record['message'] === 'string' && record['message'] ? record['message'] : GENERIC_ERROR_MESSAGE;
  return { status, code, message, requestId };
}
