<script lang="ts" setup>
import type { PhotoGroupFormState } from "@/types";
import { nextTick, ref } from "vue";

withDefaults(
  defineProps<{
    formState?: PhotoGroupFormState;
  }>(),
  {
    formState: undefined,
  },
);

const emit = defineEmits<{
  (event: "submit", data: PhotoGroupFormState): void;
}>();

const annotationsForm = ref();

async function onSubmit({ displayName, priority }: { displayName: string; priority: number }) {
  annotationsForm.value?.handleSubmit();
  await nextTick();
  const { customAnnotations, annotations, customFormInvalid, specFormInvalid } = annotationsForm.value || {};
  if (customFormInvalid || specFormInvalid) {
    return;
  }

  emit("submit", {
    displayName: displayName,
    priority: priority,
    annotations: {
      ...annotations,
      ...customAnnotations,
    },
  });
}
</script>
<template>
  <FormKit id="photo-group-form" name="photo-group-form" type="form" @submit="onSubmit">
    <div class=":uno: grid gap-4 md:grid-cols-4 md:gap-6">
      <div class=":uno: md:col-span-1">
        <div class=":uno: sticky top-0">
          <span class=":uno: text-base text-gray-900 font-medium"> 常规 </span>
        </div>
      </div>
      <div class=":uno: md:col-span-3">
        <FormKit v-if="formState?.name" label="名称" :value="formState?.name" type="text" readonly></FormKit>
        <FormKit
          name="displayName"
          label="显示名称"
          type="text"
          validation="required"
          :value="formState?.displayName"
        ></FormKit>
        <FormKit
          name="priority"
          label="权重"
          type="number"
          validation="required|number"
          :value="formState?.priority ?? 0"
          help="数字越大越靠前"
        ></FormKit>
      </div>
    </div>
  </FormKit>
  <div class=":uno: py-5">
    <div class=":uno: border-t border-gray-200"></div>
  </div>
  <div class=":uno: grid gap-4 md:grid-cols-4 md:gap-6">
    <div class=":uno: md:col-span-1">
      <div class=":uno: sticky top-0">
        <span class=":uno: text-base text-gray-900 font-medium">元数据</span>
      </div>
    </div>
    <div class=":uno: md:col-span-3">
      <AnnotationsForm
        ref="annotationsForm"
        :value="formState?.annotations || {}"
        kind="PhotoGroup"
        group="core.halo.run"
      />
    </div>
  </div>
</template>
