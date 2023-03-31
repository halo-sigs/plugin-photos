<script lang="ts" setup>
import { IconSave, VButton, VModal } from "@halo-dev/components";
import { computed, defineProps, ref, watch } from "vue";
import type { Photo } from "@/types";
import apiClient from "@/utils/api-client";
import cloneDeep from "lodash.clonedeep";
import { reset, submitForm } from "@formkit/core";

const props = withDefaults(
  defineProps<{
    visible: boolean;
    photo?: Photo;
    group?: string;
  }>(),
  {
    visible: false,
    photo: undefined,
    group: undefined,
  }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "close"): void;
  (event: "saved", photo: Photo): void;
}>();

const formSchema = [
  {
    $formkit: "text",
    name: "displayName",
    label: "名称",
    validation: "required",
  },
  {
    $formkit: "attachment",
    name: "url",
    label: "图片地址",
    validation: "required",
  },
  {
    $formkit: "attachment",
    name: "cover",
    label: "封面",
  },
  {
    $formkit: "textarea",
    name: "description",
    label: "描述",
  },
];

const initialFormState: Photo = {
  metadata: {
    name: "",
    generateName: "photo-",
  },
  spec: {
    displayName: "",
    url: "",
    cover: "",
    groupName: props.group || "",
  },
  kind: "Photo",
  apiVersion: "core.halo.run/v1alpha1",
};

const formState = ref<Photo>(cloneDeep(initialFormState));

const saving = ref<boolean>(false);

const isUpdateMode = computed(() => {
  return !!formState.value.metadata.creationTimestamp;
});

const modalTitle = computed(() => {
  return isUpdateMode.value ? "编辑图片" : "添加图片";
});

const onVisibleChange = (visible: boolean) => {
  emit("update:visible", visible);
  if (!visible) {
    emit("close");
  }
};

const handleResetForm = () => {
  formState.value = cloneDeep(initialFormState);
  reset("photo-form");
};

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      handleResetForm();
    }
  }
);

watch(
  () => props.photo,
  (photo) => {
    if (photo) {
      formState.value = cloneDeep(photo);
    } else {
      handleResetForm();
    }
  }
);

const handleSavePhoto = async () => {
  try {
    saving.value = true;
    if (isUpdateMode.value) {
      await apiClient.put<Photo>(
        `/apis/core.halo.run/v1alpha1/photos/${formState.value.metadata.name}`,
        formState.value
      );
    } else {
      if (props.group) {
        formState.value.spec.groupName = props.group;
      }
      const { data } = await apiClient.post<Photo>(
        `/apis/core.halo.run/v1alpha1/photos`,
        formState.value
      );
      emit("saved", data);
    }
    onVisibleChange(false);
  } catch (e) {
    console.error(e);
  } finally {
    saving.value = false;
  }
};
</script>
<template>
  <VModal
    :title="modalTitle"
    :visible="visible"
    :width="650"
    @update:visible="onVisibleChange"
  >
    <template #actions>
      <slot name="append-actions" />
    </template>

    <FormKit
      id="photo-form"
      v-model="formState.spec"
      :actions="false"
      :config="{ validationVisibility: 'submit' }"
      type="form"
      @submit="handleSavePhoto"
    >
      <FormKitSchema :schema="formSchema" />
    </FormKit>
    <template #footer>
      <VButton
        :loading="saving"
        type="secondary"
        @click="submitForm('photo-form')"
      >
        <template #icon>
          <IconSave class="photos-h-full photos-w-full" />
        </template>
        保存
      </VButton>
    </template>
  </VModal>
</template>
