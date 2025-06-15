<script lang="ts" setup>
import { onMounted, ref } from "vue";

const props = withDefaults(
  defineProps<{
    src: string;
    alt?: string;
    classes?: string | string[];
  }>(),
  {
    src: "",
    alt: "",
    classes: "",
  }
);

const isLoading = ref(false);
const error = ref(false);

const loadImage = async () => {
  const image = new Image();
  image.src = props.src;
  return new Promise((resolve, reject) => {
    image.onload = () => resolve(image);
    image.onerror = (err) => reject(err);
  });
};

onMounted(async () => {
  isLoading.value = true;
  try {
    await loadImage();
  } catch (e) {
    error.value = true;
  } finally {
    isLoading.value = false;
  }
});
</script>
<template>
  <div :class="classes">
    <template v-if="isLoading">
      <slot name="loading"> loading... </slot>
    </template>
    <template v-else-if="error">
      <slot name="error"> error </slot>
    </template>
    <img v-else class=":uno: size-full object-cover" :src="src" :alt="alt" />
  </div>
</template>
