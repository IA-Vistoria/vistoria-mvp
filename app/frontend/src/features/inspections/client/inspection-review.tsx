"use client";

import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Check,
  CheckCircle2,
  FileText,
  MessageSquareMore,
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
import { aiResultLabel, findEnvironmentResult, issueTypeLabel } from "./ai-report";
import { evidenceContextLabel } from "./evidence-context";

interface InspectionReviewProps {
  inspection: Inspection;
  onChange: (inspection: Inspection) => void;
  onRefresh: () => Promise<Inspection>;
  onOpenReport?: () => void;
}

interface FindingEntry {
  analysis: AiImageAnalysis;
  evidence: Evidence | null;
  finding: AiFinding;
}

type ManifestationMode = Extract<ReviewDecision, "CONTESTO" | "CONTEXTO_ADICIONAL">;

const REVIEW_ERROR_ID = "inspection-review-error";

interface ReviewError {
  message: string;
  target: "context" | "form";
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

const manifestationLabels: Partial<Record<ReviewDecision, string>> = {
  CONCORDO: "Concordância registrada",
  CONTESTO: "Contestação registrada",
  CONTEXTO_ADICIONAL: "Contexto adicional registrado",
  CONFIRMADO: "Confirmação histórica registrada",
  CORRIGIDO: "Correção histórica registrada",
  REJEITADO: "Rejeição histórica registrada",
};

function flattenFindings(inspection: Inspection): FindingEntry[] {
  return (inspection.analiseIa?.imagens ?? []).flatMap((analysis) => {
    const evidence = inspection.imagens.find((item) => item.id === analysis.imagemId) ?? null;
    return analysis.achados.map((finding) => ({ analysis, evidence, finding }));
  });
}

function savedReview(inspection: Inspection, entry: FindingEntry | null) {
  if (!entry) return null;
  return inspection.manifestacoes.find(
    (review) => review.imagemId === entry.analysis.imagemId && review.indiceAchado === entry.finding.indice,
  ) ?? null;
}

function normalizedValue(value: string | null | undefined) {
  return value?.trim().toLocaleUpperCase("pt-BR") ?? "";
}

export function InspectionReview({ inspection, onChange, onRefresh, onOpenReport }: InspectionReviewProps) {
  const entries = useMemo(() => flattenFindings(inspection), [inspection]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [mode, setMode] = useState<ManifestationMode | null>(null);
  const [context, setContext] = useState("");
  const [error, setError] = useState<ReviewError | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [completing, setCompleting] = useState(false);
  const mutationLock = useRef(false);
  const completionLock = useRef(false);
  const contextRef = useRef<HTMLTextAreaElement>(null);
  const errorRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (error?.target === "form") errorRef.current?.focus();
  }, [error]);

  const activeIndex = Math.min(currentIndex, Math.max(0, entries.length - 1));
  const current = entries[activeIndex] ?? null;
  const currentReview = savedReview(inspection, current);
  const generalResult = inspection.analiseIa?.resultadoGeral ?? null;
  const generalReason = inspection.analiseIa?.motivoResultadoGeral
    ?? "A análise anterior não possui um motivo estruturado disponível.";

  function openEntry(index: number) {
    setCurrentIndex(Math.min(Math.max(index, 0), Math.max(0, entries.length - 1)));
    setMode(null);
    setContext("");
    setError(null);
    setNotice(null);
  }

  function selectMode(nextMode: ManifestationMode) {
    setMode(nextMode);
    setContext(currentReview?.decisao === nextMode ? currentReview.contexto : "");
    setError(null);
    setNotice(null);
    requestAnimationFrame(() => contextRef.current?.focus());
  }

  async function persistManifestation(decision: ReviewDecision, text: string) {
    if (!current || mutationLock.current) return;
    mutationLock.current = true;
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await reviewFinding(inspection.id, {
        imagemId: current.analysis.imagemId,
        indiceAchado: current.finding.indice,
        decisao: decision,
        contexto: text,
      });
      onChange(updated);
      setMode(null);
      setContext("");
      setNotice(manifestationLabels[decision] ?? "Manifestação registrada");
    } catch (cause) {
      if (cause instanceof ApiError && cause.problem.status === 409) {
        try {
          const latest = await onRefresh();
          onChange(latest);
          setMode(null);
          setContext("");
          const latestEntry = flattenFindings(latest)[activeIndex] ?? null;
          const latestReview = savedReview(latest, latestEntry);
          setNotice(latestReview ? manifestationLabels[latestReview.decisao] ?? "Manifestação atualizada" : null);
          if (!latestReview) {
            setError({ message: "A vistoria mudou em outra sessão. Exibimos agora o estado mais recente.", target: "form" });
          }
        } catch {
          setError({ message: cause.problem.detail, target: "form" });
        }
      } else {
        setError({
          message: cause instanceof ApiError ? cause.problem.detail : "Não foi possível registrar sua manifestação.",
          target: "form",
        });
      }
    } finally {
      mutationLock.current = false;
      setSaving(false);
    }
  }

  async function saveManifestation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!mode) return;
    const trimmedContext = context.trim();
    if (!trimmedContext) {
      setError({
        message: mode === "CONTESTO"
          ? "Explique sua contestação para que ela acompanhe o resultado da IA."
          : "Descreva o contexto que ajuda a interpretar esta evidência.",
        target: "context",
      });
      contextRef.current?.focus();
      return;
    }
    await persistManifestation(mode, trimmedContext);
  }

  async function generateReport() {
    if (onOpenReport) {
      onOpenReport();
      return;
    }
    if (completionLock.current) return;
    completionLock.current = true;
    setCompleting(true);
    setError(null);
    try {
      const updated = await completeReport(inspection.id);
      onChange(updated);
    } catch (cause) {
      setError({
        message: cause instanceof ApiError ? cause.problem.detail : "Não foi possível abrir o relatório.",
        target: "form",
      });
      completionLock.current = false;
    } finally {
      setCompleting(false);
    }
  }

  if (entries.length === 0) {
    return (
      <section className="review-empty" aria-labelledby="review-empty-title" aria-live="polite">
        <CheckCircle2 aria-hidden="true" size={32} />
        <h1 id="review-empty-title">Nenhum indício visual foi identificado</h1>
        <p>A IA não registrou achados nas fotos. As evidências e os dados da execução permanecem no relatório.</p>
        {error ? <p className="item-error" id={REVIEW_ERROR_ID} ref={errorRef} role="alert" tabIndex={-1}>{error.message}</p> : null}
        <button className="button button--primary" disabled={completing} onClick={() => void generateReport()}>{onOpenReport ? "Voltar ao relatório" : completing ? "Abrindo relatório..." : "Abrir relatório por IA"}<FileText aria-hidden="true" size={18} /></button>
      </section>
    );
  }

  if (!current) return null;

  const environment = evidenceContextLabel(current.evidence);
  const environmentResult = findEnvironmentResult(
    inspection.analiseIa,
    current.evidence?.ambienteId ?? current.analysis.ambiente?.id ?? null,
    current.evidence?.ambienteNome ?? current.analysis.ambiente?.nome,
  );
  const severity = normalizedValue(current.finding.gravidadeNormalizada ?? current.finding.gravidade);
  const confidence = normalizedValue(current.finding.confiancaNormalizada ?? current.finding.confianca);

  return (
    <section className="review-workspace" aria-labelledby="review-title">
      <header className="review-workspace__header">
        <div>
          <h1 id="review-title">Resultado da análise visual por IA</h1>
          <p>A conclusão abaixo foi produzida pela IA a partir das evidências enviadas.</p>
          <p>Sua manifestação é opcional e não altera esta conclusão.</p>
        </div>
        <div className={`ai-result ai-result--${generalResult?.toLocaleLowerCase("pt-BR") ?? "legacy"}`}>
          <ShieldCheck aria-hidden="true" size={22} />
          <div><span>Resultado do imóvel</span><strong>{aiResultLabel(generalResult)}</strong></div>
        </div>
      </header>

      <div className="review-result-reason">
        <strong>Por que a IA chegou a este resultado</strong>
        <span>{generalReason}</span>
        <button className="button button--primary" disabled={completing} onClick={() => void generateReport()}>{onOpenReport ? "Voltar ao relatório" : completing ? "Abrindo relatório..." : "Gerar relatório por IA"}<FileText aria-hidden="true" size={18} /></button>
      </div>

      <div className="review-layout">
        <aside className="finding-navigation" aria-label="Achados da análise">
          <p className="finding-navigation__label">Evidências analisadas</p>
          {entries.map((entry, index) => {
            const review = savedReview(inspection, entry);
            return (
              <button className={activeIndex === index ? "is-active" : ""} type="button" aria-current={activeIndex === index ? "step" : undefined} onClick={() => openEntry(index)} key={`${entry.analysis.imagemId}-${entry.finding.indice}`}>
                <span>{review ? <Check aria-hidden="true" size={14} /> : index + 1}</span>
                <div><strong>{issueTypeLabel(entry.finding.tipo)}</strong><small>{evidenceContextLabel(entry.evidence)} · {review ? "Com manifestação" : "Resultado da IA"}</small></div>
              </button>
            );
          })}
        </aside>

        <div className="finding-evidence">
          <div className="finding-evidence__meta"><span className="mono">EVIDÊNCIA {current.analysis.imagemId}</span><strong>{environment}</strong></div>
          {current.evidence ? <EvidenceImage evidence={current.evidence} alt={`Evidência ${current.analysis.imagemId}, achado ${activeIndex + 1}`} /> : <div className="evidence-unavailable">A foto não pôde ser vinculada.</div>}
          <div className="finding-marker" aria-label={`Marcador do achado ${activeIndex + 1}`}>{activeIndex + 1}</div>
          {current.analysis.limitacoes.length > 0 ? (
            <aside className="analysis-limit" aria-label="Limitações da análise"><AlertTriangle aria-hidden="true" size={17} /><div><strong>O que esta imagem não permite concluir</strong>{current.analysis.limitacoes.map((limitation) => <span key={limitation}>{limitation}</span>)}</div></aside>
          ) : null}
        </div>

        <article className="finding-decision" aria-labelledby="finding-title">
          <div className="finding-decision__meta"><span>Achado {activeIndex + 1} de {entries.length}</span><span className="mono">IMG {current.analysis.imagemId} · REF {current.finding.indice}</span></div>
          <div className="environment-result">
            <span>{environment}</span>
            <strong>Resultado do ambiente: {aiResultLabel(environmentResult?.resultado)}</strong>
            <small>{environmentResult?.motivoResultado ?? "Resultado estruturado indisponível para esta análise anterior."}</small>
          </div>
          <h2 id="finding-title">{issueTypeLabel(current.finding.tipo)}</h2>
          {current.finding.area ? <p className="finding-area">{current.finding.area}</p> : null}

          <dl className="finding-explanation">
            {current.finding.criterio ? <div><dt>Critério avaliado</dt><dd>{current.finding.criterio}</dd></div> : null}
            {current.finding.descricao ? <div><dt>O que foi observado</dt><dd>{current.finding.descricao}</dd></div> : null}
            {current.finding.evidencia ? <div><dt>Indício na imagem</dt><dd>{current.finding.evidencia}</dd></div> : null}
            {current.finding.impacto ? <div><dt>Por que merece atenção</dt><dd>{current.finding.impacto}</dd></div> : null}
          </dl>

          <div className="finding-confidence" aria-label="Classificação do achado">
            {severity ? <span><AlertTriangle aria-hidden="true" size={15} />{severityLabels[severity] ?? `Gravidade ${severity.toLocaleLowerCase("pt-BR")}`}</span> : null}
            {confidence ? <span><ShieldCheck aria-hidden="true" size={15} />{confidenceLabels[confidence] ?? `Confiança ${confidence.toLocaleLowerCase("pt-BR")}`}</span> : null}
          </div>

          {current.finding.recomendacao ? <div className="finding-recommendation"><strong>Próxima ação recomendada</strong><span>{current.finding.recomendacao}</span></div> : null}

          <section className="manifestation-panel" aria-labelledby="manifestation-title">
            <div>
              <h3 id="manifestation-title">Sua manifestação</h3>
              <p>Opcional. Registre sua percepção sem substituir o resultado da IA.</p>
            </div>

            {currentReview ? (
              <div className="manifestation-saved" role="status">
                <CheckCircle2 aria-hidden="true" size={18} />
                <div><strong>{manifestationLabels[currentReview.decisao] ?? "Manifestação registrada"}</strong>{currentReview.contexto ? <span>{currentReview.contexto}</span> : null}</div>
              </div>
            ) : null}
            {notice && !currentReview ? <p className="review-notice" role="status">{notice}</p> : null}

            <div className="manifestation-actions" aria-label="Opções de manifestação">
              <button className="button button--secondary" type="button" disabled={saving} onClick={() => void persistManifestation("CONCORDO", "")}><Check aria-hidden="true" size={17} />Concordo</button>
              <button className="button button--secondary" type="button" disabled={saving} aria-pressed={mode === "CONTESTO"} onClick={() => selectMode("CONTESTO")}><X aria-hidden="true" size={17} />Contestar análise</button>
              <button className="button button--secondary" type="button" disabled={saving} aria-pressed={mode === "CONTEXTO_ADICIONAL"} onClick={() => selectMode("CONTEXTO_ADICIONAL")}><MessageSquareMore aria-hidden="true" size={17} />Adicionar contexto</button>
            </div>

            {mode ? (
              <form className="manifestation-form" onSubmit={saveManifestation}>
                <label className="review-field">
                  {mode === "CONTESTO" ? "Por que você contesta esta análise?" : "Qual contexto ajuda a interpretar a evidência?"}
                  <textarea ref={contextRef} rows={4} maxLength={1000} value={context} aria-invalid={error?.target === "context" || undefined} aria-describedby={error?.target === "context" ? REVIEW_ERROR_ID : undefined} onChange={(event) => { setContext(event.target.value); if (error?.target === "context") setError(null); }} placeholder={mode === "CONTESTO" ? "Explique o que na imagem ou no contexto diverge da análise." : "Exemplo: quando percebeu, se já existia ou se houve reparo recente."} />
                </label>
                <div className="review-field__counter">{context.length}/1000</div>
                <div className="manifestation-form__actions">
                  <button className="button button--ghost" type="button" disabled={saving} onClick={() => { setMode(null); setContext(""); setError(null); }}>Cancelar</button>
                  <button className="button button--primary" type="submit" disabled={saving}>{saving ? "Salvando..." : mode === "CONTESTO" ? "Salvar contestação" : "Salvar contexto"}<ArrowRight aria-hidden="true" size={17} /></button>
                </div>
              </form>
            ) : null}

            {error ? <p className="item-error" id={REVIEW_ERROR_ID} ref={errorRef} role="alert" tabIndex={error.target === "form" ? -1 : undefined}>{error.message}</p> : null}
          </section>

          <nav className="review-actions" aria-label="Navegação entre achados">
            <button className="button button--secondary" type="button" disabled={activeIndex === 0 || saving} onClick={() => openEntry(activeIndex - 1)}><ArrowLeft aria-hidden="true" size={17} />Anterior</button>
            <button className="button button--secondary" type="button" disabled={activeIndex === entries.length - 1 || saving} onClick={() => openEntry(activeIndex + 1)}>Próximo<ArrowRight aria-hidden="true" size={17} /></button>
          </nav>
        </article>
      </div>
    </section>
  );
}
