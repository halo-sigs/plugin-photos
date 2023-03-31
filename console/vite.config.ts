import { fileURLToPath, URL } from "url";

import { defineConfig } from "vite";
import Vue from "@vitejs/plugin-vue";
import Icons from "unplugin-icons/vite";

export default defineConfig(({ mode }) => {
  if (mode === "dev") {
    commonConfig.build.outDir = fileURLToPath(
      new URL("../build/resources/main/console", import.meta.url)
    );
  }
  return commonConfig;
});

const commonConfig = {
  plugins: [Vue(), Icons({ compiler: "vue3" })],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  build: {
    outDir: fileURLToPath(
      new URL("../src/main/resources/console", import.meta.url)
    ),
    emptyOutDir: true,
    lib: {
      entry: "src/index.ts",
      name: "PluginPhotos",
      formats: ["iife"],
      fileName: () => "main.js",
    },
    rollupOptions: {
      external: [
        "vue",
        "vue-router",
        "@vueuse/core",
        "@vueuse/components",
        "@vueuse/router",
        "@halo-dev/console-shared",
        "@halo-dev/components",
      ],
      output: {
        globals: {
          vue: "Vue",
          "vue-router": "VueRouter",
          "@vueuse/core": "VueUse",
          "@vueuse/components": "VueUse",
          "@vueuse/router": "VueUse",
          "@halo-dev/console-shared": "HaloConsoleShared",
          "@halo-dev/components": "HaloComponents",
        },
      },
    },
  },
  define: {
    "process.env": {},
  },
};
