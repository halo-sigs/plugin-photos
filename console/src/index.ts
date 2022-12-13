import "./styles/tailwind.css";
import { definePlugin } from "@halo-dev/console-shared";
import PhotoList from "@/views/PhotoList.vue";

export default definePlugin({
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/pages/functional/photos",
        name: "Photos",
        component: PhotoList,
        meta: {
          permissions: ["plugin:photos:view"],
        },
      },
    },
  ],
  extensionPoints: {
    "page:functional:create": () => {
      return [
        {
          name: "相册",
          url: "/photos",
          path: "/pages/functional/photos",
          permissions: ["plugin:photos:view"],
        },
      ];
    },
  },
});
