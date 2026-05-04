<script lang="ts" setup>
import type { Photo } from "@/api/generated";
import { IconCheckboxFill } from "@halo-dev/components";
import { utils } from "@halo-dev/ui-shared";

const props = defineProps<{
  photo: Photo;
  selectMode: boolean;
  isSelected: boolean;
}>();

const emit = defineEmits<{
  (e: "select"): void;
  (e: "openEdit"): void;
}>();

function handleClick() {
  if (props.selectMode) {
    emit("select");
  } else {
    emit("openEdit");
  }
}
</script>
<template>
  <div
    :class="{
      ':uno: border-primary shadow-sm ring-2 ring-primary/20': isSelected,
      ':uno: border-red-300 ring-2 ring-red-100': photo.metadata.deletionTimestamp,
    }"
    class=":uno: group relative cursor-pointer overflow-hidden border border-gray-200 rounded-md bg-gray-100 transition-all hover:border-gray-300 hover:shadow-md hover:-translate-y-0.5"
    @click="handleClick"
  >
    <!-- Image -->
    <div class=":uno: aspect-[4/3] w-full overflow-hidden bg-gray-100">
      <img
        :key="photo.metadata.name"
        :alt="photo.spec.displayName"
        :src="utils.attachment.getThumbnailUrl(photo.spec.url, 'S')"
        class=":uno: size-full object-cover transition-transform duration-300 group-hover:scale-105"
        loading="lazy"
        decoding="async"
      />
    </div>

    <!-- Name overlay -->
    <div
      class=":uno: absolute inset-x-0 bottom-0 from-black/75 via-black/35 to-transparent bg-gradient-to-t px-2.5 pb-2 pt-8"
    >
      <p class=":uno: truncate text-xs text-white font-medium leading-5">
        {{ photo.spec.displayName }}
      </p>
      <p v-if="photo.exif?.dateTimeOriginal" class=":uno: truncate text-[10px] text-white/70 leading-4">
        {{ new Date(photo.exif.dateTimeOriginal).toLocaleDateString("zh-CN") }}
      </p>
    </div>

    <!-- Deleting badge -->
    <div
      v-if="photo.metadata.deletionTimestamp"
      class=":uno: absolute inset-0 flex items-center justify-center bg-white/70 backdrop-blur-[1px]"
    >
      <span class=":uno: rounded-full bg-red-500 px-2.5 py-1 text-xs text-white font-medium shadow-sm">
        删除中...
      </span>
    </div>

    <!-- Batch select checkbox -->
    <div
      v-if="!photo.metadata.deletionTimestamp"
      v-permission="['plugin:photos:manage']"
      class=":uno: absolute left-2 top-2 z-10"
    >
      <div
        :class="[
          ':uno: flex h-6 w-6 cursor-pointer items-center justify-center rounded-md border transition-all',
          {
            ':uno: border-primary bg-primary opacity-100 shadow-sm': isSelected,
            ':uno: border-white/80 bg-black/30 opacity-0 backdrop-blur-sm group-hover:opacity-100 hover:bg-black/50':
              !isSelected,
            ':uno: !opacity-100': selectMode,
          },
        ]"
        @click.stop="emit('select')"
      >
        <IconCheckboxFill v-if="isSelected" class=":uno: h-3.5 w-3.5 text-white" />
      </div>
    </div>

    <!-- Tags mini badges -->
    <div
      v-if="photo.spec.tags?.length"
      class=":uno: absolute right-2 top-2 max-w-[58%] flex flex-wrap justify-end gap-1"
    >
      <span
        v-for="tag in photo.spec.tags.slice(0, 2)"
        :key="tag"
        class=":uno: max-w-[6rem] truncate rounded bg-black/45 px-1.5 py-0.5 text-[10px] text-white leading-none backdrop-blur-sm"
      >
        {{ tag }}
      </span>
      <span
        v-if="photo.spec.tags.length > 2"
        class=":uno: rounded bg-black/45 px-1.5 py-0.5 text-[10px] text-white leading-none backdrop-blur-sm"
      >
        +{{ photo.spec.tags.length - 2 }}
      </span>
    </div>
  </div>
</template>
