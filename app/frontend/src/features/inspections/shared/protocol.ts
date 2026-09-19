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

export const PROTOCOL_GROUPS: ProtocolGroup[] = [
  {
    name: "Sala",
    description: "Registre o estado geral das superfícies e da iluminação.",
    items: [
      { code: "SALA_PISO", label: "Piso", guidance: "Enquadre a maior área possível e eventuais danos." },
      { code: "SALA_PAREDES_REVESTIMENTOS", label: "Paredes e revestimentos", guidance: "Inclua cantos, fissuras ou sinais de umidade." },
      { code: "SALA_TETO_ILUMINACAO", label: "Teto e iluminação", guidance: "Mostre teto, luminárias e pontos aparentes." },
    ],
  },
  {
    name: "Cozinha",
    description: "Documente acabamentos, bancadas e instalações visíveis.",
    items: [
      { code: "COZINHA_PISO", label: "Piso", guidance: "Registre paginação, rejuntes e áreas próximas às bancadas." },
      { code: "COZINHA_PAREDES_BANCADAS", label: "Paredes e bancadas", guidance: "Mostre revestimentos, encontros e vedação." },
      { code: "COZINHA_INSTALACOES", label: "Instalações", guidance: "Fotografe pontos hidráulicos, elétricos e de gás aparentes." },
    ],
  },
  {
    name: "Banheiro",
    description: "Capture revestimentos e pontos hidráulicos sem ocultar encontros.",
    items: [
      { code: "BANHEIRO_REVESTIMENTOS", label: "Revestimentos", guidance: "Inclua piso, paredes, box e rejuntes." },
      { code: "BANHEIRO_HIDRAULICA", label: "Hidráulica", guidance: "Mostre louças, metais, ralos e sinais de vazamento." },
    ],
  },
  {
    name: "Quarto",
    description: "Registre superfícies e encontros construtivos do ambiente.",
    items: [
      { code: "QUARTO_PISO", label: "Piso", guidance: "Mostre acabamento, rodapés e possíveis desníveis." },
      { code: "QUARTO_PAREDES_TETO", label: "Paredes e teto", guidance: "Inclua cantos, esquadrias, fissuras e manchas." },
    ],
  },
  {
    name: "Instalações",
    description: "Finalize com uma visão dos sistemas aparentes do imóvel.",
    items: [
      { code: "INSTALACOES_ELETRICAS", label: "Elétricas", guidance: "Registre quadro, tomadas e pontos visíveis com segurança." },
      { code: "INSTALACOES_HIDRAULICAS", label: "Hidráulicas", guidance: "Mostre registros, tubulações e sinais aparentes." },
    ],
  },
];

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

