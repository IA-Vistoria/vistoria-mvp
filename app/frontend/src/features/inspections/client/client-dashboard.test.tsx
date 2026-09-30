import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError } from "@/lib/api";
import type { Inspection, PageResponse } from "../types";
import { createInspection, listMyInspections, submitInspection } from "../api";
import { ClientDashboard } from "./client-dashboard";
import { NewInspectionForm } from "./new-inspection-form";

function page(content: Inspection[]): PageResponse<Inspection> {
  return { content, pagina: 0, tamanho: 10, totalElementos: content.length, totalPaginas: content.length === 0 ? 0 : 1 };
}

const { replace } = vi.hoisted(() => ({
  replace: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("../api", () => ({
  createInspection: vi.fn(),
  listMyInspections: vi.fn(),
  submitInspection: vi.fn(),
}));

const draftInspection: Inspection = {
  id: 42,
  version: 0,
  clienteId: 1,
  status: "EM_RASCUNHO",
  endereco: "Rua das Obras, 10",
  tipoImovel: "APARTAMENTO",
  ambientes: [
    { id: 11, tipo: "SALA", nome: "Sala", ordem: 0 },
    { id: 12, tipo: "QUARTO", nome: "Quarto", ordem: 1 },
  ],
  dataCriacao: "2026-09-18T10:00:00",
  dataConclusao: null,
  imagens: [],
  analiseIa: null,
  revisoes: [],
};

describe("ClientDashboard", () => {
  beforeEach(() => {
    replace.mockReset();
    vi.mocked(listMyInspections).mockReset();
    vi.mocked(createInspection).mockReset();
    vi.mocked(submitInspection).mockReset();
  });

  it("ordena vistorias pela data de criação mais recente e desempata pelo id", async () => {
    vi.mocked(listMyInspections).mockResolvedValue(page([
      { ...draftInspection, id: 1, endereco: "Antiga", dataCriacao: "2026-09-10T10:00:00" },
      { ...draftInspection, id: 2, endereco: "Recente 2", dataCriacao: "2026-09-19T10:00:00" },
      { ...draftInspection, id: 3, endereco: "Recente 3", dataCriacao: "2026-09-19T10:00:00" },
    ]));

    render(<ClientDashboard />);

    expect((await screen.findByTestId("next-action")).textContent).toContain("Recente 3");
    const cards = screen.getAllByTestId("inspection-card");
    expect(cards.map((card) => card.textContent)).toEqual([
      expect.stringContaining("Recente 2"),
      expect.stringContaining("Antiga"),
    ]);
  });

  it("mostra endereço, status real e próxima ação", async () => {
    vi.mocked(listMyInspections).mockResolvedValue(page([draftInspection]));

    render(<ClientDashboard />);

    const card = await screen.findByTestId("next-action");
    expect(card.textContent).toContain("Rua das Obras, 10");
    expect(card.textContent).toContain("Em preenchimento");
    expect(within(card).getByRole("link", { name: "Continuar vistoria" }).getAttribute("href")).toBe(
      "/client/vistorias/42",
    );
  });

  it("mostra o progresso real dos ambientes sem mencionar protocolo fixo", async () => {
    vi.mocked(listMyInspections).mockResolvedValue(page([{
      ...draftInspection,
      imagens: [{
        id: 91,
        ambienteId: 11,
        ambienteNome: "Sala",
        categoria: "VISAO_GERAL",
        protocoloItem: "SALA_VISAO_GERAL",
        dataUpload: "2026-09-18T10:05:00",
        conteudoUrl: "/api/vistorias/42/imagens/91/conteudo",
      }],
    }]));

    render(<ClientDashboard />);

    const card = await screen.findByTestId("next-action");
    expect(card.textContent).toContain("1 de 2 ambientes com visão geral");
    expect(card.textContent).not.toContain("12 itens");
  });

  it("prioriza a próxima ação da vistoria mais recente antes dos relatórios", async () => {
    vi.mocked(listMyInspections).mockResolvedValue(page([
      {
        ...draftInspection,
        id: 7,
        status: "RELATORIO_DISPONIVEL",
        endereco: "Rua do Relatório, 7",
        dataCriacao: "2026-09-17T10:00:00",
        dataConclusao: "2026-09-18T10:00:00",
      },
      {
        ...draftInspection,
        id: 9,
        status: "AGUARDANDO_IA",
        endereco: "Avenida Mais Recente, 9",
        dataCriacao: "2026-09-20T10:00:00",
      },
    ]));

    render(<ClientDashboard />);

    const nextAction = await screen.findByTestId("next-action");
    expect(nextAction.textContent).toContain("Avenida Mais Recente, 9");
    expect(within(nextAction).getByRole("link", { name: "Acompanhar análise" })).toBeDefined();

    const report = screen.getByRole("heading", { name: "Relatórios disponíveis" });
    expect(nextAction.compareDocumentPosition(report) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(screen.getByText("Rua do Relatório, 7")).toBeDefined();
  });

  it("abre a visão de relatórios e inclui o documento mais recente", async () => {
    vi.mocked(listMyInspections).mockResolvedValue(page([
      {
        ...draftInspection,
        id: 12,
        status: "RELATORIO_DISPONIVEL",
        endereco: "Relatório mais recente",
        dataCriacao: "2026-09-20T10:00:00",
        dataConclusao: "2026-09-21T10:00:00",
      },
      {
        ...draftInspection,
        id: 7,
        status: "RELATORIO_DISPONIVEL",
        endereco: "Relatório anterior",
        dataCriacao: "2026-09-17T10:00:00",
        dataConclusao: "2026-09-18T10:00:00",
      },
    ]));

    render(<ClientDashboard view="reports" />);

    expect(await screen.findByRole("heading", { name: "Seus relatórios" })).toBeDefined();
    expect(screen.getByText("Relatório mais recente")).toBeDefined();
    expect(screen.getByText("Relatório anterior")).toBeDefined();
    expect(screen.getAllByRole("link", { name: "Ver relatório" })).toHaveLength(2);
    expect(screen.queryByTestId("next-action")).toBeNull();
    expect(listMyInspections).toHaveBeenCalledWith(0, 10, "RELATORIO_DISPONIVEL");
  });

  it("oferece uma única ação quando a lista está vazia", async () => {
    vi.mocked(listMyInspections).mockResolvedValue(page([]));

    render(<ClientDashboard />);

    expect(await screen.findByText("Nenhuma vistoria iniciada")).toBeDefined();
    expect(screen.getByText(/fotos, contexto e relatório/i)).toBeDefined();
    expect(screen.getAllByRole("link", { name: "Iniciar primeira vistoria" })).toHaveLength(1);
  });

  it("recupera um erro inicial após retry", async () => {
    vi.mocked(listMyInspections)
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce(page([draftInspection]));
    const user = userEvent.setup();
    render(<ClientDashboard />);

    await user.click(await screen.findByRole("button", { name: "Tentar novamente" }));

    expect(await screen.findByText("Rua das Obras, 10")).toBeDefined();
    expect(listMyInspections).toHaveBeenCalledTimes(2);
  });
});

describe("NewInspectionForm", () => {
  beforeEach(() => {
    replace.mockReset();
    vi.mocked(createInspection).mockReset();
    vi.mocked(submitInspection).mockReset();
  });

  it("explica a regra mínima antes da criação", () => {
    render(<NewInspectionForm />);

    expect(screen.getByText(/uma visão geral por ambiente/i)).toBeDefined();
    expect(screen.getByText(/você pode adaptar/i)).toBeDefined();
    expect(screen.queryByText(/12 itens/i)).toBeNull();
  });

  it("troca as sugestões sem transformar os ambientes em obrigação", async () => {
    const user = userEvent.setup();
    render(<NewInspectionForm />);

    await user.click(screen.getByRole("radio", { name: "Apartamento" }));

    const roomNames = screen.getAllByLabelText(/Nome do ambiente/i) as HTMLInputElement[];
    expect(roomNames.some((input) => input.value === "Área de serviço")).toBe(true);
    expect(roomNames.some((input) => input.value === "Área externa")).toBe(false);
    expect(screen.getAllByText(/sugestão inicial/i).length).toBeGreaterThan(0);
  });

  it("permite adicionar, renomear, remover e reordenar ambientes", async () => {
    const user = userEvent.setup();
    render(<NewInspectionForm />);

    await user.click(screen.getByRole("button", { name: "Adicionar ambiente" }));
    const names = screen.getAllByLabelText(/Nome do ambiente/i) as HTMLInputElement[];
    await user.clear(names.at(-1)!);
    await user.type(names.at(-1)!, "Biblioteca");
    await user.click(screen.getByRole("button", { name: "Mover Biblioteca para cima" }));
    await user.click(screen.getByRole("button", { name: "Remover Entrada e fachada" }));

    expect(screen.getByDisplayValue("Biblioteca")).toBeDefined();
    expect(screen.queryByDisplayValue("Entrada e fachada")).toBeNull();
  });

  it("mostra validação acessível para nomes duplicados", async () => {
    const user = userEvent.setup();
    render(<NewInspectionForm />);
    const names = screen.getAllByLabelText(/Nome do ambiente/i) as HTMLInputElement[];
    await user.clear(names[1]);
    await user.type(names[1], names[0].value.toUpperCase());
    await user.type(screen.getByLabelText("Endereço do imóvel"), "Rua das Obras, 10");

    await user.click(screen.getByRole("button", { name: "Começar a registrar fotos" }));

    const alert = await screen.findByRole("alert");
    expect(alert.textContent).toContain("nomes diferentes");
    expect(alert.id).not.toBe("");
    expect(names[0].getAttribute("aria-invalid")).toBe("true");
    expect(names[0].getAttribute("aria-describedby")).toBe(alert.id);
    expect(document.activeElement).toBe(names[0]);
    expect(createInspection).not.toHaveBeenCalled();
  });

  it("associa e leva o foco ao erro de endereço", async () => {
    const user = userEvent.setup();
    render(<NewInspectionForm />);

    await user.click(screen.getByRole("button", { name: "Começar a registrar fotos" }));

    const alert = await screen.findByRole("alert");
    const address = screen.getByLabelText("Endereço do imóvel");
    expect(alert.textContent).toContain("Informe o endereço");
    expect(address.getAttribute("aria-invalid")).toBe("true");
    expect(address.getAttribute("aria-describedby")).toBe(alert.id);
    expect(document.activeElement).toBe(address);
  });

  it("cria endereço, tipo e roteiro em uma única operação sem submeter", async () => {
    vi.mocked(createInspection).mockResolvedValue(draftInspection);
    const user = userEvent.setup();
    render(<NewInspectionForm />);

    await user.type(screen.getByLabelText("Endereço do imóvel"), "Rua das Obras, 10");
    await user.click(screen.getByRole("radio", { name: "Apartamento" }));
    await user.dblClick(screen.getByRole("button", { name: "Começar a registrar fotos" }));

    await waitFor(() => expect(createInspection).toHaveBeenCalledTimes(1));
    expect(createInspection).toHaveBeenCalledWith(expect.objectContaining({
      endereco: "Rua das Obras, 10",
      tipoImovel: "APARTAMENTO",
      ambientes: expect.arrayContaining([
        expect.objectContaining({ tipo: "SALA", nome: "Sala" }),
        expect.objectContaining({ tipo: "QUARTO", nome: "Quarto" }),
      ]),
    }));
    expect(submitInspection).not.toHaveBeenCalled();
    expect(replace).toHaveBeenCalledWith(`/client/vistorias/${draftInspection.id}`);
  });

  it("preserva o endereço após ProblemDetail", async () => {
    vi.mocked(createInspection).mockRejectedValue(
      new ApiError({
        type: "urn:vistoria:problem:validation-error",
        title: "Dados inválidos",
        status: 422,
        detail: "Confirme o endereço informado.",
      }),
    );
    const user = userEvent.setup();
    render(<NewInspectionForm />);

    await user.type(screen.getByLabelText("Endereço do imóvel"), "Rua incompleta");
    await user.click(screen.getByRole("button", { name: "Começar a registrar fotos" }));

    expect((await screen.findByRole("alert")).textContent).toContain("Confirme o endereço");
    expect((screen.getByLabelText("Endereço do imóvel") as HTMLInputElement).value).toBe(
      "Rua incompleta",
    );
  });

  it("mantém a ação ocupada enquanto a única criação está em andamento", async () => {
    let finish!: (inspection: Inspection) => void;
    vi.mocked(createInspection).mockReturnValue(
      new Promise<Inspection>((resolve) => {
        finish = resolve;
      }),
    );
    const user = userEvent.setup();
    render(<NewInspectionForm />);
    await user.type(screen.getByLabelText("Endereço do imóvel"), "Av. Estrutural, 100");

    await user.click(screen.getByRole("button", { name: "Começar a registrar fotos" }));

    const busyButton = screen.getByRole("button", { name: "Preparando roteiro..." });
    expect((busyButton as HTMLButtonElement).disabled).toBe(true);
    finish(draftInspection);
    await waitFor(() => expect(replace).toHaveBeenCalled());
  });
});
