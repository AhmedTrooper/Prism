import { useEffect, type ReactNode } from "react";

import { useThemeStore, ensureThemeSystemListener } from "@/stores/themeStore";

/**
 * Global providers and side-effects. Mounts once at app start.
 */
export function Providers({ children }: { children: ReactNode }) {
  useEffect(() => {
    ensureThemeSystemListener();
    // Ensure the theme attribute is applied at boot, even before any UI
    // reads from the store.
    useThemeStore.getState().setMode(useThemeStore.getState().mode);
  }, []);
  return <>{children}</>;
}
