export interface ApiError {
  readonly status: number;
  readonly message: string;
}

export function toApiError(status: number, message: string): ApiError {
  return { status, message };
}
