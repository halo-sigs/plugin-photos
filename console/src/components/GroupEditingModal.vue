<script lang="ts" setup>
import type { PhotoGroup } from "@/types";
import { submitForm } from "@formkit/core";
import { axiosInstance } from "@halo-dev/api-client";
import { VButton, VModal, VSpace } from "@halo-dev/components";
import { useMagicKeys } from "@vueuse/core";
import { cloneDeep } from "es-toolkit";
import { computed, nextTick, onMounted, ref, useTemplateRef, watch } from "vue";

const props = withDefaults(
  defineProps<{
    group?: PhotoGroup;
  }>(),
  {
    group: undefined,
  },
);

const emit = defineEmits<{
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
const isSubmitting = ref(false);
const modal = useTemplateRef<InstanceType<typeof VModal> | null>("modal");

const isUpdateMode = computed(() => {
  return !!formState.value.metadata.creationTimestamp;
});

const isMac = /macintosh|mac os x/i.test(navigator.userAgent);

const modalTitle = computed(() => {
  return isUpdateMode.value ? "编辑分组" : "新建分组";
});

const annotationsGroupFormRef = ref();

const handleCreateOrUpdateGroup = async () => {
  annotationsGroupFormRef.value?.handleSubmit();
  await nextTick();
  const { customAnnotations, annotations, customFormInvalid, specFormInvalid } = annotationsGroupFormRef.value || {};
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
      await axiosInstance.put(
        `/apis/core.halo.run/v1alpha1/photogroups/${formState.value.metadata.name}`,
        formState.value,
      );
    } else {
      await axiosInstance.post("/apis/core.halo.run/v1alpha1/photogroups", formState.value);
    }
    modal.value?.close();
  } catch (e) {
    console.error("Failed to create photo group", e);
  } finally {
    isSubmitting.value = false;
  }
};

onMounted(() => {
  if (props.group) {
    formState.value = cloneDeep(props.group);
  }
});

const { ControlLeft_Enter, Meta_Enter } = useMagicKeys();

watch(
  () => ControlLeft_Enter?.value,
  (v) => {
    if (v && !isMac) {
      submitForm("photo-group-form");
    }
  },
);

watch(
  () => Meta_Enter?.value,
  (v) => {
    if (v && isMac) {
      submitForm("photo-group-form");
    }
  },
);
</script>
<template>
  <VModal ref="modal" :width="600" :title="modalTitle" @close="emit('close')">
    <FormKit
      id="photo-group-form"
      v-model="formState.spec"
      name="photo-group-form"
      type="form"
      @submit="handleCreateOrUpdateGroup"
    >
      <div class=":uno: md:grid md:grid-cols-4 md:gap-6">
        <div class=":uno: md:col-span-1">
          <div class=":uno: sticky top-0">
            <span class=":uno: text-base text-gray-900 font-medium"> 常规 </span>
          </div>
        </div>
        <div class=":uno: mt-5 md:col-span-3 md:mt-0 divide-y divide-gray-100">
          <FormKit
            name="displayName"
            label="分组名称"
            type="text"
            validation="required"
            help="可根据此名称查询图片"
          ></FormKit>
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
          ref="annotationsGroupFormRef"
          :value="formState.metadata.annotations"
          kind="PhotoGroup"
          group="core.halo.run"
        />
      </div>
    </div>
    <template #footer>
      <VSpace>
        <VButton :loading="isSubmitting" type="secondary" @click="submitForm('photo-group-form')">
          提交 {{ `${isMac ? "⌘" : "Ctrl"} + ↵` }}
        </VButton>
        <VButton @click="emit('close')">取消 Esc</VButton>
      </VSpace>
    </template>
  </VModal>
</template>
