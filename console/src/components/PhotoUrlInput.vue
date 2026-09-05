<script lang="ts" setup>
import { usePhotoPreferences } from "@/composables/usePhotoPreferences";
import { VButton } from "@halo-dev/components";
import { defineAsyncComponent, ref, watch } from "vue";

const props = defineProps<{
  value?: string;
}>();

const PhotoPreviewModal = defineAsyncComponent(() => import("./PhotoPreviewModal.vue"));
const { informationOnly } = usePhotoPreferences();
const url = ref(props.value || "");
const previewVisible = ref(false);

watch(
  () => props.value,
  (value) => {
    url.value = value || "";
  },
);

watch(url, () => {
  previewVisible.value = false;
});
</script>

<template>
  <FormKit
    :key="informationOnly ? 'text' : 'attachment'"
    v-model="url"
    :preserve="true"
    name="url"
    label="图片地址"
    :type="informationOnly ? 'text' : 'attachment'"
    width="50%"
    aspect-ratio="16/9"
    :accepts="['image/*']"
    validation="required"
  />
  <div v-if="informationOnly" class=":uno: mb-4">
    <VButton size="sm" :disabled="!url" @click="previewVisible = true">预览图片</VButton>
    <p class=":uno: mt-2 text-xs text-gray-500">仅显示信息模式下，点击预览后才加载图片。</p>
  </div>
  <PhotoPreviewModal v-if="previewVisible && url" :url="url" @close="previewVisible = false" />
</template>
