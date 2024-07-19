import PhotoList from "@/views/PhotoList.vue";
import { definePlugin } from "@halo-dev/console-shared";
import { markRaw } from "vue";
import RiImage2Line from "~icons/ri/image-2-line";

export default definePlugin({
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/photos",
        name: "Photos",
        component: PhotoList,
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
