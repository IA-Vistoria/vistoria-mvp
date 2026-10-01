import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  getMyInspection,
  loadEvidence,
  submitInspection,
  updateInspectionRoute,
  uploadEvidence,
} from "../api";
import type { Evidence, Inspection } from "../types";
import { MAX_EVIDENCE_BYTES } from "../shared/protocol";
import { InspectionWorkflow } from "./inspection-workflow";

vi.mock("../api", () => ({
  getMyInspection: vi.fn(),
  loadEvidence: vi.fn(),
  submitInspection: vi.fn(),
  updateInspectionRoute: vi.fn(),
  uploadEvidence: vi.fn(),
}));

const environments = [
  { id: 11, tipo: "SALA" as const, nome: "Sala", ordem: 0 },
  { id: 12, tipo: "QUARTO" as const, nome: "Quarto de hóspedes", ordem: 1 },
  { id: 13, tipo: "VARANDA" as const, nome: "Varanda", ordem: 2 },
];

const overview: Evidence = {
  id: 1,
  ambienteId: 11,
  ambienteNome: "Sala",
  categoria: "VISAO_GERAL",
  protocoloItem: "SALA_VISAO_GERAL",
  dataUpload: "2026-09-19T08:10:00",
  conteudoUrl: "/api/foto/1",
};

const draft: Inspection = {
  id: 10,
  version: 2,
  clienteId: 1,
  status: "EM_RASCUNHO",
  endereco: "Rua das Estruturas, 80",
  tipoImovel: "APARTAMENTO",
  ambientes: environments,
  dataCriacao: "2026-09-19T08:00:00",
  dataConclusao: null,
  imagens: [],
  analiseIa: null,
  manifestacoes: [],
};

const withOverview: Inspection = { ...draft, imagens: [overview] };
const complete: Inspection = {
  ...draft,
  imagens: environments.map((environment, index) => ({
    ...overview,
    id: index + 1,
    ambienteId: environment.id,
    ambienteNome: environment.nome,
    protocoloItem: `${environment.tipo}_VISAO_GERAL`,
  })),
};

const reportReady: Inspection = {
  ...complete,
  status: "RELATORIO_DISPONIVEL",
  dataConclusao: "2026-09-19T12:00:00Z",
  analiseIa: {
    version: 2,
    resultadoGeral: "APROVADO",
    motivoResultadoGeral: "Nenhum indício visual relevante foi identificado nas evidências utilizáveis.",
    ambientes: environments.map((environment) => ({
      id: environment.id,
      nome: environment.nome,
      resultado: "APROVADO" as const,
      motivoResultado: "Nenhum indício visual relevante foi identificado.",
    })),
    imagens: [],
  },
};

describe("InspectionWorkflow adaptativo", () => {
  beforeEach(() => {
    vi.mocked(getMyInspection).mockReset();
    vi.mocked(loadEvidence).mockReset();
    vi.mocked(submitInspection).mockReset();
    vi.mocked(updateInspectionRoute).mockReset();
    vi.mocked(uploadEvidence).mockReset();
    vi.mocked(getMyInspection).mockResolvedValue(draft);
    vi.mocked(loadEvidence).mockResolvedValue(new Blob(["foto"], { type: "image/jpeg" }));
  });

  it("usa somente os ambientes persistidos e o progresso real", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(withOverview);
    render(<InspectionWorkflow inspectionId={10} />);

    expect(await screen.findByText("1 de 3 ambientes com visão geral")).toBeDefined();
    expect(screen.getAllByTestId("environment-step")).toHaveLength(3);
    expect(screen.getByRole("button", { name: /Quarto de hóspedes/ }).getAttribute("aria-current")).toBe("step");
    expect(screen.queryByText(/12 itens/i)).toBeNull();
  });

  it("explica a regra mínima e mantém detalhe como opcional", async () => {
    render(<InspectionWorkflow inspectionId={10} />);

    expect(await screen.findByText(/uma visão geral é obrigatória/i)).toBeDefined();
    expect(screen.getByText(/detalhes são opcionais/i)).toBeDefined();
  });

  it("oferece câmera e galeria para visão geral e detalhe", async () => {
    render(<InspectionWorkflow inspectionId={10} />);
    const room = await screen.findByTestId("environment-capture-11");

    expect(within(room).getByLabelText("Tirar visão geral de Sala")).toBeDefined();
    expect(within(room).getByLabelText("Escolher detalhe de Sala da galeria")).toBeDefined();
    expect(within(room).getAllByText(/JPEG, PNG ou WebP · até 7 MB/i).length).toBeGreaterThan(0);
  });

  it.each([
    ["vazio", new File([], "vazio.jpg", { type: "image/jpeg" }), "não pode estar vazio"],
    ["tipo", new File(["texto"], "laudo.pdf", { type: "application/pdf" }), "JPEG, PNG ou WebP"],
    ["tamanho", oversizedFile(), "7 MB"],
  ])("rejeita arquivo %s sem chamar a API", async (_, file, message) => {
    render(<InspectionWorkflow inspectionId={10} />);
    const room = await screen.findByTestId("environment-capture-11");

    fireEvent.change(within(room).getByLabelText("Escolher visão geral de Sala da galeria"), {
      target: { files: [file] },
    });

    expect((await within(room).findByRole("alert")).textContent).toContain(message);
    expect(uploadEvidence).not.toHaveBeenCalled();
  });

  it("envia ambiente e categoria exatos e avança após a visão geral", async () => {
    vi.mocked(uploadEvidence).mockResolvedValue(withOverview);
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    const room = await screen.findByTestId("environment-capture-11");
    const file = imageFile("sala.jpg");

    await user.upload(within(room).getByLabelText("Escolher visão geral de Sala da galeria"), file);

    await waitFor(() => expect(uploadEvidence).toHaveBeenCalledWith(10, {
      ambienteId: 11,
      categoria: "VISAO_GERAL",
      file,
    }));
    expect(screen.getByRole("button", { name: /Quarto de hóspedes/ }).getAttribute("aria-current")).toBe("step");
  });

  it("envia detalhe sem marcar o ambiente como concluído", async () => {
    const detail: Evidence = { ...overview, categoria: "DETALHE", protocoloItem: "SALA_DETALHE" };
    vi.mocked(uploadEvidence).mockResolvedValue({ ...draft, imagens: [detail] });
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    const room = await screen.findByTestId("environment-capture-11");
    const file = imageFile("detalhe.jpg");

    await user.upload(within(room).getByLabelText("Escolher detalhe de Sala da galeria"), file);

    expect(await screen.findByText("0 de 3 ambientes com visão geral")).toBeDefined();
    expect(uploadEvidence).toHaveBeenCalledWith(10, { ambienteId: 11, categoria: "DETALHE", file });
  });

  it("pula para o próximo ambiente sem alterar o progresso", async () => {
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("0 de 3 ambientes com visão geral");

    await user.click(screen.getByRole("button", { name: "Pular por agora" }));

    expect(screen.getByRole("button", { name: /Quarto de hóspedes/ }).getAttribute("aria-current")).toBe("step");
    expect(screen.getByText("0 de 3 ambientes com visão geral")).toBeDefined();
  });

  it("leva ao primeiro ambiente ausente antes de enviar", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(withOverview);
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("1 de 3 ambientes com visão geral");

    await user.click(screen.getByRole("button", { name: "Enviar para análise da IA" }));

    expect((await screen.findByRole("alert")).textContent).toContain("Quarto de hóspedes e Varanda");
    expect(screen.getByRole("button", { name: /Quarto de hóspedes/ }).getAttribute("aria-current")).toBe("step");
    expect(submitInspection).not.toHaveBeenCalled();
  });

  it("salva adição, renomeação e ordem do roteiro", async () => {
    const updated = {
      ...draft,
      version: 3,
      ambientes: [
        { ...environments[1], nome: "Suíte", ordem: 0 },
        { ...environments[0], ordem: 1 },
        { id: 20, tipo: "OUTRO" as const, nome: "Ateliê", ordem: 2 },
      ],
    };
    vi.mocked(updateInspectionRoute).mockResolvedValue(updated);
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("0 de 3 ambientes com visão geral");

    await user.click(screen.getByRole("button", { name: "Editar roteiro" }));
    const roomNames = screen.getAllByLabelText(/Nome do ambiente/i) as HTMLInputElement[];
    await user.clear(roomNames[1]);
    await user.type(roomNames[1], "Suíte");
    await user.click(screen.getByRole("button", { name: "Mover Suíte para cima" }));
    await user.click(screen.getByRole("button", { name: "Remover Varanda" }));
    await user.click(screen.getByRole("button", { name: "Adicionar ambiente" }));
    const updatedNames = screen.getAllByLabelText(/Nome do ambiente/i) as HTMLInputElement[];
    await user.clear(updatedNames.at(-1)!);
    await user.type(updatedNames.at(-1)!, "Ateliê");
    await user.click(screen.getByRole("button", { name: "Salvar roteiro" }));

    await waitFor(() => expect(updateInspectionRoute).toHaveBeenCalledWith(10, expect.objectContaining({
      version: 2,
      tipoImovel: "APARTAMENTO",
      ambientes: [
        expect.objectContaining({ id: 12, nome: "Suíte" }),
        expect.objectContaining({ id: 11, nome: "Sala" }),
        expect.objectContaining({ nome: "Ateliê", tipo: "OUTRO" }),
      ],
    })));
    expect(await screen.findByRole("button", { name: /Ateliê/ })).toBeDefined();
  });

  it("não oferece remover ambiente que já possui evidência", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(withOverview);
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("1 de 3 ambientes com visão geral");

    await user.click(screen.getByRole("button", { name: "Editar roteiro" }));

    expect((screen.getByRole("button", { name: "Remover Sala" }) as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByText(/possui fotos e não pode ser removido/i)).toBeDefined();
  });

  it("submete uma única vez quando todas as visões gerais existem", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(complete);
    vi.mocked(submitInspection).mockResolvedValue({ ...complete, status: "AGUARDANDO_IA" });
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);
    await screen.findByText("3 de 3 ambientes com visão geral");

    await user.dblClick(screen.getByRole("button", { name: "Enviar para análise da IA" }));

    await waitFor(() => expect(submitInspection).toHaveBeenCalledTimes(1));
    expect(await screen.findByText("Análise da IA em andamento")).toBeDefined();
  });

  it("mantém o último estado e libera retry quando a IA falha", async () => {
    vi.mocked(getMyInspection).mockResolvedValue({ ...complete, status: "FALHA_IA" });
    vi.mocked(submitInspection).mockResolvedValue({ ...complete, status: "AGUARDANDO_IA" });
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);

    await user.click(await screen.findByRole("button", { name: "Reenviar para análise" }));

    expect(submitInspection).toHaveBeenCalledWith(10);
    expect(await screen.findByText("Análise da IA em andamento")).toBeDefined();
  });

  it("abre a análise detalhada a partir do relatório e permite voltar ao documento", async () => {
    vi.mocked(getMyInspection).mockResolvedValue(reportReady);
    const user = userEvent.setup();
    render(<InspectionWorkflow inspectionId={10} />);

    await user.click(await screen.findByRole("button", { name: "Revisar análise ou registrar manifestação" }));
    expect(await screen.findByRole("heading", { name: "Nenhum indício visual foi identificado" })).toBeDefined();

    await user.click(screen.getByRole("button", { name: "Voltar ao relatório" }));
    expect(await screen.findByRole("heading", { name: "Relatório de vistoria por IA" })).toBeDefined();
  });
});

function imageFile(name: string): File {
  return new File([new Uint8Array([0xff, 0xd8, 0xff])], name, { type: "image/jpeg" });
}

function oversizedFile(): File {
  const file = new File(["x"], "grande.webp", { type: "image/webp" });
  Object.defineProperty(file, "size", { value: MAX_EVIDENCE_BYTES + 1 });
  return file;
}
