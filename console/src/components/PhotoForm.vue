<script lang="ts" setup>
import { useGroupsFetch } from "@/composables/useGroupsFetch";
import { usePhotoTags } from "@/composables/usePhotoTags";
import type { PhotoFormState } from "@/types";
import { utils } from "@halo-dev/ui-shared";
import { computed, nextTick, ref } from "vue";

 defineProps<{
  name?: string;
  formState?: PhotoFormState;
}>();

const emit = defineEmits<{
  (event: "submit", data: PhotoFormState): void;
}>();

const { tagOptions } = usePhotoTags();

const { data: groups } = useGroupsFetch();

const groupOptions = computed(() => {
  return [
    { label: "无分组", value: "" },
    ...(groups.value?.map((g) => ({
      label: g.spec.displayName || g.metadata?.name,
      value: g.metadata?.name,
    })) || []),
  ];
});

const annotationsForm = ref();

async function onSubmit(data: PhotoFormState) {
  annotationsForm.value?.handleSubmit();
  await nextTick();
  const { customAnnotations, annotations, customFormInvalid, specFormInvalid } = annotationsForm.value || {};
  if (customFormInvalid || specFormInvalid) {
    return;
  }
  emit("submit", {
    ...data,
    annotations: {
      ...annotations,
      ...customAnnotations,
    },
  });
}
</script>
<template>
  <FormKit
    id="photo-form"
    name="photo-form"
    :config="{ validationVisibility: 'submit' }"
    :preserve="true"
    type="form"
    @submit="onSubmit"
  >
    <div class=":uno: grid gap-4 md:grid-cols-4 md:gap-6">
      <div class=":uno: md:col-span-1">
        <div class=":uno: sticky top-0">
          <span class=":uno: text-base text-gray-900 font-medium">常规</span>
        </div>
      </div>
      <div class=":uno: md:col-span-3">
        <FormKit
          name="url"
          label="图片地址"
          type="attachment"
          width="50%"
          aspect-ratio="16/9"
          :accepts="['image/*']"
          validation="required"
          :value="formState?.url"
        ></FormKit>
        <FormKit
          name="displayName"
          label="名称"
          type="text"
          validation="required"
          :value="formState?.displayName"
        ></FormKit>
        <FormKit
          name="groupName"
          label="分组"
          type="select"
          :options="groupOptions"
          :value="formState?.groupName"
        ></FormKit>
        <FormKit name="description" label="描述" type="textarea" :value="formState?.description"></FormKit>
        <FormKit
          name="tags"
          label="标签"
          type="select"
          :multiple="true"
          :searchable="true"
          :allowCreate="true"
          :options="tagOptions"
          placeholder="输入标签"
          :value="formState?.tags"
        ></FormKit>
      </div>
    </div>
  </FormKit>

  <!-- EXIF Info Display -->
  <div v-if="formState?.exif" class=":uno: py-5">
    <div class=":uno: border-t border-gray-200"></div>
  </div>

  <div v-if="formState?.exif" class=":uno: grid gap-4 md:grid-cols-4 md:gap-6">
    <div class=":uno: md:col-span-1">
      <div class=":uno: sticky top-0">
        <span class=":uno: text-base text-gray-900 font-medium">EXIF 信息</span>
      </div>
    </div>
    <div class=":uno: md:col-span-3">
      <div class=":uno: grid grid-cols-1 gap-2 text-sm sm:grid-cols-2">
        <div v-if="formState.exif?.make" class=":uno: rounded-md bg-gray-50 px-3 py-2">
          <span class=":uno: block text-xs text-gray-500">相机品牌</span>{{ formState.exif.make }}
        </div>
        <div v-if="formState.exif?.model" class=":uno: rounded-md bg-gray-50 px-3 py-2">
          <span class=":uno: block text-xs text-gray-500">相机型号</span>{{ formState.exif.model }}
        </div>
        <div v-if="formState.exif?.lensModel" class=":uno: rounded-md bg-gray-50 px-3 py-2">
          <span class=":uno: block text-xs text-gray-500">镜头</span>{{ formState.exif.lensModel }}
        </div>
        <div v-if="formState.exif?.dateTimeOriginal" class=":uno: rounded-md bg-gray-50 px-3 py-2">
          <span class=":uno: block text-xs text-gray-500">拍摄时间</span
          >{{ utils.date.format(formState.exif.dateTimeOriginal) }}
        </div>
        <div v-if="formState.exif?.fnumber" class=":uno: rounded-md bg-gray-50 px-3 py-2">
          <span class=":uno: block text-xs text-gray-500">光圈</span>f/{{ formState.exif.fnumber }}
        </div>
        <div v-if="formState.exif?.exposureTime" class=":uno: rounded-md bg-gray-50 px-3 py-2">
          <span class=":uno: block text-xs text-gray-500">快门速度</span>{{ formState.exif.exposureTime }}s
        </div>
        <div v-if="formState.exif?.iso" class=":uno: rounded-md bg-gray-50 px-3 py-2">
          <span class=":uno: block text-xs text-gray-500">ISO</span>{{ formState.exif.iso }}
        </div>
        <div v-if="formState.exif?.focalLength" class=":uno: rounded-md bg-gray-50 px-3 py-2">
          <span class=":uno: block text-xs text-gray-500">焦距</span>{{ formState.exif.focalLength }}mm
        </div>
        <div
          v-if="formState.exif?.imageWidth && formState.exif?.imageHeight"
          class=":uno: rounded-md bg-gray-50 px-3 py-2"
        >
          <span class=":uno: block text-xs text-gray-500">尺寸</span>{{ formState.exif.imageWidth }} x
          {{ formState.exif.imageHeight }}
        </div>
        <div
          v-if="formState.exif?.gpsLatitude && formState.exif?.gpsLongitude"
          class=":uno: rounded-md bg-gray-50 px-3 py-2 sm:col-span-2"
        >
          <span class=":uno: block text-xs text-gray-500">位置</span>{{ formState.exif.gpsLatitude?.toFixed(6) }},
          {{ formState.exif.gpsLongitude?.toFixed(6) }}
        </div>
      </div>
    </div>
  </div>
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
        :key="name"
        ref="annotationsForm"
        :value="formState?.annotations || {}"
        kind="Photo"
        group="core.halo.run"
      />
    </div>
  </div>
</template>
