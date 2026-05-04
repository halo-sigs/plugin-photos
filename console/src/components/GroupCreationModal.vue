<script lang="ts" setup>
import { photosCoreApiClient } from "@/api";
import { useGroupSelection } from "@/composables/useGroupSelection";
import { QK_PHOTO_GROUPS } from "@/composables/useGroupsFetch";
import type { PhotoGroupFormState } from "@/types";
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { useMutation, useQueryClient } from "@tanstack/vue-query";
import { ref } from "vue";
import GroupForm from "./GroupForm.vue";

const emit = defineEmits<{
  (event: "close"): void;
}>();

const queryClient = useQueryClient();

const modal = ref<InstanceType<typeof VModal> | null>(null);

const { selectedGroup } = useGroupSelection();

const { mutate, isPending } = useMutation({
  mutationFn: (data: PhotoGroupFormState) => {
    return photosCoreApiClient.group.createPhotoGroup({
      photoGroup: {
        apiVersion: "core.halo.run/v1alpha1",
        kind: "PhotoGroup",
        metadata: {
          name: "",
          generateName: "photo-group-",
        },
        spec: {
          displayName: data.displayName,
          priority: data.priority,
        },
      },
    });
  },
  onSuccess(data) {
    Toast.success("创建分组成功");
    modal.value?.close();
    queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
    selectedGroup.value = data.data.metadata.name;
  },
});

function onSubmit(data: PhotoGroupFormState) {
  mutate(data);
}
</script>

<template>
  <VModal :width="620" title="创建分组" ref="modal" @close="emit('close')">
    <GroupForm @submit="onSubmit" />
    <template #footer>
      <VSpace>
        <!-- @vue-ignore -->
        <VButton type="secondary" :loading="isPending" @click="$formkit.submit('photo-group-form')">创建</VButton>
        <VButton @click="modal?.close()">关闭</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
