import "./styles/tailwind.css";
import type {PagesPublicState} from "@halo-dev/console-shared";
import {definePlugin} from "@halo-dev/console-shared";
import PhotoList from "@/views/PhotoList.vue";
import type {Ref} from "vue";

export default definePlugin({
  name: "PluginPhotos",
  components: [],
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
    PAGES: (state: Ref<PagesPublicState>) => {
      state.value.functionalPages.push({
        name: "相册",
        url: "/photos",
        path: "/pages/functional/photos",
        permissions: ["plugin:photos:view"],
      });
    },
  },
});
