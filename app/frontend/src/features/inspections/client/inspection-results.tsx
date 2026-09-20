"use client";

import { AlertTriangle, CheckCircle2, ImageIcon } from "lucide-react";

import { EvidenceImage } from "../shared/evidence-image";
import type { Evidence, Inspection } from "../types";
import { issueTypeLabel, parseAiReport, type AiImageResult } from "./ai-report";

function matchResult(evidence: Evidence, results: AiImageResult[], index: number): AiImageResult | null {
  const byPath = results.find((item) => item.storagePath === evidence.storagePath);
  if (byPath) return byPath;
  return results[index] ?? null;
}

export function InspectionResults({ inspection }: { inspection: Inspection }) {
  const report = parseAiReport(inspection.preLaudoIa);
  const results = report?.images ?? [];

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
          {inspection.preLaudoIa ? <pre className="results-fallback">{inspection.preLaudoIa}</pre> : null}
        </section>
      ) : (
        <div className="results-grid">
          {inspection.imagens.map((evidence, index) => {
            const result = matchResult(evidence, results, index);
            return (
              <article className="result-card" key={evidence.id}>
                <div className="result-card__media">
                  <EvidenceImage evidence={evidence} alt={`Parede — foto ${index + 1}`} />
                </div>
                <div className="result-card__body">
                  <p className="eyebrow">Foto {index + 1}</p>
                  {!result ? (
                    <p className="result-card__empty">Sem análise vinculada a esta imagem.</p>
                  ) : !result.imageQuality.usable ? (
                    <>
                      <h2>Imagem não utilizável</h2>
                      <ul className="result-findings">
                        {result.imageQuality.issues.map((issue) => (
                          <li key={issue}>{issue}</li>
                        ))}
                      </ul>
                    </>
                  ) : (
                    <>
                      <h2>{result.overallSummary || "Análise visual"}</h2>
                      {result.areas.length === 0 ? (
                        <p className="result-card__ok">
                          <CheckCircle2 size={18} /> Nenhum problema visual evidente.
                        </p>
                      ) : (
                        <ul className="result-findings">
                          {result.areas.map((area, areaIndex) => (
                            <li key={`${area.issueType}-${areaIndex}`}>
                              <strong>{issueTypeLabel(area.issueType)}</strong>
                              {area.confidence ? <span>Confiança: {area.confidence}</span> : null}
                              {area.description ? <p>{area.description}</p> : null}
                              {area.recommendation ? <small>{area.recommendation}</small> : null}
                            </li>
                          ))}
                        </ul>
                      )}
                      {result.limitations.length ? (
                        <p className="result-limitations">{result.limitations.join(" ")}</p>
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
