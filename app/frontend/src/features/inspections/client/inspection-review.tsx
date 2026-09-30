"use client";

import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Check,
  CheckCircle2,
  FileText,
  PencilLine,
  ShieldCheck,
  X,
} from "lucide-react";
import { FormEvent, useEffect, useMemo, useRef, useState } from "react";

import { ApiError } from "@/lib/api";
import { completeReport, reviewFinding } from "../api";
import { EvidenceImage } from "../shared/evidence-image";
import type {
  AiFinding,
  AiImageAnalysis,
  Evidence,
  Inspection,
  ReviewDecision,
} from "../types";
import { evidenceContextLabel } from "./evidence-context";

interface InspectionReviewProps {
  inspection: Inspection;
  onChange: (inspection: Inspection) => void;
  onRefresh: () => Promise<Inspection>;
}

interface FindingEntry {
  analysis: AiImageAnalysis;
  evidence: Evidence | null;
  finding: AiFinding;
}

const decisions: Array<{
  value: ReviewDecision;
  label: string;
  help: string;
  icon: typeof Check;
}> = [
  { value: "CONFIRMADO", label: "Confirmar achado", help: "A descrição combina com o que você observou.", icon: Check },
  { value: "CORRIGIDO", label: "Corrigir informação", help: "Existe um indício, mas ele precisa de outro nome.", icon: PencilLine },
  { value: "REJEITADO", label: "Rejeitar achado", help: "A interpretação não corresponde à evidência ou ao contexto.", icon: X },
];

const REVIEW_ERROR_ID = "inspection-review-error";

interface ReviewError {
  message: string;
  target: "decision" | "context" | "correctedType" | "form";
}

function flattenFindings(inspection: Inspection): FindingEntry[] {
  return (inspection.analiseIa?.imagens ?? []).flatMap((analysis) => {
    const evidence = inspection.imagens.find((item) => item.id === analysis.imagemId) ?? null;
    return analysis.achados.map((finding) => ({ analysis, evidence, finding }));
  });
}

function hasReview(inspection: Inspection, entry: FindingEntry) {
  return inspection.revisoes.some(
    (review) => review.imagemId === entry.analysis.imagemId && review.indiceAchado === entry.finding.indice,
  );
}

function firstPendingIndex(inspection: Inspection, entries: FindingEntry[]) {
  const pendingIndex = entries.findIndex((entry) => !hasReview(inspection, entry));
  return pendingIndex >= 0 ? pendingIndex : 0;
}

function savedReview(inspection: Inspection, entry: FindingEntry | null) {
  if (!entry) return null;
  return inspection.revisoes.find(
    (review) => review.imagemId === entry.analysis.imagemId && review.indiceAchado === entry.finding.indice,
  ) ?? null;
}

export function InspectionReview({ inspection, onChange, onRefresh }: InspectionReviewProps) {
  const entries = useMemo(() => flattenFindings(inspection), [inspection]);
  const startingIndex = firstPendingIndex(inspection, entries);
  const startingReview = savedReview(inspection, entries[startingIndex] ?? null);
  const [currentIndex, setCurrentIndex] = useState(startingIndex);
  const [decision, setDecision] = useState<ReviewDecision | "">(startingReview?.decisao ?? "");
  const [context, setContext] = useState(startingReview?.contexto ?? "");
  const [correctedType, setCorrectedType] = useState(startingReview?.tipoCorrigido ?? "");
  const [error, setError] = useState<ReviewError | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [completing, setCompleting] = useState(false);
  const mutationLock = useRef(false);
  const lastSavedMutationKey = useRef<string | null>(null);
  const completionLock = useRef(false);
  const firstDecisionRef = useRef<HTMLInputElement>(null);
  const contextRef = useRef<HTMLTextAreaElement>(null);
  const correctedTypeRef = useRef<HTMLInputElement>(null);
  const errorRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (error?.target === "form") errorRef.current?.focus();
  }, [error]);

  const activeIndex = Math.min(currentIndex, Math.max(0, entries.length - 1));
  const current = entries[activeIndex] ?? null;
  const reviewedCount = entries.filter((entry) => hasReview(inspection, entry)).length;
  const allReviewed = reviewedCount === entries.length;

  function openEntry(index: number, source: Inspection = inspection) {
    const sourceEntries = flattenFindings(source);
    const boundedIndex = Math.min(Math.max(index, 0), Math.max(0, sourceEntries.length - 1));
    const review = savedReview(source, sourceEntries[boundedIndex] ?? null);
    setCurrentIndex(boundedIndex);
    setDecision(review?.decisao ?? "");
    setContext(review?.contexto ?? "");
    setCorrectedType(review?.tipoCorrigido ?? "");
    setError(null);
    setNotice(null);
  }

  function chooseDecision(value: ReviewDecision) {
    setDecision(value);
    if (value !== "CORRIGIDO") setCorrectedType("");
    setError(null);
    lastSavedMutationKey.current = null;
  }

  async function saveReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!current || mutationLock.current) return;
    const mutationKey = `${current.analysis.imagemId}:${current.finding.indice}`;
    if (lastSavedMutationKey.current === mutationKey) return;
    if (!decision) {
      setError({ message: "Escolha como este achado deve ser tratado.", target: "decision" });
      firstDecisionRef.current?.focus();
      return;
    }
    if (!context.trim()) {
      setError({ message: "Descreva o contexto observado no imóvel.", target: "context" });
      contextRef.current?.focus();
      return;
    }
    if (decision === "CORRIGIDO" && !correctedType.trim()) {
      setError({ message: "Informe o tipo corrigido para o relatório.", target: "correctedType" });
      correctedTypeRef.current?.focus();
      return;
    }

    mutationLock.current = true;
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await reviewFinding(inspection.id, {
        imagemId: current.analysis.imagemId,
        indiceAchado: current.finding.indice,
        decisao: decision,
        contexto: context.trim(),
        tipoCorrigido: decision === "CORRIGIDO" ? correctedType.trim() : null,
      });
      onChange(updated);
      lastSavedMutationKey.current = mutationKey;
      const updatedEntries = flattenFindings(updated);
      openEntry(firstPendingIndex(updated, updatedEntries), updated);
      setNotice("Decisão salva. Avançamos para o próximo achado pendente.");
    } catch (cause) {
      if (cause instanceof ApiError && cause.problem.status === 409) {
        try {
          const latest = await onRefresh();
          onChange(latest);
          openEntry(firstPendingIndex(latest, flattenFindings(latest)), latest);
          setError({ message: "A vistoria mudou em outra sessão. Exibimos agora o estado mais recente.", target: "form" });
        } catch {
          setError({ message: cause.problem.detail, target: "form" });
        }
      } else {
        setError({
          message: cause instanceof ApiError ? cause.problem.detail : "Não foi possível salvar esta decisão.",
          target: "form",
        });
      }
    } finally {
      mutationLock.current = false;
      setSaving(false);
    }
  }

  async function generateReport() {
    if (!allReviewed || completionLock.current) return;
    completionLock.current = true;
    setCompleting(true);
    setError(null);
    try {
      const updated = await completeReport(inspection.id);
      onChange(updated);
    } catch (cause) {
      setError({
        message: cause instanceof ApiError ? cause.problem.detail : "Não foi possível gerar o relatório.",
        target: "form",
      });
      completionLock.current = false;
    } finally {
      setCompleting(false);
    }
  }

  if (entries.length === 0) {
    return (
      <section className="review-empty" aria-live="polite">
        <CheckCircle2 aria-hidden="true" size={32} />
        <p className="eyebrow">Análise concluída</p>
        <h1>Nenhum indício visual registrado</h1>
        <p>A análise não criou achados para revisar. As fotos continuam no conjunto de evidências do relatório.</p>
        {error ? <p className="item-error" id={REVIEW_ERROR_ID} ref={errorRef} role="alert" tabIndex={-1}>{error.message}</p> : null}
        <button className="button button--primary" disabled={completing} onClick={() => void generateReport()}>{completing ? "Gerando relatório..." : "Gerar relatório por IA"}<FileText size={18} /></button>
      </section>
    );
  }

  if (!current) return null;

  const environment = evidenceContextLabel(current.evidence);

  return (
    <section className="review-workspace" aria-labelledby="review-title">
      <header className="review-workspace__header">
        <div>
          <p className="eyebrow">Análise concluída</p>
          <h1 id="review-title">Confirme o que a imagem não consegue contar.</h1>
          <p>A análise sugere indícios. Seu contexto decide o que entra no relatório.</p>
        </div>
        <div className="review-progress" aria-label={`${reviewedCount} de ${entries.length} achados revisados`}>
          <strong>{reviewedCount}/{entries.length}</strong>
          <span>revisados</span>
          <progress value={reviewedCount} max={entries.length}>{reviewedCount} de {entries.length}</progress>
        </div>
      </header>

      <div className="review-layout">
        <aside className="finding-navigation" aria-label="Achados da análise">
          <p className="finding-navigation__label">Achados</p>
          {entries.map((entry, index) => {
            const reviewed = hasReview(inspection, entry);
            return (
              <button className={activeIndex === index ? "is-active" : ""} type="button" aria-current={activeIndex === index ? "step" : undefined} onClick={() => openEntry(index)} key={`${entry.analysis.imagemId}-${entry.finding.indice}`}>
                <span>{reviewed ? <Check size={14} /> : index + 1}</span>
                <div><strong>{entry.finding.tipo || "Indício visual"}</strong><small>{evidenceContextLabel(entry.evidence)} · {reviewed ? "Revisado" : "Pendente"}</small></div>
              </button>
            );
          })}
        </aside>

        <div className="finding-evidence">
          <div className="finding-evidence__meta"><span className="mono">EVIDÊNCIA {current.analysis.imagemId}</span><strong>{environment}</strong></div>
          {current.evidence ? <EvidenceImage evidence={current.evidence} alt={`Evidência ${current.analysis.imagemId}, achado ${activeIndex + 1}`} /> : <div className="evidence-unavailable">A foto não pôde ser vinculada.</div>}
          <div className="finding-marker" aria-label={`Marcador do achado ${activeIndex + 1}`}>{activeIndex + 1}</div>
          {current.analysis.limitacoes.length > 0 ? (
            <aside className="analysis-limit"><AlertTriangle size={17} /><div><strong>Limite desta leitura</strong>{current.analysis.limitacoes.map((limitation) => <span key={limitation}>{limitation}</span>)}</div></aside>
          ) : null}
        </div>

        <form className="finding-decision" onSubmit={saveReview}>
          <div className="finding-decision__meta"><span>Achado {activeIndex + 1} de {entries.length}</span><span className="mono">IMG {current.analysis.imagemId} · REF {current.finding.indice}</span></div>
          <p className="eyebrow">Observação sugerida pela IA</p>
          <h2>{current.finding.tipo || "Indício visual"}</h2>
          {current.finding.area ? <p className="finding-area">{current.finding.area}</p> : null}
          {current.finding.descricao ? <p>{current.finding.descricao}</p> : null}
          {current.finding.evidencia ? <blockquote>{current.finding.evidencia}</blockquote> : null}

          <p className="decision-owner-label">Decisão do responsável</p>
          <fieldset className="decision-options" aria-invalid={error?.target === "decision" || undefined} aria-describedby={error?.target === "decision" ? REVIEW_ERROR_ID : undefined}>
            <legend>Isso corresponde ao que você observou?</legend>
            {decisions.map((option) => {
              const Icon = option.icon;
              return (
                <label className={decision === option.value ? "is-selected" : ""} key={option.value}>
                  <input ref={option.value === "CONFIRMADO" ? firstDecisionRef : undefined} type="radio" name="decision" value={option.value} aria-label={option.label} aria-invalid={error?.target === "decision" || undefined} aria-describedby={error?.target === "decision" ? REVIEW_ERROR_ID : undefined} checked={decision === option.value} onChange={() => chooseDecision(option.value)} />
                  <Icon aria-hidden="true" size={19} />
                  <span><strong>{option.label}</strong><small>{option.help}</small></span>
                </label>
              );
            })}
          </fieldset>

          {decision === "CORRIGIDO" ? (
            <label className="review-field">Como deve aparecer no relatório<input ref={correctedTypeRef} maxLength={80} value={correctedType} aria-invalid={error?.target === "correctedType" || undefined} aria-describedby={error?.target === "correctedType" ? REVIEW_ERROR_ID : undefined} onChange={(event) => { setCorrectedType(event.target.value); if (error?.target === "correctedType") setError(null); lastSavedMutationKey.current = null; }} /></label>
          ) : null}
          <label className="review-field">Contexto observado<textarea ref={contextRef} rows={4} maxLength={1000} value={context} aria-invalid={error?.target === "context" || undefined} aria-describedby={error?.target === "context" ? REVIEW_ERROR_ID : undefined} onChange={(event) => { setContext(event.target.value); if (error?.target === "context") setError(null); lastSavedMutationKey.current = null; }} placeholder="Explique quando percebeu, se já existia ou por que a leitura precisa ser corrigida." /></label>
          <div className="review-field__counter">{context.length}/1000</div>

          {error ? <p className="item-error" id={REVIEW_ERROR_ID} ref={errorRef} role="alert" tabIndex={error.target === "form" ? -1 : undefined}>{error.message}</p> : null}
          {notice ? <p className="review-notice" role="status">{notice}</p> : null}

          <div className="review-actions">
            <button className="button button--secondary" type="button" disabled={activeIndex === 0 || saving} onClick={() => openEntry(activeIndex - 1)}><ArrowLeft size={17} />Anterior</button>
            <button className="button button--primary" type="submit" disabled={saving}>{saving ? "Salvando..." : "Salvar e continuar"}<ArrowRight size={17} /></button>
          </div>
        </form>
      </div>

      {allReviewed ? (
        <footer className="review-complete" aria-live="polite">
          <ShieldCheck aria-hidden="true" size={25} />
          <div><strong>Todos os achados têm contexto</strong><span>Você ainda pode voltar e editar qualquer decisão antes de gerar o documento.</span></div>
          <button className="button button--primary" disabled={completing} onClick={() => void generateReport()}>{completing ? "Gerando relatório..." : "Gerar relatório por IA"}<FileText size={18} /></button>
        </footer>
      ) : null}
    </section>
  );
}
