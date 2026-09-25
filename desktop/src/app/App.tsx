import { useState } from "react";

import {
  AppShell,
  Sidebar,
  StatusBar,
  WindowChrome,
  type SidebarItem,
} from "@/components/layout";
import {
  IconButton,
  Toaster,
  Toolbar,
  ToolbarDivider,
  ToolbarGroup,
  ToolbarSpacer,
} from "@/components/ui";
import { useShortcut } from "@/hooks/useShortcut";
import { useWindowStore } from "@/stores/windowStore";
import { useThemeStore } from "@/stores/themeStore";
import { routes, getRoute, type RouteId } from "./routes";

/**
 * Top-level App component. Wires sidebar/toolbar/statusbar/main together
 * and owns the currently selected route.
 */
export function App() {
  const [route, setRoute] = useState<RouteId>("library");
  const toggleSidebar = useWindowStore((s) => s.toggleSidebar);
  const toggleTheme = useThemeStore((s) => s.cycle);

  // Mod+B toggles the sidebar; Mod+Shift+T cycles the theme.
  useShortcut("Mod+B", () => toggleSidebar());
  useShortcut("Mod+Shift+T", () => toggleTheme());

  const items: SidebarItem[] = routes.map((r) => ({
    id: r.id,
    label: r.label,
    active: r.id === route,
    onSelect: () => setRoute(r.id),
  }));

  const RouteComp = getRoute(route).component;

  return (
    <AppShell
      sidebar={<Sidebar brand="Prism" items={items} />}
      toolbar={
        <>
          <WindowChrome title={`Prism · ${getRoute(route).label}`} />
          <Toolbar>
            <ToolbarGroup>
              <IconButton
                label="Toggle sidebar"
                variant="ghost"
                onClick={toggleSidebar}
                icon={<GlyphBurger />}
              />
            </ToolbarGroup>
            <ToolbarDivider />
            <span className="text-sm font-medium">{getRoute(route).label}</span>
            <ToolbarSpacer />
            <ToolbarGroup>
              <IconButton
                label="Cycle theme"
                variant="ghost"
                onClick={toggleTheme}
                icon={<GlyphTheme />}
              />
            </ToolbarGroup>
          </Toolbar>
        </>
      }
      statusBar={
        <StatusBar
          items={[
            { id: "ver", label: "Prism Desktop" },
            { id: "route", label: getRoute(route).label },
            { id: "ready", label: "Ready", tone: "success" },
          ]}
        />
      }
    >
      <RouteComp />
      <Toaster />
    </AppShell>
  );
}

function GlyphBurger() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
    >
      <path d="M2 4 H14 M2 8 H14 M2 12 H14" />
    </svg>
  );
}

function GlyphTheme() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="8" cy="8" r="5.5" />
      <path d="M8 2.5 V5 M8 11 V13.5 M2.5 8 H5 M11 8 H13.5" />
    </svg>
  );
}
