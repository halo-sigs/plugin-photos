<script lang="ts" setup>
import LazyImage from "@/components/LazyImage.vue";
import PhotoEditingModal from "@/components/PhotoEditingModal.vue";
import type { Photo, PhotoList } from "@/types";
import { axiosInstance } from "@halo-dev/api-client";
import {
  Dialog,
  IconAddCircle,
  IconArrowLeft,
  IconArrowRight,
  IconCheckboxFill,
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
import type { AttachmentLike } from "@halo-dev/console-shared";
import { useQuery } from "@tanstack/vue-query";
import Fuse from "fuse.js";
import { computed, nextTick, ref, watch } from "vue";
import RiImage2Line from "~icons/ri/image-2-line";
import GroupList from "../components/GroupList.vue";

const selectedPhoto = ref<Photo | undefined>();
const selectedPhotos = ref<Set<Photo>>(new Set<Photo>());
const selectedGroup = ref<string>();
const editingModal = ref(false);
const checkedAll = ref(false);
const groupListRef = ref();

const page = ref(1);
const size = ref(20);
const total = ref(0);
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
    const { data } = await axiosInstance.get<PhotoList>("/apis/console.api.photo.halo.run/v1alpha1/photos", {
      params: {
        page: page.value,
        size: size.value,
        keyword: keyword.value,
        group: selectedGroup.value,
      },
    });
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
  refetchInterval(data) {
    const deletingGroups = data?.filter((group) => !!group.metadata.deletionTimestamp);

    return deletingGroups?.length ? 1000 : false;
  },
  refetchOnWindowFocus: false,
});

const handleSelectPrevious = () => {
  if (!photos.value) {
    return;
  }

  const currentIndex = photos.value.findIndex((photo) => photo.metadata.name === selectedPhoto.value?.metadata.name);

  if (currentIndex > 0) {
    selectedPhoto.value = photos.value[currentIndex - 1];
    return;
  }

  if (currentIndex <= 0) {
    selectedPhoto.value = undefined;
  }
};

const handleSelectNext = () => {
  if (!photos.value) {
    return;
  }

  if (!selectedPhoto.value) {
    selectedPhoto.value = photos.value[0];
    return;
  }
  const currentIndex = photos.value.findIndex((photo) => photo.metadata.name === selectedPhoto.value?.metadata.name);
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
          return axiosInstance.delete(`/apis/core.halo.run/v1alpha1/photos/${photo.metadata.name}`);
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
    photos.value?.forEach((photo) => {
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
    checkedAll.value = newValue === photos.value?.length;
  }
);

// search
let fuse: Fuse<Photo> | undefined = undefined;

watch(
  () => photos.value,
  () => {
    if (!photos.value) {
      return;
    }

    fuse = new Fuse(photos.value, {
      keys: ["spec.displayName", "metadata.name", "spec.description", "spec.url"],
      useExtendedSearch: true,
    });
  }
);

const searchResults = computed({
  get() {
    if (!fuse || !keyword.value) {
      return photos.value || [];
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
    return axiosInstance.post<Photo>("/apis/core.halo.run/v1alpha1/photos", {
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
  <AttachmentSelectorModal v-model:visible="attachmentModal" :accepts="['image/*']" @select="onAttachmentsSelect" />
  <VPageHeader title="图库">
    <template #icon>
      <RiImage2Line class="mr-2 self-center" />
    </template>
  </VPageHeader>
  <div class="p-4">
    <div class="flex flex-col gap-2 lg:flex-row">
      <div class="w-full flex-none lg:w-96">
        <GroupList ref="groupListRef" @select="groupSelectHandle" />
      </div>
      <div class="flex-1 shrink min-w-0">
        <VCard>
          <template #header>
            <div class="block w-full bg-gray-50 px-4 py-3">
              <div class="relative flex flex-col items-start sm:flex-row sm:items-center">
                <div class="mr-4 hidden items-center sm:flex">
                  <input v-model="checkedAll" type="checkbox" @change="handleCheckAllChange" />
                </div>
                <div class="flex w-full flex-1 sm:w-auto">
                  <SearchInput v-if="!selectedPhotos.size" v-model="keyword" />
                  <VSpace v-else>
                    <VButton type="danger" @click="handleDeleteInBatch"> 删除 </VButton>
                  </VSpace>
                </div>
                <div v-if="selectedGroup" v-permission="['plugin:photos:manage']" class="mt-4 flex sm:mt-0">
                  <VDropdown>
                    <VButton size="xs"> 新增 </VButton>
                    <template #popper>
                      <VDropdownItem @click="handleOpenEditingModal()"> 新增 </VDropdownItem>
                      <VDropdownItem @click="attachmentModal = true"> 从附件库选择 </VDropdownItem>
                    </template>
                  </VDropdown>
                </div>
              </div>
            </div>
          </template>
          <VLoading v-if="isLoading" />
          <Transition v-else-if="!selectedGroup" appear name="fade">
            <VEmpty message="请选择或新建分组" title="未选择分组"></VEmpty>
          </Transition>
          <Transition v-else-if="!searchResults.length" appear name="fade">
            <VEmpty message="你可以尝试刷新或者新建图片" title="当前没有图片">
              <template #actions>
                <VSpace>
                  <VButton @click="refetch"> 刷新</VButton>
                  <VButton v-permission="['plugin:photos:manage']" type="primary" @click="handleOpenEditingModal()">
                    <template #icon>
                      <IconAddCircle class="size-full" />
                    </template>
                    新增图片
                  </VButton>
                </VSpace>
              </template>
            </VEmpty>
          </Transition>
          <Transition v-else appear name="fade">
            <div class="mt-2 grid grid-cols-1 gap-x-2 gap-y-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5" role="list">
              <VCard
                v-for="photo in photos"
                :key="photo.metadata.name"
                :body-class="['!p-0']"
                :class="{
                  'ring-primary ring-1': isChecked(photo),
                  'ring-1 ring-red-600': photo.metadata.deletionTimestamp,
                }"
                class="hover:shadow"
                @click="handleOpenEditingModal(photo)"
              >
                <div class="group relative bg-white">
                  <div class="aspect-16/9 block size-full cursor-pointer overflow-hidden bg-gray-100">
                    <LazyImage
                      :key="photo.metadata.name"
                      :alt="photo.spec.displayName"
                      :src="photo.spec.cover || photo.spec.url"
                      classes="size-full pointer-events-none group-hover:opacity-75"
                    >
                      <template #loading>
                        <div class="flex h-full justify-center">
                          <VLoading></VLoading>
                        </div>
                      </template>
                      <template #error>
                        <div class="flex h-full items-center justify-center object-cover">
                          <span class="text-xs text-red-400"> 加载异常 </span>
                        </div>
                      </template>
                    </LazyImage>
                  </div>

                  <p
                    v-tooltip="photo.spec.displayName"
                    class="block cursor-pointer truncate px-2 py-1 text-center text-xs font-medium text-gray-700"
                  >
                    {{ photo.spec.displayName }}
                  </p>

                  <div v-if="photo.metadata.deletionTimestamp" class="absolute top-1 right-1 text-xs text-red-300">
                    删除中...
                  </div>

                  <div
                    v-if="!photo.metadata.deletionTimestamp"
                    v-permission="['plugin:photos:manage']"
                    :class="{ '!flex': selectedPhotos.has(photo) }"
                    class="absolute top-0 left-0 hidden h-1/3 w-full cursor-pointer justify-end bg-gradient-to-b from-gray-300 to-transparent ease-in-out group-hover:flex"
                    @click.stop="selectedPhotos.has(photo) ? selectedPhotos.delete(photo) : selectedPhotos.add(photo)"
                  >
                    <IconCheckboxFill
                      :class="{
                        '!text-primary': selectedPhotos.has(photo),
                      }"
                      class="hover:text-primary mt-1 mr-1 h-6 w-6 cursor-pointer text-white transition-all"
                    />
                  </div>
                </div>
              </VCard>
            </div>
          </Transition>

          <template #footer>
            <VPagination v-model:page="page" v-model:size="size" :total="total" :size-options="[20, 30, 50, 100]" />
          </template>
        </VCard>
      </div>
    </div>
  </div>
</template>
