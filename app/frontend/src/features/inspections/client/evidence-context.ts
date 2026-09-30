import { PROTOCOL_GROUPS } from "../shared/protocol";
import type { Evidence, EvidenceCategory, Inspection } from "../types";

const protocolLabels = new Map<string, string>(
  PROTOCOL_GROUPS.flatMap((group) =>
    group.items.map((item) => [item.code, `${group.name} — ${item.label}`] as const),
  ),
);

export interface EvidenceGroup {
  key: string;
  name: string;
  legacy: boolean;
  evidence: Evidence[];
}

export function categoryLabel(category: EvidenceCategory | null): string {
  if (category === "VISAO_GERAL") return "Visão geral";
  if (category === "DETALHE") return "Detalhe";
  return "Evidência legada";
}

export function evidenceContextLabel(evidence: Evidence | null): string {
  if (!evidence) return "Evidência indisponível";
  if (evidence.ambienteNome && evidence.categoria) {
    return `${evidence.ambienteNome} · ${categoryLabel(evidence.categoria)}`;
  }
  return protocolLabels.get(evidence.protocoloItem) ?? evidence.protocoloItem;
}

export function groupEvidenceByEnvironment(inspection: Inspection): EvidenceGroup[] {
  const groups = [...inspection.ambientes]
    .sort((left, right) => left.ordem - right.ordem)
    .map((environment) => ({
      key: `environment-${environment.id}`,
      name: environment.nome,
      legacy: false,
      evidence: inspection.imagens.filter((item) => item.ambienteId === environment.id),
    }));

  const knownEnvironmentIds = new Set(inspection.ambientes.map(({ id }) => id));
  const unassignedStructured = inspection.imagens.filter((item) => (
    item.ambienteId !== null && !knownEnvironmentIds.has(item.ambienteId)
  ));
  for (const evidence of unassignedStructured) {
    const key = `unassigned-${evidence.ambienteId}`;
    const existing = groups.find((group) => group.key === key);
    if (existing) existing.evidence.push(evidence);
    else groups.push({
      key,
      name: evidence.ambienteNome || "Ambiente não disponível",
      legacy: false,
      evidence: [evidence],
    });
  }

  const legacyEvidence = inspection.imagens.filter((item) => item.ambienteId === null);
  if (legacyEvidence.length > 0) {
    groups.push({
      key: "legacy",
      name: "Evidências legadas",
      legacy: true,
      evidence: legacyEvidence,
    });
  }
  return groups;
}
