<script lang="ts" setup>
import { photosCoreApiClient } from "@/api";
import type { Photo } from "@/api/generated";
import AddButton from "@/components/AddButton.vue";
import GroupFilter from "@/components/GroupFilter.vue";
import PhotoGridItem from "@/components/PhotoGridItem.vue";
import PhotoTable from "@/components/PhotoTable.vue";
import { useBatchOperations } from "@/composables/useBatchOperations";
import { ALL_GROUPS, UNGROUPED, useGroupSelection } from "@/composables/useGroupSelection";
import { QK_PHOTO_GROUPS, useGroupsFetch } from "@/composables/useGroupsFetch";
import { usePhotoSelection } from "@/composables/usePhotoSelection";
import { QK_PHOTOS, usePhotosFetch } from "@/composables/usePhotosFetch";
import { QK_PHOTO_TAGS, usePhotoTags } from "@/composables/usePhotoTags";
import {
  Dialog,
  IconAddCircle,
  IconArrowLeft,
  IconArrowRight,
  IconExternalLinkLine,
  IconGrid,
  IconList,
  IconRefreshLine,
  Toast,
  VButton,
  VCard,
  VDropdown,
  VDropdownItem,
  VEmpty,
  VLoading,
  VPageHeader,
  VPagination,
  VSpace,
} from "@halo-dev/components";
import { utils } from "@halo-dev/ui-shared";
import { useQueryClient } from "@tanstack/vue-query";
import { useLocalStorage } from "@vueuse/core";
import { useRouteQuery } from "@vueuse/router";
import { cloneDeep } from "es-toolkit";
import { computed, defineAsyncComponent, ref, shallowRef, watch } from "vue";
import RiImage2Line from "~icons/ri/image-2-line";

const queryClient = useQueryClient();

// Modals are heavy and only render when their `v-if` flag flips on,
// so lazy-load them to keep the initial bundle small.
const PhotoEditingModal = defineAsyncComponent(() => import("@/components/PhotoEditingModal.vue"));
const PhotoUploadModal = defineAsyncComponent(() => import("@/components/PhotoUploadModal.vue"));

// ==================== Group selection (sentinel-aware) ====================
const { selectedGroup, resolveGroupForWrite } = useGroupSelection();

// ==================== View mode ====================

const viewModes = [
  {
    name: "grid",
    icon: IconGrid,
    tooltip: "网格视图",
  },
  {
    name: "list",
    icon: IconList,
    tooltip: "列表视图",
  },
];

const viewMode = useLocalStorage<string>("plugin:photos:viewMode", "grid");

// ==================== Tags ====================
const { tagOptions } = usePhotoTags();

// ==================== Listing state ====================
const page = useRouteQuery<number>("page", 1);
const size = useRouteQuery<number>("size", 60);
const keyword = useRouteQuery<string>("keyword", "");
const tagFilter = useRouteQuery<string | undefined>("tag");
const sortOptions = [
  { label: "默认", value: undefined },
  { label: "拍摄时间（新→旧）", value: "exif.dateTimeOriginal,desc" },
  { label: "拍摄时间（旧→新）", value: "exif.dateTimeOriginal,asc" },
  { label: "创建时间（新→旧）", value: "metadata.creationTimestamp,desc" },
  { label: "创建时间（旧→新）", value: "metadata.creationTimestamp,asc" },
];

const selectedSort = useRouteQuery<string | undefined>("sort", undefined);

// ==================== Modals & dialogs ====================
const selectedPhoto = shallowRef<Photo | undefined>();
const editingModal = shallowRef(false);
const uploadModal = shallowRef(false);

const { data: groups } = useGroupsFetch();

const {
  data: photos,
  isLoading,
  refetch,
  isFetching,
} = usePhotosFetch({
  page,
  size,
  keyword,
  selectedGroup,
  tagFilter,
  selectedSort,
});

// ==================== Selection ====================
const { selectedPhotos, selectedCount, checkedAll, isSelected, toggle, setAll, clear } = usePhotoSelection(
  computed(() => photos.value?.items || []),
);

// ==================== Batch operations ====================
const { isBatchOperating, runWithConcurrency } = useBatchOperations();

// ==================== Photo navigation ====================
const handleSelectPrevious = () => {
  if (!photos.value) return;
  const currentIndex = photos.value.items.findIndex(
    (photo) => photo.metadata.name === selectedPhoto.value?.metadata.name,
  );
  if (currentIndex > 0) {
    selectedPhoto.value = photos.value.items[currentIndex - 1];
    return;
  }
  if (currentIndex <= 0) {
    selectedPhoto.value = undefined;
  }
};

const handleSelectNext = () => {
  if (!photos.value) return;
  if (!selectedPhoto.value) {
    selectedPhoto.value = photos.value.items[0];
    return;
  }
  const currentIndex = photos.value.items.findIndex(
    (photo) => photo.metadata.name === selectedPhoto.value?.metadata.name,
  );
  if (currentIndex !== photos.value.items.length - 1) {
    selectedPhoto.value = photos.value.items[currentIndex + 1];
  }
};

// ==================== Batch handlers ====================
const handleDeleteInBatch = () => {
  Dialog.warning({
    title: `是否确认删除所选的 ${selectedCount.value} 张图片？`,
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      isBatchOperating.value = true;
      try {
        const items = Array.from(selectedPhotos.value);
        await runWithConcurrency(items, (photo) =>
          photosCoreApiClient.photo.deletePhoto({
            name: photo.metadata.name,
          }),
        );
        Toast.success(`已删除 ${items.length} 张图片`);
      } catch {
        Toast.error("部分图片删除失败，请重试");
      } finally {
        isBatchOperating.value = false;
        clear();
        refetchAll();
      }
    },
  });
};

const batchMoveGroup = shallowRef("");

const handleBatchMoveGroup = async () => {
  if (!batchMoveGroup.value) return;
  const items = Array.from(selectedPhotos.value);
  isBatchOperating.value = true;
  try {
    await runWithConcurrency(items, (photo) => {
      const update = cloneDeep(photo);
      update.spec.groupName = batchMoveGroup.value;
      return photosCoreApiClient.photo.updatePhoto({
        name: photo.metadata.name,
        photo: update,
      });
    });
    Toast.success(`已移动 ${items.length} 张图片到目标分组`);
    clear();
  } catch {
    Toast.error("部分图片移动失败，请重试");
  } finally {
    isBatchOperating.value = false;
    batchMoveGroup.value = "";
    refetchAll();
  }
};

const batchTags = ref<string[]>([]);
const handleBatchAddTags = async () => {
  if (!batchTags.value.length) return;
  const items = Array.from(selectedPhotos.value);
  isBatchOperating.value = true;
  try {
    await runWithConcurrency(items, (photo) => {
      const update = cloneDeep(photo);
      const existing = new Set(update.spec.tags || []);
      batchTags.value.forEach((tag) => existing.add(tag));
      update.spec.tags = Array.from(existing);
      return photosCoreApiClient.photo.updatePhoto({
        name: photo.metadata.name,
        photo: update,
      });
    });
    Toast.success(`已为 ${items.length} 张图片添加标签`);
    clear();
  } catch {
    Toast.error("部分标签添加失败，请重试");
  } finally {
    isBatchOperating.value = false;
    batchTags.value = [];
    refetchAll();
  }
};

const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  setAll(checked);
};

const hasFilters = computed(() => {
  return tagFilter.value || selectedSort.value || keyword.value;
});

function handleClearFilters() {
  tagFilter.value = "";
  selectedSort.value = undefined;
}

watch([keyword, tagFilter, selectedSort, selectedGroup], () => {
  page.value = 1;
});

// ==================== Refetch helpers ====================
const refetchAll = async () => {
  queryClient.invalidateQueries({ queryKey: [QK_PHOTOS] });
  queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
  queryClient.invalidateQueries({ queryKey: [QK_PHOTO_TAGS] });
  clear();
};

const handleOpenEditingModal = (photo?: Photo) => {
  selectedPhoto.value = photo;
  editingModal.value = true;
};

const onEditingModalClose = () => {
  editingModal.value = false;
  selectedPhoto.value = undefined;
};

// ==================== Computed ====================
const currentGroupName = computed(() => {
  if (selectedGroup.value === ALL_GROUPS) {
    return "全部";
  }
  if (selectedGroup.value === UNGROUPED) {
    return "未分组";
  }
  const group = groups.value?.find((g) => g.metadata.name === selectedGroup.value);
  return group?.spec.displayName || "全部";
});

const hasActiveFilters = computed(() => !!keyword.value || !!tagFilter.value);

const handleRouteToFront = () => {
  window.open("/photos", "_blank");
};

function onUploadModalClose() {
  uploadModal.value = false;
  queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
  queryClient.invalidateQueries({ queryKey: [QK_PHOTOS] });
}
</script>

<template>
  <VPageHeader title="图库">
    <template #icon>
      <RiImage2Line />
    </template>
    <template #actions>
      <VButton @click="handleRouteToFront" size="sm" ghost>
        <template #icon>
          <IconExternalLinkLine class=":uno: size-full" />
        </template>
        跳转到前台
      </VButton>
    </template>
  </VPageHeader>

  <div class=":uno: m-0 md:m-4">
    <GroupFilter />

    <VCard :body-class="['!p-0']">
      <template #header>
        <div class=":uno: block w-full bg-gray-50 px-4 py-3">
          <div class=":uno: relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center">
            <div v-if="utils.permission.has(['plugin:photos:manage'])" class=":uno: hidden items-center sm:flex">
              <input
                v-model="checkedAll"
                :disabled="!photos?.items?.length || isBatchOperating"
                type="checkbox"
                @change="handleCheckAllChange"
              />
            </div>
            <div class=":uno: w-full flex flex-1 items-center sm:w-auto">
              <SearchInput v-if="selectedCount === 0" v-model="keyword" />
              <VSpace v-else>
                <VButton type="danger" :disabled="isBatchOperating" @click="handleDeleteInBatch"> 删除 </VButton>
                <VDropdown>
                  <VButton :disabled="isBatchOperating"> 移动到分组 </VButton>
                  <template #popper>
                    <VDropdownItem
                      v-for="g in groups"
                      :key="g.metadata.name"
                      @click="
                        batchMoveGroup = g.metadata.name;
                        handleBatchMoveGroup();
                      "
                    >
                      {{ g.spec.displayName }}
                    </VDropdownItem>
                  </template>
                </VDropdown>
                <VDropdown>
                  <VButton :disabled="isBatchOperating"> 添加标签 </VButton>
                  <template #popper>
                    <div class=":uno: min-w-[200px] p-2">
                      <FormKit
                        v-model="batchTags"
                        type="select"
                        :multiple="true"
                        :searchable="true"
                        :allowCreate="true"
                        :options="tagOptions"
                        placeholder="输入标签"
                      />
                      <VButton type="secondary" class=":uno: mt-2 w-full" @click="handleBatchAddTags">
                        确认添加
                      </VButton>
                    </div>
                  </template>
                </VDropdown>
                <VButton :disabled="isBatchOperating" @click="clear()"> 取消选择 </VButton>
              </VSpace>
            </div>
            <VSpace spacing="lg" class=":uno: flex-wrap">
              <FilterCleanButton v-if="hasFilters" @click="handleClearFilters" />
              <FilterDropdown
                v-model="tagFilter"
                label="标签"
                :items="[
                  {
                    label: '全部',
                  },
                  ...tagOptions,
                ]"
              />
              <FilterDropdown v-model="selectedSort" label="排序" :items="sortOptions" />
              <div class=":uno: flex flex-row gap-2">
                <div
                  v-for="(item, index) in viewModes"
                  :key="index"
                  v-tooltip="`${item.tooltip}`"
                  :class="{
                    ':uno: bg-gray-200 font-bold text-black': viewMode === item.name,
                  }"
                  class=":uno: cursor-pointer rounded p-1 hover:bg-gray-200"
                  @click="viewMode = item.name"
                >
                  <component :is="item.icon" class=":uno: h-4 w-4" />
                </div>
              </div>
              <div class=":uno: flex flex-row gap-2">
                <button
                  @click="refetch()"
                  class=":uno: group cursor-pointer rounded p-1 hover:bg-gray-200"
                  v-tooltip="'刷新'"
                >
                  <IconRefreshLine
                    :class="{ ':uno: animate-spin text-gray-900': isFetching }"
                    class=":uno: h-4 w-4 text-gray-600 group-hover:text-gray-900"
                  />
                </button>
              </div>
              <AddButton :default-group="resolveGroupForWrite(selectedGroup)" />
            </VSpace>
          </div>
        </div>
      </template>
      <!-- Content -->
      <VLoading v-if="isLoading" class=":uno: py-16" />

      <Transition v-else-if="!photos?.items?.length" appear name="fade">
        <div class=":uno: min-h-[420px] flex items-center justify-center px-4">
          <VEmpty
            :message="hasActiveFilters ? '没有符合当前筛选条件的图片。' : `当前分组「${currentGroupName}」还没有图片。`"
            title="当前没有图片"
          >
            <template #actions>
              <VSpace>
                <VButton @click="refetch">刷新</VButton>
                <VButton
                  v-if="utils.permission.has(['plugin:photos:manage'])"
                  type="primary"
                  @click="uploadModal = true"
                >
                  <template #icon>
                    <IconAddCircle class=":uno: h-4 w-4" />
                  </template>
                  上传图片
                </VButton>
              </VSpace>
            </template>
          </VEmpty>
        </div>
      </Transition>

      <div v-else class=":uno: p-4" :class="viewMode === 'list' ? ':uno: !p-0' : ''">
        <div
          v-if="viewMode === 'grid'"
          class=":uno: grid grid-cols-2 gap-3 2xl:grid-cols-7 lg:grid-cols-5 md:grid-cols-4 sm:grid-cols-3 xl:grid-cols-6"
        >
          <PhotoGridItem
            v-for="photo in photos.items"
            :key="photo.metadata.name"
            :photo="photo"
            :select-mode="selectedCount > 0"
            :is-selected="isSelected(photo)"
            @select="toggle(photo)"
            @open-edit="handleOpenEditingModal(photo)"
          />
        </div>

        <PhotoTable
          v-else-if="viewMode === 'list'"
          :photos="photos.items"
          :is-selected="isSelected"
          @toggle-select="toggle"
          @open-edit="handleOpenEditingModal"
        />
      </div>

      <template #footer>
        <VPagination v-model:page="page" v-model:size="size" :total="photos?.total || 0" :size-options="[30, 60, 120, 240]" />
      </template>
    </VCard>
  </div>

  <PhotoEditingModal v-if="editingModal && selectedPhoto" :photo="selectedPhoto" @close="onEditingModalClose">
    <template #append-actions>
      <span @click="handleSelectPrevious">
        <IconArrowLeft />
      </span>
      <span @click="handleSelectNext">
        <IconArrowRight />
      </span>
    </template>
  </PhotoEditingModal>

  <PhotoUploadModal
    v-if="uploadModal"
    :default-group="resolveGroupForWrite(selectedGroup)"
    @close="onUploadModalClose"
  />
</template>
