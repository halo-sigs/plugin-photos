<script lang="ts" setup>
import { useGroupsFetch } from "@/composables/useGroupsFetch";
import { VButton, VModal } from "@halo-dev/components";
import { computed, shallowRef, useTemplateRef } from "vue";

const props = defineProps<{
  defaultGroup?: string;
}>();

const emit = defineEmits<{
  (event: "close"): void;
}>();

const uploadGroup = shallowRef(props.defaultGroup || "");
const modal = useTemplateRef<InstanceType<typeof VModal>>("modal");

const { data: groups } = useGroupsFetch();

const groupOptions = computed(() => [
  { label: "未分组", value: "" },
  ...(groups.value?.map((g) => ({
    label: g.spec.displayName || g.metadata?.name,
    value: g.metadata?.name,
  })) || []),
]);

function onUploadDone() {
  modal.value?.close();
}
</script>

<template>
  <VModal title="上传图片" ref="modal" :width="900" @close="emit('close')">
    <div class=":uno: flex flex-col gap-4">
      <FormKit v-model="uploadGroup" label="上传到分组" type="select" :options="groupOptions" />

      <UppyUpload
        endpoint="/apis/console.api.photo.halo.run/v1alpha1/photos/upload"
        :meta="{
          group: uploadGroup,
        }"
        width="100%"
        :doneButtonHandler="onUploadDone"
      />
    </div>

    <template #footer>
      <VButton @click="modal?.close()">关闭</VButton>
    </template>
  </VModal>
</template>
