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
  status: "EM_RASCUNHO",
  endereco: "Rua das Estruturas, 80",
  dataCriacao: "2026-09-19T08:00:00",
  dataConclusao: null,
  imagens: [],
  analiseIa: null,
  revisoes: [],
};

const withEvidence: Inspection = {
  ...draft,
  imagens: [
    {
      id: 1,
      protocoloItem: "SALA_PAREDES_REVESTIMENTOS",
      dataUpload: "2026-09-19T08:10:00",
      conteudoUrl: "/api/foto/1",
    },
  ],
};

const reviewPending: Inspection = {
  ...withEvidence,
  status: "REVISAO_PENDENTE",
  analiseIa: {
    version: 1,
    imagens: [
      {
        imagemId: 1,
        identificadorAnalise: "evidencia-1",
        resumoGeral: "Há indícios visuais de possível umidade.",
        limitacoes: ["Análise baseada apenas em imagem."],
        qualidade: { utilizavel: true, problemas: [] },
        achados: [
          {
            indice: 0,
            area: "Parede",
            tipo: "Umidade possível",
            descricao: "Mancha aparente.",
            evidencia: "Alteração de cor.",
            gravidade: "media",
            confianca: "media",
            recomendacao: "Observar a evolução.",
            localizacao: null,
          },
        ],
      },
    ],
  },
};

describe("contrato do protocolo", () => {
  it("define os doze itens do MVP em cinco grupos", () => {
    expect(PROTOCOL_GROUPS.map((group) => group.name)).toEqual([
      "Sala",
      "Cozinha",
      "Banheiro",
      "Quarto",
      "Instalações",
    ]);
    expect(PROTOCOL_ITEM_TOTAL).toBe(12);
    expect(ACCEPTED_EVIDENCE_TYPES).toEqual(["image/jpeg", "image/png", "image/webp"]);
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

    expect(await screen.findByText("1 de 12 itens documentados")).toBeDefined();
    expect(screen.getAllByTestId("protocol-group")).toHaveLength(5);
    expect(document.querySelectorAll(".protocol-item")).toHaveLength(12);
    expect(screen.getByRole("button", { name: "Sala — Piso" }).getAttribute("aria-current")).toBe("step");
  });

  it("oferece câmera e galeria para o item selecionado", async () => {
    render(<InspectionWorkflow inspectionId={10} />);

    const item = await screen.findByTestId("protocol-item-SALA_PISO");
    expect(within(item).getByLabelText("Tirar foto de Sala — Piso")).toBeDefined();
    expect(within(item).getByLabelText("Escolher da galeria para Sala — Piso")).toBeDefined();
  });

  it.each([
    ["vazio", new File([], "vazio.jpg", { type: "image/jpeg" }), "não pode estar vazio"],
    ["tipo", new File(["texto"], "laudo.pdf", { type: "application/pdf" }), "JPEG, PNG ou WebP"],
    ["tamanho", oversizedFile(), "10 MB"],
  ])("rejeita arquivo %s sem chamar a API", async (_, file, message) => {
    render(<InspectionWorkflow inspectionId={10} />);
    const item = await screen.findByTestId("protocol-item-SALA_PISO");

    fireEvent.change(within(item).getByLabelText("Escolher da galeria para Sala — Piso"), {
      target: { files: [file] },
    });

    expect((await within(item).findByRole("alert")).textContent).toContain(message);
    expect(uploadEvidence).not.toHaveBeenCalled();
  });

  it("envia o código exato e avança após confirmação da API", async () => {
    const updated = {
      ...draft,
      imagens: [
        {
          id: 2,
          protocoloItem: "SALA_PISO",
          dataUpload: "2026-09-19T08:20:00",
          conteudoUrl: "/api/foto/2",
        },
      ],
    } satisfies Inspection;
    vi.mocked(uploadEvidence).mockResolvedValue(updated);
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    const item = await screen.findByTestId("protocol-item-SALA_PISO");
    const file = new File([new Uint8Array([0xff, 0xd8, 0xff])], "piso.jpg", { type: "image/jpeg" });

    await user.upload(within(item).getByLabelText("Escolher da galeria para Sala — Piso"), file);

    await waitFor(() =>
      expect(uploadEvidence).toHaveBeenCalledWith(10, "SALA_PISO", file),
    );
    expect(await screen.findByText("1 de 12 itens documentados")).toBeDefined();
    expect(screen.getByRole("button", { name: "Sala — Paredes e revestimentos" }).getAttribute("aria-current")).toBe("step");
  });

  it("mantém a evidência confirmada quando outro upload falha", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(withEvidence);
    vi.mocked(uploadEvidence).mockRejectedValue(new ApiError({
      type: "urn:vistoria:problem:invalid-evidence",
      title: "Imagem inválida",
      status: 422,
      detail: "A assinatura da imagem não corresponde ao tipo informado.",
    }));
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    const item = await screen.findByTestId("protocol-item-SALA_PAREDES_REVESTIMENTOS");
    const file = new File(["conteudo"], "outra.png", { type: "image/png" });

    await user.upload(within(item).getByLabelText("Escolher da galeria para Sala — Paredes e revestimentos"), file);

    expect((await within(item).findByRole("alert")).textContent).toContain("assinatura da imagem");
    expect(await screen.findByText("1 de 12 itens documentados")).toBeDefined();
  });

  it("impede submissão sem evidência confirmada", async () => {
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("0 de 12 itens documentados");

    await user.click(screen.getByRole("button", { name: "Enviar para análise da IA" }));

    expect((await screen.findByRole("alert")).textContent).toContain("ao menos uma foto");
    expect(submitInspection).not.toHaveBeenCalled();
  });

  it("submete uma única vez e mostra o estado assíncrono real", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(withEvidence);
    vi.mocked(submitInspection).mockResolvedValue({ ...withEvidence, status: "AGUARDANDO_IA" });
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("1 de 12 itens documentados");

    await user.dblClick(screen.getByRole("button", { name: "Enviar para análise da IA" }));

    await waitFor(() => expect(submitInspection).toHaveBeenCalledTimes(1));
    expect(await screen.findByText("Análise da IA em andamento")).toBeDefined();
    expect(screen.getByText(/seguro sair e voltar/i)).toBeDefined();
  });

  it("reenvia o mesmo caso somente quando a IA falhou", async () => {
    vi.mocked(getMyInspection).mockResolvedValue({ ...withEvidence, status: "FALHA_IA" });
    vi.mocked(submitInspection).mockResolvedValue({ ...withEvidence, status: "AGUARDANDO_IA" });
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);

    const retryButton = await screen.findByRole("button", { name: "Reenviar para análise" });
    expect(screen.queryByLabelText("Tirar foto de Sala — Piso")).toBeNull();
    expect(screen.queryByLabelText("Escolher da galeria para Sala — Piso")).toBeNull();

    await user.click(retryButton);

    expect(submitInspection).toHaveBeenCalledWith(10);
    expect(await screen.findByText("Análise da IA em andamento")).toBeDefined();
  });

  it("libera nova tentativa quando o polling informa falha da IA", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      vi.mocked(getMyInspection)
        .mockResolvedValueOnce(withEvidence)
        .mockResolvedValueOnce({ ...withEvidence, status: "FALHA_IA" });
      vi.mocked(submitInspection).mockResolvedValue({ ...withEvidence, status: "AGUARDANDO_IA" });
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
      render(<InspectionWorkflow inspectionId={10} />);

      await user.click(await screen.findByRole("button", { name: "Enviar para análise da IA" }));
      expect(await screen.findByText("Análise da IA em andamento")).toBeDefined();

      await vi.advanceTimersByTimeAsync(4000);
      await user.click(await screen.findByRole("button", { name: "Reenviar para análise" }));

      await waitFor(() => expect(submitInspection).toHaveBeenCalledTimes(2));
    } finally {
      vi.useRealTimers();
    }
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
        .mockResolvedValueOnce(reviewPending);

      render(<InspectionWorkflow inspectionId={10} />);

      expect(await screen.findByText("Análise da IA em andamento")).toBeDefined();

      await vi.advanceTimersByTimeAsync(4000);

      await waitFor(() => expect(screen.getByText("Análise concluída")).toBeDefined());
      expect(getMyInspection).toHaveBeenCalledTimes(2);
    } finally {
      vi.useRealTimers();
    }
  });

  it("cancela novas consultas quando a tela é desmontada", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      vi.mocked(getMyInspection).mockResolvedValue({ ...withEvidence, status: "AGUARDANDO_IA" });
      const { unmount } = render(<InspectionWorkflow inspectionId={10} />);
      expect(await screen.findByText("Análise da IA em andamento")).toBeDefined();

      unmount();
      await vi.advanceTimersByTimeAsync(8000);

      expect(getMyInspection).toHaveBeenCalledTimes(1);
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
