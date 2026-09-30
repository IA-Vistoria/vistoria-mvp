import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

describe("fundações visuais", () => {
  it("define a paleta Lente Operacional sem azul elétrico ou amarelo fluorescente", () => {
    const css = readFileSync("src/app/globals.css", "utf8");
    const tokens = css.match(/:root\s*\{[\s\S]*?\}/)?.[0] ?? "";

    expect(tokens).toContain("--canvas: #f3f1e9");
    expect(tokens).toContain("--text: #102a2e");
    expect(tokens).toContain("--primary: #0f766e");
    expect(tokens).toContain("--warning: #9a6518");
    expect(tokens).not.toMatch(/#2f5bea|#1d3fbb|#faff00/i);
  });

  it("remove vidro decorativo e torna visível o foco dos cartões interativos", () => {
    const css = readFileSync("src/app/globals.css", "utf8");

    expect(css).not.toMatch(/backdrop-filter:\s*blur/i);
    expect(css).toMatch(/\.property-type-picker label:focus-within/);
    expect(css).toMatch(/\.capture-action:focus-within/);
    expect(css).toMatch(/\.decision-options label:focus-within/);
  });
});
