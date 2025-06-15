<script lang="ts" setup>
import type { Photo } from "@/types";
import { submitForm } from "@formkit/core";
import { axiosInstance } from "@halo-dev/api-client";
import { VButton, VModal, VSpace } from "@halo-dev/components";
import { cloneDeep } from "lodash-es";
import { computed, nextTick, onMounted, ref, useTemplateRef } from "vue";

const props = withDefaults(
  defineProps<{
    photo?: Photo;
    group?: string;
  }>(),
  {
    photo: undefined,
    group: undefined,
  }
);

const emit = defineEmits<{
  (event: "close"): void;
  (event: "saved", photo: Photo): void;
}>();

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
} as Photo;

const formState = ref<Photo>(cloneDeep(initialFormState));
const isSubmitting = ref<boolean>(false);
const modal = useTemplateRef<InstanceType<typeof VModal> | null>("modal");

const isUpdateMode = computed(() => {
  return !!formState.value.metadata.creationTimestamp;
});

const modalTitle = computed(() => {
  return isUpdateMode.value ? "编辑图片" : "添加图片";
});

onMounted(() => {
  if (props.photo) {
    formState.value = cloneDeep(props.photo);
  }
});

const annotationsFormRef = ref();

const handleSavePhoto = async () => {
  annotationsFormRef.value?.handleSubmit();
  await nextTick();
  const { customAnnotations, annotations, customFormInvalid, specFormInvalid } = annotationsFormRef.value || {};
  if (customFormInvalid || specFormInvalid) {
    return;
  }
  formState.value.metadata.annotations = {
    ...annotations,
    ...customAnnotations,
  };
  try {
    isSubmitting.value = true;
    if (isUpdateMode.value) {
      await axiosInstance.put<Photo>(
        `/apis/core.halo.run/v1alpha1/photos/${formState.value.metadata.name}`,
        formState.value
      );
    } else {
      if (props.group) {
        formState.value.spec.groupName = props.group;
      }
      const { data } = await axiosInstance.post<Photo>(`/apis/core.halo.run/v1alpha1/photos`, formState.value);
      emit("saved", data);
    }
    modal.value?.close();
  } catch (e) {
    console.error(e);
  } finally {
    isSubmitting.value = false;
  }
};
</script>
<template>
  <VModal ref="modal" :title="modalTitle" :width="650" @close="emit('close')">
    <template #actions>
      <slot name="append-actions" />
    </template>

    <FormKit
      id="photo-form"
      v-model="formState.spec"
      name="photo-form"
      :actions="false"
      :config="{ validationVisibility: 'submit' }"
      type="form"
      @submit="handleSavePhoto"
    >
      <div class=":uno: md:grid md:grid-cols-4 md:gap-6">
        <div class=":uno: md:col-span-1">
          <div class=":uno: sticky top-0">
            <span class=":uno: text-base text-gray-900 font-medium"> 常规 </span>
          </div>
        </div>
        <div class=":uno: mt-5 md:col-span-3 md:mt-0 divide-y divide-gray-100">
          <FormKit name="displayName" label="名称" type="text" validation="required"></FormKit>
          <FormKit name="url" label="图片地址" type="attachment" :accepts="['image/*']" validation="required"></FormKit>
          <FormKit name="cover" label="封面" type="attachment" :accepts="['image/*']"></FormKit>
          <FormKit name="description" label="描述" type="textarea"></FormKit>
        </div>
      </div>
    </FormKit>
    <div class=":uno: py-5">
      <div class=":uno: border-t border-gray-200"></div>
    </div>
    <div class=":uno: md:grid md:grid-cols-4 md:gap-6">
      <div class=":uno: md:col-span-1">
        <div class=":uno: sticky top-0">
          <span class=":uno: text-base text-gray-900 font-medium"> 元数据 </span>
        </div>
      </div>
      <div class=":uno: mt-5 md:col-span-3 md:mt-0 divide-y divide-gray-100">
        <AnnotationsForm
          :key="formState.metadata.name"
          ref="annotationsFormRef"
          :value="formState.metadata.annotations"
          kind="Photo"
          group="core.halo.run"
        />
      </div>
    </div>
    <template #footer>
      <VSpace>
        <VButton :loading="isSubmitting" type="secondary" @click="submitForm('photo-form')"> 保存 </VButton>
        <VButton @click="modal?.close()">取消</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
