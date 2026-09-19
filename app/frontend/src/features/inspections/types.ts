export type InspectionStatus =
  | "EM_RASCUNHO"
  | "AGUARDANDO_IA"
  | "FALHA_IA"
  | "AGUARDANDO_ENGENHEIRO"
  | "CONCLUIDA"
  | "DEVOLVIDA_CLIENTE";

export interface Evidence {
  id: number;
  protocoloItem: string;
  dataUpload: string;
  conteudoUrl: string;
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

