"use client";

import { AlertTriangle, CheckCircle2, ImageIcon } from "lucide-react";

import { EvidenceImage } from "../shared/evidence-image";
import type { AiImageAnalysis, Evidence, Inspection } from "../types";

function matchResult(evidence: Evidence, results: AiImageAnalysis[]): AiImageAnalysis | null {
  return results.find((item) => item.imagemId === evidence.id) ?? null;
}

export function InspectionResults({ inspection }: { inspection: Inspection }) {
  const report = inspection.analiseIa;
  const results = report?.imagens ?? [];

  return (
    <main className="results-page">
      <header className="results-hero">
        <p className="eyebrow">Resultado da análise</p>
        <h1>Vistoria concluída</h1>
        <p>
          A IA analisou {inspection.imagens.length}{" "}
          {inspection.imagens.length === 1 ? "imagem" : "imagens"} de paredes em{" "}
          <strong>{inspection.endereco}</strong>.
        </p>
      </header>

      {!report ? (
        <section className="results-empty" role="status">
          <AlertTriangle size={22} />
          <p>O relatório estruturado não está disponível para esta vistoria.</p>
        </section>
      ) : (
        <div className="results-grid">
          {inspection.imagens.map((evidence, index) => {
            const result = matchResult(evidence, results);
            return (
              <article className="result-card" key={evidence.id}>
                <div className="result-card__media">
                  <EvidenceImage evidence={evidence} alt={`Parede — foto ${index + 1}`} />
                </div>
                <div className="result-card__body">
                  <p className="eyebrow">Foto {index + 1}</p>
                  {!result ? (
                    <p className="result-card__empty">Sem análise vinculada a esta imagem.</p>
                  ) : !result.qualidade.utilizavel ? (
                    <>
                      <h2>Imagem não utilizável</h2>
                      <ul className="result-findings">
                        {result.qualidade.problemas.map((issue) => (
                          <li key={issue}>{issue}</li>
                        ))}
                      </ul>
                    </>
                  ) : (
                    <>
                      <h2>{result.resumoGeral || "Análise visual"}</h2>
                      {result.achados.length === 0 ? (
                        <p className="result-card__ok">
                          <CheckCircle2 size={18} /> Nenhum problema visual evidente.
                        </p>
                      ) : (
                        <ul className="result-findings">
                          {result.achados.map((finding) => (
                            <li key={`${finding.indice}-${finding.tipo ?? "achado"}`}>
                              <strong>{finding.tipo || "Indício visual"}</strong>
                              {finding.confianca ? <span>Confiança: {finding.confianca}</span> : null}
                              {finding.descricao ? <p>{finding.descricao}</p> : null}
                              {finding.recomendacao ? <small>{finding.recomendacao}</small> : null}
                            </li>
                          ))}
                        </ul>
                      )}
                      {result.limitacoes.length ? (
                        <p className="result-limitations">{result.limitacoes.join(" ")}</p>
                      ) : null}
                    </>
                  )}
                </div>
              </article>
            );
          })}
          {inspection.imagens.length === 0 ? (
            <section className="results-empty">
              <ImageIcon size={22} />
              <p>Nenhuma imagem nesta vistoria.</p>
            </section>
          ) : null}
        </div>
      )}
    </main>
  );
}
