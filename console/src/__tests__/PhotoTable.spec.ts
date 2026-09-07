import PhotoTable from "@/components/PhotoTable.vue";
import { utils } from "@halo-dev/ui-shared";
import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { photos, vueQuery } from "./fixtures";

function mountTable(informationOnly = true) {
  return mount(PhotoTable, {
    props: { photos, isSelected: () => false, informationOnly },
    global: { plugins: [vueQuery()] },
  });
}

describe("PhotoTable", () => {
  it("renders metadata without creating images or resolving thumbnails", () => {
    const wrapper = mountTable();

    expect(wrapper.text()).toContain("山间日出");
    expect(wrapper.text()).toContain("Test Camera");
    expect(wrapper.text()).toContain("风景");
    expect(wrapper.findAll("img")).toHaveLength(0);
    expect(utils.attachment.getThumbnailUrl).not.toHaveBeenCalled();
  });

  it("keeps preview, edit and selection actions available without loading images", async () => {
    const wrapper = mountTable();
    const row = wrapper.get("tbody tr:nth-child(2)");
    const buttons = row.findAll("button");

    await buttons.find((button) => button.text() === "预览")!.trigger("click");
    expect(wrapper.emitted("preview")).toEqual([[photos[1]]]);
    await buttons.find((button) => button.text() === "编辑")!.trigger("click");
    expect(wrapper.emitted("openEdit")).toEqual([[photos[1]]]);
    await row.get("input[type=checkbox]").setValue(true);
    expect(wrapper.emitted("toggleSelect")).toEqual([[photos[1], true]]);
    expect(wrapper.findAll("img")).toHaveLength(0);
  });

  it("updates memoized rows when image visibility changes", async () => {
    const wrapper = mountTable(false);
    expect(wrapper.findAll("img")).toHaveLength(photos.length);
    expect(wrapper.get("img").attributes("src")).toBe(`${photos[0]!.spec.url}?width=400`);

    await wrapper.setProps({ informationOnly: true });
    expect(wrapper.findAll("img")).toHaveLength(0);
    expect(wrapper.findAll("button").filter((button) => button.text() === "预览")).toHaveLength(photos.length);

    await wrapper.setProps({ informationOnly: false });
    expect(wrapper.findAll("img")).toHaveLength(photos.length);
  });

  it("allows viewers to preview and view details without management controls", async () => {
    vi.mocked(utils.permission.has).mockReturnValue(false);
    try {
      const wrapper = mountTable();
      const row = wrapper.get("tbody tr");
      expect(row.find("input[type=checkbox]").exists()).toBe(false);
      await row
        .findAll("button")
        .find((button) => button.text() === "查看")!
        .trigger("click");
      expect(wrapper.emitted("openEdit")).toEqual([[photos[0]]]);
      expect(row.findAll("button").some((button) => button.text() === "预览")).toBe(true);
      expect(wrapper.findAll("img")).toHaveLength(0);
    } finally {
      vi.mocked(utils.permission.has).mockReturnValue(true);
    }
  });
});
