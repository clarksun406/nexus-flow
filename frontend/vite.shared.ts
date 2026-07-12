import { fileURLToPath, URL } from "node:url";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

export function createViteConfig(port: number, appRoot: string) {
  const buildId = process.env.NEXUSFLOW_FRONTEND_BUILD_ID ?? Date.now().toString(36);

  return defineConfig({
    base: "./",
    plugins: [vue()],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", appRoot)),
        "@nexusflow/api-client": fileURLToPath(new URL("./packages/api-client/src/index.ts", import.meta.url)),
        "@nexusflow/config": fileURLToPath(new URL("./packages/config/src/index.ts", import.meta.url)),
        "@nexusflow/ui": fileURLToPath(new URL("./packages/ui/src/index.ts", import.meta.url))
      }
    },
    server: {
      port,
      host: "0.0.0.0",
      proxy: {
        "/api": {
          target: "http://localhost:8080",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, "")
        }
      }
    },
    build: {
      outDir: "dist-app",
      emptyOutDir: false,
      rollupOptions: {
        output: {
          entryFileNames: `assets/[name]-${buildId}-[hash].js`,
          chunkFileNames: `assets/[name]-${buildId}-[hash].js`,
          assetFileNames: `assets/[name]-${buildId}-[hash][extname]`
        }
      }
    }
  });
}
