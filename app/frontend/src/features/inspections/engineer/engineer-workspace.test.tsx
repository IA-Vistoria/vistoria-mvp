import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError } from "@/lib/api";
import { getPendingInspection, listPendingInspections, loadEvidence, reviewInspection } from "../api";
import { EvidenceImage } from "../shared/evidence-image";
import type { Inspection } from "../types";
import { EngineerDashboard } from "./engineer-dashboard";
import { EngineerReview } from "./engineer-review";
import { splitPreReport } from "./pre-report";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace }) }));
vi.mock("../api", () => ({
  getPendingInspection: vi.fn(),
  listPendingInspections: vi.fn(),
  loadEvidence: vi.fn(),
  reviewInspection: vi.fn(),
}));

const pending: Inspection = {
  id: 20,
  clienteId: 7,
  engenheiroId: null,
  status: "AGUARDANDO_ENGENHEIRO",
  preLaudoIa: "- Fissura aparente na parede norte\n• Sinal de umidade próximo à janela",
  parecerEngenheiro: null,
  endereco: "Av. Concreto, 180",
  dataCriacao: "2026-09-19T09:00:00",
  dataConclusao: null,
  imagens: [
    { id: 3, protocoloItem: "SALA_PAREDES_REVESTIMENTOS", dataUpload: "2026-09-19T09:10:00", conteudoUrl: "/api/foto/3" },
  ],
};

describe("pré-laudo", () => {
  it("mantém somente linhas reais, removendo marcadores sem inferir conteúdo", () => {
    expect(splitPreReport("- Linha A\n\n* Linha B\r\n• Linha C")).toEqual(["Linha A", "Linha B", "Linha C"]);
    expect(splitPreReport("  ")).toEqual([]);
  });
});

describe("EngineerDashboard", () => {
  beforeEach(() => {
    vi.mocked(listPendingInspections).mockReset();
  });

  it("ordena a fila por data e id, exibindo somente dados do DTO", async () => {
    vi.mocked(listPendingInspections).mockResolvedValue([
      { ...pending, id: 1, endereco: "Primeiro", dataCriacao: "2026-09-18T09:00:00" },
      { ...pending, id: 2, endereco: "Segundo", dataCriacao: "2026-09-19T09:00:00" },
      { ...pending, id: 3, endereco: "Terceiro", dataCriacao: "2026-09-19T09:00:00" },
    ]);
    render(<EngineerDashboard />);

    const cards = await screen.findAllByTestId("review-card");
    expect(cards.map((card) => card.textContent)).toEqual([
      expect.stringContaining("Terceiro"),
      expect.stringContaining("Segundo"),
      expect.stringContaining("Primeiro"),
    ]);
    expect(within(cards[0]).getByRole("link", { name: "Revisar vistoria #3" }).getAttribute("href")).toBe("/engineer/vistorias/3");
  });
});

describe("EngineerReview", () => {
  beforeEach(() => {
    replace.mockReset();
    vi.mocked(getPendingInspection).mockReset();
    vi.mocked(listPendingInspections).mockReset();
    vi.mocked(reviewInspection).mockReset();
    vi.mocked(loadEvidence).mockReset();
    vi.mocked(getPendingInspection).mockResolvedValue(pending);
    vi.mocked(listPendingInspections).mockResolvedValue([pending]);
    vi.mocked(loadEvidence).mockResolvedValue(new Blob(["foto"], { type: "image/jpeg" }));
  });

  it("integra fila, galeria, pré-laudo e decisão no mesmo workspace", async () => {
    const secondEvidence = {
      id: 4,
      protocoloItem: "SALA_PISO" as const,
      dataUpload: "2026-09-19T09:12:00",
      conteudoUrl: "/api/foto/4",
    };
    vi.mocked(getPendingInspection).mockResolvedValue({ ...pending, imagens: [...pending.imagens, secondEvidence] });
    vi.mocked(listPendingInspections).mockResolvedValue([
      pending,
      { ...pending, id: 19, endereco: "Rua do Projeto, 50" },
    ]);
    const user = userEvent.setup();

    render(<EngineerReview inspectionId={20} />);

    const queue = await screen.findByRole("region", { name: "Fila de revisão" });
    expect(within(queue).getAllByRole("link")).toHaveLength(2);
    expect(within(queue).getByRole("link", { name: /vistoria #20/i }).getAttribute("aria-current")).toBe("page");
    expect(await screen.findByAltText("Evidência em destaque: Sala — Paredes e revestimentos")).toBeDefined();
    expect(screen.getByRole("heading", { name: "Pré-laudo da IA" })).toBeDefined();
    expect(screen.getByRole("heading", { name: "Decisão técnica" })).toBeDefined();

    await user.click(screen.getByRole("button", { name: "Visualizar Sala — Piso, evidência 2" }));
    expect(await screen.findByAltText("Evidência em destaque: Sala — Piso")).toBeDefined();
  });

  it("exibe evidências e linhas reais sem inventar severidade ou confiança", async () => {
    render(<EngineerReview inspectionId={20} />);

    expect(await screen.findByText("Fissura aparente na parede norte")).toBeDefined();
    expect(screen.getByText("Sinal de umidade próximo à janela")).toBeDefined();
    expect(await screen.findByAltText("Sala — Paredes e revestimentos, evidência 1")).toBeDefined();
    expect(screen.queryByText(/severidade/i)).toBeNull();
    expect(screen.queryByText(/confiança/i)).toBeNull();
  });

  it("mantém próximos os dois avisos profissionais Human-in-the-Loop", async () => {
    render(<EngineerReview inspectionId={20} />);
    expect(await screen.findByText("A IA sugere. O engenheiro decide.")).toBeDefined();
    expect(screen.getByText("Este conteúdo não substitui a avaliação técnica profissional.")).toBeDefined();
  });

  it("mantém ambas decisões desabilitadas com parecer em branco", async () => {
    const user = userEvent.setup();
    render(<EngineerReview inspectionId={20} />);
    await screen.findByText("Pré-laudo da IA");
    await user.type(screen.getByLabelText("Parecer técnico"), "   ");

    expect((screen.getByRole("button", { name: "Aprovar vistoria" }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByRole("button", { name: "Devolver ao cliente" }) as HTMLButtonElement).disabled).toBe(true);
  });

  it("aprova uma única vez com parecer aparado e reconcilia o status", async () => {
    vi.mocked(reviewInspection).mockResolvedValue({ ...pending, status: "CONCLUIDA", parecerEngenheiro: "Estrutura em condições de uso." });
    const user = userEvent.setup();
    render(<EngineerReview inspectionId={20} />);
    await user.type(await screen.findByLabelText("Parecer técnico"), "  Estrutura em condições de uso.  ");

    await user.dblClick(screen.getByRole("button", { name: "Aprovar vistoria" }));

    await waitFor(() => expect(reviewInspection).toHaveBeenCalledTimes(1));
    expect(reviewInspection).toHaveBeenCalledWith(20, true, "Estrutura em condições de uso.");
    expect(await screen.findAllByText("Vistoria concluída")).toHaveLength(2);
  });

  it("devolve uma única vez com false e retorna à fila", async () => {
    vi.mocked(reviewInspection).mockResolvedValue({ ...pending, status: "DEVOLVIDA_CLIENTE", parecerEngenheiro: "Complemente a parede norte." });
    const user = userEvent.setup();
    render(<EngineerReview inspectionId={20} />);
    await user.type(await screen.findByLabelText("Parecer técnico"), "Complemente a parede norte.");

    await user.dblClick(screen.getByRole("button", { name: "Devolver ao cliente" }));

    await waitFor(() => expect(reviewInspection).toHaveBeenCalledTimes(1));
    expect(reviewInspection).toHaveBeenCalledWith(20, false, "Complemente a parede norte.");
    expect(replace).toHaveBeenCalledWith("/engineer");
  });

  it("trata 409 como processamento concorrente e refaz a fila", async () => {
    vi.mocked(reviewInspection).mockRejectedValue(new ApiError({ type: "urn:vistoria:problem:stale", title: "Conflito", status: 409, detail: "O caso foi alterado." }));
    vi.mocked(listPendingInspections).mockResolvedValue([]);
    const user = userEvent.setup();
    render(<EngineerReview inspectionId={20} />);
    await user.type(await screen.findByLabelText("Parecer técnico"), "Parecer já revisado.");
    await user.click(screen.getByRole("button", { name: "Aprovar vistoria" }));

    expect(await screen.findByText("Este caso já foi processado.")).toBeDefined();
    expect(listPendingInspections).toHaveBeenCalledTimes(2);
    expect(screen.queryByRole("button", { name: "Aprovar vistoria" })).toBeNull();
  });
});

describe("EvidenceImage", () => {
  beforeEach(() => {
    vi.mocked(loadEvidence).mockReset();
  });

  it("isola foto indisponível sem bloquear o restante", async () => {
    vi.mocked(loadEvidence).mockRejectedValue(new Error("arquivo ausente"));
    render(<EvidenceImage evidence={pending.imagens[0]} alt="Parede norte" />);
    expect(await screen.findByText("Foto indisponível")).toBeDefined();
  });

  it("revoga object URLs na troca e na desmontagem", async () => {
    vi.mocked(loadEvidence).mockResolvedValue(new Blob(["foto"], { type: "image/jpeg" }));
    const create = vi.spyOn(URL, "createObjectURL").mockReturnValueOnce("blob:primeira").mockReturnValueOnce("blob:segunda");
    const revoke = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => undefined);
    const { rerender, unmount } = render(<EvidenceImage evidence={pending.imagens[0]} alt="Primeira" />);
    await screen.findByAltText("Primeira");

    rerender(<EvidenceImage evidence={{ ...pending.imagens[0], id: 4, conteudoUrl: "/api/foto/4" }} alt="Segunda" />);
    await screen.findByAltText("Segunda");
    expect(revoke).toHaveBeenCalledWith("blob:primeira");

    unmount();
    expect(revoke).toHaveBeenCalledWith("blob:segunda");
    create.mockRestore();
    revoke.mockRestore();
  });
});
