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
  status: "REVISAO_PENDENTE",
  endereco: "Rua do Contexto, 31",
  tipoImovel: null,
  ambientes: [],
  dataCriacao: "2026-09-21T09:00:00",
  dataConclusao: null,
  imagens: [
    {
      id: 14,
      ambienteId: null,
      ambienteNome: null,
      categoria: null,
      protocoloItem: "SALA_PAREDES_REVESTIMENTOS",
      dataUpload: "2026-09-21T09:05:00",
      conteudoUrl: "/api/vistorias/31/imagens/14/conteudo",
    },
  ],
  analiseIa: {
    version: 1,
    imagens: [
      {
        imagemId: 14,
        identificadorAnalise: "img-14",
        resumoGeral: "Dois indícios visuais pedem contexto.",
        limitacoes: ["A imagem não revela a origem da marca."],
        qualidade: { utilizavel: true, problemas: [] },
        achados: [
          {
            indice: 0,
            area: "Parede próxima à janela",
            tipo: "Possível umidade",
            descricao: "Mancha com alteração de cor.",
            evidencia: "Região mais escura junto ao encontro da parede.",
            gravidade: "moderada",
            confianca: "média",
            recomendacao: "Observar se a marca evolui.",
            localizacao: "Sala",
          },
          {
            indice: 1,
            area: "Canto superior",
            tipo: "Fissura aparente",
            descricao: "Linha fina visível no acabamento.",
            evidencia: "Descontinuidade linear na pintura.",
            gravidade: "baixa",
            confianca: "alta",
            recomendacao: "Registrar nova foto se houver mudança.",
            localizacao: "Sala",
          },
        ],
      },
    ],
  },
  manifestacoes: [],
};

const adaptiveInspection: Inspection = {
  ...inspection,
  tipoImovel: "APARTAMENTO",
  ambientes: [{ id: 7, tipo: "SALA", nome: "Sala de estar", ordem: 0 }],
  imagens: [{
    ...inspection.imagens[0],
    ambienteId: 7,
    ambienteNome: "Sala de estar",
    categoria: "VISAO_GERAL",
    protocoloItem: "SALA_VISAO_GERAL",
  }],
};

function review(overrides: Partial<FindingReview> = {}): FindingReview {
  return {
    imagemId: 14,
    indiceAchado: 0,
    decisao: "CONFIRMADO",
    contexto: "A marca já existia quando recebi as chaves.",
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

  it("retoma no primeiro achado sem revisão e mantém a foto dominante vinculada", () => {
    render(<InspectionReview inspection={{ ...inspection, manifestacoes: [review()] }} onChange={vi.fn()} onRefresh={vi.fn()} />);

    expect(screen.getByText("Achado 2 de 2")).toBeDefined();
    expect(screen.getByRole("heading", { name: "Fissura aparente" })).toBeDefined();
    expect(screen.getByRole("img", { name: "Evidência 14, achado 2" })).toBeDefined();
    expect(screen.getByText("A imagem não revela a origem da marca.")).toBeDefined();
    expect(screen.getAllByRole("textbox")).toHaveLength(1);
  });

  it("mantém ambiente e categoria visíveis junto da foto", () => {
    render(<InspectionReview inspection={adaptiveInspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    expect(screen.getAllByText("Sala de estar · Visão geral").length).toBeGreaterThan(0);
    expect(screen.getByRole("img", { name: "Evidência 14, achado 1" })).toBeDefined();
  });

  it("contextualiza cada achado na navegação pelo ambiente real", () => {
    render(<InspectionReview inspection={adaptiveInspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    const navigation = screen.getByRole("complementary", { name: "Achados da análise" });
    expect(navigation.textContent).toContain("Sala de estar");
    expect(navigation.textContent).toContain("Pendente");
  });

  it("separa explicitamente a sugestão da IA da decisão do responsável", () => {
    render(<InspectionReview inspection={adaptiveInspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    expect(screen.getByText("Observação sugerida pela IA")).toBeDefined();
    expect(screen.getByText("Decisão do responsável")).toBeDefined();
  });

  it("exige contexto e tipo corrigido antes de chamar a API", async () => {
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    await user.click(screen.getByLabelText("Confirmar achado"));
    await user.click(screen.getByRole("button", { name: "Salvar e continuar" }));
    const contextAlert = await screen.findByRole("alert");
    const contextField = screen.getByLabelText("Contexto observado");
    expect(contextAlert.textContent).toContain("Descreva o contexto");
    expect(contextField.getAttribute("aria-invalid")).toBe("true");
    expect(contextField.getAttribute("aria-describedby")).toBe(contextAlert.id);
    expect(document.activeElement).toBe(contextField);

    await user.click(screen.getByLabelText("Corrigir informação"));
    await user.type(screen.getByLabelText("Contexto observado"), "É uma marca de tinta antiga.");
    await user.click(screen.getByRole("button", { name: "Salvar e continuar" }));
    const correctedAlert = await screen.findByRole("alert");
    const correctedField = screen.getByLabelText("Como deve aparecer no relatório");
    expect(correctedAlert.textContent).toContain("tipo corrigido");
    expect(correctedField.getAttribute("aria-invalid")).toBe("true");
    expect(correctedField.getAttribute("aria-describedby")).toBe(correctedAlert.id);
    expect(document.activeElement).toBe(correctedField);
    expect(reviewFinding).not.toHaveBeenCalled();
  });

  it("associa a decisão obrigatória e leva o foco ao primeiro rádio", async () => {
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "Salvar e continuar" }));

    const alert = await screen.findByRole("alert");
    const firstDecision = screen.getByLabelText("Confirmar achado");
    expect(alert.textContent).toContain("Escolha como este achado");
    expect(firstDecision.getAttribute("aria-invalid")).toBe("true");
    expect(firstDecision.getAttribute("aria-describedby")).toBe(alert.id);
    expect(document.activeElement).toBe(firstDecision);
  });

  it("persiste a decisão uma vez antes de avançar para o próximo achado", async () => {
    const saved = review();
    vi.mocked(reviewFinding).mockResolvedValue({ ...inspection, manifestacoes: [saved] });
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={onChange} onRefresh={vi.fn()} />);

    await user.click(screen.getByLabelText("Confirmar achado"));
    await user.type(screen.getByLabelText("Contexto observado"), saved.contexto);
    await user.dblClick(screen.getByRole("button", { name: "Salvar e continuar" }));

    await waitFor(() => expect(reviewFinding).toHaveBeenCalledTimes(1));
    expect(reviewFinding).toHaveBeenCalledWith(31, {
      imagemId: 14,
      indiceAchado: 0,
      decisao: "CONFIRMADO",
      contexto: saved.contexto,
      tipoCorrigido: null,
    });
    expect(onChange).toHaveBeenCalled();
    expect(await screen.findByText("Achado 2 de 2")).toBeDefined();
  });

  it("preserva o achado atual e mostra ProblemDetail quando a gravação falha", async () => {
    vi.mocked(reviewFinding).mockRejectedValue(new ApiError({
      type: "urn:vistoria:problem:invalid-review",
      title: "Revisão inválida",
      status: 422,
      detail: "O contexto deve ter no máximo 1000 caracteres.",
    }));
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={vi.fn()} />);

    await user.click(screen.getByLabelText("Rejeitar achado"));
    await user.type(screen.getByLabelText("Contexto observado"), "Não corresponde ao que observei no imóvel.");
    await user.click(screen.getByRole("button", { name: "Salvar e continuar" }));

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
    const latest = { ...inspection, manifestacoes: [review()] };
    const onRefresh = vi.fn().mockResolvedValue(latest);
    const user = userEvent.setup();
    render(<InspectionReview inspection={inspection} onChange={vi.fn()} onRefresh={onRefresh} />);

    await user.click(screen.getByLabelText("Confirmar achado"));
    await user.type(screen.getByLabelText("Contexto observado"), "Marca anterior à vistoria.");
    await user.click(screen.getByRole("button", { name: "Salvar e continuar" }));

    await waitFor(() => expect(onRefresh).toHaveBeenCalledTimes(1));
    expect(reviewFinding).toHaveBeenCalledTimes(1);
    expect(await screen.findByText("Achado 2 de 2")).toBeDefined();
  });

  it("só conclui depois de todas as revisões persistidas", async () => {
    const completeReviews = [review(), review({ indiceAchado: 1, decisao: "REJEITADO" })];
    const reviewed = { ...inspection, manifestacoes: completeReviews };
    const completed = { ...reviewed, status: "RELATORIO_DISPONIVEL" as const, dataConclusao: "2026-09-21T11:00:00" };
    vi.mocked(completeReport).mockResolvedValue(completed);
    const onChange = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(<InspectionReview inspection={inspection} onChange={onChange} onRefresh={vi.fn()} />);

    expect(screen.queryByRole("button", { name: "Gerar relatório por IA" })).toBeNull();
    rerender(<InspectionReview inspection={reviewed} onChange={onChange} onRefresh={vi.fn()} />);
    await user.click(screen.getByRole("button", { name: "Gerar relatório por IA" }));

    expect(completeReport).toHaveBeenCalledTimes(1);
    expect(completeReport).toHaveBeenCalledWith(31);
    expect(onChange).toHaveBeenCalledWith(completed);
  });
});
