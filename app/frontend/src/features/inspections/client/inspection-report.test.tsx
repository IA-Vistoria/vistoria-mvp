import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { Inspection } from "../types";
import { InspectionReport } from "./inspection-report";

vi.mock("../shared/evidence-image", () => ({
  EvidenceImage: ({ alt }: { alt: string }) => <span role="img" aria-label={alt}>foto</span>,
}));

const report: Inspection = {
  id: 31,
  version: 0,
  clienteId: 4,
  status: "RELATORIO_DISPONIVEL",
  endereco: "Rua do Contexto, 31",
  tipoImovel: null,
  ambientes: [],
  dataCriacao: "2026-09-21T09:00:00Z",
  dataConclusao: "2026-09-21T11:00:00Z",
  imagens: [
    { id: 14, ambienteId: null, ambienteNome: null, categoria: null, protocoloItem: "SALA_PAREDES_REVESTIMENTOS", dataUpload: "2026-09-21T09:05:00Z", conteudoUrl: "/api/foto/14" },
    { id: 15, ambienteId: null, ambienteNome: null, categoria: null, protocoloItem: "SALA_TETO_ILUMINACAO", dataUpload: "2026-09-21T09:06:00Z", conteudoUrl: "/api/foto/15" },
  ],
  analiseIa: {
    version: 1,
    imagens: [
      {
        imagemId: 14,
        identificadorAnalise: "img-14",
        resumoGeral: "Parede com dois indícios visuais.",
        limitacoes: ["A origem da marca não pode ser determinada pela foto."],
        qualidade: { utilizavel: true, problemas: [] },
        achados: [
          { indice: 0, area: "Parede próxima à janela", tipo: "Possível umidade", descricao: "Mancha escura.", evidencia: "Alteração de cor.", gravidade: "moderada", confianca: "média", recomendacao: "Acompanhar evolução.", localizacao: "Sala" },
          { indice: 1, area: "Canto superior", tipo: "Fissura rejeitada", descricao: "Linha aparente.", evidencia: "Traço fino.", gravidade: "baixa", confianca: "baixa", recomendacao: null, localizacao: "Sala" },
        ],
      },
      {
        imagemId: 15,
        identificadorAnalise: "img-15",
        resumoGeral: "Teto com alteração visual.",
        limitacoes: [],
        qualidade: { utilizavel: true, problemas: [] },
        achados: [
          { indice: 0, area: "Teto", tipo: "Possível infiltração", descricao: "Marca clara.", evidencia: "Diferença de tonalidade.", gravidade: "baixa", confianca: "média", recomendacao: "Registrar novamente se mudar.", localizacao: "Sala" },
        ],
      },
    ],
  },
  revisoes: [
    { imagemId: 14, indiceAchado: 0, decisao: "CONFIRMADO", contexto: "A marca já existia na entrega das chaves.", tipoCorrigido: null, revisadoEm: "2026-09-21T10:00:00Z" },
    { imagemId: 14, indiceAchado: 1, decisao: "REJEITADO", contexto: "Era um fio solto diante da parede.", tipoCorrigido: null, revisadoEm: "2026-09-21T10:02:00Z" },
    { imagemId: 15, indiceAchado: 0, decisao: "CORRIGIDO", contexto: "A marca é superficial e não mudou desde a pintura.", tipoCorrigido: "Mancha de acabamento", revisadoEm: "2026-09-21T10:04:00Z" },
  ],
};

const adaptiveReport: Inspection = {
  ...report,
  tipoImovel: "APARTAMENTO",
  ambientes: [
    { id: 7, tipo: "SALA", nome: "Sala de estar", ordem: 0 },
    { id: 8, tipo: "QUARTO", nome: "Quarto de hóspedes", ordem: 1 },
  ],
  imagens: [
    { ...report.imagens[1], ambienteId: 8, ambienteNome: "Quarto de hóspedes", categoria: "DETALHE", protocoloItem: "QUARTO_DETALHE" },
    { ...report.imagens[0], ambienteId: 7, ambienteNome: "Sala de estar", categoria: "VISAO_GERAL", protocoloItem: "SALA_VISAO_GERAL" },
  ],
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
  it("monta o relatório somente com conclusões confirmadas ou corrigidas", () => {
    render(<InspectionReport inspection={report} />);

    expect(screen.getByRole("heading", { name: "Relatório de vistoria por IA" })).toBeDefined();
    expect(screen.getByText("Possível umidade")).toBeDefined();
    expect(screen.getByText("Mancha de acabamento")).toBeDefined();
    expect(screen.queryByText("Fissura rejeitada")).toBeNull();
    expect(screen.getByText("A marca já existia na entrega das chaves.")).toBeDefined();
    expect(screen.getByText("A marca é superficial e não mudou desde a pintura.")).toBeDefined();
    expect(screen.getByText("IMG 0014 · ACHADO 00")).toBeDefined();
    expect(screen.getByText("IMG 0015 · ACHADO 00")).toBeDefined();
  });

  it("preserva todas as fotos como evidência, inclusive quando um achado foi rejeitado", () => {
    render(<InspectionReport inspection={report} />);

    expect(screen.getByRole("img", { name: "Evidência 14 — Sala — Paredes e revestimentos" })).toBeDefined();
    expect(screen.getByRole("img", { name: "Evidência 15 — Sala — Teto e iluminação" })).toBeDefined();
    expect(screen.getByText("fotos preservadas").previousElementSibling?.textContent).toBe("2");
  });

  it("agrupa as evidências na ordem dos ambientes persistidos", () => {
    render(<InspectionReport inspection={adaptiveReport} />);

    const groups = screen.getAllByTestId("report-environment");
    expect(groups).toHaveLength(2);
    expect(groups[0].textContent).toContain("Sala de estar");
    expect(groups[1].textContent).toContain("Quarto de hóspedes");
  });

  it("identifica visão geral e detalhe dentro do ambiente", () => {
    render(<InspectionReport inspection={adaptiveReport} />);

    expect(screen.getByText("Visão geral")).toBeDefined();
    expect(screen.getByText("Detalhe")).toBeDefined();
  });

  it("distingue observação da IA de decisão e contexto do responsável", () => {
    render(<InspectionReport inspection={adaptiveReport} />);

    expect(screen.getAllByText("Observação da IA").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Decisão do responsável").length).toBeGreaterThan(0);
    expect(screen.getByText("A marca já existia na entrega das chaves.")).toBeDefined();
  });

  it("mantém evidências antigas em um grupo de fallback legível", () => {
    render(<InspectionReport inspection={report} />);

    expect(screen.getByRole("heading", { name: "Evidências legadas" })).toBeDefined();
    expect(screen.getByText("Sala — Paredes e revestimentos")).toBeDefined();
  });

  it("expõe o limite do documento sem prometer laudo, diagnóstico ou certificação", () => {
    render(<InspectionReport inspection={report} />);

    expect(screen.getByText(/não constitui laudo técnico, diagnóstico estrutural ou certificação profissional/i)).toBeDefined();
  });

  it("abre a impressão do navegador sem fabricar arquivo", async () => {
    const print = vi.spyOn(window, "print").mockImplementation(() => undefined);
    const user = userEvent.setup();
    render(<InspectionReport inspection={report} />);

    await user.click(screen.getByRole("button", { name: "Imprimir ou salvar em PDF" }));

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
