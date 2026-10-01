"use client";

import {
  AlertTriangle,
  CalendarDays,
  CheckCircle2,
  FileText,
  ImageIcon,
  MapPin,
  Printer,
  Share2,
  ShieldAlert,
} from "lucide-react";
import { useState } from "react";

import { BrandMark } from "@/components/brand/BrandMark";
import { EvidenceImage } from "../shared/evidence-image";
import type { AiFinding, AiImageAnalysis, Evidence, FindingReview, Inspection } from "../types";
import { categoryLabel, evidenceContextLabel, groupEvidenceByEnvironment } from "./evidence-context";

interface ReportEntry {
  analysis: AiImageAnalysis;
  finding: AiFinding;
  review: FindingReview;
}

type ShareFeedback = { tone: "success" | "error"; message: string } | null;

function acceptedEntries(inspection: Inspection): ReportEntry[] {
  return (inspection.analiseIa?.imagens ?? []).flatMap((analysis) =>
    analysis.achados.flatMap((finding) => {
      const review = inspection.manifestacoes.find(
        (item) => item.imagemId === analysis.imagemId && item.indiceAchado === finding.indice,
      );
      return review && review.decisao !== "REJEITADO" ? [{ analysis, finding, review }] : [];
    }),
  );
}

function entriesForEvidence(entries: ReportEntry[], evidence: Evidence) {
  return entries.filter((entry) => entry.analysis.imagemId === evidence.id);
}

function formatDate(value: string | null) {
  if (!value) return "Data não registrada";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "long",
    timeStyle: "short",
    timeZone: "America/Sao_Paulo",
  }).format(new Date(value));
}

function referenceFor(entry: ReportEntry) {
  return `IMG ${String(entry.analysis.imagemId).padStart(4, "0")} · ACHADO ${String(entry.finding.indice).padStart(2, "0")}`;
}

export function InspectionReport({ inspection }: { inspection: Inspection }) {
  const [shareFeedback, setShareFeedback] = useState<ShareFeedback>(null);
  const entries = acceptedEntries(inspection);
  const rejectedCount = inspection.manifestacoes.filter((review) => review.decisao === "REJEITADO").length;
  const completedAt = formatDate(inspection.dataConclusao);
  const evidenceGroups = groupEvidenceByEnvironment(inspection);
  const evidenceOrder = new Map(inspection.imagens.map((evidence, index) => [evidence.id, index + 1]));

  async function shareReport() {
    setShareFeedback(null);
    try {
      if (typeof navigator.share === "function") {
        await navigator.share({
          title: "Relatório de vistoria por IA",
          text: `Vistoria ${String(inspection.id).padStart(4, "0")} — ${inspection.endereco}`,
          url: window.location.href,
        });
        setShareFeedback({ tone: "success", message: "Relatório compartilhado pelo dispositivo." });
        return;
      }
      if (typeof navigator.clipboard?.writeText === "function") {
        await navigator.clipboard.writeText(window.location.href);
        setShareFeedback({ tone: "success", message: "Link copiado para a área de transferência." });
        return;
      }
      setShareFeedback({
        tone: "error",
        message: "Este navegador não oferece compartilhamento ou cópia de link.",
      });
    } catch {
      setShareFeedback({ tone: "error", message: "Não foi possível compartilhar este relatório." });
    }
  }

  return (
    <section className="inspection-report" aria-labelledby="report-title">
      <div className="report-actions no-print" aria-label="Ações do relatório">
        <div>
          <span className="report-ready"><CheckCircle2 size={16} />Documento disponível</span>
          <small>Gerado a partir das evidências e decisões salvas</small>
        </div>
        <div className="report-actions__buttons">
          <button className="button button--secondary" type="button" onClick={() => window.print()}><Printer size={17} />Imprimir ou salvar em PDF</button>
          <button className="button button--primary" type="button" onClick={() => void shareReport()}><Share2 size={17} />Compartilhar relatório</button>
        </div>
      </div>

      {shareFeedback ? (
        <p className={`share-feedback share-feedback--${shareFeedback.tone}`} role={shareFeedback.tone === "error" ? "alert" : "status"}>
          {shareFeedback.message}
        </p>
      ) : null}

      <article className="report-sheet">
        <header className="report-cover">
          <div className="report-brand"><BrandMark compact inverse /></div>
          <div className="report-cover__title">
            <p className="eyebrow">Registro visual assistido</p>
            <h1 id="report-title">Relatório de vistoria por IA</h1>
            <p>Evidências fotográficas organizadas com análise visual e contexto confirmado pelo usuário.</p>
          </div>
          <dl className="report-metadata">
            <div><dt><FileText size={16} />Identificação</dt><dd className="mono">VISTORIA {String(inspection.id).padStart(4, "0")}</dd></div>
            <div><dt><MapPin size={16} />Imóvel</dt><dd>{inspection.endereco}</dd></div>
            <div><dt><CalendarDays size={16} />Concluído em</dt><dd>{completedAt}</dd></div>
          </dl>
        </header>

        <section className="report-summary" aria-labelledby="summary-title">
          <div>
            <p className="eyebrow">Resumo documental</p>
            <h2 id="summary-title">O que compõe este registro</h2>
          </div>
          <dl>
            <div><dt>{inspection.imagens.length}</dt><dd>{inspection.imagens.length === 1 ? "foto preservada" : "fotos preservadas"}</dd></div>
            <div><dt>{entries.length}</dt><dd>{entries.length === 1 ? "constatação incluída" : "constatações incluídas"}</dd></div>
            <div><dt>{rejectedCount}</dt><dd>{rejectedCount === 1 ? "sugestão rejeitada" : "sugestões rejeitadas"}</dd></div>
          </dl>
        </section>

        <section className="report-findings" aria-labelledby="findings-title">
          <header>
            <p className="eyebrow">Evidência e contexto</p>
            <h2 id="findings-title">Constatações registradas</h2>
            <p>Somente achados confirmados ou corrigidos aparecem como constatação. Sugestões rejeitadas permanecem fora das conclusões.</p>
          </header>

          {evidenceGroups.map((group) => (
            <section className={`report-environment ${group.legacy ? "report-environment--legacy" : ""}`} data-testid="report-environment" key={group.key}>
              <header className="report-environment__heading">
                <div><span className="mono">{group.legacy ? "ARQUIVO ANTERIOR" : "AMBIENTE"}</span><h3>{group.name}</h3></div>
                <span>{group.evidence.length} {group.evidence.length === 1 ? "foto" : "fotos"}</span>
              </header>
              {group.evidence.map((evidence) => (
                <ReportEvidenceCard
                  key={evidence.id}
                  evidence={evidence}
                  photoIndex={evidenceOrder.get(evidence.id) ?? 0}
                  entries={entriesForEvidence(entries, evidence)}
                  analysis={inspection.analiseIa?.imagens.find((item) => item.imagemId === evidence.id)}
                />
              ))}
              {group.evidence.length === 0 ? <div className="report-evidence__empty"><ImageIcon size={21} /><p><strong>Nenhuma foto neste ambiente</strong>O roteiro não possui evidência vinculada a este ambiente.</p></div> : null}
            </section>
          ))}

          {inspection.imagens.length === 0 ? (
            <div className="report-evidence__empty"><ImageIcon size={21} /><p><strong>Nenhuma evidência disponível</strong>Este registro não contém fotos vinculadas.</p></div>
          ) : null}
        </section>

        <footer className="report-disclaimer">
          <ShieldAlert size={25} />
          <div>
            <strong>Limite de uso</strong>
            <p>Este relatório organiza evidências e observações assistidas por IA. Não constitui laudo técnico, diagnóstico estrutural ou certificação profissional.</p>
          </div>
        </footer>
      </article>
    </section>
  );
}

function ReportEvidenceCard({ evidence, photoIndex, entries, analysis }: {
  evidence: Evidence;
  photoIndex: number;
  entries: ReportEntry[];
  analysis: AiImageAnalysis | undefined;
}) {
  const label = evidenceContextLabel(evidence);
  return (
    <article className="report-evidence">
      <div className="report-evidence__photo">
        <div className="report-evidence__label"><span className="mono">FOTO {String(photoIndex).padStart(2, "0")}</span><strong>{evidence.categoria ? categoryLabel(evidence.categoria) : label}</strong></div>
        <EvidenceImage evidence={evidence} alt={`Evidência ${evidence.id} — ${label}`} />
        <small className="mono">IMG {String(evidence.id).padStart(4, "0")} · {formatDate(evidence.dataUpload)}</small>
      </div>
      <div className="report-evidence__content">
        {entries.length > 0 ? entries.map((entry) => (
          <section className="report-finding" key={`${entry.analysis.imagemId}-${entry.finding.indice}`}>
            <div className="report-finding__heading"><span className={`review-chip review-chip--${entry.review.decisao.toLowerCase()}`}>{entry.review.decisao === "CORRIGIDO" ? "Corrigido pelo responsável" : "Confirmado pelo responsável"}</span><span className="mono">{referenceFor(entry)}</span></div>
            <div className="report-finding__ai">
              <p className="report-source-label">Observação da IA</p>
              <h3>{entry.finding.tipo || "Indício visual"}</h3>
              {entry.finding.area ? <p className="report-finding__area">{entry.finding.area}</p> : null}
              {entry.finding.descricao ? <p>{entry.finding.descricao}</p> : null}
            </div>
            <blockquote className="report-finding__decision"><strong>Decisão do responsável</strong>{entry.review.decisao === "CORRIGIDO" && entry.review.tipoCorrigido ? <b>{entry.review.tipoCorrigido}</b> : null}<span>{entry.review.contexto}</span></blockquote>
            {entry.finding.recomendacao ? <p className="report-finding__recommendation"><strong>Próxima observação sugerida</strong>{entry.finding.recomendacao}</p> : null}
          </section>
        )) : <div className="report-evidence__empty"><ImageIcon size={21} /><p><strong>Foto preservada sem constatação incluída</strong>Não houve achado confirmado ou corrigido nesta evidência.</p></div>}
        {analysis?.limitacoes.length ? <aside className="report-limitations"><AlertTriangle size={18} /><p><strong>Limites desta leitura</strong>{analysis.limitacoes.join(" ")}</p></aside> : null}
      </div>
    </article>
  );
}
