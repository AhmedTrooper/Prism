# Prism Desktop

Prism's native desktop build. A Tauri v2 shell that hosts a React 19 + Vite renderer, talking to Rust through a typed IPC bridge. The same brand tokens, gesture vocabulary, and shortcut ergonomics as the Android player, laid out for desktop.

See the [root README](../README.md) for the full project context (mobile build, distribution, licensing).

## Quick Start

```bash
bun install
bun run dev           # vite dev server at http://localhost:1420
bun run tauri dev     # full Tauri shell, hot-reload renderer + Rust
bun run tauri build   # platform-native release binary
```

## Layout

```
src/
  app/        # Providers, App shell, routes registry, PlaceholderView
  components/ # ui/ primitives (raw Tailwind) and layout/ shell pieces
  features/   # feature-sliced design — every feature has the same shape
  hooks/      # cross-feature hooks
  ipc/        # typed Tauri command + event wrappers
  lib/        # pure helpers (cn, result, format, paths)
  schemas/    # zod schemas for non-IPC boundary inputs
  stores/     # cross-feature Zustand stores
  styles/     # Tailwind v4 entry + Prism brand tokens
src-tauri/    # Rust backend, Tauri commands, integration tests
```

## Testing

```bash
bun run test
bun run lint
bun run typecheck
cargo test  --manifest-path src-tauri/Cargo.toml
```

## Conventions

- Every feature exports its public surface from `index.ts`. Consumers
  import from `@/features/<name>`, never reach into `components/` directly.
- IPC commands and events are registered in `src/ipc/registry.ts` so the
  runtime validator has a single source of truth.
- Tailwind v4 `@theme` tokens live in `src/styles/prism.css`. Add a
  brand color there and every component picks it up.
