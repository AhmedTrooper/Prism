/**
 * Route registry. Each feature contributes a route entry. The sidebar
 * uses these IDs; the router uses these components.
 */

import type { ComponentType } from "react";

import { LibraryView } from "@/features/library";

import { PlaceholderView } from "./PlaceholderView";

export type RouteId = "library" | "online-streams" | "player" | "settings" | "about";

export interface RouteDef {
  id: RouteId;
  label: string;
  group: "media" | "config";
  component: ComponentType;
}

export const routes: RouteDef[] = [
  {
    id: "library",
    label: "Library",
    group: "media",
    component: LibraryView,
  },
  {
    id: "online-streams",
    label: "Online streams",
    group: "media",
    component: () => <PlaceholderView name="Online streams" />,
  },
  {
    id: "player",
    label: "Player",
    group: "media",
    component: () => <PlaceholderView name="Player" />,
  },
  {
    id: "settings",
    label: "Settings",
    group: "config",
    component: () => <PlaceholderView name="Settings" />,
  },
  {
    id: "about",
    label: "About",
    group: "config",
    component: () => <PlaceholderView name="About" />,
  },
];

export function getRoute(id: RouteId): RouteDef {
  const r = routes.find((x) => x.id === id);
  if (!r) throw new Error(`unknown route: ${id}`);
  return r;
}
