<script lang="ts" setup>
import { photosConsoleApiClient } from "@/api";
import type { PhotoGroup } from "@/api/generated";
import { ALL_GROUPS, UNGROUPED, useGroupSelection } from "@/composables/useGroupSelection";
import { QK_PHOTO_GROUPS, useGroupsFetch } from "@/composables/useGroupsFetch";
import { QK_PHOTOS } from "@/composables/usePhotosFetch";
import { QK_PHOTO_TAGS } from "@/composables/usePhotoTags";
import { Dialog, IconAddCircle, IconMore, Toast, VDropdown, VDropdownItem, VStatusDot } from "@halo-dev/components";
import { useQueryClient } from "@tanstack/vue-query";
import { defineAsyncComponent, ref } from "vue";
import GroupFilterItem from "./GroupFilterItem.vue";

const GroupEditingModal = defineAsyncComponent(() => import("@/components/GroupEditingModal.vue"));
const GroupCreationModal = defineAsyncComponent(() => import("@/components/GroupCreationModal.vue"));

const queryClient = useQueryClient();

const { data: groups } = useGroupsFetch();

const { selectedGroup } = useGroupSelection();

const editingModalVisible = ref(false);
const creationModalVisible = ref(false);
const updateGroup = ref<PhotoGroup>();

function handleOpenGroupModal(group: PhotoGroup) {
  updateGroup.value = group;
  editingModalVisible.value = true;
}

const handleDeleteGroup = async (group: PhotoGroup) => {
  Dialog.warning({
    title: "确定要删除该分组吗？",
    description: `将同时删除该分组下的 ${group.status?.photoCount || 0} 张图片，该操作不可恢复。`,
    confirmType: "danger",
    onConfirm: async () => {
      await photosConsoleApiClient.group.deletePhotoGroup({
        name: group.metadata.name,
      });
      queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
      queryClient.invalidateQueries({ queryKey: [QK_PHOTOS] });
      queryClient.invalidateQueries({ queryKey: [QK_PHOTO_TAGS] });
      Toast.success("删除分组成功");
    },
  });
};
</script>
<template>
  <div class=":uno: mb-4 rounded-md bg-white px-4 py-3">
    <div class=":uno: min-h-10 flex flex-wrap items-center gap-2">
      <GroupFilterItem
        :selected="selectedGroup === ALL_GROUPS"
        displayName="全部分组"
        @click="selectedGroup = ALL_GROUPS"
      />

      <GroupFilterItem
        :selected="selectedGroup === UNGROUPED"
        displayName="未分组"
        @click="selectedGroup = UNGROUPED"
      />

      <GroupFilterItem
        v-for="group in groups"
        :key="group.metadata.name"
        :selected="selectedGroup === group.metadata.name"
        :display-name="group.spec.displayName"
        @click="selectedGroup = group.metadata.name"
      >
        <span
          :class="
            selectedGroup === group.metadata.name ? ':uno: bg-white text-primary' : ':uno: bg-gray-100 text-gray-500'
          "
          class=":uno: rounded px-1.5 py-0.5 text-xs leading-none"
        >
          {{ group.status?.photoCount || 0 }}
        </span>
        <VStatusDot v-if="group.metadata.deletionTimestamp" class=":uno: ml-0.5" state="warning" animate />
        <VDropdown v-else>
          <IconMore
            class=":uno: ml-0.5 h-4 w-4 rounded text-gray-400 opacity-60 transition-opacity hover:bg-gray-100 hover:text-gray-600 hover:opacity-100"
            @click.stop
          />
          <template #popper>
            <VDropdownItem @click="handleOpenGroupModal(group)">修改</VDropdownItem>
            <VDropdownItem type="danger" @click="handleDeleteGroup(group)">删除</VDropdownItem>
          </template>
        </VDropdown>
      </GroupFilterItem>
      <button
        v-permission="['plugin:photos:manage']"
        class=":uno: min-h-9 inline-flex cursor-pointer items-center gap-1.5 border border-gray-300 rounded-md border-dashed bg-white px-2.5 py-1.5 text-sm text-gray-500 font-medium transition-all hover:border-gray-400 hover:text-gray-700"
        @click="creationModalVisible = true"
      >
        <IconAddCircle class=":uno: h-4 w-4" />
        新建分组
      </button>
    </div>
  </div>

  <GroupEditingModal
    v-if="editingModalVisible && updateGroup"
    :group="updateGroup"
    @close="editingModalVisible = false"
  />

  <GroupCreationModal v-if="creationModalVisible" @close="creationModalVisible = false" />
</template>
