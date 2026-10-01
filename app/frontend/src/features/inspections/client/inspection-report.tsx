"use client";

import {
  AlertTriangle,
  CalendarDays,
  CheckCircle2,
  CircleHelp,
  FileText,
  ImageIcon,
  MapPin,
  MessageSquareMore,
  Printer,
  Share2,
  ShieldAlert,
  ShieldCheck,
} from "lucide-react";
import { useState } from "react";

import { BrandMark } from "@/components/brand/BrandMark";
import { EvidenceImage } from "../shared/evidence-image";
import type {
  AiEnvironmentResult,
  AiFinding,
  AiImageAnalysis,
  AiResult,
  Evidence,
  FindingReview,
  Inspection,
  ReviewDecision,
} from "../types";
import { aiResultLabel, findEnvironmentResult, issueTypeLabel } from "./ai-report";
import { categoryLabel, evidenceContextLabel, groupEvidenceByEnvironment } from "./evidence-context";

interface ReportEntry {
  analysis: AiImageAnalysis;
  finding: AiFinding;
  review: FindingReview | null;
}

type ShareFeedback = { tone: "success" | "error"; message: string } | null;

interface InspectionReportProps {
  inspection: Inspection;
  onOpenAnalysis?: () => void;
}

const severityLabels: Record<string, string> = {
  BAIXA: "Gravidade baixa",
  MEDIA: "Gravidade média",
  ALTA: "Gravidade alta",
  CRITICA: "Gravidade crítica",
};

const confidenceLabels: Record<string, string> = {
  BAIXA: "Confiança baixa",
  MEDIA: "Confiança média",
  ALTA: "Confiança alta",
};

const reviewLabels: Partial<Record<ReviewDecision, string>> = {
  CONCORDO: "Concordância do responsável",
  CONTESTO: "Contestação do responsável",
  CONTEXTO_ADICIONAL: "Contexto adicional do responsável",
  CONFIRMADO: "Confirmação histórica do responsável",
  CORRIGIDO: "Correção histórica do responsável",
  REJEITADO: "Rejeição histórica do responsável",
};

function reportEntries(inspection: Inspection): ReportEntry[] {
  return (inspection.analiseIa?.imagens ?? []).flatMap((analysis) =>
    analysis.achados.map((finding) => ({
      analysis,
      finding,
      review: inspection.manifestacoes.find(
        (item) => item.imagemId === analysis.imagemId && item.indiceAchado === finding.indice,
      ) ?? null,
    })),
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

function normalizedValue(value: string | null | undefined) {
  return value?.trim().toLocaleUpperCase("pt-BR") ?? "";
}

function providerLabel(provider: string) {
  return provider.toLocaleLowerCase("pt-BR") === "oci" ? "OCI Generative AI" : provider;
}

function resultCountLabel(result: AiResult, count: number) {
  const labels: Record<AiResult, [string, string]> = {
    APROVADO: ["aprovado", "aprovados"],
    APROVADO_COM_RESSALVAS: ["aprovado com ressalvas", "aprovados com ressalvas"],
    NAO_APROVADO: ["não aprovado", "não aprovados"],
    INCONCLUSIVO: ["inconclusivo", "inconclusivos"],
  };
  return `${count} ${count === 1 ? labels[result][0] : labels[result][1]}`;
}

function severityCountLabel(severity: string, count: number) {
  return `${count} ${count === 1 ? "achado" : "achados"} de gravidade ${severity.toLocaleLowerCase("pt-BR")}`;
}

export function InspectionReport({ inspection, onOpenAnalysis }: InspectionReportProps) {
  const [shareFeedback, setShareFeedback] = useState<ShareFeedback>(null);
  const entries = reportEntries(inspection);
  const completedAt = formatDate(inspection.dataConclusao);
  const evidenceGroups = groupEvidenceByEnvironment(inspection);
  const evidenceOrder = new Map(inspection.imagens.map((evidence, index) => [evidence.id, index + 1]));
  const analysis = inspection.analiseIa;
  const generalResult = analysis?.resultadoGeral ?? null;
  const resultCounts = (analysis?.ambientes ?? []).reduce<Partial<Record<AiResult, number>>>((counts, environment) => {
    counts[environment.resultado] = (counts[environment.resultado] ?? 0) + 1;
    return counts;
  }, {});
  const severityCounts = entries.reduce<Record<string, number>>((counts, entry) => {
    const severity = normalizedValue(entry.finding.gravidadeNormalizada ?? entry.finding.gravidade);
    if (severity) counts[severity] = (counts[severity] ?? 0) + 1;
    return counts;
  }, {});

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
          <span className="report-ready"><CheckCircle2 aria-hidden="true" size={16} />Documento disponível</span>
          <small>Resultado automatizado preservado com suas evidências e manifestações</small>
        </div>
        <div className="report-actions__buttons">
          {onOpenAnalysis ? <button className="button button--secondary" type="button" onClick={onOpenAnalysis}><MessageSquareMore aria-hidden="true" size={17} />Revisar análise ou registrar manifestação</button> : null}
          <button className="button button--secondary" type="button" onClick={() => window.print()}><Printer aria-hidden="true" size={17} />Imprimir ou salvar em PDF</button>
          <button className="button button--primary" type="button" onClick={() => void shareReport()}><Share2 aria-hidden="true" size={17} />Compartilhar relatório</button>
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
            <h1 id="report-title">Relatório de vistoria por IA</h1>
            <p>Análise visual automatizada das evidências fotográficas, organizada por ambiente e preservada com rastreabilidade.</p>
          </div>
          <dl className="report-metadata">
            <div><dt><FileText aria-hidden="true" size={16} />Identificação</dt><dd className="mono">VISTORIA {String(inspection.id).padStart(4, "0")}</dd></div>
            <div><dt><MapPin aria-hidden="true" size={16} />Imóvel</dt><dd>{inspection.endereco}</dd></div>
            <div><dt><CalendarDays aria-hidden="true" size={16} />Concluído em</dt><dd>{completedAt}</dd></div>
          </dl>
        </header>

        <section className={`report-ai-summary report-ai-summary--${generalResult?.toLocaleLowerCase("pt-BR") ?? "legacy"}`} aria-labelledby="ai-summary-title">
          <div className="report-ai-summary__result">
            <span>Resultado do imóvel</span>
            <h2 id="ai-summary-title">{aiResultLabel(generalResult)}</h2>
            <p>{analysis?.motivoResultadoGeral ?? "Esta análise anterior não possui um motivo geral estruturado."}</p>
          </div>
          <div className="report-ai-summary__counts" aria-label="Resumo dos resultados e gravidades">
            {(Object.entries(resultCounts) as Array<[AiResult, number]>).map(([result, count]) => (
              <span key={result}>{resultCountLabel(result, count)}</span>
            ))}
            {Object.entries(severityCounts).map(([severity, count]) => (
              <span key={severity}>{severityCountLabel(severity, count)}</span>
            ))}
            {Object.keys(resultCounts).length === 0 ? <span>Resultado estruturado indisponível para análise anterior</span> : null}
          </div>
        </section>

        <section className="report-summary" aria-labelledby="summary-title">
          <div>
            <h2 id="summary-title">Composição do documento</h2>
            <p>O relatório conserva as fotos analisadas, os achados originais da IA e manifestações opcionais em camadas separadas.</p>
          </div>
          <dl>
            <div><dt>{inspection.imagens.length}</dt><dd>{inspection.imagens.length === 1 ? "foto analisada" : "fotos analisadas"}</dd></div>
            <div><dt>{analysis?.ambientes?.length ?? inspection.ambientes.length}</dt><dd>{(analysis?.ambientes?.length ?? inspection.ambientes.length) === 1 ? "ambiente" : "ambientes"}</dd></div>
            <div><dt>{entries.length}</dt><dd>{entries.length === 1 ? "achado da IA" : "achados da IA"}</dd></div>
          </dl>
        </section>

        <section className="report-findings" aria-labelledby="findings-title">
          <header>
            <h2 id="findings-title">Resultado por ambiente</h2>
            <p>Cada seção reúne somente o resultado, as fotos e os achados vinculados àquele ambiente.</p>
          </header>

          {evidenceGroups.map((group) => {
            const firstEvidence = group.evidence[0] ?? null;
            const environmentResult = findEnvironmentResult(
              analysis,
              firstEvidence?.ambienteId ?? null,
              group.name,
            );
            return (
              <ReportEnvironment
                key={group.key}
                group={group}
                environmentResult={environmentResult}
                evidenceOrder={evidenceOrder}
                entries={entries}
                inspection={inspection}
              />
            );
          })}

          {inspection.imagens.length === 0 ? (
            <div className="report-evidence__empty"><ImageIcon aria-hidden="true" size={21} /><p><strong>Nenhuma evidência disponível</strong>Este registro não contém fotos vinculadas.</p></div>
          ) : null}
        </section>

        {analysis?.execucao ? (
          <section className="report-traceability" aria-labelledby="traceability-title">
            <div><ShieldCheck aria-hidden="true" size={22} /><h2 id="traceability-title">Rastreabilidade da análise</h2></div>
            <dl>
              <div><dt>Provedor</dt><dd>{providerLabel(analysis.execucao.provider)}</dd></div>
              <div><dt>Modelo</dt><dd>{analysis.execucao.modelo}</dd></div>
              <div><dt>Versão do prompt</dt><dd>{analysis.execucao.versaoPrompt}</dd></div>
              <div><dt>Identificador da análise</dt><dd>{analysis.execucao.identificadorAnalise}</dd></div>
              <div><dt>Processamento concluído</dt><dd>{formatDate(analysis.execucao.concluidaEm)}</dd></div>
            </dl>
          </section>
        ) : (
          <section className="report-traceability report-traceability--legacy" aria-label="Rastreabilidade da análise anterior">
            <CircleHelp aria-hidden="true" size={22} />
            <p><strong>Metadados de execução indisponíveis</strong>Este documento foi criado por uma versão anterior do analisador.</p>
          </section>
        )}

        <footer className="report-disclaimer">
          <ShieldAlert aria-hidden="true" size={25} />
          <div>
            <strong>Escopo e limite de uso</strong>
            <p>Este relatório registra uma análise visual automatizada das imagens enviadas. Não constitui laudo técnico, diagnóstico estrutural, conformidade normativa ou certificação profissional.</p>
          </div>
        </footer>
      </article>
    </section>
  );
}

function ReportEnvironment({ group, environmentResult, evidenceOrder, entries, inspection }: {
  group: ReturnType<typeof groupEvidenceByEnvironment>[number];
  environmentResult: AiEnvironmentResult | null;
  evidenceOrder: Map<number, number>;
  entries: ReportEntry[];
  inspection: Inspection;
}) {
  return (
    <section className={`report-environment ${group.legacy ? "report-environment--legacy" : ""}`} data-testid="report-environment">
      <header className="report-environment__heading">
        <div><span className="mono">{group.legacy ? "ARQUIVO ANTERIOR" : "AMBIENTE ANALISADO"}</span><h3>{group.name}</h3></div>
        <div className={`report-environment__result report-environment__result--${environmentResult?.resultado.toLocaleLowerCase("pt-BR") ?? "legacy"}`}>
          <small>Resultado do ambiente</small>
          <strong>{aiResultLabel(environmentResult?.resultado)}</strong>
          <span>{environmentResult?.motivoResultado ?? "Resultado estruturado indisponível para esta análise anterior."}</span>
        </div>
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
      {group.evidence.length === 0 ? <div className="report-evidence__empty"><ImageIcon aria-hidden="true" size={21} /><p><strong>Nenhuma foto neste ambiente</strong>O roteiro não possui evidência vinculada a este ambiente.</p></div> : null}
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
        {analysis?.resumoGeral ? <p className="report-evidence__summary">{analysis.resumoGeral}</p> : null}
        {entries.length > 0 ? entries.map((entry) => (
          <ReportFinding key={`${entry.analysis.imagemId}-${entry.finding.indice}`} entry={entry} />
        )) : (
          <div className="report-evidence__empty"><CheckCircle2 aria-hidden="true" size={21} /><p><strong>{analysis?.qualidade.utilizavel === false ? "Evidência insuficiente para uma conclusão segura." : "Nenhum indício visual foi identificado nesta evidência."}</strong>{analysis?.qualidade.utilizavel === false ? "Consulte a orientação de nova captura abaixo." : "A foto permanece preservada como parte do registro analisado."}</p></div>
        )}
        {analysis?.limitacoes.length ? <aside className="report-limitations"><AlertTriangle aria-hidden="true" size={18} /><p><strong>Limitações desta leitura</strong>{analysis.limitacoes.join(" ")}</p></aside> : null}
        {analysis?.orientacaoNovaCaptura ? <aside className="report-recapture"><ImageIcon aria-hidden="true" size={18} /><p><strong>Nova captura necessária</strong>{analysis.orientacaoNovaCaptura}</p></aside> : null}
      </div>
    </article>
  );
}

function ReportFinding({ entry }: { entry: ReportEntry }) {
  const severity = normalizedValue(entry.finding.gravidadeNormalizada ?? entry.finding.gravidade);
  const confidence = normalizedValue(entry.finding.confiancaNormalizada ?? entry.finding.confianca);
  return (
    <section className="report-finding">
      <div className="report-finding__heading"><span className="report-source-label">Achado da IA</span><span className="mono">{referenceFor(entry)}</span></div>
      <div className="report-finding__ai">
        <h3>{issueTypeLabel(entry.finding.tipo)}</h3>
        {entry.finding.area ? <p className="report-finding__area">{entry.finding.area}</p> : null}
        <dl className="report-finding__details">
          {entry.finding.criterio ? <div><dt>Critério avaliado</dt><dd>{entry.finding.criterio}</dd></div> : null}
          {entry.finding.descricao ? <div><dt>O que foi observado</dt><dd>{entry.finding.descricao}</dd></div> : null}
          {entry.finding.evidencia ? <div><dt>Indício na imagem</dt><dd>{entry.finding.evidencia}</dd></div> : null}
          {entry.finding.impacto ? <div><dt>Impacto possível</dt><dd>{entry.finding.impacto}</dd></div> : null}
        </dl>
        <div className="report-finding__classification">
          {severity ? <span><AlertTriangle aria-hidden="true" size={14} />{severityLabels[severity] ?? `Gravidade ${severity.toLocaleLowerCase("pt-BR")}`}</span> : null}
          {confidence ? <span><ShieldCheck aria-hidden="true" size={14} />{confidenceLabels[confidence] ?? `Confiança ${confidence.toLocaleLowerCase("pt-BR")}`}</span> : null}
        </div>
      </div>
      {entry.finding.recomendacao ? <p className="report-finding__recommendation"><strong>Próxima ação recomendada</strong>{entry.finding.recomendacao}</p> : null}
      {entry.review ? (
        <blockquote className="report-finding__manifestation">
          <strong>{reviewLabels[entry.review.decisao] ?? "Manifestação do responsável"}</strong>
          {entry.review.tipoCorrigido ? <b>Registro histórico: {entry.review.tipoCorrigido}</b> : null}
          {entry.review.contexto ? <span>{entry.review.contexto}</span> : <span>Sem contexto adicional.</span>}
          <small>Esta manifestação não altera o achado nem o resultado original da IA.</small>
        </blockquote>
      ) : null}
    </section>
  );
}
