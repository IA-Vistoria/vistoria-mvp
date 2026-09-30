import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { BrandMark } from "./BrandMark";

describe("BrandMark", () => {
  it("expõe a marca aprovada com nome acessível", () => {
    render(<BrandMark />);

    const logo = screen.getByRole("img", { name: "Vistor.IA — vistoria inteligente" });
    expect(logo.getAttribute("src")).toContain("vistoria-logo");
  });
});
