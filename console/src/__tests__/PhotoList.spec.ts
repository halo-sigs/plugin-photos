import { photosConsoleApiClient } from "@/api";
import PhotoGridItem from "@/components/PhotoGridItem.vue";
import PhotoTable from "@/components/PhotoTable.vue";
import PhotoList from "@/views/PhotoList.vue";
import { utils } from "@halo-dev/ui-shared";
import { flushPromises, mount } from "@vue/test-utils";
import { AxiosHeaders } from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createMemoryHistory, createRouter } from "vue-router";
import { formKit, photos, vueQuery } from "./fixtures";

vi.mock("@/components/AddButton.vue", () => ({ default: { template: "<div />" } }));
vi.mock("@/components/GroupFilter.vue", () => ({ default: { template: "<div />" } }));

const listPhotos = vi.mocked(photosConsoleApiClient.photo.listPhotos);

beforeEach(() => {
  listPhotos.mockResolvedValue({
    data: {
      items: photos,
      page: 1,
      size: 60,
      total: 120,
      totalPages: 2,
      first: true,
      last: false,
      hasNext: true,
      hasPrevious: false,
    },
    status: 200,
    statusText: "OK",
    headers: {},
    config: { headers: new AxiosHeaders() },
  });
});

async function mountView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/photos", component: PhotoList }],
  });
  await router.push("/photos");
  await router.isReady();
  const wrapper = mount(PhotoList, {
    global: {
      plugins: [router, vueQuery(), formKit],
      stubs: {
        SearchInput: true,
        FilterDropdown: true,
        FilterCleanButton: true,
        AnnotationsForm: true,
      },
    },
  });
  await vi.waitFor(() => expect(wrapper.text()).toContain("山间日出"));
  return { wrapper, router };
}

describe("PhotoList information-only mode", () => {
  it("defaults to metadata without creating gallery images", async () => {
    const { wrapper } = await mountView();
    expect(wrapper.get("label input[type=checkbox]").element).toHaveProperty("checked", true);
    expect(wrapper.findComponent(PhotoTable).exists()).toBe(true);
    expect(wrapper.findComponent(PhotoGridItem).exists()).toBe(false);
    expect(wrapper.findAll("img")).toHaveLength(0);
    expect(utils.attachment.getThumbnailUrl).not.toHaveBeenCalled();
    expect(listPhotos).toHaveBeenCalledWith(expect.objectContaining({ page: 1, size: 60 }));
  });

  it.each(["grid", "list"])(
    "restores the saved %s view and persists information-only mode across visits",
    async (view) => {
      localStorage.setItem("plugin:photos:viewMode", view);
      const { wrapper } = await mountView();
      await wrapper.get("label input[type=checkbox]").setValue(false);
      expect(wrapper.findAll("img")).toHaveLength(photos.length);
      expect(wrapper.findComponent(PhotoGridItem).exists()).toBe(view === "grid");
      expect(localStorage.getItem("plugin:photos:informationOnly")).toBe("false");
      wrapper.unmount();

      const second = await mountView();
      expect(second.wrapper.findAll("img")).toHaveLength(photos.length);
      await second.wrapper.get("label input[type=checkbox]").setValue(true);
      expect(second.wrapper.findAll("img")).toHaveLength(0);
      expect(localStorage.getItem("plugin:photos:viewMode")).toBe(view);
      second.wrapper.unmount();

      const third = await mountView();
      expect(third.wrapper.findAll("img")).toHaveLength(0);
      expect(third.wrapper.findComponent(PhotoTable).exists()).toBe(true);
    },
  );

  it("previews only the selected photo and removes it when closed", async () => {
    const { wrapper } = await mountView();
    const row = wrapper.get("tbody tr:nth-child(2)");
    await row
      .findAll("button")
      .find((button) => button.text() === "预览")!
      .trigger("click");
    await vi.waitFor(() => expect(wrapper.find("img").exists()).toBe(true));
    expect(wrapper.findAll("img")).toHaveLength(1);
    expect(wrapper.get("img").attributes("src")).toBe(photos[1]!.spec.url);
    expect(wrapper.get("[role=dialog]").attributes("aria-label")).toBe("海边落日");
    expect(utils.attachment.getThumbnailUrl).not.toHaveBeenCalled();

    await wrapper.get("[role=dialog] button").trigger("click");
    expect(wrapper.findAll("img")).toHaveLength(0);
  });

  it("opens the real editing form without automatically previewing the photo", async () => {
    const { wrapper } = await mountView();
    await wrapper
      .get("tbody tr")
      .findAll("button")
      .find((button) => button.text() === "编辑")!
      .trigger("click");
    await vi.waitFor(() => expect(wrapper.find("input[name=url]").exists()).toBe(true));
    expect(wrapper.get("input[name=url]").element).toHaveProperty("value", photos[0]!.spec.url);
    expect(wrapper.findAll("img")).toHaveLength(0);

    await wrapper
      .findAll("button")
      .find((button) => button.text() === "预览图片")!
      .trigger("click");
    await vi.waitFor(() => expect(wrapper.find("img").exists()).toBe(true));
    expect(wrapper.findAll("img")).toHaveLength(1);
    expect(wrapper.get("img").attributes("src")).toBe(photos[0]!.spec.url);
  });

  it("keeps pagination and filters from creating image elements", async () => {
    const { wrapper, router } = await mountView();
    await router.replace({ path: "/photos", query: { page: "2" } });
    await vi.waitFor(() => expect(listPhotos).toHaveBeenCalledWith(expect.objectContaining({ page: "2" })));
    await flushPromises();
    expect(wrapper.findAll("img")).toHaveLength(0);

    await router.replace({ path: "/photos", query: { tag: "风景", group: "__ungrouped__" } });
    await vi.waitFor(() =>
      expect(listPhotos).toHaveBeenCalledWith(expect.objectContaining({ tag: "风景", ungrouped: true })),
    );
    await flushPromises();
    expect(wrapper.findAll("img")).toHaveLength(0);
    expect(utils.attachment.getThumbnailUrl).not.toHaveBeenCalled();
  });
});
