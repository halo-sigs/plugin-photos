import type { Extension } from "@halo-dev/api-client";
import { definePlugin, type CommentSubjectRefResult } from "@halo-dev/ui-shared";
import "uno.css";
import { markRaw } from "vue";
import RiImage2Line from "~icons/ri/image-2-line";
import type { Photo } from "./api/generated";

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
  extensionPoints: {
    "comment:subject-ref:create": () => {
      return [
        {
          kind: "Photo",
          group: "core.halo.run",
          resolve: (subject: Extension): CommentSubjectRefResult => {
            const photo = subject as Photo;
            return {
              label: "图库",
              title: photo.spec.displayName,
              externalUrl: `/photos/${photo.metadata.name}`,
              route: {
                name: "Photos",
              },
            };
          },
        },
      ];
    },
  },
});
