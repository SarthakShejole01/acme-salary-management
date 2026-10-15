import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    // Vitest's on-disk module cache is not invalidated when an Angular template
    // changes, which produced false-green runs locally. Builds are fast; keep it off.
    fsModuleCache: false,
  },
});
