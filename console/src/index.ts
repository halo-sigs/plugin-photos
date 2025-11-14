import { definePlugin } from "@halo-dev/ui-shared";
import "uno.css";
import { markRaw } from "vue";
import RiImage2Line from "~icons/ri/image-2-line";

export default definePlugin({
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/photos",
        name: "Photos",
        component: () => import("./views/PhotoList.vue"),
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
