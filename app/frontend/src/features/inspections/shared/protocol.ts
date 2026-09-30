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
    description: "Registre o ambiente principal de forma ampla e depois aproxime os detalhes.",
    items: [
      {
        code: "SALA_PISO",
        label: "Piso",
        guidance: "Fotografe o piso em perspectiva, incluindo encontros com rodapés e mudanças de nível.",
      },
      {
        code: "SALA_PAREDES_REVESTIMENTOS",
        label: "Paredes e revestimentos",
        guidance: "Enquadre as paredes completas e aproxime fissuras, manchas ou sinais de umidade.",
      },
      {
        code: "SALA_TETO_ILUMINACAO",
        label: "Teto e iluminação",
        guidance: "Inclua teto, luminárias e encontros com as paredes, evitando contraluz.",
      },
    ],
  },
  {
    name: "Cozinha",
    description: "Cubra acabamentos e pontos que concentram água, calor e instalações.",
    items: [
      {
        code: "COZINHA_PISO",
        label: "Piso",
        guidance: "Registre piso, rejuntes, rodapés e áreas próximas à pia ou eletrodomésticos.",
      },
      {
        code: "COZINHA_PAREDES_BANCADAS",
        label: "Paredes e bancadas",
        guidance: "Mostre paredes, revestimentos, bancadas, cubas e seus encontros.",
      },
      {
        code: "COZINHA_INSTALACOES",
        label: "Instalações",
        guidance: "Fotografe torneiras, registros, tomadas e pontos aparentes sem desmontar componentes.",
      },
    ],
  },
  {
    name: "Banheiro",
    description: "Documente superfícies e pontos hidráulicos sujeitos à umidade.",
    items: [
      {
        code: "BANHEIRO_REVESTIMENTOS",
        label: "Revestimentos",
        guidance: "Inclua piso, paredes, teto, rejuntes e a área do box em imagens nítidas.",
      },
      {
        code: "BANHEIRO_HIDRAULICA",
        label: "Hidráulica",
        guidance: "Registre louças, metais, ralos, registros e sinais visíveis próximos aos pontos de água.",
      },
    ],
  },
  {
    name: "Quarto",
    description: "Registre superfícies, encontros e iluminação de cada dormitório.",
    items: [
      {
        code: "QUARTO_PISO",
        label: "Piso",
        guidance: "Fotografe o piso, rodapés e transições para portas ou outros ambientes.",
      },
      {
        code: "QUARTO_PAREDES_TETO",
        label: "Paredes e teto",
        guidance: "Mostre paredes, teto, cantos e áreas próximas a janelas com boa iluminação.",
      },
    ],
  },
  {
    name: "Instalações",
    description: "Registre apenas componentes visíveis, sem abrir quadros ou desmontar peças.",
    items: [
      {
        code: "INSTALACOES_ELETRICAS",
        label: "Elétricas",
        guidance: "Fotografe quadro fechado, tomadas, interruptores e pontos com marcas aparentes.",
      },
      {
        code: "INSTALACOES_HIDRAULICAS",
        label: "Hidráulicas",
        guidance: "Registre registros, pontos de água e sinais visíveis de vazamento ou umidade.",
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
