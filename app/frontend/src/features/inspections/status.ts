import type { InspectionStatus } from "./types";

interface StatusPresentation {
  label: string;
  action: string;
  tone: "neutral" | "warning" | "success" | "danger";
}

export const inspectionStatus: Record<InspectionStatus, StatusPresentation> = {
  EM_RASCUNHO: { label: "Em preenchimento", action: "Continuar vistoria", tone: "neutral" },
  DEVOLVIDA_CLIENTE: { label: "Complementação solicitada", action: "Adicionar evidências", tone: "warning" },
  AGUARDANDO_IA: { label: "Pré-análise em andamento", action: "Acompanhar análise", tone: "warning" },
  FALHA_IA: { label: "Pré-análise indisponível", action: "Ver andamento", tone: "danger" },
  AGUARDANDO_ENGENHEIRO: { label: "Em revisão técnica", action: "Acompanhar revisão", tone: "warning" },
  CONCLUIDA: { label: "Concluída", action: "Ver resultado da IA", tone: "success" },
};

