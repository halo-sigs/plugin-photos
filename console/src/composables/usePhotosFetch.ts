import { photosConsoleApiClient } from "@/api";
import type { PhotoList } from "@/api/generated";
import { useQuery } from "@tanstack/vue-query";
import type { Ref } from "vue";
import { useGroupSelection } from "./useGroupSelection";

export const QK_PHOTOS = "plugin:photos:photos";

export function usePhotosFetch({
  page,
  size,
  keyword,
  selectedGroup,
  tagFilter,
  selectedSort,
}: {
  page: Ref<number>;
  size: Ref<number>;
  keyword: Ref<string>;
  selectedGroup: Ref<string>;
  tagFilter: Ref<string | undefined>;
  selectedSort: Ref<string | undefined>;
}) {
  const { buildGroupParams } = useGroupSelection();
  return useQuery<PhotoList>({
    queryKey: [QK_PHOTOS, page, size, keyword, selectedGroup, tagFilter, selectedSort],
    queryFn: async () => {
      const { data } = await photosConsoleApiClient.photo.listPhotos({
        page: page.value,
        size: size.value,
        keyword: keyword.value,
        tag: tagFilter.value || undefined,
        sort: selectedSort.value || undefined,
        ...buildGroupParams(selectedGroup.value),
      });
      return data;
    },
    refetchInterval(data) {
      const hasDeletingPhoto = data?.items?.some((photo) => !!photo.metadata.deletionTimestamp);
      return hasDeletingPhoto ? 1000 : false;
    },
  });
}
