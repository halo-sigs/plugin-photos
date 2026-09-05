import type { Photo } from "@/api/generated";
import { createInput, defaultConfig, plugin } from "@formkit/vue";
import { QueryClient, VueQueryPlugin } from "@tanstack/vue-query";
import { afterEach } from "vitest";

export const photos: Photo[] = [
  {
    apiVersion: "core.halo.run/v1alpha1",
    kind: "Photo",
    metadata: { name: "photo-1" },
    spec: { displayName: "山间日出", url: "https://images.example.com/sunrise.jpg", tags: ["风景"] },
    exif: { model: "Test Camera", dateTimeOriginal: "2026-05-01T08:00:00Z" },
  },
  {
    apiVersion: "core.halo.run/v1alpha1",
    kind: "Photo",
    metadata: { name: "photo-2" },
    spec: { displayName: "海边落日", url: "https://images.example.com/sunset.jpg" },
  },
];

// Simulate the host attachment input's automatic preview. The surrounding
// FormKit node, value binding, validation and form submission remain real.
export const formKit: [typeof plugin, ReturnType<typeof defaultConfig>] = [
  plugin,
  defaultConfig({
    inputs: {
      attachment: createInput({
        props: ["context"],
        template: "<img v-if='context._value' :src='context._value' />",
      }),
    },
  }),
];

const clients: QueryClient[] = [];
export function vueQuery(): [typeof VueQueryPlugin, { queryClient: QueryClient }] {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  clients.push(queryClient);
  return [VueQueryPlugin, { queryClient }];
}

afterEach(() => {
  clients.splice(0).forEach((client) => client.clear());
});
