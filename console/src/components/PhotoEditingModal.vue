<script lang="ts" setup>
import { photosCoreApiClient } from "@/api";
import type { Photo } from "@/api/generated";
import { QK_PHOTO_GROUPS } from "@/composables/useGroupsFetch";
import { QK_PHOTOS } from "@/composables/usePhotosFetch";
import { QK_PHOTO_TAGS } from "@/composables/usePhotoTags";
import type { PhotoFormState } from "@/types";
import type { JsonPatchInner } from "@halo-dev/api-client";
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { useMutation, useQueryClient } from "@tanstack/vue-query";
import { useTemplateRef } from "vue";
import PhotoForm from "./PhotoForm.vue";

const props = withDefaults(
  defineProps<{
    photo: Photo;
  }>(),
  {},
);

const emit = defineEmits<{
  (event: "close"): void;
}>();

const queryClient = useQueryClient();

defineSlots<{
  "append-actions"?: () => unknown;
}>();

const modal = useTemplateRef<InstanceType<typeof VModal> | null>("modal");

const { mutate, isPending } = useMutation({
  mutationFn: (data: PhotoFormState) => {
    const jsonPatchInner: JsonPatchInner[] = [
      {
        op: "add",
        path: "/spec/url",
        value: data.url,
      },
      {
        op: "add",
        path: "/spec/displayName",
        value: data.displayName,
      },
      {
        op: "add",
        path: "/spec/groupName",
        value: data.groupName || "",
      },
      {
        op: "add",
        path: "/spec/description",
        value: data.description || "",
      },
      {
        op: "add",
        path: "/spec/tags",
        value: data.tags || [],
      },
      {
        op: "add",
        path: "/metadata/annotations",
        value: data.annotations || {},
      },
    ];

    if (data.exif) {
      jsonPatchInner.push({
        op: "add",
        path: "/exif",
        value: data.exif,
      });
    }

    return photosCoreApiClient.photo.patchPhoto({
      name: props.photo?.metadata.name,
      jsonPatchInner,
    });
  },
  onSuccess() {
    Toast.success("保存图片成功");
    modal.value?.close();
    queryClient.invalidateQueries({ queryKey: [QK_PHOTOS] });
    queryClient.invalidateQueries({ queryKey: [QK_PHOTO_TAGS] });
    queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
  },
});

function onSubmit(data: PhotoFormState) {
  mutate(data);
}
</script>
<template>
  <VModal ref="modal" title="编辑图片" :width="800" @close="emit('close')">
    <template #actions>
      <slot name="append-actions" />
    </template>

    <div>
      <PhotoForm
        :key="photo.metadata.name"
        :formState="{
          url: photo.spec.url,
          displayName: photo.spec.displayName,
          groupName: photo.spec.groupName,
          description: photo.spec.description,
          tags: photo.spec.tags,
          annotations: photo.metadata.annotations || {},
          exif: photo.exif,
        }"
        @submit="onSubmit"
      />
    </div>

    <template #footer>
      <VSpace>
        <!-- @vue-ignore -->
        <VButton :loading="isPending" type="secondary" @click="$formkit.submit('photo-form')">保存</VButton>
        <VButton @click="modal?.close()">取消</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
