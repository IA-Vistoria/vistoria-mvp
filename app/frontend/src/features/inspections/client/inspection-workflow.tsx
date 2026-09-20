"use client";

import Link from "next/link";
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Camera,
  Check,
  CheckCircle2,
  FileCheck2,
  Info,
  RefreshCw,
  Send,
} from "lucide-react";
import { ChangeEvent, useEffect, useMemo, useRef, useState } from "react";

import { AsyncState } from "@/components/ui/async-state";
import { StatusBadge } from "@/components/ui/status-badge";
import { ApiError } from "@/lib/api";
import { getMyInspection, submitInspection, uploadEvidence } from "../api";
import { EvidenceImage } from "../shared/evidence-image";
import {
  PROTOCOL_GROUPS,
  PROTOCOL_ITEM_TOTAL,
  calculateProgress,
  validateEvidenceFile,
} from "../shared/protocol";
import type { Inspection, InspectionStatus, ProtocolItemCode } from "../types";
import { InspectionResults } from "./inspection-results";

const trackingTitles = {
  AGUARDANDO_IA: "Análise da IA em andamento",
  CONCLUIDA: "Vistoria concluída",
} as const;

const POLLING_INTERVAL_MS = 4000;
const POLLED_STATUSES: ReadonlySet<InspectionStatus> = new Set(["AGUARDANDO_IA"]);

const protocolItems = PROTOCOL_GROUPS.flatMap((group) =>
  group.items.map((item) => ({ group, item })),
);

const journeySteps = [
  ["Imóvel", "Dados do imóvel"],
  ["Paredes", "Fotos das paredes"],
  ["Envio", "Análise pela IA"],
  ["Resultado", "Relatório por imagem"],
] as const;

type JourneyStepState = "complete" | "active" | "pending";

function getJourneyStepState(status: InspectionStatus, index: number): JourneyStepState {
  if (status === "EM_RASCUNHO" || status === "DEVOLVIDA_CLIENTE") {
    if (index === 0) return "complete";
    return index === 1 ? "active" : "pending";
  }

  if (status === "FALHA_IA" || status === "AGUARDANDO_IA") {
    return index < 2 ? "complete" : index === 2 ? "active" : "pending";
  }

  if (status === "CONCLUIDA") {
    return "complete";
  }

  return index < journeySteps.length - 1 ? "complete" : "active";
}

export function InspectionWorkflow({ inspectionId }: { inspectionId: number }) {
  const [inspection, setInspection] = useState<Inspection | null>(null);
  const [selectedCode, setSelectedCode] = useState<ProtocolItemCode>("SALA_PAREDES_REVESTIMENTOS");
  const [loadError, setLoadError] = useState(false);
  const [uploading, setUploading] = useState<ProtocolItemCode | null>(null);
  const [itemErrors, setItemErrors] = useState<Partial<Record<ProtocolItemCode, string>>>({});
  const [retryFiles, setRetryFiles] = useState<Partial<Record<ProtocolItemCode, File>>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const submitLock = useRef(false);

  useEffect(() => {
    let active = true;
    getMyInspection(inspectionId)
      .then((loaded) => {
        if (!active) return;
        setInspection(loaded);
        setLoadError(false);
      })
      .catch(() => {
        if (active) setLoadError(true);
      });
    return () => {
      active = false;
    };
  }, [inspectionId]);

  useEffect(() => {
    if (!inspection || !POLLED_STATUSES.has(inspection.status)) return;

    let active = true;
    const timer = setInterval(() => {
      getMyInspection(inspectionId)
        .then((loaded) => {
          if (active) setInspection(loaded);
        })
        .catch(() => {
          /* keep last known state */
        });
    }, POLLING_INTERVAL_MS);

    return () => {
      active = false;
      clearInterval(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inspectionId, inspection?.status]);

  const selectedIndex = useMemo(
    () => Math.max(0, protocolItems.findIndex(({ item }) => item.code === selectedCode)),
    [selectedCode],
  );

  async function reload() {
    try {
      setInspection(await getMyInspection(inspectionId));
      setLoadError(false);
    } catch {
      setLoadError(true);
    }
  }

  async function sendFile(code: ProtocolItemCode, file: File) {
    setSelectedCode(code);
    const validationError = validateEvidenceFile(file);
    if (validationError) {
      setItemErrors((current) => ({ ...current, [code]: validationError }));
      return;
    }

    setUploading(code);
    setItemErrors((current) => ({ ...current, [code]: undefined }));
    try {
      const updated = await uploadEvidence(inspectionId, code, file);
      setInspection(updated);
      setRetryFiles((current) => ({ ...current, [code]: undefined }));
    } catch (cause) {
      setRetryFiles((current) => ({ ...current, [code]: file }));
      setItemErrors((current) => ({
        ...current,
        [code]: cause instanceof ApiError ? cause.problem.detail : "Não foi possível enviar a foto.",
      }));
    } finally {
      setUploading(null);
    }
  }

  function chooseFile(code: ProtocolItemCode, event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (file) void sendFile(code, file);
  }

  async function sendInspection() {
    if (!inspection || submitLock.current) return;
    if (inspection.imagens.length === 0) {
      setSubmitError("Adicione ao menos uma foto de parede antes de enviar.");
      return;
    }
    submitLock.current = true;
    setSubmitting(true);
    setSubmitError(null);
    try {
      setInspection(await submitInspection(inspection.id));
    } catch (cause) {
      setSubmitError(cause instanceof ApiError ? cause.problem.detail : "Não foi possível enviar a vistoria.");
      submitLock.current = false;
    } finally {
      setSubmitting(false);
    }
  }

  function selectRelative(offset: number) {
    const next = protocolItems[selectedIndex + offset];
    if (next) setSelectedCode(next.item.code);
  }

  if (loadError) {
    return <AsyncState role="alert" title="Não foi possível abrir a vistoria" description="Tente carregar novamente sem criar outro rascunho." action={<button className="button button--secondary" onClick={() => void reload()}>Tentar novamente</button>} />;
  }
  if (!inspection) return <AsyncState title="Carregando protocolo" description="Buscando as evidências já confirmadas para este imóvel." />;

  if (inspection.status === "CONCLUIDA") {
    return (
      <div className="workflow-page">
        <div className="workflow-titlebar">
          <div>
            <Link className="back-link" href="/client"><ArrowLeft size={17} />Voltar para vistorias</Link>
            <StatusBadge status={inspection.status} />
          </div>
        </div>
        <InspectionResults inspection={inspection} />
      </div>
    );
  }

  const editable = inspection.status === "EM_RASCUNHO" || inspection.status === "DEVOLVIDA_CLIENTE";
  const progress = calculateProgress(inspection.imagens);
  const trackedTitle = inspection.status in trackingTitles
    ? trackingTitles[inspection.status as keyof typeof trackingTitles]
    : null;

  return (
    <main className="workflow-page">
      <div className="workflow-titlebar">
        <div>
          <Link className="back-link" href="/client"><ArrowLeft size={17} />Voltar para vistorias</Link>
          <p className="eyebrow">MVP — paredes</p>
          <h1>Análise visual de paredes</h1>
          <p>Envie fotos das paredes; a IA analisa e libera o resultado automaticamente.</p>
        </div>
        <StatusBadge status={inspection.status} />
      </div>

      <nav className="journey-steps" aria-label="Etapas da vistoria">
        <ol>
          {journeySteps.map(([title, description], index) => {
            const state = getJourneyStepState(inspection.status, index);
            return (
              <li
                className={state === "complete" ? "is-complete" : state === "active" ? "is-active" : ""}
                aria-current={state === "active" ? "step" : undefined}
                key={title}
              >
                <span>{state === "complete" ? <Check size={16} /> : index + 1}</span>
                <div><strong>{index + 1}. {title}</strong><small>{description}</small></div>
              </li>
            );
          })}
        </ol>
      </nav>

      {inspection.status === "FALHA_IA" ? (
        <section className="tracking-panel" role="alert">
          <AlertTriangle size={28} />
          <div><p className="eyebrow">Falha no processamento</p><h2>A análise da IA não foi concluída</h2><p>Suas fotos continuam salvas. Reenvie para tentar novamente.</p></div>
          <button className="button button--primary" disabled={submitting} onClick={() => void sendInspection()}>{submitting ? "Reenviando..." : "Reenviar para análise"}</button>
        </section>
      ) : null}

      {trackedTitle && inspection.status === "AGUARDANDO_IA" ? (
        <section className="tracking-panel" aria-live="polite">
          <FileCheck2 size={28} />
          <div>
            <p className="eyebrow">Acompanhamento</p>
            <h2>{trackedTitle}</h2>
            <p>Esta página atualiza sozinha. Em seguida o resultado aparece por imagem.</p>
          </div>
        </section>
      ) : null}

      <div className="guided-workspace">
        <aside className="protocol-sidebar">
          <div className="property-summary">
            <p className="eyebrow">Imóvel vistoriado</p>
            <strong>{inspection.endereco}</strong>
            <span>Vistoria #{inspection.id}</span>
          </div>
          <section className="workflow-progress" aria-label="Progresso da documentação">
            <div>
              <strong>{progress} de {PROTOCOL_ITEM_TOTAL} {PROTOCOL_ITEM_TOTAL === 1 ? "item documentado" : "itens documentados"}</strong>
              <span>Você pode enviar várias fotos do mesmo item.</span>
            </div>
            <progress value={progress} max={PROTOCOL_ITEM_TOTAL}>{progress} de {PROTOCOL_ITEM_TOTAL}</progress>
          </section>
          <nav className="protocol-navigation" aria-label="Itens do protocolo">
            {PROTOCOL_GROUPS.map((group, groupIndex) => {
              const completed = group.items.filter((item) => inspection.imagens.some((entry) => entry.protocoloItem === item.code)).length;
              return (
                <section data-testid="protocol-group" key={group.name}>
                  <header><span>{String(groupIndex + 1).padStart(2, "0")}</span><strong>{group.name}</strong><small>{completed} de {group.items.length}</small></header>
                  {group.items.map((item) => {
                    const complete = inspection.imagens.some((entry) => entry.protocoloItem === item.code);
                    return (
                      <button className={selectedCode === item.code ? "is-active" : ""} type="button" aria-label={`${group.name} — ${item.label}`} aria-current={selectedCode === item.code ? "step" : undefined} onClick={() => setSelectedCode(item.code)} key={item.code}>
                        <span className={`protocol-check ${complete ? "is-complete" : ""}`} aria-hidden="true">{complete ? <Check size={14} /> : null}</span>
                        {item.label}
                      </button>
                    );
                  })}
                </section>
              );
            })}
          </nav>
        </aside>

        <section className="focused-protocol" aria-live="polite">
          <aside className="photo-guidance">
            <Camera size={24} />
            <div><strong>Dicas para boas fotos</strong><ul><li>Use boa iluminação e enquadre a parede completa.</li><li>Registre fissuras, manchas ou sinais de umidade.</li><li>Evite fotos tremidas ou muito próximas.</li></ul></div>
          </aside>
          {protocolItems.map(({ group, item }, index) => {
            const evidence = inspection.imagens.filter((entry) => entry.protocoloItem === item.code);
            const isBusy = uploading === item.code;
            const active = selectedCode === item.code;
            const previous = protocolItems[index - 1]?.item;
            const next = protocolItems[index + 1]?.item;
            return (
              <article className="protocol-item" data-testid={`protocol-item-${item.code}`} hidden={!active} key={item.code}>
                <header className="focused-protocol__heading">
                  <p className="eyebrow">{group.name}</p>
                  <h2>{item.label}</h2>
                  <p>{item.guidance}</p>
                </header>

                {editable ? (
                  <div className="protocol-item__actions">
                    <label className={`upload-zone ${isBusy ? "is-disabled" : ""}`}>
                      <Camera size={28} />
                      <strong>{isBusy ? "Enviando foto..." : "Clique para enviar uma foto"}</strong>
                      <span>JPEG, PNG ou WebP · máximo 10 MB</span>
                      <input className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" disabled={isBusy || uploading !== null} aria-label={`Adicionar foto de ${group.name} — ${item.label}`} onChange={(event) => chooseFile(item.code, event)} />
                    </label>
                    {retryFiles[item.code] ? <button className="retry-link" disabled={isBusy} onClick={() => void sendFile(item.code, retryFiles[item.code]!)}><RefreshCw size={15} />Tentar novamente</button> : null}
                  </div>
                ) : null}

                {evidence.length ? (
                  <div className="evidence-strip">
                    {evidence.map((photo, evidenceIndex) => <EvidenceImage evidence={photo} alt={`${group.name} — ${item.label}, foto ${evidenceIndex + 1}`} key={photo.id} />)}
                  </div>
                ) : <p className="empty-evidence">Nenhuma foto registrada neste item.</p>}
                {itemErrors[item.code] ? <p className="item-error" role="alert">{itemErrors[item.code]}</p> : null}

                <footer className="protocol-pager">
                  <button className="button button--secondary" type="button" disabled={!previous} onClick={() => selectRelative(-1)}><ArrowLeft size={17} />{previous ? `Voltar: ${previous.label}` : "Primeiro item"}</button>
                  <span>{evidence.length} {evidence.length === 1 ? "foto registrada" : "fotos registradas"}</span>
                  <button className="button button--primary" type="button" disabled={!next} onClick={() => selectRelative(1)}>{next ? `Próximo: ${next.label}` : "Pronto para enviar"}<ArrowRight size={17} /></button>
                </footer>
              </article>
            );
          })}
        </section>

        <aside className="submission-guide" aria-label="Orientações antes do envio">
          <section>
            <FileCheck2 size={24} />
            <div><h2>Antes de enviar</h2><p>Confira se as fotos mostram bem as paredes do imóvel.</p></div>
          </section>
          <ul className="submission-checklist">
            <li><CheckCircle2 size={18} />Fotos nítidas e bem iluminadas</li>
            <li className={progress >= 1 ? "is-complete" : ""}><span>{inspection.imagens.length}</span>foto(s) de parede</li>
          </ul>
          <section className="next-steps">
            <Info size={22} />
            <div>
              <h2>O que acontece depois?</h2>
              <ol>
                <li><strong>IA analisa as fotos</strong><span>Indícios visuais por imagem.</span></li>
                <li><strong>Vistoria concluída</strong><span>Resultado liberado automaticamente.</span></li>
              </ol>
            </div>
          </section>
          <div className="professional-warning"><AlertTriangle size={19} /><p><strong>Importante</strong>Esta é uma análise visual preliminar e não substitui vistoria técnica presencial.</p></div>
          {editable ? (
            <div className="workflow-submit">
              <p>Revise as fotos antes de concluir.</p>
              {submitError ? <p className="item-error" role="alert">{submitError}</p> : null}
              <button className="button button--primary" disabled={submitting || uploading !== null} onClick={() => void sendInspection()}><Send size={17} />{submitting ? "Analisando..." : "Enviar para análise da IA"}</button>
              <small>O processamento pode levar alguns instantes por foto. O resultado aparece nesta página.</small>
            </div>
          ) : null}
        </aside>
      </div>
    </main>
  );
}
