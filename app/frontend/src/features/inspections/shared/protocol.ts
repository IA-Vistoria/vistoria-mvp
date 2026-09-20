import type { Evidence, ProtocolItemCode } from "../types";

export const MAX_EVIDENCE_BYTES = 10 * 1024 * 1024;
export const ACCEPTED_EVIDENCE_TYPES = ["image/jpeg", "image/png", "image/webp"] as const;

export interface ProtocolItem {
  code: ProtocolItemCode;
  label: string;
  guidance: string;
}

export interface ProtocolGroup {
  name: string;
  description: string;
  items: ProtocolItem[];
}

/** MVP: apenas paredes. */
export const PROTOCOL_GROUPS: ProtocolGroup[] = [
  {
    name: "Paredes",
    description: "Envie fotos das paredes do imóvel para análise visual.",
    items: [
      {
        code: "SALA_PAREDES_REVESTIMENTOS",
        label: "Paredes",
        guidance: "Enquadre a parede completa; inclua cantos, fissuras ou sinais de umidade.",
      },
    ],
  },
];

export const PROTOCOL_ITEM_TOTAL = PROTOCOL_GROUPS.reduce((sum, group) => sum + group.items.length, 0);

export function calculateProgress(evidence: Evidence[]): number {
  return new Set(evidence.map((item) => item.protocoloItem)).size;
}

export function validateEvidenceFile(file: File): string | null {
  if (file.size === 0) return "O arquivo não pode estar vazio.";
  if (file.size > MAX_EVIDENCE_BYTES) return "A imagem deve ter no máximo 10 MB.";
  if (!(ACCEPTED_EVIDENCE_TYPES as readonly string[]).includes(file.type)) {
    return "Envie uma imagem JPEG, PNG ou WebP.";
  }
  return null;
}
