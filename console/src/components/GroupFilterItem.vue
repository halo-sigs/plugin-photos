<script lang="ts" setup>
defineProps<{
  selected: boolean;
  displayName: string;
}>();
</script>
<template>
  <button
    type="button"
    :class="[
      ':uno: relative inline-flex min-h-9 cursor-pointer items-center gap-1.5 rounded-md border text-sm font-medium transition-all select-none px-2.5 py-1.5',
      {
        ':uno: border-primary/30 bg-primary/5 text-primary shadow-sm': selected,
        ':uno: border-gray-200 bg-white text-gray-600 hover:border-gray-300 hover:bg-gray-50': !selected,
      },
    ]"
  >
    <span class=":uno: max-w-[12rem] truncate">{{ displayName }}</span>
    <slot />
  </button>
</template>
