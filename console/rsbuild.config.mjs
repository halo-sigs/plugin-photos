import { rsbuildConfig } from "@halo-dev/ui-plugin-bundler-kit";
import { UnoCSSRspackPlugin } from "@unocss/webpack/rspack";
import path from "node:path";
import Icons from "unplugin-icons/rspack";

export default rsbuildConfig({
  rsbuild: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
    tools: {
      rspack: {
        cache: false,
        plugins: [Icons({ compiler: "vue3" }), UnoCSSRspackPlugin()],
      },
    },
  },
});
