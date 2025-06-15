import { definePlugin } from "@halo-dev/console-shared";
import { defineAsyncComponent, markRaw } from "vue";
import RiImage2Line from "~icons/ri/image-2-line";
import "uno.css";
import { VLoading } from "@halo-dev/components";

export default definePlugin({
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/photos",
        name: "Photos",
        component: defineAsyncComponent({
          loader: () => import("@/views/PhotoList.vue"),
          loadingComponent: VLoading,
        }),
        meta: {
          permissions: ["plugin:photos:view"],
          menu: {
            name: "图库",
            group: "content",
            icon: markRaw(RiImage2Line),
          },
        },
      },
    },
  ],
});
