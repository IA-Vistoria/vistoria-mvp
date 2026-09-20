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
  PROTOCOL_ITEM_TOTAL,
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
    {
      id: 1,
      protocoloItem: "SALA_PAREDES_REVESTIMENTOS",
      dataUpload: "2026-09-19T08:10:00",
      conteudoUrl: "/api/foto/1",
      storagePath: "uploads/a.jpg",
    },
  ],
};

const concluded: Inspection = {
  ...withEvidence,
  status: "CONCLUIDA",
  dataConclusao: "2026-09-19T09:00:00",
  preLaudoIa: JSON.stringify({
    version: 1,
    images: [
      {
        storagePath: "uploads/a.jpg",
        overallSummary: "Há indícios visuais de possível umidade.",
        limitations: ["Análise baseada apenas em imagem."],
        imageQuality: { usable: true, issues: [] },
        areas: [
          {
            area: "parede",
            issueType: "possible_moisture",
            description: "Mancha aparente.",
            evidence: "Alteração de cor.",
            severity: "media",
            confidence: "media",
            recommendation: "Avaliação presencial.",
            location: null,
          },
        ],
      },
    ],
  }),
};

describe("contrato do protocolo", () => {
  it("define apenas paredes no MVP", () => {
    expect(PROTOCOL_GROUPS).toHaveLength(1);
    expect(PROTOCOL_ITEM_TOTAL).toBe(1);
    expect(ACCEPTED_EVIDENCE_TYPES).toEqual(["image/jpeg", "image/png", "image/webp"]);
    expect(PROTOCOL_GROUPS.flatMap((group) => group.items.map((item) => item.code))).toEqual([
      "SALA_PAREDES_REVESTIMENTOS",
    ]);
  });

  it("calcula progresso por item único confirmado pela API", () => {
    expect(calculateProgress([
      ...withEvidence.imagens,
      { ...withEvidence.imagens[0], id: 2 },
    ])).toBe(1);
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

  it("renderiza o item de paredes e progresso persistido", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(withEvidence);
    render(<InspectionWorkflow inspectionId={10} />);

    expect(await screen.findByText("1 de 1 item documentado")).toBeDefined();
    expect(screen.getAllByTestId("protocol-group")).toHaveLength(1);
    expect(document.querySelectorAll(".protocol-item")).toHaveLength(1);
  });

  it.each([
    ["vazio", new File([], "vazio.jpg", { type: "image/jpeg" }), "não pode estar vazio"],
    ["tipo", new File(["texto"], "laudo.pdf", { type: "application/pdf" }), "JPEG, PNG ou WebP"],
    ["tamanho", oversizedFile(), "10 MB"],
  ])("rejeita arquivo %s sem chamar a API", async (_, file, message) => {
    render(<InspectionWorkflow inspectionId={10} />);
    const item = await screen.findByTestId("protocol-item-SALA_PAREDES_REVESTIMENTOS");

    fireEvent.change(within(item).getByLabelText("Adicionar foto de Paredes — Paredes"), {
      target: { files: [file] },
    });

    expect((await within(item).findByRole("alert")).textContent).toContain(message);
    expect(uploadEvidence).not.toHaveBeenCalled();
  });

  it("envia FormData com código de paredes", async () => {
    const updated = {
      ...withEvidence,
      imagens: [
        ...withEvidence.imagens,
        {
          id: 2,
          protocoloItem: "SALA_PAREDES_REVESTIMENTOS",
          dataUpload: "2026-09-19T08:20:00",
          conteudoUrl: "/api/foto/2",
          storagePath: "uploads/b.jpg",
        },
      ],
    } satisfies Inspection;
    vi.mocked(getMyInspection).mockResolvedValue(withEvidence);
    vi.mocked(uploadEvidence).mockResolvedValue(updated);
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    const item = await screen.findByTestId("protocol-item-SALA_PAREDES_REVESTIMENTOS");
    const file = new File([new Uint8Array([0xff, 0xd8, 0xff])], "parede.jpg", { type: "image/jpeg" });

    await user.upload(within(item).getByLabelText("Adicionar foto de Paredes — Paredes"), file);

    await waitFor(() =>
      expect(uploadEvidence).toHaveBeenCalledWith(10, "SALA_PAREDES_REVESTIMENTOS", file),
    );
    expect(await screen.findByText("1 de 1 item documentado")).toBeDefined();
    expect(screen.getByTestId("protocol-item-SALA_PAREDES_REVESTIMENTOS").textContent).toContain("2 foto");
  });

  it("impede submissão sem evidência confirmada", async () => {
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("0 de 1 item documentado");

    await user.click(screen.getByRole("button", { name: "Enviar para análise da IA" }));

    expect((await screen.findByRole("alert")).textContent).toContain("ao menos uma foto");
    expect(submitInspection).not.toHaveBeenCalled();
  });

  it("submete e mostra o resultado quando a IA conclui", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(withEvidence);
    vi.mocked(submitInspection).mockResolvedValue(concluded);
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("1 de 1 item documentado");

    await user.dblClick(screen.getByRole("button", { name: "Enviar para análise da IA" }));

    await waitFor(() => expect(submitInspection).toHaveBeenCalledTimes(1));
    expect(await screen.findByText("Vistoria concluída")).toBeDefined();
    expect(await screen.findByText("Possível umidade")).toBeDefined();
  });

  it("reenvia o mesmo caso somente quando a IA falhou", async () => {
    vi.mocked(getMyInspection).mockResolvedValue({ ...withEvidence, status: "FALHA_IA" });
    vi.mocked(submitInspection).mockResolvedValue(concluded);
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);

    await user.click(await screen.findByRole("button", { name: "Reenviar para análise" }));

    expect(submitInspection).toHaveBeenCalledWith(10);
    expect(await screen.findByText("Possível umidade")).toBeDefined();
  });

  it("mantém acompanhamento sem upload enquanto a IA processa", async () => {
    vi.mocked(getMyInspection).mockResolvedValue({ ...withEvidence, status: "AGUARDANDO_IA" });
    render(<InspectionWorkflow inspectionId={10} />);

    expect(await screen.findByText("Análise da IA em andamento")).toBeDefined();
    expect(screen.queryByRole("button", { name: "Enviar para análise da IA" })).toBeNull();
  });

  it("atualiza sozinha quando a análise termina (polling)", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      vi.mocked(getMyInspection)
        .mockResolvedValueOnce({ ...withEvidence, status: "AGUARDANDO_IA" })
        .mockResolvedValueOnce(concluded);

      render(<InspectionWorkflow inspectionId={10} />);

      expect(await screen.findByText("Análise da IA em andamento")).toBeDefined();

      await vi.advanceTimersByTimeAsync(4000);

      await waitFor(() => expect(screen.getByText("Possível umidade")).toBeDefined());
      expect(getMyInspection).toHaveBeenCalledTimes(2);
    } finally {
      vi.useRealTimers();
    }
  });
});

function oversizedFile(): File {
  const file = new File(["x"], "grande.webp", { type: "image/webp" });
  Object.defineProperty(file, "size", { value: MAX_EVIDENCE_BYTES + 1 });
  return file;
}
