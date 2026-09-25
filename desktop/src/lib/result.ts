/**
 * `Result<T, E>` — a tiny Rust-style sum type for operations that can fail
 * in expected ways (file-not-found, network down, IPC timeout, …).
 *
 * Throwing is reserved for genuine bugs (invariant violations); for any
 * failure a caller might want to handle, return a Result.
 */

export type Result<T, E = Error> =
  | { ok: true; value: T }
  | { ok: false; error: E };

export const ok = <T>(value: T): Result<T, never> => ({ ok: true, value });
export const err = <E>(error: E): Result<never, E> => ({ ok: false, error });

export const isOk = <T, E>(r: Result<T, E>): r is { ok: true; value: T } =>
  r.ok === true;
export const isErr = <T, E>(r: Result<T, E>): r is { ok: false; error: E } =>
  r.ok === false;

/** Wrap an async function so its rejections become Err. */
export async function tryAsync<T>(fn: () => Promise<T>): Promise<Result<T, Error>> {
  try {
    const value = await fn();
    return ok(value);
  } catch (e) {
    return err(e instanceof Error ? e : new Error(String(e)));
  }
}

/** Wrap a synchronous function so its throws become Err. */
export function trySync<T>(fn: () => T): Result<T, Error> {
  try {
    return ok(fn());
  } catch (e) {
    return err(e instanceof Error ? e : new Error(String(e)));
  }
}
