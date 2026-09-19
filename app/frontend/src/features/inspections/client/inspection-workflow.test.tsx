import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError } from "@/lib/api";
import { getMyInspection, loadEvidence, submitInspection, uploadEvidence } from "../api";
import type { Inspection } from "../types";
import { InspectionWorkflow } from "./inspection-workflow";
import {
  ACCEPTED_EVIDENCE_TYPES,
  MAX_EVIDENCE_BYTES,
  PROTOCOL_GROUPS,
  calculateProgress,
} from "../shared/protocol";

vi.mock("../api", () => ({
  getMyInspection: vi.fn(),
  submitInspection: vi.fn(),
  uploadEvidence: vi.fn(),
  loadEvidence: vi.fn(),
}));

const draft: Inspection = {
  id: 10,
  clienteId: 1,
  engenheiroId: null,
  status: "EM_RASCUNHO",
  preLaudoIa: null,
  parecerEngenheiro: null,
  endereco: "Rua das Estruturas, 80",
  dataCriacao: "2026-09-19T08:00:00",
  dataConclusao: null,
  imagens: [],
};

const withEvidence: Inspection = {
  ...draft,
  imagens: [
    { id: 1, protocoloItem: "SALA_PISO", dataUpload: "2026-09-19T08:10:00", conteudoUrl: "/api/foto/1" },
  ],
};

describe("contrato do protocolo", () => {
  it("define cinco grupos e os mesmos doze códigos do backend", () => {
    expect(PROTOCOL_GROUPS).toHaveLength(5);
    expect(ACCEPTED_EVIDENCE_TYPES).toEqual(["image/jpeg", "image/png", "image/webp"]);
    expect(PROTOCOL_GROUPS.flatMap((group) => group.items)).toHaveLength(12);
    expect(PROTOCOL_GROUPS.flatMap((group) => group.items.map((item) => item.code))).toEqual([
      "SALA_PISO",
      "SALA_PAREDES_REVESTIMENTOS",
      "SALA_TETO_ILUMINACAO",
      "COZINHA_PISO",
      "COZINHA_PAREDES_BANCADAS",
      "COZINHA_INSTALACOES",
      "BANHEIRO_REVESTIMENTOS",
      "BANHEIRO_HIDRAULICA",
      "QUARTO_PISO",
      "QUARTO_PAREDES_TETO",
      "INSTALACOES_ELETRICAS",
      "INSTALACOES_HIDRAULICAS",
    ]);
  });

  it("calcula progresso por item único confirmado pela API", () => {
    expect(calculateProgress([
      ...withEvidence.imagens,
      { ...withEvidence.imagens[0], id: 2 },
      { ...withEvidence.imagens[0], id: 3, protocoloItem: "COZINHA_PISO" },
    ])).toBe(2);
  });
});

describe("InspectionWorkflow", () => {
  beforeEach(() => {
    vi.mocked(getMyInspection).mockReset();
    vi.mocked(uploadEvidence).mockReset();
    vi.mocked(submitInspection).mockReset();
    vi.mocked(loadEvidence).mockReset();
    vi.mocked(getMyInspection).mockResolvedValue(draft);
    vi.mocked(loadEvidence).mockResolvedValue(new Blob(["foto"], { type: "image/jpeg" }));
  });

  it("renderiza cinco grupos, doze itens e progresso persistido", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(withEvidence);
    render(<InspectionWorkflow inspectionId={10} />);

    expect(await screen.findByText("1 de 12 itens documentados")).toBeDefined();
    expect(screen.getAllByTestId("protocol-group")).toHaveLength(5);
    expect(document.querySelectorAll(".protocol-item")).toHaveLength(12);
  });

  it.each([
    ["vazio", new File([], "vazio.jpg", { type: "image/jpeg" }), "não pode estar vazio"],
    ["tipo", new File(["texto"], "laudo.pdf", { type: "application/pdf" }), "JPEG, PNG ou WebP"],
    ["tamanho", oversizedFile(), "10 MB"],
  ])("rejeita arquivo %s sem chamar a API", async (_, file, message) => {
    render(<InspectionWorkflow inspectionId={10} />);
    const item = await screen.findByTestId("protocol-item-SALA_PISO");

    fireEvent.change(within(item).getByLabelText("Adicionar foto de Sala — Piso"), { target: { files: [file] } });

    expect((await within(item).findByRole("alert")).textContent).toContain(message);
    expect(uploadEvidence).not.toHaveBeenCalled();
  });

  it("envia FormData com código exato e preserva evidências anteriores", async () => {
    const updated = {
      ...withEvidence,
      imagens: [
        ...withEvidence.imagens,
        { id: 2, protocoloItem: "COZINHA_PISO", dataUpload: "2026-09-19T08:20:00", conteudoUrl: "/api/foto/2" },
      ],
    } satisfies Inspection;
    vi.mocked(getMyInspection).mockResolvedValue(withEvidence);
    vi.mocked(uploadEvidence).mockResolvedValue(updated);
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    const item = await screen.findByTestId("protocol-item-COZINHA_PISO");
    const file = new File([new Uint8Array([0xff, 0xd8, 0xff])], "cozinha.jpg", { type: "image/jpeg" });

    await user.upload(within(item).getByLabelText("Adicionar foto de Cozinha — Piso"), file);

    await waitFor(() => expect(uploadEvidence).toHaveBeenCalledWith(10, "COZINHA_PISO", file));
    expect(await screen.findByText("2 de 12 itens documentados")).toBeDefined();
    expect(screen.getByTestId("protocol-item-SALA_PISO").textContent).toContain("1 foto");
  });

  it("permite repetir upload falho sem apagar o restante do rascunho", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(withEvidence);
    vi.mocked(uploadEvidence)
      .mockRejectedValueOnce(new ApiError({ type: "about:blank", title: "Falha", status: 503, detail: "Serviço temporariamente indisponível." }))
      .mockResolvedValueOnce({ ...withEvidence, imagens: [...withEvidence.imagens, { id: 2, protocoloItem: "QUARTO_PISO", dataUpload: "2026-09-19T08:30:00", conteudoUrl: "/api/foto/2" }] });
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    const item = await screen.findByTestId("protocol-item-QUARTO_PISO");
    const file = new File([new Uint8Array([0x89, 0x50, 0x4e, 0x47])], "quarto.png", { type: "image/png" });

    await user.upload(within(item).getByLabelText("Adicionar foto de Quarto — Piso"), file);
    await user.click(await within(item).findByRole("button", { name: "Tentar novamente" }));

    expect(uploadEvidence).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId("protocol-item-SALA_PISO").textContent).toContain("1 foto");
    expect(await screen.findByText("2 de 12 itens documentados")).toBeDefined();
  });

  it("impede submissão sem evidência confirmada", async () => {
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("0 de 12 itens documentados");

    await user.click(screen.getByRole("button", { name: "Enviar para análise" }));

    expect((await screen.findByRole("alert")).textContent).toContain("ao menos uma evidência");
    expect(submitInspection).not.toHaveBeenCalled();
  });

  it("submete uma vez e renderiza o status devolvido", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(withEvidence);
    vi.mocked(submitInspection).mockResolvedValue({ ...withEvidence, status: "AGUARDANDO_ENGENHEIRO" });
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("1 de 12 itens documentados");

    await user.dblClick(screen.getByRole("button", { name: "Enviar para análise" }));

    await waitFor(() => expect(submitInspection).toHaveBeenCalledTimes(1));
    expect(await screen.findByText("Aguardando revisão do engenheiro")).toBeDefined();
  });

  it("exibe parecer e mantém upload disponível em devolução", async () => {
    vi.mocked(getMyInspection).mockResolvedValue({ ...withEvidence, status: "DEVOLVIDA_CLIENTE", parecerEngenheiro: "Fotografe novamente a parede norte." });
    render(<InspectionWorkflow inspectionId={10} />);

    expect(await screen.findByText("Fotografe novamente a parede norte.")).toBeDefined();
    expect(screen.getByLabelText("Adicionar foto de Sala — Piso")).toBeDefined();
  });

  it("reenvia o mesmo caso somente quando a IA falhou", async () => {
    vi.mocked(getMyInspection).mockResolvedValue({ ...withEvidence, status: "FALHA_IA" });
    vi.mocked(submitInspection).mockResolvedValue({ ...withEvidence, status: "AGUARDANDO_ENGENHEIRO" });
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);

    await user.click(await screen.findByRole("button", { name: "Reenviar para pré-análise" }));

    expect(submitInspection).toHaveBeenCalledWith(10);
    expect(await screen.findByText("Aguardando revisão do engenheiro")).toBeDefined();
    expect(screen.queryByLabelText("Adicionar foto de Sala — Piso")).toBeNull();
  });

  it("mantém acompanhamento sem upload quando o caso não é editável", async () => {
    vi.mocked(getMyInspection).mockResolvedValue({ ...withEvidence, status: "AGUARDANDO_IA" });
    render(<InspectionWorkflow inspectionId={10} />);

    expect(await screen.findAllByText("Pré-análise em andamento")).toHaveLength(2);
    expect(screen.getByText("A vistoria está em modo de acompanhamento.")).toBeDefined();
    expect(screen.queryByRole("button", { name: "Enviar para análise" })).toBeNull();
  });
});

function oversizedFile(): File {
  const file = new File(["x"], "grande.webp", { type: "image/webp" });
  Object.defineProperty(file, "size", { value: MAX_EVIDENCE_BYTES + 1 });
  return file;
}
