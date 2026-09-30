export type InspectionStatus =
  | "EM_RASCUNHO"
  | "AGUARDANDO_IA"
  | "FALHA_IA"
  | "REVISAO_PENDENTE"
  | "RELATORIO_DISPONIVEL"
  | "AGUARDANDO_ENGENHEIRO"
  | "CONCLUIDA"
  | "DEVOLVIDA_CLIENTE";

export type PropertyType = "CASA" | "APARTAMENTO" | "COMERCIAL" | "OUTRO";

export type EnvironmentType =
  | "ENTRADA"
  | "SALA"
  | "COZINHA"
  | "BANHEIRO"
  | "QUARTO"
  | "AREA_SERVICO"
  | "VARANDA"
  | "GARAGEM"
  | "AREA_EXTERNA"
  | "ESCRITORIO"
  | "OUTRO";

export type EvidenceCategory = "VISAO_GERAL" | "DETALHE";

export interface InspectionEnvironment {
  id: number;
  tipo: EnvironmentType;
  nome: string;
  ordem: number;
}

export interface InspectionEnvironmentInput {
  id?: number | null;
  tipo: EnvironmentType;
  nome: string;
}

export interface CreateInspectionRequest {
  endereco: string;
  tipoImovel: PropertyType;
  ambientes: InspectionEnvironmentInput[];
}

export interface UpdateInspectionRouteRequest {
  version: number;
  tipoImovel: PropertyType;
  ambientes: InspectionEnvironmentInput[];
}

export interface UploadEvidenceRequest {
  ambienteId: number;
  categoria: EvidenceCategory;
  file: File;
}

export type ProtocolItemCode =
  | "SALA_PISO"
  | "SALA_PAREDES_REVESTIMENTOS"
  | "SALA_TETO_ILUMINACAO"
  | "COZINHA_PISO"
  | "COZINHA_PAREDES_BANCADAS"
  | "COZINHA_INSTALACOES"
  | "BANHEIRO_REVESTIMENTOS"
  | "BANHEIRO_HIDRAULICA"
  | "QUARTO_PISO"
  | "QUARTO_PAREDES_TETO"
  | "INSTALACOES_ELETRICAS"
  | "INSTALACOES_HIDRAULICAS";

export interface Evidence {
  id: number;
  ambienteId: number | null;
  ambienteNome: string | null;
  categoria: EvidenceCategory | null;
  protocoloItem: string;
  dataUpload: string;
  conteudoUrl: string;
}

export interface AiFinding {
  indice: number;
  area: string | null;
  tipo: string | null;
  descricao: string | null;
  evidencia: string | null;
  gravidade: string | null;
  confianca: string | null;
  recomendacao: string | null;
  localizacao: string | null;
}

export interface AiImageAnalysis {
  imagemId: number;
  identificadorAnalise: string | null;
  resumoGeral: string | null;
  limitacoes: string[];
  qualidade: {
    utilizavel: boolean;
    problemas: string[];
  };
  achados: AiFinding[];
}

export interface AiAnalysis {
  version: number;
  imagens: AiImageAnalysis[];
}

export type ReviewDecision = "CONFIRMADO" | "CORRIGIDO" | "REJEITADO";

export interface FindingReview {
  imagemId: number;
  indiceAchado: number;
  decisao: ReviewDecision;
  contexto: string;
  tipoCorrigido: string | null;
  revisadoEm: string;
}

export interface PageResponse<T> {
  content: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface Inspection {
  id: number;
  version: number;
  clienteId: number;
  status: InspectionStatus;
  endereco: string;
  tipoImovel: PropertyType | null;
  ambientes: InspectionEnvironment[];
  dataCriacao: string;
  dataConclusao: string | null;
  imagens: Evidence[];
  analiseIa: AiAnalysis | null;
  revisoes: FindingReview[];
}

/** Contrato isolado das telas antigas, sem uso na jornada pública do MVP. */
export type LegacyInspection = Inspection & {
  preLaudoIa?: string | null;
  parecerEngenheiro?: string | null;
};
