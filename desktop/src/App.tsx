import { Providers } from "./app/Providers";
import { App } from "./app/App";

/**
 * Slim wrapper around <Providers><App/></Providers>.
 * Kept separate from the app-level App so Vite can HMR the inner tree
 * without remounting providers.
 */
export default function Root() {
  return (
    <Providers>
      <App />
    </Providers>
  );
}
