"use client";

import Link from "next/link";
import { AlertTriangle, ArrowLeft, Camera, Check, FileCheck2, RefreshCw, Send } from "lucide-react";
import { ChangeEvent, useEffect, useRef, useState } from "react";

import { AsyncState } from "@/components/ui/async-state";
import { StatusBadge } from "@/components/ui/status-badge";
import { ApiError } from "@/lib/api";
import { getMyInspection, submitInspection, uploadEvidence } from "../api";
import { EvidenceImage } from "../shared/evidence-image";
import { PROTOCOL_GROUPS, calculateProgress, validateEvidenceFile } from "../shared/protocol";
import type { Inspection, ProtocolItemCode } from "../types";

const trackingTitles = {
  AGUARDANDO_IA: "Pré-análise em andamento",
  AGUARDANDO_ENGENHEIRO: "Aguardando revisão do engenheiro",
  CONCLUIDA: "Vistoria concluída",
} as const;

export function InspectionWorkflow({ inspectionId }: { inspectionId: number }) {
  const [inspection, setInspection] = useState<Inspection | null>(null);
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

  async function reload() {
    try {
      setInspection(await getMyInspection(inspectionId));
      setLoadError(false);
    } catch {
      setLoadError(true);
    }
  }

  async function sendFile(code: ProtocolItemCode, file: File) {
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
      setSubmitError("Adicione ao menos uma evidência antes de enviar a vistoria.");
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

  if (loadError) {
    return <AsyncState role="alert" title="Não foi possível abrir a vistoria" description="Tente carregar novamente sem criar outro rascunho." action={<button className="button button--secondary" onClick={() => void reload()}>Tentar novamente</button>} />;
  }
  if (!inspection) return <AsyncState title="Carregando protocolo" description="Buscando as evidências já confirmadas para este imóvel." />;

  const editable = inspection.status === "EM_RASCUNHO" || inspection.status === "DEVOLVIDA_CLIENTE";
  const progress = calculateProgress(inspection.imagens);
  const trackedTitle = inspection.status in trackingTitles
    ? trackingTitles[inspection.status as keyof typeof trackingTitles]
    : null;

  return (
    <main className="workflow-page">
      <Link className="back-link" href="/client"><ArrowLeft size={17} />Voltar para vistorias</Link>
      <header className="workflow-heading">
        <div>
          <p className="eyebrow">Vistoria #{inspection.id}</p>
          <h1>{inspection.endereco}</h1>
        </div>
        <StatusBadge status={inspection.status} />
      </header>

      {inspection.status === "DEVOLVIDA_CLIENTE" ? (
        <section className="engineer-note" aria-labelledby="engineer-note-title">
          <AlertTriangle size={22} />
          <div><h2 id="engineer-note-title">Complementação solicitada</h2><p>{inspection.parecerEngenheiro || "O engenheiro solicitou novas evidências."}</p></div>
        </section>
      ) : null}

      {inspection.status === "FALHA_IA" ? (
        <section className="tracking-panel" role="alert">
          <AlertTriangle size={28} />
          <div><p className="eyebrow">Falha real do processamento</p><h2>Pré-análise não foi concluída</h2><p>Suas evidências continuam salvas. Reenvie este mesmo caso para uma nova tentativa.</p></div>
          <button className="button button--primary" disabled={submitting} onClick={() => void sendInspection()}>{submitting ? "Reenviando..." : "Reenviar para pré-análise"}</button>
        </section>
      ) : null}

      {trackedTitle ? (
        <section className="tracking-panel" aria-live="polite">
          <FileCheck2 size={28} />
          <div><p className="eyebrow">Acompanhamento</p><h2>{trackedTitle}</h2><p>A vistoria está em modo de acompanhamento.</p></div>
        </section>
      ) : null}

      <section className="workflow-progress" aria-label="Progresso da documentação">
        <div><strong>{progress} de 12 itens documentados</strong><span>O progresso considera itens confirmados pela Vistor.IA.</span></div>
        <progress value={progress} max={12}>{progress} de 12</progress>
      </section>

      <div className="protocol-groups">
        {PROTOCOL_GROUPS.map((group, groupIndex) => (
          <section className="protocol-group" data-testid="protocol-group" key={group.name}>
            <header><span>{String(groupIndex + 1).padStart(2, "0")}</span><div><h2>{group.name}</h2><p>{group.description}</p></div></header>
            <div className="protocol-items">
              {group.items.map((item) => {
                const evidence = inspection.imagens.filter((entry) => entry.protocoloItem === item.code);
                const isBusy = uploading === item.code;
                return (
                  <article className="protocol-item" data-testid={`protocol-item-${item.code}`} key={item.code}>
                    <div className="protocol-item__copy">
                      <span className={`protocol-check ${evidence.length ? "is-complete" : ""}`} aria-hidden="true">{evidence.length ? <Check size={16} /> : null}</span>
                      <div><h3>{item.label}</h3><p>{item.guidance}</p><small>{evidence.length} {evidence.length === 1 ? "foto" : "fotos"}</small></div>
                    </div>
                    {evidence.length ? <div className="evidence-strip">{evidence.map((photo, index) => <EvidenceImage evidence={photo} alt={`${group.name} — ${item.label}, foto ${index + 1}`} key={photo.id} />)}</div> : null}
                    {itemErrors[item.code] ? <p className="item-error" role="alert">{itemErrors[item.code]}</p> : null}
                    {editable ? (
                      <div className="protocol-item__actions">
                        <label className={`button button--secondary ${isBusy ? "is-disabled" : ""}`}>
                          <Camera size={17} />{isBusy ? "Enviando..." : "Adicionar foto"}
                          <input className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" disabled={isBusy || uploading !== null} aria-label={`Adicionar foto de ${group.name} — ${item.label}`} onChange={(event) => chooseFile(item.code, event)} />
                        </label>
                        {retryFiles[item.code] ? <button className="retry-link" disabled={isBusy} onClick={() => void sendFile(item.code, retryFiles[item.code]!)}><RefreshCw size={15} />Tentar novamente</button> : null}
                      </div>
                    ) : null}
                  </article>
                );
              })}
            </div>
          </section>
        ))}
      </div>

      {editable ? (
        <footer className="workflow-submit">
          <div><p className="eyebrow">Revisão final</p><h2>As fotos representam o estado atual do imóvel?</h2><p>Você poderá acompanhar o processamento após o envio.</p>{submitError ? <p className="item-error" role="alert">{submitError}</p> : null}</div>
          <button className="button button--primary" disabled={submitting || uploading !== null} onClick={() => void sendInspection()}><Send size={17} />{submitting ? "Enviando..." : "Enviar para análise"}</button>
        </footer>
      ) : null}
    </main>
  );
}

