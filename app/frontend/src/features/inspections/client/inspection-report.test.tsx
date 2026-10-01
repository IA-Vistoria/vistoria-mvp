import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { Inspection } from "../types";
import { InspectionReport } from "./inspection-report";
import { InspectionResults } from "./inspection-results";

vi.mock("../shared/evidence-image", () => ({
  EvidenceImage: ({ alt }: { alt: string }) => <span role="img" aria-label={alt}>foto</span>,
}));

const report: Inspection = {
  id: 31,
  version: 4,
  clienteId: 4,
  status: "RELATORIO_DISPONIVEL",
  endereco: "Rua do Contexto, 31",
  tipoImovel: "APARTAMENTO",
  ambientes: [
    { id: 7, tipo: "SALA", nome: "Sala de estar", ordem: 0 },
    { id: 8, tipo: "QUARTO", nome: "Quarto de hóspedes", ordem: 1 },
    { id: 9, tipo: "BANHEIRO", nome: "Banheiro social", ordem: 2 },
  ],
  dataCriacao: "2026-09-21T09:00:00Z",
  dataConclusao: "2026-09-21T11:00:00Z",
  imagens: [
    { id: 14, ambienteId: 7, ambienteNome: "Sala de estar", categoria: "VISAO_GERAL", protocoloItem: "SALA_VISAO_GERAL", dataUpload: "2026-09-21T09:05:00Z", conteudoUrl: "/api/foto/14" },
    { id: 15, ambienteId: 8, ambienteNome: "Quarto de hóspedes", categoria: "VISAO_GERAL", protocoloItem: "QUARTO_VISAO_GERAL", dataUpload: "2026-09-21T09:06:00Z", conteudoUrl: "/api/foto/15" },
    { id: 16, ambienteId: 9, ambienteNome: "Banheiro social", categoria: "VISAO_GERAL", protocoloItem: "BANHEIRO_VISAO_GERAL", dataUpload: "2026-09-21T09:07:00Z", conteudoUrl: "/api/foto/16" },
  ],
  analiseIa: {
    version: 2,
    execucao: {
      provider: "oci",
      modelo: "google.gemini-2.5-flash",
      versaoPrompt: "vistoria-visual-v2",
      identificadorAnalise: "ana-31",
      concluidaEm: "2026-09-21T11:00:00Z",
    },
    resultadoGeral: "NAO_APROVADO",
    motivoResultadoGeral: "A sala possui um indício visual de alta gravidade.",
    ambientes: [
      { id: 7, nome: "Sala de estar", resultado: "NAO_APROVADO", motivoResultado: "Foi identificado indício de umidade com gravidade alta." },
      { id: 8, nome: "Quarto de hóspedes", resultado: "APROVADO", motivoResultado: "Nenhum indício visual foi identificado na evidência utilizável." },
      { id: 9, nome: "Banheiro social", resultado: "INCONCLUSIVO", motivoResultado: "A foto não possui nitidez suficiente para concluir a análise." },
    ],
    imagens: [
      {
        imagemId: 14,
        storagePath: "uploads/sala.webp",
        identificadorAnalise: "ana-31",
        ambiente: { id: 7, nome: "Sala de estar", categoria: "VISAO_GERAL" },
        resumoGeral: "A parede apresenta manchas escuras extensas.",
        limitacoes: ["A origem da umidade não pode ser confirmada apenas pela foto."],
        orientacaoNovaCaptura: null,
        qualidade: { utilizavel: true, nivel: "SUFICIENTE", problemas: [] },
        achados: [{
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
        }],
      },
      {
        imagemId: 15,
        storagePath: "uploads/quarto.webp",
        identificadorAnalise: "ana-31",
        ambiente: { id: 8, nome: "Quarto de hóspedes", categoria: "VISAO_GERAL" },
        resumoGeral: "Ambiente sem indícios visuais relevantes.",
        limitacoes: [],
        orientacaoNovaCaptura: null,
        qualidade: { utilizavel: true, nivel: "SUFICIENTE", problemas: [] },
        achados: [],
      },
      {
        imagemId: 16,
        storagePath: "uploads/banheiro.webp",
        identificadorAnalise: "ana-31",
        ambiente: { id: 9, nome: "Banheiro social", categoria: "VISAO_GERAL" },
        resumoGeral: "A evidência não permite avaliar o ambiente.",
        limitacoes: ["Imagem desfocada e com baixa iluminação."],
        orientacaoNovaCaptura: "Refaça a foto com o ambiente iluminado e a câmera estável.",
        qualidade: { utilizavel: false, nivel: "INSUFICIENTE", problemas: ["DESFOQUE", "BAIXA_ILUMINACAO"] },
        achados: [],
      },
    ],
  },
  manifestacoes: [
    { imagemId: 14, indiceAchado: 0, decisao: "CONTESTO", contexto: "A marca é tinta antiga, não umidade ativa.", tipoCorrigido: null, revisadoEm: "2026-09-21T11:10:00Z" },
  ],
};

const legacyReport: Inspection = {
  ...report,
  ambientes: [],
  tipoImovel: null,
  imagens: [{ id: 21, ambienteId: null, ambienteNome: null, categoria: null, protocoloItem: "SALA_PAREDES_REVESTIMENTOS", dataUpload: "2026-09-20T09:05:00Z", conteudoUrl: "/api/foto/21" }],
  analiseIa: {
    version: 1,
    imagens: [{
      imagemId: 21,
      identificadorAnalise: "img-21",
      resumoGeral: "Registro anterior.",
      limitacoes: [],
      qualidade: { utilizavel: true, problemas: [] },
      achados: [{ indice: 0, area: "Parede", tipo: "stain", descricao: "Mancha aparente.", evidencia: "Alteração de cor.", gravidade: "moderada", confianca: "média", recomendacao: "Acompanhar.", localizacao: "Sala" }],
    }],
  },
  manifestacoes: [],
};

const originalShare = Object.getOwnPropertyDescriptor(navigator, "share");
const originalClipboard = Object.getOwnPropertyDescriptor(navigator, "clipboard");

function setNavigatorCapability(name: "share" | "clipboard", value: unknown) {
  Object.defineProperty(navigator, name, { configurable: true, value });
}

afterEach(() => {
  if (originalShare) Object.defineProperty(navigator, "share", originalShare);
  else setNavigatorCapability("share", undefined);
  if (originalClipboard) Object.defineProperty(navigator, "clipboard", originalClipboard);
  else setNavigatorCapability("clipboard", undefined);
  vi.restoreAllMocks();
});

describe("InspectionReport", () => {
  it("não cria outro landmark principal dentro do shell autenticado", () => {
    const { unmount } = render(<InspectionReport inspection={report} />);
    expect(screen.queryByRole("main")).toBeNull();
    unmount();

    render(<InspectionResults inspection={{ ...report, status: "CONCLUIDA" }} />);
    expect(screen.queryByRole("main")).toBeNull();
  });

  it("apresenta resultado geral e motivo antes dos detalhes por ambiente", () => {
    const { container } = render(<InspectionReport inspection={report} />);
    const summary = container.querySelector(".report-ai-summary");

    expect(screen.getByRole("heading", { name: "Relatório de vistoria por IA" })).toBeDefined();
    expect(summary).not.toBeNull();
    expect(within(summary as HTMLElement).getByText("Não aprovado na análise visual")).toBeDefined();
    expect(screen.getByText("A sala possui um indício visual de alta gravidade.")).toBeDefined();
    const environments = container.querySelector(".report-findings");
    expect(environments).not.toBeNull();
    expect((summary as HTMLElement).compareDocumentPosition(environments as Node) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("mostra contagem por resultado e gravidade", () => {
    render(<InspectionReport inspection={report} />);

    expect(screen.getByText("1 não aprovado")).toBeDefined();
    expect(screen.getByText("1 aprovado")).toBeDefined();
    expect(screen.getByText("1 inconclusivo")).toBeDefined();
    expect(screen.getByText("1 achado de gravidade alta")).toBeDefined();
  });

  it("mantém cada resultado, evidência e achado dentro do próprio ambiente", () => {
    render(<InspectionReport inspection={report} />);
    const groups = screen.getAllByTestId("report-environment");
    const sala = groups[0];
    const quarto = groups[1];

    expect(within(sala).getByText("Não aprovado na análise visual")).toBeDefined();
    expect(within(sala).getByText("Umidade ou mofo aparente")).toBeDefined();
    expect(within(sala).queryByText("Ambiente sem indícios visuais relevantes.")).toBeNull();
    expect(within(quarto).getByText("Aprovado na análise visual")).toBeDefined();
    expect(within(quarto).getByText("Ambiente sem indícios visuais relevantes.")).toBeDefined();
    expect(within(quarto).queryByText("Umidade ou mofo aparente")).toBeNull();
  });

  it("explica critério, observação, evidência, impacto, gravidade, confiança e recomendação", () => {
    render(<InspectionReport inspection={report} />);

    expect(screen.getByText("Superfície e sinais de umidade")).toBeDefined();
    expect(screen.getByText("Manchas escuras e irregulares na pintura.")).toBeDefined();
    expect(screen.getByText("Distribuição extensa junto ao encontro da parede.")).toBeDefined();
    expect(screen.getByText("Pode indicar degradação do revestimento.")).toBeDefined();
    expect(screen.getByText("Gravidade alta")).toBeDefined();
    expect(screen.getByText("Confiança alta")).toBeDefined();
    expect(screen.getByText("Avaliar a origem da umidade presencialmente.")).toBeDefined();
  });

  it("preserva achado contestado e apresenta a manifestação em seção separada", () => {
    render(<InspectionReport inspection={report} />);

    expect(screen.getByRole("heading", { name: "Umidade ou mofo aparente" })).toBeDefined();
    expect(screen.getByText("Contestação do responsável")).toBeDefined();
    expect(screen.getByText("A marca é tinta antiga, não umidade ativa.")).toBeDefined();
  });

  it("explica quando um ambiente não possui achados", () => {
    render(<InspectionReport inspection={report} />);
    const quarto = screen.getAllByTestId("report-environment")[1];

    expect(within(quarto).getByText("Nenhum indício visual foi identificado nesta evidência.")).toBeDefined();
  });

  it("orienta uma nova captura quando o resultado é inconclusivo", () => {
    render(<InspectionReport inspection={report} />);
    const banheiro = screen.getAllByTestId("report-environment")[2];

    expect(within(banheiro).getByText("Análise inconclusiva")).toBeDefined();
    expect(within(banheiro).getByText("Refaça a foto com o ambiente iluminado e a câmera estável.")).toBeDefined();
    expect(within(banheiro).getByText("Imagem desfocada e com baixa iluminação.")).toBeDefined();
  });

  it("expõe a rastreabilidade da execução da IA", () => {
    render(<InspectionReport inspection={report} />);

    expect(screen.getByText("OCI Generative AI")).toBeDefined();
    expect(screen.getByText("google.gemini-2.5-flash")).toBeDefined();
    expect(screen.getByText("vistoria-visual-v2")).toBeDefined();
    expect(screen.getByText("ana-31")).toBeDefined();
  });

  it("mantém dados v1 legíveis sem fabricar aprovação", () => {
    const { container } = render(<InspectionReport inspection={legacyReport} />);
    const summary = container.querySelector(".report-ai-summary");

    expect(summary).not.toBeNull();
    expect(within(summary as HTMLElement).getByText("Resultado não disponível — análise anterior")).toBeDefined();
    expect(screen.getByRole("heading", { name: "Evidências legadas" })).toBeDefined();
    expect(screen.getByText("Mancha")).toBeDefined();
  });

  it("expõe o limite do documento sem prometer responsabilidade técnica", () => {
    render(<InspectionReport inspection={report} />);

    expect(screen.getByText(/não constitui laudo técnico, diagnóstico estrutural, conformidade normativa ou certificação profissional/i)).toBeDefined();
  });

  it("abre a impressão com identificação, resultados, evidências e metadados presentes", async () => {
    const print = vi.spyOn(window, "print").mockImplementation(() => undefined);
    const user = userEvent.setup();
    render(<InspectionReport inspection={report} />);

    await user.click(screen.getByRole("button", { name: "Imprimir ou salvar em PDF" }));

    expect(screen.getByText("VISTORIA 0031")).toBeDefined();
    expect(screen.getAllByRole("img", { name: /Evidência/ })).toHaveLength(3);
    expect(screen.getByText("ana-31")).toBeDefined();
    expect(print).toHaveBeenCalledTimes(1);
  });

  it("usa o compartilhamento nativo quando disponível", async () => {
    const share = vi.fn().mockResolvedValue(undefined);
    setNavigatorCapability("share", share);
    const user = userEvent.setup();
    render(<InspectionReport inspection={report} />);

    await user.click(screen.getByRole("button", { name: "Compartilhar relatório" }));

    expect(share).toHaveBeenCalledWith(expect.objectContaining({ title: "Relatório de vistoria por IA" }));
    expect((await screen.findByRole("status")).textContent).toContain("Relatório compartilhado");
  });

  it("copia o link quando Web Share não está disponível", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    setNavigatorCapability("share", undefined);
    setNavigatorCapability("clipboard", { writeText });
    render(<InspectionReport inspection={report} />);

    await user.click(screen.getByRole("button", { name: "Compartilhar relatório" }));

    expect(writeText).toHaveBeenCalledWith(window.location.href);
    expect((await screen.findByRole("status")).textContent).toContain("Link copiado");
  });

  it("mostra uma falha real quando não há recurso de compartilhamento", async () => {
    const user = userEvent.setup();
    setNavigatorCapability("share", undefined);
    setNavigatorCapability("clipboard", undefined);
    render(<InspectionReport inspection={report} />);

    await user.click(screen.getByRole("button", { name: "Compartilhar relatório" }));

    expect((await screen.findByRole("alert")).textContent).toContain("não oferece compartilhamento");
    expect(screen.queryByText(/compartilhado|copiado/i)).toBeNull();
  });
});
