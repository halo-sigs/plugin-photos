<script lang="ts" setup>
import type { Photo } from "@/api/generated";
import { QK_PHOTO_GROUPS, useGroupsFetch } from "@/composables/useGroupsFetch";
import { useInlineEdit } from "@/composables/useInlineEdit";
import { QK_PHOTOS } from "@/composables/usePhotosFetch";
import { QK_PHOTO_TAGS, usePhotoTags } from "@/composables/usePhotoTags";
import { utils } from "@halo-dev/ui-shared";
import { useQueryClient } from "@tanstack/vue-query";
import { computed } from "vue";

defineProps<{
  photos: Photo[];
  isSelected: (photo: Photo) => boolean;
}>();

const emit = defineEmits<{
  (e: "toggleSelect", photo: Photo, checked: boolean): void;
  (e: "openEdit", photo: Photo): void;
}>();

const queryClient = useQueryClient();

const { data: groups } = useGroupsFetch();

// O(1) group lookup, recomputed only when groups change.
const groupNameMap = computed(() => {
  const map = new Map<string, string>();
  for (const g of groups.value || []) {
    map.set(g.metadata.name, g.spec.displayName || g.metadata.name);
  }
  return map;
});

const groupLabel = (groupName?: string) => {
  if (!groupName) return "";
  return groupNameMap.value.get(groupName) || groupName;
};

const { tagOptions } = usePhotoTags();

// Inline editing lives in the table since the grid view does not need it.
const { editingCell, editingValue, editingTags, start, commit, cancel } = useInlineEdit({
  onSaved: () => {
    queryClient.invalidateQueries({ queryKey: [QK_PHOTOS] });
    queryClient.invalidateQueries({ queryKey: [QK_PHOTO_TAGS] });
    queryClient.invalidateQueries({ queryKey: [QK_PHOTO_GROUPS] });
  },
});
</script>

<template>
  <div class=":uno: overflow-x-auto bg-white">
    <table class=":uno: min-w-[920px] w-full table-fixed text-sm">
      <thead class=":uno: bg-gray-50 text-left text-xs text-gray-500 font-medium tracking-normal uppercase">
        <tr>
          <th v-if="utils.permission.has(['plugin:photos:manage'])" class=":uno: w-11 px-3 py-3"></th>
          <th class=":uno: w-20 px-3 py-3">缩略图</th>
          <th class=":uno: w-[28%] px-3 py-3">名称</th>
          <th class=":uno: w-36 px-3 py-3">分组</th>
          <th class=":uno: w-40 px-3 py-3">拍摄时间</th>
          <th class=":uno: w-36 px-3 py-3">相机</th>
          <th class=":uno: w-[22%] px-3 py-3">标签</th>
        </tr>
      </thead>
      <tbody class=":uno: divide-y divide-gray-100">
        <tr
          v-for="photo in photos"
          :key="photo.metadata.name"
          v-memo="[
            isSelected(photo),
            !!photo.metadata.deletionTimestamp,
            editingCell?.photoName === photo.metadata.name ? editingCell.field : null,
            editingValue,
            editingTags,
            photo,
          ]"
          :class="{
            ':uno: bg-primary/5 shadow-[inset_3px_0_0_var(--halo-primary-color)]': isSelected(photo),
            ':uno: bg-red-50/70 opacity-70': photo.metadata.deletionTimestamp,
          }"
          class=":uno: group transition-colors hover:bg-gray-50"
        >
          <!-- Checkbox -->
          <td v-if="utils.permission.has(['plugin:photos:manage'])" class=":uno: px-3 py-2 align-middle">
            <input
              :checked="isSelected(photo)"
              :disabled="!!photo.metadata.deletionTimestamp"
              class=":uno: h-4 w-4 cursor-pointer border-gray-300 rounded disabled:cursor-not-allowed disabled:opacity-40"
              type="checkbox"
              @change="
                (e) => {
                  const checked = (e.target as HTMLInputElement).checked;
                  emit('toggleSelect', photo, checked);
                }
              "
            />
          </td>

          <!-- Thumbnail -->
          <td class=":uno: px-3 py-2 align-middle">
            <div
              class=":uno: h-11 w-16 cursor-pointer overflow-hidden border border-gray-200 rounded-md bg-gray-100"
              @click="emit('openEdit', photo)"
            >
              <img
                :src="utils.attachment.getThumbnailUrl(photo.spec.url, 'S')"
                :alt="photo.spec.displayName"
                class=":uno: h-full w-full object-cover"
                loading="lazy"
              />
            </div>
          </td>

          <!-- Name (inline editable) -->
          <td class=":uno: px-3 py-2 align-middle">
            <div v-if="editingCell?.photoName === photo.metadata.name && editingCell?.field === 'displayName'">
              <input
                :data-edit-field="`displayName-${photo.metadata.name}`"
                :value="editingValue"
                class=":uno: border-primary ring-primary/10 h-8 w-full border rounded-md bg-white px-2 text-sm outline-none ring-2"
                type="text"
                @input="editingValue = ($event.target as HTMLInputElement).value"
                @blur="commit(photo, 'displayName')"
                @keydown.enter="commit(photo, 'displayName')"
                @keydown.esc="cancel"
              />
            </div>
            <div
              v-else
              :class="[
                ':uno: max-w-full truncate text-gray-900 font-medium',
                utils.permission.has(['plugin:photos:manage']) ? ':uno: cursor-pointer hover:text-primary' : '',
              ]"
              @click="utils.permission.has(['plugin:photos:manage']) && start(photo, 'displayName')"
            >
              {{ photo.spec.displayName }}
            </div>
          </td>

          <!-- Group (inline editable) -->
          <td class=":uno: px-3 py-2 align-middle">
            <div v-if="editingCell?.photoName === photo.metadata.name && editingCell?.field === 'groupName'">
              <select
                :data-edit-field="`groupName-${photo.metadata.name}`"
                :value="editingValue"
                class=":uno: border-primary ring-primary/10 h-8 w-full border rounded-md bg-white px-2 text-sm outline-none ring-2"
                @change="
                  (e) => {
                    editingValue = (e.target as HTMLSelectElement).value;
                    commit(photo, 'groupName');
                  }
                "
                @keydown.esc="cancel"
              >
                <option value="">无分组</option>
                <option v-for="g in groups" :key="g.metadata.name" :value="g.metadata.name">
                  {{ g.spec.displayName }}
                </option>
              </select>
            </div>
            <div
              v-else
              :class="[
                ':uno: max-w-full truncate',
                photo.spec.groupName ? ':uno: text-gray-600' : ':uno: text-gray-400',
                utils.permission.has(['plugin:photos:manage']) ? ':uno: cursor-pointer hover:text-primary' : '',
              ]"
              @click="utils.permission.has(['plugin:photos:manage']) && start(photo, 'groupName')"
            >
              <template v-if="photo.spec.groupName">
                {{ groupLabel(photo.spec.groupName) }}
              </template>
              <template v-else>未分组</template>
            </div>
          </td>

          <!-- DateTimeOriginal -->
          <td class=":uno: whitespace-nowrap px-3 py-2 align-middle text-gray-500">
            {{ utils.date.format(photo.exif?.dateTimeOriginal) }}
          </td>

          <!-- Camera -->
          <td class=":uno: px-3 py-2 align-middle text-gray-500">
            <span v-if="photo.exif?.make || photo.exif?.model" class=":uno: block truncate whitespace-nowrap">
              {{ photo.exif?.make }} {{ photo.exif?.model }}
            </span>
            <span v-else class=":uno: text-gray-300">-</span>
          </td>

          <!-- Tags (inline editable) -->
          <td class=":uno: px-3 py-2 align-middle">
            <div v-if="editingCell?.photoName === photo.metadata.name && editingCell?.field === 'tags'">
              <div :data-edit-field="`tags-${photo.metadata.name}`" class=":uno: min-w-[180px]">
                <FormKit
                  v-model="editingTags"
                  type="select"
                  :multiple="true"
                  :searchable="true"
                  :allowCreate="true"
                  :options="tagOptions"
                  placeholder="输入标签"
                />
              </div>
              <div class=":uno: mt-1 flex gap-2">
                <button
                  class=":uno: text-primary hover:bg-primary/10 rounded px-1.5 py-0.5 text-xs font-medium"
                  @click="commit(photo, 'tags')"
                >
                  保存
                </button>
                <button class=":uno: rounded px-1.5 py-0.5 text-xs text-gray-500 hover:bg-gray-100" @click="cancel">
                  取消
                </button>
              </div>
            </div>
            <div
              v-else
              :class="[
                ':uno: min-h-7 flex items-center gap-1',
                utils.permission.has(['plugin:photos:manage']) ? ':uno: cursor-pointer' : '',
              ]"
              @click="utils.permission.has(['plugin:photos:manage']) && start(photo, 'tags')"
            >
              <span
                v-for="tag in (photo.spec.tags || []).slice(0, 2)"
                :key="tag"
                class=":uno: bg-primary/10 text-primary max-w-[88px] truncate rounded px-2 py-0.5 text-xs"
              >
                {{ tag }}
              </span>
              <span
                v-if="(photo.spec.tags || []).length > 2"
                class=":uno: rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500"
              >
                +{{ photo.spec.tags!.length - 2 }}
              </span>
              <span v-if="!photo.spec.tags?.length" class=":uno: text-gray-300">-</span>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
