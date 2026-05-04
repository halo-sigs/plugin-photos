<script lang="ts" setup>
import { QK_CONFIG, useConfigFetch } from "@/composables/useConfigFetch";
import { useGroupsFetch } from "@/composables/useGroupsFetch";
import { VAlert, VButton, VLoading, VModal } from "@halo-dev/components";
import { useQueryClient } from "@tanstack/vue-query";
import { computed, ref, shallowRef, useTemplateRef } from "vue";

const props = defineProps<{
  defaultGroup?: string;
}>();

const emit = defineEmits<{
  (event: "close"): void;
}>();

const queryClient = useQueryClient();

const uploadGroup = shallowRef(props.defaultGroup || "");
const modal = useTemplateRef<InstanceType<typeof VModal>>("modal");

const { data: config, isLoading } = useConfigFetch();

const { data: groups, refetch } = useGroupsFetch();

const groupOptions = computed(() => [
  { label: "未分组", value: "" },
  ...(groups.value?.map((g) => ({
    label: g.spec.displayName || g.metadata?.name,
    value: g.metadata?.name,
  })) || []),
]);

function onUploadDone() {
  modal.value?.close();
}

const pluginDetailModalVisible = ref(false);

function onPluginDetailModalClose() {
  pluginDetailModalVisible.value = false;
  queryClient.invalidateQueries({ queryKey: [QK_CONFIG] });
}
</script>

<template>
  <VModal title="上传图片" ref="modal" :centered="false" :width="900" @close="emit('close')">
    <div>
      <VLoading v-if="isLoading" />
      <div v-else>
        <div v-if="!config?.base.policyName">
          <VAlert
            title="提示"
            description="当前没有配置附件存储策略，请先在插件设置中配置，推荐使用一个全新的附件存储策略，专门用于存储图库上传的图片。"
            type="warning"
          >
            <template #actions>
              <VButton type="secondary" size="sm" @click="pluginDetailModalVisible = true"> 配置 </VButton>
            </template>
          </VAlert>
        </div>
        <div v-else class=":uno: flex flex-col gap-4">
          <FormKit v-model="uploadGroup" label="上传到分组" type="select" :options="groupOptions" />

          <VAlert
            title="提示"
            description="上传的图片可能包含 EXIF 信息（如拍摄时间、设备型号或 GPS 位置）。Halo 暂不支持在上传时自动移除这些信息，图库插件可能会读取并展示部分 EXIF 内容。若不希望公开相关隐私信息，请在上传前使用外部工具清除 EXIF 信息。"
            :closable="false"
          />

          <UppyUpload
            endpoint="/apis/console.api.photo.halo.run/v1alpha1/photos/upload"
            :meta="{
              group: uploadGroup,
            }"
            width="100%"
            :doneButtonHandler="onUploadDone"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <VButton @click="modal?.close()">关闭</VButton>
    </template>
  </VModal>

  <PluginDetailModal v-if="pluginDetailModalVisible" @close="onPluginDetailModalClose" name="PluginPhotos" />
</template>
