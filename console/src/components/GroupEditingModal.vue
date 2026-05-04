<script lang="ts" setup>
import { photosCoreApiClient } from "@/api";
import type { PhotoGroup } from "@/api/generated";
import { QK_PHOTO_GROUPS } from "@/composables/useGroupsFetch";
import type { PhotoGroupFormState } from "@/types";
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { useMutation, useQueryClient } from "@tanstack/vue-query";
import { useTemplateRef } from "vue";
import GroupForm from "./GroupForm.vue";

const props = withDefaults(
  defineProps<{
    group: PhotoGroup;
  }>(),
  {},
);

const emit = defineEmits<{
  (event: "close"): void;
}>();

const queryClient = useQueryClient();

const modal = useTemplateRef<InstanceType<typeof VModal> | null>("modal");

const { isPending, mutate } = useMutation({
  mutationFn: (data: PhotoGroupFormState) => {
    return photosCoreApiClient.group.patchPhotoGroup({
      name: props.group?.metadata.name,
      jsonPatchInner: [
        { op: "add", path: "/spec/displayName", value: data.displayName },
        { op: "add", path: "/spec/priority", value: data.priority },
        { op: "add", path: "/metadata/annotations", value: data.annotations },
      ],
    });
  },
  onSuccess() {
    Toast.success("保存分组成功");
    modal.value?.close();
    queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
  },
});

function onSubmit(data: PhotoGroupFormState) {
  mutate(data);
}
</script>
<template>
  <VModal ref="modal" :width="620" title="编辑分组" @close="emit('close')">
    <GroupForm
      :formState="{
        name: group.metadata.name,
        displayName: group.spec.displayName,
        priority: group.spec.priority ?? 0,
        annotations: group.metadata.annotations || {},
      }"
      @submit="onSubmit"
    />
    <template #footer>
      <VSpace>
        <!-- @vue-ignore -->
        <VButton :loading="isPending" type="secondary" @click="$formkit.submit('photo-group-form')"> 保存 </VButton>
        <VButton @click="modal?.close()">取消</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
