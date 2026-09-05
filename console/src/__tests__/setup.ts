import { config, enableAutoUnmount } from "@vue/test-utils";
import { afterEach, beforeEach, vi } from "vitest";

// Halo provides these components and utilities at runtime. Keep their slots and
// events, while mounting the plugin's own components and FormKit inputs normally.
vi.mock("@halo-dev/components", () => {
  const slots = { template: "<div><slot name='header' /><slot /><slot name='footer' /></div>" };
  const icon = { template: "<span />" };
  return {
    VButton: {
      props: ["disabled", "size", "type"],
      template: "<button type='button' :disabled='disabled'><slot /></button>",
    },
    VModal: {
      props: ["title"],
      emits: ["close"],
      template:
        "<section role='dialog' :aria-label='title'><button type='button' @click=\"$emit('close')\">关闭预览</button><slot /><slot name='footer' /></section>",
    },
    VCard: slots,
    VSpace: slots,
    VPageHeader: slots,
    VDropdown: slots,
    VDropdownItem: slots,
    VEmpty: slots,
    VLoading: slots,
    VPagination: { props: ["page", "size", "total"], emits: ["update:page", "update:size"], template: "<div />" },
    IconAddCircle: icon,
    IconArrowLeft: icon,
    IconArrowRight: icon,
    IconExternalLinkLine: icon,
    IconGrid: icon,
    IconList: icon,
    IconRefreshLine: icon,
    IconCheckboxFill: icon,
    Dialog: { warning: vi.fn() },
    Toast: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
  };
});

vi.mock("@halo-dev/ui-shared", () => ({
  utils: {
    attachment: { getThumbnailUrl: vi.fn((url: string) => `${url}?width=400`) },
    permission: { has: vi.fn(() => true) },
    date: { format: (value?: string) => value || "-" },
  },
}));

vi.mock("@/api", () => ({
  photosCoreApiClient: { photo: { patchPhoto: vi.fn() } },
  photosConsoleApiClient: { photo: { listPhotos: vi.fn() } },
}));

vi.mock("@/composables/useGroupsFetch", async () => {
  const { ref } = await import("vue");
  return { QK_PHOTO_GROUPS: "test:groups", useGroupsFetch: () => ({ data: ref([]) }) };
});

vi.mock("@/composables/usePhotoTags", async () => {
  const { ref } = await import("vue");
  return { QK_PHOTO_TAGS: "test:tags", usePhotoTags: () => ({ tagOptions: ref([]) }) };
});

config.global.directives.tooltip = {};
enableAutoUnmount(afterEach);
beforeEach(() => localStorage.clear());
