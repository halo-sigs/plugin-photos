import { photosConsoleApiClient } from "@/api";
import type { PhotoGroup } from "@/api/generated";
import { useQuery } from "@tanstack/vue-query";

export const QK_PHOTO_GROUPS = "plugin:photos:groups";

export function useGroupsFetch() {
  return useQuery<PhotoGroup[]>({
    queryKey: [QK_PHOTO_GROUPS],
    queryFn: async () => {
      const { data } = await photosConsoleApiClient.group.listPhotoGroups();
      return data;
    },
    refetchInterval(data) {
      const hasDeletingGroup = data?.some((group) => !!group.metadata.deletionTimestamp);
      return hasDeletingGroup ? 1000 : false;
    },
  });
}
