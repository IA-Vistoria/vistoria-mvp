import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  createInspection,
  listMyInspections,
  updateInspectionRoute,
  uploadEvidence,
} from "./api";

function successfulResponse() {
  return new Response(JSON.stringify({ id: 10 }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

describe("contratos do roteiro adaptativo", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    fetchMock.mockResolvedValue(successfulResponse());
    vi.stubGlobal("fetch", fetchMock);
  });

  it("cria a vistoria com tipo e ambientes ordenados", async () => {
    await createInspection({
      endereco: " Rua das Flores, 10 ",
      tipoImovel: "APARTAMENTO",
      ambientes: [
        { tipo: "SALA", nome: "Sala" },
        { tipo: "QUARTO", nome: "Quarto principal" },
      ],
    });

    const request = fetchMock.mock.calls[0][1] as RequestInit;
    expect(JSON.parse(request.body as string)).toEqual({
      endereco: "Rua das Flores, 10",
      tipoImovel: "APARTAMENTO",
      ambientes: [
        { tipo: "SALA", nome: "Sala" },
        { tipo: "QUARTO", nome: "Quarto principal" },
      ],
    });
  });

  it("atualiza o roteiro com a versão recebida do servidor", async () => {
    await updateInspectionRoute(10, {
      version: 3,
      tipoImovel: "CASA",
      ambientes: [
        { id: 21, tipo: "SALA", nome: "Sala integrada" },
        { id: null, tipo: "AREA_EXTERNA", nome: "Quintal" },
      ],
    });

    const [url, request] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/vistorias/10/roteiro");
    expect(request.method).toBe("PUT");
    expect(JSON.parse(request.body as string)).toMatchObject({ version: 3 });
  });

  it("envia ambiente e categoria junto do arquivo", async () => {
    const file = new File(["imagem"], "sala.jpg", { type: "image/jpeg" });

    await uploadEvidence(10, {
      ambienteId: 21,
      categoria: "DETALHE",
      file,
    });

    const request = fetchMock.mock.calls[0][1] as RequestInit;
    const body = request.body as FormData;
    expect(body.get("ambienteId")).toBe("21");
    expect(body.get("categoria")).toBe("DETALHE");
    expect(body.get("file")).toBe(file);
  });

  it("filtra relatórios no servidor sem carregar outras páginas no cliente", async () => {
    await listMyInspections(2, 8, "RELATORIO_DISPONIVEL");

    const [url] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/vistorias/minhas?page=2&size=8&status=RELATORIO_DISPONIVEL");
  });
});
