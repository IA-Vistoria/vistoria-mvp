import { describe, expect, it } from "vitest";

import type { AiAnalysis, AiResult } from "../types";
import {
  aiResultLabel,
  findEnvironmentResult,
  findImageAnalysis,
  issueTypeLabel,
} from "./ai-report";

const analysisV2: AiAnalysis = {
  version: 2,
  execucao: {
    provider: "oci",
    modelo: "google.gemini-2.5-flash",
    versaoPrompt: "vistoria-visual-v2",
    identificadorAnalise: "ana-7",
    concluidaEm: "2026-09-30T20:00:00Z",
  },
  resultadoGeral: "NAO_APROVADO",
  motivoResultadoGeral: "Existe indício visual de alta gravidade.",
  ambientes: [
    {
      id: 8,
      nome: "Banheiro",
      resultado: "NAO_APROVADO",
      motivoResultado: "Mofo aparente em parede.",
    },
    {
      id: 9,
      nome: "Sala",
      resultado: "APROVADO",
      motivoResultado: "Nenhum indício visual identificado.",
    },
  ],
  imagens: [
    {
      imagemId: 31,
      storagePath: "uploads/banheiro.webp",
      identificadorAnalise: "ana-7",
      ambiente: { id: 8, nome: "Banheiro", categoria: "VISAO_GERAL" },
      resumoGeral: "Mofo aparente.",
      limitacoes: ["Sem medição de umidade."],
      orientacaoNovaCaptura: null,
      qualidade: { utilizavel: true, nivel: "SUFICIENTE", problemas: [] },
      achados: [{
        indice: 0,
        criterio: "Superfície e sinais de umidade",
        area: "Parede",
        tipo: "UMIDADE_OU_MOFO_APARENTE",
        descricao: "Manchas escuras e irregulares.",
        evidencia: "Distribuição extensa.",
        impacto: "Pode indicar degradação do revestimento.",
        gravidade: "ALTA",
        gravidadeNormalizada: "ALTA",
        confianca: "ALTA",
        confiancaNormalizada: "ALTA",
        recomendacao: "Avaliar a origem da umidade.",
        localizacao: "Parede ao lado da porta",
      }],
    },
    {
      imagemId: 32,
      storagePath: "uploads/sala.webp",
      identificadorAnalise: "ana-7",
      ambiente: { id: 9, nome: "Sala", categoria: "VISAO_GERAL" },
      resumoGeral: "Sem indícios visuais.",
      limitacoes: [],
      orientacaoNovaCaptura: null,
      qualidade: { utilizavel: true, nivel: "SUFICIENTE", problemas: [] },
      achados: [],
    },
  ],
};

describe("contrato visual da análise", () => {
  it.each<[AiResult, string]>([
    ["APROVADO", "Aprovado na análise visual"],
    ["APROVADO_COM_RESSALVAS", "Aprovado com ressalvas"],
    ["NAO_APROVADO", "Não aprovado na análise visual"],
    ["INCONCLUSIVO", "Análise inconclusiva"],
  ])("traduz o resultado %s", (result, label) => {
    expect(aiResultLabel(result)).toBe(label);
  });

  it("não transforma análise legada sem resultado em aprovação", () => {
    expect(aiResultLabel(undefined)).toBe("Resultado não disponível — análise anterior");
  });

  it("localiza a análise pela identidade da imagem", () => {
    expect(findImageAnalysis(analysisV2, 32)?.ambiente?.nome).toBe("Sala");
  });

  it("não associa análise de outra imagem", () => {
    expect(findImageAnalysis(analysisV2, 99)).toBeNull();
  });

  it("localiza o resultado pelo identificador do ambiente", () => {
    expect(findEnvironmentResult(analysisV2, 8)?.resultado).toBe("NAO_APROVADO");
  });

  it("usa o nome apenas como fallback para dados legados", () => {
    expect(findEnvironmentResult(analysisV2, null, " sala ")?.resultado).toBe("APROVADO");
  });

  it("traduz a taxonomia legada stain", () => {
    expect(issueTypeLabel("stain")).toBe("Mancha");
  });

  it("traduz a taxonomia v2 de umidade e mofo", () => {
    expect(issueTypeLabel("UMIDADE_OU_MOFO_APARENTE")).toBe("Umidade ou mofo aparente");
  });
});
