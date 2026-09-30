import { describe, expect, it } from "vitest";

import {
  createEnvironmentDrafts,
  createSuggestedEnvironments,
  normalizeEnvironmentName,
  validateEnvironmentDrafts,
} from "./route-builder";

describe("roteiro sugerido", () => {
  it("sugere composições diferentes para casa, apartamento e comercial", () => {
    const house = createSuggestedEnvironments("CASA");
    const apartment = createSuggestedEnvironments("APARTAMENTO");
    const commercial = createSuggestedEnvironments("COMERCIAL");

    expect(house.map((room) => room.nome)).toContain("Área externa");
    expect(apartment.map((room) => room.nome)).not.toContain("Área externa");
    expect(commercial.map((room) => room.nome)).toContain("Área principal");
    expect(new Set([house.length, apartment.length, commercial.length]).size).toBeGreaterThan(1);
  });

  it("não inclui garagem como obrigação em nenhum roteiro sugerido", () => {
    for (const type of ["CASA", "APARTAMENTO", "COMERCIAL", "OUTRO"] as const) {
      expect(createSuggestedEnvironments(type).map((room) => room.tipo)).not.toContain("GARAGEM");
    }
  });

  it("normaliza espaços e maiúsculas para detectar duplicidade", () => {
    expect(normalizeEnvironmentName("  Sala   Íntima ")).toBe("sala íntima");
  });

  it("rejeita nomes duplicados sem diferenciar maiúsculas", () => {
    expect(validateEnvironmentDrafts([
      { key: "1", tipo: "SALA", nome: "Sala" },
      { key: "2", tipo: "OUTRO", nome: " sala " },
    ])).toContain("nomes diferentes");
  });

  it("exige de 1 a 30 ambientes com nomes entre 2 e 60 caracteres", () => {
    expect(validateEnvironmentDrafts([])).toContain("ao menos um ambiente");
    expect(validateEnvironmentDrafts([{ key: "1", tipo: "OUTRO", nome: "A" }])).toContain("2 e 60");
    expect(validateEnvironmentDrafts(Array.from({ length: 31 }, (_, index) => ({
      key: String(index),
      tipo: "OUTRO" as const,
      nome: `Ambiente ${index}`,
    })))).toContain("no máximo 30");
  });

  it("aceita um roteiro válido com nomes personalizados", () => {
    expect(validateEnvironmentDrafts([
      { key: "1", tipo: "QUARTO", nome: "Suíte principal" },
      { key: "2", tipo: "OUTRO", nome: "Ateliê" },
    ])).toBeNull();
  });

  it("ordena ambientes persistidos e preserva seus identificadores", () => {
    expect(createEnvironmentDrafts([
      { id: 12, tipo: "QUARTO", nome: "Quarto", ordem: 1 },
      { id: 11, tipo: "SALA", nome: "Sala", ordem: 0 },
    ])).toEqual([
      expect.objectContaining({ id: 11, nome: "Sala" }),
      expect.objectContaining({ id: 12, nome: "Quarto" }),
    ]);
  });

  it("gera novas cópias das sugestões a cada seleção", () => {
    const first = createSuggestedEnvironments("CASA");
    first[0].nome = "Alterado";

    expect(createSuggestedEnvironments("CASA")[0].nome).toBe("Entrada e fachada");
  });
});
