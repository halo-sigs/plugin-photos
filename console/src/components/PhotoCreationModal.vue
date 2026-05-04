<script lang="ts" setup>
import { photosCoreApiClient } from "@/api";
import { QK_PHOTO_GROUPS } from "@/composables/useGroupsFetch";
import { QK_PHOTOS } from "@/composables/usePhotosFetch";
import { QK_PHOTO_TAGS } from "@/composables/usePhotoTags";
import type { PhotoFormState } from "@/types";
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { useMutation, useQueryClient } from "@tanstack/vue-query";
import { ref } from "vue";
import PhotoForm from "./PhotoForm.vue";

const emit = defineEmits<{
  (event: "close"): void;
}>();

const queryClient = useQueryClient();

const modal = ref<InstanceType<typeof VModal> | null>(null);

const { mutate, isPending } = useMutation({
  mutationFn: (data: PhotoFormState) => {
    return photosCoreApiClient.photo.createPhoto({
      photo: {
        metadata: {
          name: "",
          generateName: "photo-",
        },
        spec: {
          url: data.url,
          displayName: data.displayName,
          groupName: data.groupName,
          description: data.description,
          tags: data.tags,
        },
        exif: data.exif,
        kind: "Photo",
        apiVersion: "core.halo.run/v1alpha1",
      },
    });
  },
  onSuccess() {
    Toast.success("添加图片成功");
    modal.value?.close();
    queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
    queryClient.invalidateQueries({ queryKey: [QK_PHOTO_TAGS] });
    queryClient.invalidateQueries({ queryKey: [QK_PHOTOS] });
  },
});

function onSubmit(data: PhotoFormState) {
  mutate(data);
}
</script>

<template>
  <VModal ref="modal" :width="800" title="添加图片" @close="emit('close')">
    <PhotoForm @submit="onSubmit" />
    <template #footer>
      <VSpace>
        <!-- @vue-ignore -->
        <VButton type="secondary" :loading="isPending" @click="$formkit.submit('photo-form')">保存</VButton>
        <VButton @click="modal?.close()">关闭</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
