<script lang="ts" setup>
import { photosCoreApiClient } from "@/api";
import type { Photo } from "@/api/generated";
import { QK_PHOTO_GROUPS, useGroupsFetch } from "@/composables/useGroupsFetch";
import { QK_PHOTOS } from "@/composables/usePhotosFetch";
import { QK_PHOTO_TAGS } from "@/composables/usePhotoTags";
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { useQueryClient } from "@tanstack/vue-query";
import { chunk } from "es-toolkit";
import { computed, ref, useTemplateRef } from "vue";

const props = defineProps<{
  defaultGroup?: string;
}>();

const emit = defineEmits<{
  (event: "close"): void;
}>();

const queryClient = useQueryClient();
const modal = useTemplateRef<InstanceType<typeof VModal>>("modal");

const { data: groups } = useGroupsFetch();

const groupName = ref(props.defaultGroup || "");
const urlText = ref("");
const isSubmitting = ref(false);

const groupOptions = computed(() => [
  { label: "未分组", value: "" },
  ...(groups.value?.map((g) => ({
    label: g.spec.displayName || g.metadata?.name,
    value: g.metadata?.name,
  })) || []),
]);

function deriveNameFromUrl(url: string): string {
  try {
    const pathname = new URL(url).pathname;
    const filename = pathname.split("/").pop();
    return filename && filename !== "" ? decodeURIComponent(filename) : "未命名";
  } catch {
    return "未命名";
  }
}

async function onSubmit() {
  const urls = urlText.value
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  if (urls.length === 0) {
    Toast.warning("请输入至少一个有效的图片链接");
    return;
  }

  isSubmitting.value = true;

  try {
    const items = urls.map((url) => ({
      url,
      displayName: deriveNameFromUrl(url),
    }));

    const chunks = chunk(items, 5);

    for (const chunkItem of chunks) {
      await Promise.all(
        chunkItem.map((item) => {
          const photo: Photo = {
            metadata: {
              name: "",
              generateName: "photo-",
            },
            spec: {
              url: item.url,
              displayName: item.displayName,
              groupName: groupName.value || undefined,
              priority: 0,
            },
            kind: "Photo",
            apiVersion: "core.halo.run/v1alpha1",
          };

          return photosCoreApiClient.photo.createPhoto({
            photo: photo,
          });
        }),
      );
    }

    Toast.success(`成功导入 ${items.length} 张照片`);
    queryClient.invalidateQueries({ queryKey: [QK_PHOTOS] });
    queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
    queryClient.invalidateQueries({ queryKey: [QK_PHOTO_TAGS] });
    modal.value?.close();
  } catch {
    Toast.error("部分照片导入失败，请重试");
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <VModal title="批量添加外链" ref="modal" :centered="false" :width="600" @close="emit('close')">
    <div class=":uno: flex flex-col gap-4">
      <FormKit v-model="groupName" label="分组" type="select" :options="groupOptions" />
      <FormKit
        v-model="urlText"
        label="链接列表"
        type="textarea"
        :rows="10"
        placeholder="每行一个图片链接地址"
        validation="required"
      />
    </div>

    <template #footer>
      <VSpace>
        <VButton type="secondary" :loading="isSubmitting" @click="onSubmit">添加</VButton>
        <VButton @click="modal?.close()">关闭</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
