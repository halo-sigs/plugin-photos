<script lang="ts" setup>
import { VButton, VModal, VSpace } from "@halo-dev/components";
import { computed, ref, watch } from "vue";
import apiClient from "@/utils/api-client";
import cloneDeep from "lodash.clonedeep";
import { useMagicKeys } from "@vueuse/core";
import type { PhotoGroup } from "@/types";
import { submitForm } from "@formkit/core";

const props = withDefaults(
  defineProps<{
    visible: boolean;
    group: PhotoGroup | null;
  }>(),
  {
    visible: false,
    group: null,
  }
);

const emit = defineEmits<{
  (event: "update:visible", visible: boolean): void;
  (event: "close"): void;
}>();

const initialFormState: PhotoGroup = {
  apiVersion: "core.halo.run/v1alpha1",
  kind: "PhotoGroup",
  metadata: {
    name: "",
    generateName: "photo-group-",
  },
  spec: {
    displayName: "",
    priority: 0,
  },
  status: {
    photoCount: 0,
  },
};

const formState = ref<PhotoGroup>(initialFormState);
const saving = ref(false);

const isUpdateMode = computed(() => {
  return !!formState.value.metadata.creationTimestamp;
});
const isMac = /macintosh|mac os x/i.test(navigator.userAgent);

const handleCreateGroup = async () => {
  try {
    saving.value = true;
    if (isUpdateMode.value) {
      await apiClient.put(
        `/apis/core.halo.run/v1alpha1/photogroups/${formState.value.metadata.name}`,
        formState.value
      );
    } else {
      await apiClient.post(
        "/apis/core.halo.run/v1alpha1/photogroups",
        formState.value
      );
    }
    onVisibleChange(false);
  } catch (e) {
    console.error("Failed to create photo group", e);
  } finally {
    saving.value = false;
  }
};

const onVisibleChange = (visible: boolean) => {
  emit("update:visible", visible);
  if (!visible) {
    emit("close");
  }
};

watch(
  () => props.visible,
  (visible) => {
    if (visible && props.group) {
      formState.value = cloneDeep(props.group);
    }
    formState.value = cloneDeep(initialFormState);
  }
);

const { ControlLeft_Enter, Meta_Enter } = useMagicKeys();

watch(ControlLeft_Enter, (v) => {
  if (v && !isMac) {
    submitForm("photo-group-form");
  }
});

watch(Meta_Enter, (v) => {
  if (v && isMac) {
    submitForm("photo-group-form");
  }
});
</script>
<template>
  <VModal
    :visible="visible"
    :width="500"
    title="编辑分组"
    @update:visible="onVisibleChange"
  >
    <FormKit
      id="photo-group-form"
      :classes="{ form: 'w-full' }"
      type="form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleCreateGroup"
    >
      <FormKit
        v-model="formState.spec.displayName"
        help="可根据此名称查询图片"
        label="分组名称"
        type="text"
        validation="required"
      ></FormKit>
    </FormKit>
    <template #footer>
      <VSpace>
        <VButton type="secondary" @click="$formkit.submit('photo-group-form')">
          提交 {{ `${isMac ? "⌘" : "Ctrl"} + ↵` }}
        </VButton>
        <VButton @click="onVisibleChange(false)">取消 Esc</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
