import { inspectionStatus } from "@/features/inspections/status";
import type { InspectionStatus } from "@/features/inspections/types";

export function StatusBadge({ status }: { status: InspectionStatus }) {
  const presentation = inspectionStatus[status];
  return <span className={`status-badge status-badge--${presentation.tone}`}>{presentation.label}</span>;
}

