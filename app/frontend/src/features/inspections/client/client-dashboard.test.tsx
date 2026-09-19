import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError } from "@/lib/api";
import type { Inspection } from "../types";
import { createInspection, listMyInspections, submitInspection } from "../api";
import { ClientDashboard } from "./client-dashboard";
import { NewInspectionForm } from "./new-inspection-form";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));

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
  clienteId: 1,
  engenheiroId: null,
  status: "EM_RASCUNHO",
  preLaudoIa: null,
  parecerEngenheiro: null,
  endereco: "Rua das Obras, 10",
  dataCriacao: "2026-09-18T10:00:00",
  dataConclusao: null,
  imagens: [],
};

describe("ClientDashboard", () => {
  beforeEach(() => {
    replace.mockReset();
    vi.mocked(listMyInspections).mockReset();
    vi.mocked(createInspection).mockReset();
    vi.mocked(submitInspection).mockReset();
  });

  it("ordena vistorias pela data de criação mais recente e desempata pelo id", async () => {
    vi.mocked(listMyInspections).mockResolvedValue([
      { ...draftInspection, id: 1, endereco: "Antiga", dataCriacao: "2026-09-10T10:00:00" },
      { ...draftInspection, id: 2, endereco: "Recente 2", dataCriacao: "2026-09-19T10:00:00" },
      { ...draftInspection, id: 3, endereco: "Recente 3", dataCriacao: "2026-09-19T10:00:00" },
    ]);

    render(<ClientDashboard />);

    const cards = await screen.findAllByTestId("inspection-card");
    expect(cards.map((card) => card.textContent)).toEqual([
      expect.stringContaining("Recente 3"),
      expect.stringContaining("Recente 2"),
      expect.stringContaining("Antiga"),
    ]);
  });

  it("mostra endereço, status real e próxima ação", async () => {
    vi.mocked(listMyInspections).mockResolvedValue([draftInspection]);

    render(<ClientDashboard />);

    const card = await screen.findByTestId("inspection-card");
    expect(card.textContent).toContain("Rua das Obras, 10");
    expect(card.textContent).toContain("Em preenchimento");
    expect(within(card).getByRole("link", { name: "Continuar vistoria" }).getAttribute("href")).toBe(
      "/client/vistorias/42",
    );
  });

  it("oferece uma única ação quando a lista está vazia", async () => {
    vi.mocked(listMyInspections).mockResolvedValue([]);

    render(<ClientDashboard />);

    expect(await screen.findByText("Nenhuma vistoria iniciada")).toBeDefined();
    expect(screen.getAllByRole("link", { name: "Iniciar primeira vistoria" })).toHaveLength(1);
  });

  it("recupera um erro inicial após retry", async () => {
    vi.mocked(listMyInspections)
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce([draftInspection]);
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

  it("cria um rascunho sem submetê-lo", async () => {
    vi.mocked(createInspection).mockResolvedValue(draftInspection);
    const user = userEvent.setup();
    render(<NewInspectionForm />);

    await user.type(screen.getByLabelText("Endereço do imóvel"), "Rua das Obras, 10");
    await user.dblClick(screen.getByRole("button", { name: "Criar rascunho" }));

    await waitFor(() => expect(createInspection).toHaveBeenCalledTimes(1));
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
    await user.click(screen.getByRole("button", { name: "Criar rascunho" }));

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

    await user.click(screen.getByRole("button", { name: "Criar rascunho" }));

    const busyButton = screen.getByRole("button", { name: "Criando rascunho..." });
    expect((busyButton as HTMLButtonElement).disabled).toBe(true);
    finish(draftInspection);
    await waitFor(() => expect(replace).toHaveBeenCalled());
  });
});
