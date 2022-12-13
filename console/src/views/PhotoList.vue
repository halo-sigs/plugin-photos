<script lang="ts" setup>
import { computed, ref, watch } from "vue";
import {
  VButton,
  VCard,
  VPageHeader,
  VPagination,
  VSpace,
  Dialog,
  VEmpty,
  IconAddCircle,
  VLoading,
  IconCheckboxFill,
} from "@halo-dev/components";
import GroupList from "../components/GroupList.vue";
import PhotoEditingModal from "@/components/PhotoEditingModal.vue";
import LazyImage from "@/components/LazyImage.vue";
import apiClient from "@/utils/api-client";
import type { Photo, PhotoGroup, PhotoList } from "@/types";
import cloneDeep from "lodash.clonedeep";
import Fuse from "fuse.js";

const drag = ref(false);
const photos = ref<Photo[]>([] as Photo[]);
const loading = ref(false);
const selectedPhoto = ref<Photo | undefined>();
const selectedPhotos = ref<Set<Photo>>(new Set<Photo>());
const selectedGroup = ref<PhotoGroup>();
const editingModal = ref(false);
const checkedAll = ref(false);
const groupListRef = ref();

const handleFetchPhotos = async (options?: { mute?: boolean }) => {
  try {
    if (!options?.mute) {
      loading.value = true;
    }

    if (!selectedGroup.value?.spec?.photos) {
      return;
    }

    const { data } = await apiClient.get<PhotoList>(
      "/apis/core.halo.run/v1alpha1/photos",
      {
        params: {
          fieldSelector: `name=(${selectedGroup.value.spec.photos.join(",")})`,
        },
      }
    );

    // sort by priority
    photos.value = data.items
      .map((photo) => {
        if (photo.spec) {
          photo.spec.priority = photo.spec.priority || 0;
        }
        return photo;
      })
      .sort((a, b) => {
        return (a.spec?.priority || 0) - (b.spec?.priority || 0);
      });
  } catch (e) {
    console.error("Failed to fetch photos", e);
  } finally {
    loading.value = false;
  }
};

const handleSelectPrevious = () => {
  const currentIndex = photos.value.findIndex(
    (photo) => photo.metadata.name === selectedPhoto.value?.metadata.name
  );

  if (currentIndex > 0) {
    selectedPhoto.value = photos.value[currentIndex - 1];
    return;
  }

  if (currentIndex <= 0) {
    selectedPhoto.value = undefined;
  }
};

const handleSelectNext = () => {
  if (!selectedPhoto.value) {
    selectedPhoto.value = photos.value[0];
    return;
  }
  const currentIndex = photos.value.findIndex(
    (photo) => photo.metadata.name === selectedPhoto.value?.metadata.name
  );
  if (currentIndex !== photos.value.length - 1) {
    selectedPhoto.value = photos.value[currentIndex + 1];
  }
};

const handleOpenCreateModal = (photo: Photo) => {
  selectedPhoto.value = photo;
  editingModal.value = true;
};

const handleSaveInBatch = async () => {
  try {
    const promises = photos.value?.map((photo: Photo, index) => {
      if (photo.spec) {
        photo.spec.priority = index;
      }
      return apiClient.put(
        `/apis/core.halo.run/v1alpha1/photos/${photo.metadata.name}`,
        photo
      );
    });
    if (promises) {
      await Promise.all(promises);
    }
  } catch (e) {
    console.error(e);
  } finally {
    await handleFetchPhotos({ mute: true });
  }
};

const onPhotoSaved = async (photo: Photo) => {
  const groupToUpdate = cloneDeep(selectedGroup.value);

  if (groupToUpdate) {
    groupToUpdate.spec.photos.push(photo.metadata.name);

    await apiClient.put(
      `/apis/core.halo.run/v1alpha1/photogroups/${groupToUpdate.metadata.name}`,
      groupToUpdate
    );
  }

  await groupListRef.value.handleFetchGroups();
  await handleFetchPhotos();
};

const handleDelete = (photo: Photo) => {
  Dialog.warning({
    title: "是否确认删除当前的图片？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        apiClient.delete(
          `/apis/core.halo.run/v1alpha1/photos/${photo.metadata.name}`
        );
      } catch (e) {
        console.error(e);
      } finally {
        handleFetchPhotos();
      }
    },
  });
};

const handleDeleteInBatch = () => {
  Dialog.warning({
    title: "是否确认删除所选的图片？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        const promises = Array.from(selectedPhotos.value).map((photo) => {
          return apiClient.delete(
            `/apis/core.halo.run/v1alpha1/photos/${photo}`
          );
        });
        if (promises) {
          await Promise.all(promises);
        }
      } catch (e) {
        console.error(e);
      } finally {
        await handleFetchPhotos();
      }
    },
  });
};

const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  handleCheckAll(checked);
};

const handleCheckAll = (checkAll: boolean) => {
  if (checkAll) {
    photos.value.forEach((photo) => {
      selectedPhotos.value.add(photo);
    });
  } else {
    selectedPhotos.value.clear();
  }
};

const isChecked = (photo: Photo) => {
  return (
    photo.metadata.name === selectedPhoto.value?.metadata.name ||
    Array.from(selectedPhotos.value)
      .map((item) => item.metadata.name)
      .includes(photo.metadata.name)
  );
};

watch(
  () => selectedPhotos.value.size,
  (newValue) => {
    checkedAll.value = newValue === photos.value.length;
  }
);

// search
const keyword = ref("");
let fuse: Fuse<Photo> | undefined = undefined;

watch(
  () => photos.value,
  () => {
    fuse = new Fuse(photos.value, {
      keys: [
        "spec.displayName",
        "metadata.name",
        "spec.description",
        "spec.url",
      ],
      useExtendedSearch: true,
    });
  }
);

const searchResults = computed({
  get() {
    if (!fuse || !keyword.value) {
      return photos.value;
    }

    return fuse?.search(keyword.value).map((item) => item.item);
  },
  set(value) {
    photos.value = value;
  },
});
</script>
<template>
  <PhotoEditingModal
    v-model:visible="editingModal"
    :photo="selectedPhoto"
    @close="handleFetchPhotos({ mute: true })"
    @saved="onPhotoSaved"
  >
    <template #append-actions>
      <span @click="handleSelectPrevious">
        <IconArrowLeft />
      </span>
      <span @click="handleSelectNext">
        <IconArrowRight />
      </span>
    </template>
  </PhotoEditingModal>
  <VPageHeader title="图片"> </VPageHeader>
  <div class="photos-p-4">
    <div class="photos-flex photos-flex-row photos-gap-2">
      <div class="photos-w-96">
        <GroupList
          ref="groupListRef"
          v-model:selected-group="selectedGroup"
          @select="handleFetchPhotos()"
        />
      </div>
      <div class="photos-flex-1">
        <VCard>
          <template #header>
            <div
              class="photos-block photos-w-full photos-bg-gray-50 photos-px-4 photos-py-3"
            >
              <div
                class="photos-relative photos-flex photos-flex-col photos-items-start sm:photos-flex-row sm:photos-items-center"
              >
                <div
                  class="photos-mr-4 photos-hidden photos-items-center sm:photos-flex"
                >
                  <input
                    v-model="checkedAll"
                    class="photos-h-4 photos-w-4 photos-rounded photos-border-gray-300 photos-text-indigo-600"
                    type="checkbox"
                    @change="handleCheckAllChange"
                  />
                </div>
                <div
                  class="photos-flex photos-w-full photos-flex-1 sm:photos-w-auto"
                >
                  <FormKit
                    v-if="!selectedPhotos.size"
                    v-model="keyword"
                    placeholder="输入关键词搜索"
                    type="text"
                  ></FormKit>
                  <VSpace v-else>
                    <VButton type="danger" @click="handleDeleteInBatch">
                      删除
                    </VButton>
                  </VSpace>
                </div>
                <div
                  v-permission="['plugin:photos:manage']"
                  class="photos-mt-4 photos-flex sm:photos-mt-0"
                >
                  <VButton size="xs" @click="editingModal = true">
                    新增
                  </VButton>
                </div>
              </div>
            </div>
          </template>
          <VLoading v-if="loading" />
          <Transition v-else-if="!searchResults.length" appear name="fade">
            <VEmpty message="你可以尝试刷新或者新建图片" title="当前没有图片">
              <template #actions>
                <VSpace>
                  <VButton @click="handleFetchPhotos"> 刷新</VButton>
                  <VButton
                    v-permission="['plugin:photos:manage']"
                    type="primary"
                    @click="editingModal = true"
                  >
                    <template #icon>
                      <IconAddCircle class="h-full w-full" />
                    </template>
                    新增图片
                  </VButton>
                </VSpace>
              </template>
            </VEmpty>
          </Transition>
          <Transition v-else appear name="fade">
            <div
              class="photos-mt-2 photos-grid photos-grid-cols-3 photos-gap-x-2 photos-gap-y-3 sm:photos-grid-cols-3 md:photos-grid-cols-6 xl:photos-grid-cols-8"
              role="list"
            >
              <VCard
                v-for="(photo, index) in photos"
                :key="index"
                :body-class="['!p-0']"
                :class="{
                  'ring-1 ring-primary': isChecked(photo),
                  'ring-1 ring-red-600': photo.metadata.deletionTimestamp,
                }"
                class="hover:shadow"
              >
                <div class="photos-group photos-relative photos-bg-white">
                  <div
                    class="photos-aspect-w-10 photos-aspect-h-8 photos-block photos-h-full photos-w-full photos-cursor-pointer photos-overflow-hidden photos-bg-gray-100"
                  >
                    <LazyImage
                      :key="photo.metadata.name"
                      :alt="photo.spec.displayName"
                      :src="photo.spec.cover || photo.spec.url"
                      classes="photos-pointer-events-none photos-object-cover group-hover:photos-opacity-75"
                    >
                      <template #loading>
                        <div
                          class="photos-flex photos-h-full photos-items-center photos-justify-center photos-object-cover"
                        >
                          <span class="photos-text-xs photos-text-gray-400"
                            >加载中...</span
                          >
                        </div>
                      </template>
                      <template #error>
                        <div
                          class="photos-flex photos-h-full photos-items-center photos-justify-center photos-object-cover"
                        >
                          <span class="photos-text-xs photos-text-red-400">
                            加载异常
                          </span>
                        </div>
                      </template>
                    </LazyImage>
                  </div>

                  <p
                    v-tooltip="photo.spec.displayName"
                    class="photos-block photos-cursor-pointer photos-truncate photos-px-2 photos-py-1 photos-text-center photos-text-xs photos-font-medium photos-text-gray-700"
                  >
                    {{ photo.spec.displayName }}
                  </p>

                  <div
                    v-if="photo.metadata.deletionTimestamp"
                    class="photos-absolute photos-top-1 photos-right-1 photos-text-xs photos-text-red-300"
                  >
                    删除中...
                  </div>

                  <div
                    v-if="!photo.metadata.deletionTimestamp"
                    v-permission="['plugin:photos:manage']"
                    :class="{ '!flex': selectedPhotos.has(photo) }"
                    class="photos-absolute photos-top-0 photos-left-0 photos-hidden photos-h-1/3 photos-w-full photos-cursor-pointer photos-justify-end photos-bg-gradient-to-b photos-from-gray-300 photos-to-transparent photos-ease-in-out group-hover:photos-flex"
                  >
                    <IconCheckboxFill
                      :class="{
                        '!text-primary': selectedPhotos.has(photo),
                      }"
                      class="hover:photos-text-primary photos-mt-1 photos-mr-1 photos-h-6 photos-w-6 photos-cursor-pointer photos-text-white photos-transition-all"
                    />
                  </div>
                </div>
              </VCard>
            </div>
          </Transition>

          <template #footer>
            <div
              class="photos-flex photos-items-center photos-justify-end photos-bg-white"
            >
              <div
                class="photos-flex photos-flex-1 photos-items-center photos-justify-end"
              >
                <VPagination :page="1" :size="10" :total="20" />
              </div>
            </div>
          </template>
        </VCard>
      </div>
    </div>
  </div>
</template>
<style lang="scss" scoped></style>
