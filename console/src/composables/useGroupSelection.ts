import { useRouteQuery } from "@vueuse/router";

// Sentinel values for the group selector. The sidebar surfaces three states:
//   ""              → "全部" (no group filter, returns all photos including ungrouped)
//   UNGROUPED       → "未分组" (only photos whose groupName is empty/unset)
//   <group-name>    → a specific PhotoGroup
export const ALL_GROUPS = "" as const;
export const UNGROUPED = "__ungrouped__" as const;

export type GroupSelection = typeof ALL_GROUPS | typeof UNGROUPED | string;

export function useGroupSelection(initial: GroupSelection = ALL_GROUPS) {
  const selectedGroup = useRouteQuery<GroupSelection>("group", initial);

  // Translate the sidebar selection into the wire-level query params accepted by
  // the console listing endpoint. ALL_GROUPS sends nothing; UNGROUPED sets the
  // dedicated `ungrouped` flag; a real group name is forwarded as-is.
  const buildGroupParams = (selection: GroupSelection): Record<string, unknown> => {
    if (selection === UNGROUPED) {
      return { ungrouped: true };
    }
    if (selection === ALL_GROUPS) {
      return {};
    }
    return { group: selection };
  };

  // Resolve the sidebar selection into a group name suitable for write
  // operations. Both "全部" and "未分组" map to an empty group, while a real
  // group name is forwarded unchanged.
  const resolveGroupForWrite = (selection: GroupSelection): string => {
    if (selection === ALL_GROUPS || selection === UNGROUPED) {
      return "";
    }
    return selection;
  };

  return {
    selectedGroup,
    buildGroupParams,
    resolveGroupForWrite,
  };
}
