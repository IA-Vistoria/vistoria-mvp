export type InspectionStatus =
  | "EM_RASCUNHO"
  | "AGUARDANDO_IA"
  | "FALHA_IA"
  | "AGUARDANDO_ENGENHEIRO"
  | "CONCLUIDA"
  | "DEVOLVIDA_CLIENTE";

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
  protocoloItem: ProtocolItemCode;
  dataUpload: string;
  conteudoUrl: string;
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
  clienteId: number;
  engenheiroId: number | null;
  status: InspectionStatus;
  preLaudoIa: string | null;
  parecerEngenheiro: string | null;
  endereco: string;
  dataCriacao: string;
  dataConclusao: string | null;
  imagens: Evidence[];
}
