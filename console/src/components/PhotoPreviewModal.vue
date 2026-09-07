<script lang="ts" setup>
import { VModal } from "@halo-dev/components";
import { ref, watch } from "vue";

const props = defineProps<{
  url: string;
  title?: string;
}>();

const emit = defineEmits<{
  (event: "close"): void;
}>();

const failed = ref(false);

watch(
  () => props.url,
  () => {
    failed.value = false;
  },
);
</script>

<template>
  <VModal :title="title || '图片预览'" :width="1000" @close="emit('close')">
    <p v-if="failed" class=":uno: py-10 text-center text-sm text-gray-500">图片加载失败，请检查图片地址后重试。</p>
    <img
      v-else
      :key="url"
      :src="url"
      :alt="title || '图片预览'"
      class=":uno: max-h-[75vh] w-full object-contain"
      decoding="async"
      @error="failed = true"
    />
  </VModal>
</template>
