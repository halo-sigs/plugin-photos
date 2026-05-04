<script lang="ts" setup>
import { photosCoreApiClient } from "@/api";
import type { Photo } from "@/api/generated";
import { QK_PHOTO_GROUPS } from "@/composables/useGroupsFetch";
import { QK_PHOTOS } from "@/composables/usePhotosFetch";
import { IconAddCircle, Toast, VButton, VDropdown, VDropdownItem } from "@halo-dev/components";
import { utils, type AttachmentLike, type AttachmentSimple } from "@halo-dev/ui-shared";
import { useQueryClient } from "@tanstack/vue-query";
import { chunk } from "es-toolkit";
import { defineAsyncComponent, ref } from "vue";
import PhotoCreationModal from "./PhotoCreationModal.vue";

const PhotoUploadModal = defineAsyncComponent(() => import("@/components/PhotoUploadModal.vue"));
const ExternalLinkImportModal = defineAsyncComponent(() => import("@/components/ExternalLinkImportModal.vue"));

const props = defineProps<{
  defaultGroup?: string;
}>();

const queryClient = useQueryClient();

const uploadModalVisible = ref(false);

function onUploadModalClose() {
  uploadModalVisible.value = false;
  queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
  queryClient.invalidateQueries({ queryKey: [QK_PHOTOS] });
}

const photoCreationModalVisible = ref(false);

const attachmentModalVisible = ref(false);
const externalLinkImportModalVisible = ref(false);

const onAttachmentsSelect = async (attachments: AttachmentLike[]) => {
  const items = attachments
    .map((attachment) => utils.attachment.convertToSimple(attachment))
    .filter(Boolean) as AttachmentSimple[];

  if (items.length === 0) {
    Toast.warning("请选择至少一张图片");
    return;
  }

  const chunks = chunk(items, 5);

  for (const chunk of chunks) {
    await Promise.all(
      chunk.map((item) => {
        const photo: Photo = {
          metadata: {
            name: "",
            generateName: "photo-",
          },
          spec: {
            url: item.url,
            displayName: item.alt || "",
            groupName: props.defaultGroup,
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

  Toast.success(`新建成功，一共创建了 ${items.length} 张图片`);
  queryClient.invalidateQueries({ queryKey: [QK_PHOTOS] });
  queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
  attachmentModalVisible.value = false;
};
</script>
<template>
  <VDropdown v-if="utils.permission.has(['plugin:photos:manage'])">
    <VButton type="secondary">
      <template #icon>
        <IconAddCircle />
      </template>
      新增
    </VButton>
    <template #popper>
      <VDropdownItem @click="uploadModalVisible = true">直接上传</VDropdownItem>
      <VDropdownItem @click="photoCreationModalVisible = true">手动添加</VDropdownItem>
      <VDropdownItem @click="attachmentModalVisible = true">从附件库选择</VDropdownItem>
      <VDropdownItem @click="externalLinkImportModalVisible = true">批量添加外链</VDropdownItem>
    </template>
  </VDropdown>

  <PhotoUploadModal v-if="uploadModalVisible" :default-group="defaultGroup" @close="onUploadModalClose" />

  <PhotoCreationModal v-if="photoCreationModalVisible" @close="photoCreationModalVisible = false" />

  <AttachmentSelectorModal
    v-if="attachmentModalVisible"
    @close="attachmentModalVisible = false"
    :accepts="['image/*']"
    @select="onAttachmentsSelect"
  />

  <ExternalLinkImportModal
    v-if="externalLinkImportModalVisible"
    :default-group="defaultGroup"
    @close="externalLinkImportModalVisible = false"
  />
</template>
