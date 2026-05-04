import { consoleApiClient } from "@halo-dev/api-client";
import { utils } from "@halo-dev/ui-shared";
import { useQuery } from "@tanstack/vue-query";

export const QK_CONFIG = "plugin:photos:config";

interface ConfigMapData {
  base: {
    policyName?: string;
    groupName?: string;
  };
}

export function useConfigFetch() {
  return useQuery<ConfigMapData | null>({
    queryKey: [QK_CONFIG],
    queryFn: async () => {
      if (!utils.permission.has(["system:plugins:manage"])) {
        return null;
      }

      const { data } = await consoleApiClient.plugin.plugin.fetchPluginJsonConfig({
        name: "PluginPhotos",
      });
      return data as ConfigMapData;
    },
  });
}
