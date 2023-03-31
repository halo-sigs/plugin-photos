<script lang="ts" setup>
import { computed, nextTick, ref, watch } from "vue";
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
  Toast,
  IconArrowLeft,
  IconArrowRight,
  VDropdown,
  VDropdownItem,
} from "@halo-dev/components";
import GroupList from "../components/GroupList.vue";
import PhotoEditingModal from "@/components/PhotoEditingModal.vue";
import LazyImage from "@/components/LazyImage.vue";
import apiClient from "@/utils/api-client";
import type { Photo, PhotoList } from "@/types";
import Fuse from "fuse.js";
import RiImage2Line from "~icons/ri/image-2-line";
import type { AttachmentLike } from "@halo-dev/console-shared";
import { useQuery } from "@tanstack/vue-query";

const selectedPhoto = ref<Photo | undefined>();
const selectedPhotos = ref<Set<Photo>>(new Set<Photo>());
const selectedGroup = ref<string>();
const editingModal = ref(false);
const checkedAll = ref(false);
const groupListRef = ref();

const page = ref(1);
const size = ref(20);
const total = ref(0);
const searchText = ref("");
const keyword = ref("");

const {
  data: photos,
  isLoading,
  refetch,
} = useQuery<Photo[]>({
  queryKey: [page, size, keyword, selectedGroup],
  queryFn: async () => {
    if (!selectedGroup.value) {
      return [];
    }
    const { data } = await apiClient.get<PhotoList>(
      "/apis/api.plugin.halo.run/v1alpha1/plugins/PluginPhotos/photos",
      {
        params: {
          page: page.value,
          size: size.value,
          keyword: keyword.value,
          group: selectedGroup.value,
        },
      }
    );
    total.value = data.total;
    return data.items
      .map((group) => {
        if (group.spec) {
          group.spec.priority = group.spec.priority || 0;
        }
        return group;
      })
      .sort((a, b) => {
        return (a.spec?.priority || 0) - (b.spec?.priority || 0);
      });
  },
  refetchInterval(data: Photo[]) {
    const deletingGroups = data?.filter(
      (group) => !!group.metadata.deletionTimestamp
    );

    return deletingGroups?.length ? 1000 : false;
  },
  refetchOnWindowFocus: false,
});

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

const handleOpenEditingModal = (photo?: Photo) => {
  selectedPhoto.value = photo;
  editingModal.value = true;
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
            `/apis/core.halo.run/v1alpha1/photos/${photo.metadata.name}`
          );
        });
        await Promise.all(promises);
      } catch (e) {
        console.error(e);
      } finally {
        pageRefetch();
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

// create by attachments
const attachmentModal = ref(false);

const onAttachmentsSelect = async (attachments: AttachmentLike[]) => {
  const photos: {
    url: string;
    cover?: string;
    displayName?: string;
    type?: string;
  }[] = attachments
    .map((attachment) => {
      const post = {
        groupName: selectedGroup.value || "",
      };

      if (typeof attachment === "string") {
        return {
          ...post,
          url: attachment,
          cover: attachment,
        };
      }
      if ("url" in attachment) {
        return {
          ...post,
          url: attachment.url,
          cover: attachment.url,
        };
      }
      if ("spec" in attachment) {
        return {
          ...post,
          url: attachment.status?.permalink,
          cover: attachment.status?.permalink,
          displayName: attachment.spec.displayName,
          type: attachment.spec.mediaType,
        };
      }
    })
    .filter(Boolean) as {
    url: string;
    cover?: string;
    displayName?: string;
    type?: string;
  }[];

  for (const photo of photos) {
    const type = photo.type;
    if (!type) {
      Toast.error("只支持选择图片");
      nextTick(() => {
        attachmentModal.value = true;
      });

      return;
    }
    const fileType = type.split("/")[0];
    if (fileType !== "image") {
      Toast.error("只支持选择图片");
      nextTick(() => {
        attachmentModal.value = true;
      });
      return;
    }
  }

  const createRequests = photos.map((photo) => {
    return apiClient.post<Photo>("/apis/core.halo.run/v1alpha1/photos", {
      metadata: {
        name: "",
        generateName: "photo-",
      },
      spec: photo,
      kind: "Photo",
      apiVersion: "core.halo.run/v1alpha1",
    });
  });

  await Promise.all(createRequests);

  Toast.success(`新建成功，一共创建了 ${photos.length} 张图片。`);
  pageRefetch();
};

const groupSelectHandle = (group?: string) => {
  selectedGroup.value = group;
};

const pageRefetch = async () => {
  await groupListRef.value.refetch();
  await refetch();
  selectedPhotos.value = new Set<Photo>();
};
</script>
<template>
  <PhotoEditingModal
    v-model:visible="editingModal"
    :photo="selectedPhoto"
    :group="selectedGroup"
    @close="refetch()"
    @saved="pageRefetch"
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
  <AttachmentSelectorModal
    v-model:visible="attachmentModal"
    @select="onAttachmentsSelect"
  />
  <VPageHeader title="图库">
    <template #icon>
      <RiImage2Line class="photos-mr-2 photos-self-center" />
    </template>
  </VPageHeader>
  <div class="photos-p-4">
    <div class="photos-flex photos-flex-col photos-gap-2 sm:photos-flex-row">
      <div class="photos-w-full sm:photos-w-80">
        <GroupList ref="groupListRef" @select="groupSelectHandle" />
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
                    v-model="searchText"
                    placeholder="输入关键词搜索"
                    type="text"
                    @keyup.enter="keyword = searchText"
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
                  <VDropdown>
                    <VButton size="xs"> 新增 </VButton>
                    <template #popper>
                      <VDropdownItem @click="handleOpenEditingModal()">
                        新增
                      </VDropdownItem>
                      <VDropdownItem @click="attachmentModal = true">
                        从附件库选择
                      </VDropdownItem>
                    </template>
                  </VDropdown>
                </div>
              </div>
            </div>
          </template>
          <VLoading v-if="isLoading" />
          <Transition v-else-if="!searchResults.length" appear name="fade">
            <VEmpty message="你可以尝试刷新或者新建图片" title="当前没有图片">
              <template #actions>
                <VSpace>
                  <VButton @click="refetch"> 刷新</VButton>
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
              class="photos-mt-2 photos-grid photos-grid-cols-1 photos-gap-x-2 photos-gap-y-3 sm:photos-grid-cols-2 md:photos-grid-cols-3 lg:photos-grid-cols-4"
              role="list"
            >
              <VCard
                v-for="photo in photos"
                :key="photo.metadata.name"
                :body-class="['!p-0']"
                :class="{
                  'photos-ring-primary photos-ring-1': isChecked(photo),
                  'photos-ring-1 photos-ring-red-600':
                    photo.metadata.deletionTimestamp,
                }"
                class="hover:photos-shadow"
                @click="handleOpenEditingModal(photo)"
              >
                <div class="photos-group photos-relative photos-bg-white">
                  <div
                    class="photos-aspect-w-10 photos-aspect-h-8 photos-block photos-h-full photos-w-full photos-cursor-pointer photos-overflow-hidden photos-bg-gray-100"
                  >
                    <LazyImage
                      :key="photo.metadata.name"
                      :alt="photo.spec.displayName"
                      :src="photo.spec.cover || photo.spec.url"
                      classes="photos-w-full photos-h-40 photos-pointer-events-none photos-object-cover group-hover:photos-opacity-75"
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
                    @click.stop="
                      selectedPhotos.has(photo)
                        ? selectedPhotos.delete(photo)
                        : selectedPhotos.add(photo)
                    "
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
                <VPagination
                  v-model:page="page"
                  v-model:size="size"
                  :total="total"
                  :size-options="[20, 30, 50, 100]"
                />
              </div>
            </div>
          </template>
        </VCard>
      </div>
    </div>
  </div>
</template>
