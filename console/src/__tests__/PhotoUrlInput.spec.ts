import PhotoUrlInput from "@/components/PhotoUrlInput.vue";
import { usePhotoPreferences } from "@/composables/usePhotoPreferences";
import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { defineComponent, ref } from "vue";
import { formKit, photos } from "./fixtures";

const imageUrl = photos[0]!.spec.url;

function mountForm(value = imageUrl) {
  const onSubmit = vi.fn();
  const wrapper = mount(
    defineComponent({
      components: { PhotoUrlInput },
      setup() {
        return {
          initialValue: ref(value),
          formValue: ref({ url: value }),
          onSubmit,
          ...usePhotoPreferences(),
        };
      },
      template:
        '<FormKit v-model="formValue" type="form" :actions="false" @submit="onSubmit"><PhotoUrlInput :value="initialValue" /></FormKit>',
    }),
    { global: { plugins: [formKit] } },
  );
  return { wrapper, onSubmit };
}

describe("PhotoUrlInput", () => {
  it("edits and submits a URL without mounting an attachment preview", async () => {
    const { wrapper, onSubmit } = mountForm();
    expect(wrapper.findAll("img")).toHaveLength(0);
    expect(wrapper.get("input[name=url]").element).toHaveProperty("value", imageUrl);

    const newUrl = "https://images.example.com/edited.jpg";
    await wrapper.get("input[name=url]").setValue(newUrl);
    await wrapper.get("form").trigger("submit");
    await vi.waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(onSubmit.mock.calls[0]?.[0]).toMatchObject({ url: newUrl });
    expect(wrapper.findAll("img")).toHaveLength(0);
  });

  it("loads only an explicitly requested preview and unmounts it on close", async () => {
    const { wrapper } = mountForm();
    await wrapper.get("button").trigger("click");
    await vi.waitFor(() => expect(wrapper.find("img").exists()).toBe(true));
    expect(wrapper.get("img").attributes("src")).toBe(imageUrl);

    await wrapper.get("[role=dialog] button").trigger("click");
    expect(wrapper.findAll("img")).toHaveLength(0);
    expect(wrapper.find("[role=dialog]").exists()).toBe(false);
  });

  it("previews the current edited URL and closes the preview when it changes", async () => {
    const { wrapper } = mountForm();
    const newUrl = "https://images.example.com/edited.jpg";
    await wrapper.get("input[name=url]").setValue(newUrl);
    await vi.waitFor(() => expect(wrapper.vm.formValue.url).toBe(newUrl));
    await wrapper.get("button").trigger("click");
    await vi.waitFor(() => expect(wrapper.get("img").attributes("src")).toBe(newUrl));

    wrapper.vm.initialValue = "https://images.example.com/next.jpg";
    await flushPromises();
    expect(wrapper.findAll("img")).toHaveLength(0);
    expect(wrapper.get("input[name=url]").element).toHaveProperty("value", "https://images.example.com/next.jpg");
  });

  it("preserves the form value when switching between text and attachment inputs", async () => {
    const { wrapper, onSubmit } = mountForm();
    const newUrl = "https://images.example.com/edited.jpg";
    await wrapper.get("input[name=url]").setValue(newUrl);
    await vi.waitFor(() => expect(wrapper.vm.formValue.url).toBe(newUrl));

    wrapper.vm.informationOnly = false;
    await flushPromises();
    expect(wrapper.get("img").attributes("src")).toBe(newUrl);
    wrapper.vm.informationOnly = true;
    await flushPromises();
    expect(wrapper.findAll("img")).toHaveLength(0);
    await wrapper.get("form").trigger("submit");
    await vi.waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(onSubmit.mock.calls[0]?.[0]).toMatchObject({ url: newUrl });
  });

  it("keeps URL validation and disables preview for an empty value", async () => {
    const { wrapper, onSubmit } = mountForm("");
    expect(wrapper.get("button").element).toHaveProperty("disabled", true);
    await wrapper.get("form").trigger("submit");
    await flushPromises();
    expect(onSubmit).not.toHaveBeenCalled();
    expect(wrapper.findAll("img")).toHaveLength(0);
  });

  it("reports an image error without automatically loading another resource", async () => {
    const { wrapper } = mountForm();
    await wrapper.get("button").trigger("click");
    await vi.waitFor(() => expect(wrapper.find("img").exists()).toBe(true));
    await wrapper.get("img").trigger("error");
    expect(wrapper.text()).toContain("图片加载失败");
    expect(wrapper.findAll("img")).toHaveLength(0);
  });
});
