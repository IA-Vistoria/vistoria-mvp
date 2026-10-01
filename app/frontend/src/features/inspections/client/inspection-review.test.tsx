import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError } from "@/lib/api";
import { completeReport, reviewFinding } from "../api";
import type { FindingReview, Inspection } from "../types";
import { InspectionReview } from "./inspection-review";

vi.mock("../api", () => ({
  completeReport: vi.fn(),
  reviewFinding: vi.fn(),
}));

vi.mock("../shared/evidence-image", () => ({
  EvidenceImage: ({ alt }: { alt: string }) => <span role="img" aria-label={alt}>foto</span>,
}));

const inspection: Inspection = {
  id: 31,
  version: 0,
  clienteId: 4,
  status: "RELATORIO_DISPONIVEL",
  endereco: "Rua do Contexto, 31",
  tipoImovel: "APARTAMENTO",
  ambientes: [{ id: 7, tipo: "SALA", nome: "Sala de estar", ordem: 0 }],
  dataCriacao: "2026-09-21T09:00:00",
  dataConclusao: "2026-09-21T09:10:00",
  imagens: [{
    id: 14,
    ambienteId: 7,
    ambienteNome: "Sala de estar",
    categoria: "VISAO_GERAL",
    protocoloItem: "SALA_VISAO_GERAL",
    dataUpload: "2026-09-21T09:05:00",
    conteudoUrl: "/api/vistorias/31/imagens/14/conteudo",
  }],
  analiseIa: {
    version: 2,
    execucao: {
      provider: "oci",
      modelo: "google.gemini-2.5-flash",
      versaoPrompt: "vistoria-visual-v2",
      identificadorAnalise: "ana-31",
      concluidaEm: "2026-09-21T09:10:00Z",
    },
    resultadoGeral: "NAO_APROVADO",
    motivoResultadoGeral: "Existe indício visual de alta gravidade.",
    ambientes: [{
      id: 7,
      nome: "Sala de estar",
      resultado: "NAO_APROVADO",
      motivoResultado: "Umidade aparente na parede.",
    }],
    imagens: [{
      imagemId: 14,
      storagePath: "uploads/sala.webp",
      identificadorAnalise: "ana-31",
      ambiente: { id: 7, nome: "Sala de estar", categoria: "VISAO_GERAL" },
      resumoGeral: "Dois indícios visuais foram identificados.",
      limitacoes: ["A imagem não confirma a origem da umidade."],
      orientacaoNovaCaptura: null,
      qualidade: { utilizavel: true, nivel: "SUFICIENTE", problemas: [] },
      achados: [
        {
          indice: 0,
          criterio: "Superfície e sinais de umidade",
          area: "Parede próxima à janela",
          tipo: "UMIDADE_OU_MOFO_APARENTE",
          descricao: "Manchas escuras e irregulares na pintura.",
          evidencia: "Distribuição extensa junto ao encontro da parede.",
          impacto: "Pode indicar degradação do revestimento.",
          gravidade: "ALTA",
          gravidadeNormalizada: "ALTA",
          confianca: "ALTA",
          confiancaNormalizada: "ALTA",
          recomendacao: "Avaliar a origem da umidade presencialmente.",
          localizacao: "Parede ao lado da janela",
        },
        {
          indice: 1,
          criterio: "Continuidade do acabamento",
          area: "Canto superior",
          tipo: "FISSURA_OU_TRINCA_APARENTE",
          descricao: "Linha fina visível no acabamento.",
          evidencia: "Descontinuidade linear na pintura.",
          impacto: "Pode evoluir e afetar o acabamento.",
          gravidade: "BAIXA",
          gravidadeNormalizada: "BAIXA",
          confianca: "MEDIA",
          confiancaNormalizada: "MEDIA",
          recomendacao: "Monitorar e registrar nova foto se houver mudança.",
          localizacao: "Canto superior da parede",
        },
      ],
    }],
  },
  manifestacoes: [],
};

function manifestation(overrides: Partial<FindingReview> = {}): FindingReview {
  return {
    imagemId: 14,
    indiceAchado: 0,
    decisao: "CONCORDO",
    contexto: "",
    tipoCorrigido: null,
    revisadoEm: "2026-09-21T10:00:00Z",
    ...overrides,
  };
}

describe("InspectionReview", () => {
  beforeEach(() => {
    vi.mocked(reviewFinding).mockReset();
    vi.mocked(completeReport).mockReset();
  });

  it("apresenta a autoridade da IA e o resultado geral antes da manifestação", () => {
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    expect(screen.getByRole("heading", { name: "Resultado da análise visual por IA" })).toBeDefined();
    expect(screen.getByText("Não aprovado na análise visual")).toBeDefined();
    expect(screen.getByText("Existe indício visual de alta gravidade.")).toBeDefined();
    expect(screen.getByText("Sua manifestação é opcional e não altera esta conclusão.")).toBeDefined();
  });

  it("mantém ambiente, categoria e evidência juntos", () => {
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    expect(screen.getAllByText("Sala de estar · Visão geral").length).toBeGreaterThan(0);
    expect(screen.getByRole("img", { name: "Evidência 14, achado 1" })).toBeDefined();
    expect(screen.getByText("Achado 1 de 2")).toBeDefined();
  });

  it("explica critério, observação, impacto, gravidade, confiança e recomendação", () => {
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    expect(screen.getByText("Superfície e sinais de umidade")).toBeDefined();
    expect(screen.getByText("Manchas escuras e irregulares na pintura.")).toBeDefined();
    expect(screen.getByText("Pode indicar degradação do revestimento.")).toBeDefined();
    expect(screen.getByText("Gravidade alta")).toBeDefined();
    expect(screen.getByText("Confiança alta")).toBeDefined();
    expect(screen.getByText("Avaliar a origem da umidade presencialmente.")).toBeDefined();
  });

  it("traduz a taxonomia e identifica cada achado na navegação", async () => {
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    expect(screen.getAllByText("Umidade ou mofo aparente").length).toBeGreaterThan(0);
    await user.click(screen.getByRole("button", { name: /Fissura ou trinca aparente/i }));
    expect(screen.getByText("Achado 2 de 2")).toBeDefined();
    expect(screen.getByRole("heading", { name: "Fissura ou trinca aparente" })).toBeDefined();
  });

  it("permite gerar o relatório sem manifestação humana", async () => {
    const completed = { ...inspection, status: "RELATORIO_DISPONIVEL" as const };
    vi.mocked(completeReport).mockResolvedValue(completed);
    const user = userEvent.setup();
    render(<InspectionReview inspection={{ ...inspection, status: "REVISAO_PENDENTE" }} onChange={vi.fn()} onRefresh={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "Gerar relatório por IA" }));

    expect(completeReport).toHaveBeenCalledWith(31);
  });

  it("registra concordância sem exigir texto", async () => {
    const saved = manifestation();
    vi.mocked(reviewFinding).mockResolvedValue({ ...inspection, manifestacoes: [saved] });
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "Concordo" }));

    await waitFor(() => expect(reviewFinding).toHaveBeenCalledWith(31, {
      imagemId: 14,
      indiceAchado: 0,
      decisao: "CONCORDO",
      contexto: "",
    }));
  });

  it("exige justificativa ao contestar e move o foco para o campo", async () => {
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "Contestar análise" }));
    await user.click(screen.getByRole("button", { name: "Salvar contestação" }));

    const alert = await screen.findByRole("alert");
    const field = screen.getByLabelText("Por que você contesta esta análise?");
    expect(alert.textContent).toContain("Explique sua contestação");
    expect(field.getAttribute("aria-invalid")).toBe("true");
    expect(document.activeElement).toBe(field);
    expect(reviewFinding).not.toHaveBeenCalled();
  });

  it("registra contestação sem renomear nem remover o achado da IA", async () => {
    const saved = manifestation({ decisao: "CONTESTO", contexto: "A marca é tinta antiga." });
    vi.mocked(reviewFinding).mockResolvedValue({ ...inspection, manifestacoes: [saved] });
    const user = userEvent.setup();
    const { rerender } = render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "Contestar análise" }));
    await user.type(screen.getByLabelText("Por que você contesta esta análise?"), saved.contexto);
    await user.click(screen.getByRole("button", { name: "Salvar contestação" }));

    await waitFor(() => expect(reviewFinding).toHaveBeenCalledWith(31, {
      imagemId: 14,
      indiceAchado: 0,
      decisao: "CONTESTO",
      contexto: saved.contexto,
    }));
    rerender(<InspectionReview inspection={{ ...inspection, manifestacoes: [saved] }} onChange={vi.fn()} onRefresh={vi.fn()} />);
    expect(screen.getByRole("heading", { name: "Umidade ou mofo aparente" })).toBeDefined();
    expect(screen.getByText("Contestação registrada")).toBeDefined();
  });

  it("adiciona contexto separado da conclusão", async () => {
    const saved = manifestation({ decisao: "CONTEXTO_ADICIONAL", contexto: "A parede foi pintada há duas semanas." });
    vi.mocked(reviewFinding).mockResolvedValue({ ...inspection, manifestacoes: [saved] });
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "Adicionar contexto" }));
    await user.type(screen.getByLabelText("Qual contexto ajuda a interpretar a evidência?"), saved.contexto);
    await user.click(screen.getByRole("button", { name: "Salvar contexto" }));

    await waitFor(() => expect(reviewFinding).toHaveBeenCalledWith(31, {
      imagemId: 14,
      indiceAchado: 0,
      decisao: "CONTEXTO_ADICIONAL",
      contexto: saved.contexto,
    }));
  });

  it("preserva o achado atual e mostra ProblemDetail quando a gravação falha", async () => {
    vi.mocked(reviewFinding).mockRejectedValue(new ApiError({
      type: "urn:vistoria:problem:invalid-review",
      title: "Manifestação inválida",
      status: 422,
      detail: "O contexto deve ter no máximo 1000 caracteres.",
    }));
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "Adicionar contexto" }));
    await user.type(screen.getByLabelText("Qual contexto ajuda a interpretar a evidência?"), "Contexto informado.");
    await user.click(screen.getByRole("button", { name: "Salvar contexto" }));

    expect((await screen.findByRole("alert")).textContent).toContain("no máximo 1000 caracteres");
    expect(screen.getByText("Achado 1 de 2")).toBeDefined();
  });

  it("reconcilia conflito por leitura sem repetir a mutação", async () => {
    vi.mocked(reviewFinding).mockRejectedValue(new ApiError({
      type: "urn:vistoria:problem:conflict",
      title: "Conflito",
      status: 409,
      detail: "A vistoria mudou em outra sessão.",
    }));
    const latest = { ...inspection, manifestacoes: [manifestation()] };
    const onRefresh = vi.fn().mockResolvedValue(latest);
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={onRefresh} />);

    await user.click(screen.getByRole("button", { name: "Concordo" }));

    await waitFor(() => expect(onRefresh).toHaveBeenCalledTimes(1));
    expect(reviewFinding).toHaveBeenCalledTimes(1);
    expect(await screen.findByText("Concordância registrada")).toBeDefined();
  });
});
