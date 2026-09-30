"use client";

import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Camera,
  Check,
  CheckCircle2,
  FileCheck2,
  Images,
  Info,
  RefreshCw,
  ScanSearch,
  Send,
} from "lucide-react";
import Link from "next/link";
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
import { InspectionReport } from "./inspection-report";
import { InspectionReview } from "./inspection-review";
import { InspectionResults } from "./inspection-results";

const POLLING_INTERVAL_MS = 4000;
const POLLED_STATUSES: ReadonlySet<InspectionStatus> = new Set(["AGUARDANDO_IA"]);

const protocolItems = PROTOCOL_GROUPS.flatMap((group) =>
  group.items.map((item) => ({ group, item })),
);

const journeySteps = [
  ["Imóvel", "Endereço salvo"],
  ["Evidências", "Fotos guiadas"],
  ["Revisão", "Contexto humano"],
  ["Relatório", "Documento final"],
] as const;

type JourneyStepState = "complete" | "active" | "pending";

function getJourneyStepState(status: InspectionStatus, index: number): JourneyStepState {
  if (["EM_RASCUNHO", "DEVOLVIDA_CLIENTE", "FALHA_IA"].includes(status)) {
    if (index === 0) return "complete";
    return index === 1 ? "active" : "pending";
  }
  if (status === "AGUARDANDO_IA") return index < 2 ? "complete" : index === 2 ? "active" : "pending";
  if (status === "REVISAO_PENDENTE") return index < 2 ? "complete" : index === 2 ? "active" : "pending";
  if (status === "RELATORIO_DISPONIVEL" || status === "CONCLUIDA") return "complete";
  return index < 2 ? "complete" : index === 2 ? "active" : "pending";
}

function firstIncompleteCode(inspection: Inspection, after?: ProtocolItemCode): ProtocolItemCode {
  const completed = new Set(inspection.imagens.map((evidence) => evidence.protocoloItem));
  const start = after ? protocolItems.findIndex(({ item }) => item.code === after) + 1 : 0;
  const ordered = [...protocolItems.slice(start), ...protocolItems.slice(0, start)];
  return ordered.find(({ item }) => !completed.has(item.code))?.item.code
    ?? after
    ?? protocolItems[0].item.code;
}

export function InspectionWorkflow({ inspectionId }: { inspectionId: number }) {
  const [inspection, setInspection] = useState<Inspection | null>(null);
  const [selectedCode, setSelectedCode] = useState<ProtocolItemCode>(protocolItems[0].item.code);
  const [loadError, setLoadError] = useState(false);
  const [uploading, setUploading] = useState<ProtocolItemCode | null>(null);
  const [itemErrors, setItemErrors] = useState<Partial<Record<ProtocolItemCode, string>>>({});
  const [retryFiles, setRetryFiles] = useState<Partial<Record<ProtocolItemCode, File>>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const submitLock = useRef(false);
  const polledStatus = inspection?.status;

  useEffect(() => {
    const controller = new AbortController();
    getMyInspection(inspectionId, controller.signal)
      .then((loaded) => {
        setInspection(loaded);
        setSelectedCode(firstIncompleteCode(loaded));
        setLoadError(false);
      })
      .catch((cause: unknown) => {
        if (!(cause instanceof DOMException && cause.name === "AbortError")) setLoadError(true);
      });
    return () => controller.abort();
  }, [inspectionId]);

  useEffect(() => {
    if (!polledStatus || !POLLED_STATUSES.has(polledStatus)) return;

    let active = true;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const controller = new AbortController();
    const poll = async () => {
      try {
        const loaded = await getMyInspection(inspectionId, controller.signal);
        if (active) {
          if (loaded.status === "FALHA_IA") submitLock.current = false;
          setInspection(loaded);
        }
      } catch {
        if (!controller.signal.aborted) {
          // A tela preserva o último estado confirmado e tenta novamente.
        }
      }
      if (active) timer = setTimeout(poll, POLLING_INTERVAL_MS);
    };
    timer = setTimeout(poll, POLLING_INTERVAL_MS);

    return () => {
      active = false;
      controller.abort();
      if (timer) clearTimeout(timer);
    };
  }, [inspectionId, polledStatus]);

  const selectedIndex = useMemo(
    () => Math.max(0, protocolItems.findIndex(({ item }) => item.code === selectedCode)),
    [selectedCode],
  );

  async function reload() {
    try {
      const loaded = await getMyInspection(inspectionId);
      setInspection(loaded);
      setSelectedCode(firstIncompleteCode(loaded));
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
      setSelectedCode(firstIncompleteCode(updated, code));
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
      setSubmitError("Adicione ao menos uma foto antes de enviar.");
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
  if (!inspection) return <AsyncState title="Carregando roteiro" description="Buscando as evidências já confirmadas para este imóvel." />;

  if (inspection.status === "RELATORIO_DISPONIVEL") {
    return (
      <div className="workflow-page workflow-page--report">
        <WorkflowBackHeader inspection={inspection} />
        <InspectionReport inspection={inspection} />
      </div>
    );
  }

  if (inspection.status === "CONCLUIDA") {
    return (
      <div className="workflow-page">
        <WorkflowBackHeader inspection={inspection} />
        <InspectionResults inspection={inspection} />
      </div>
    );
  }

  if (inspection.status === "REVISAO_PENDENTE") {
    return (
      <div className="workflow-page">
        <WorkflowBackHeader inspection={inspection} />
        <InspectionReview inspection={inspection} onChange={setInspection} onRefresh={() => getMyInspection(inspection.id)} />
      </div>
    );
  }

  if (inspection.status === "AGUARDANDO_IA") {
    return (
      <div className="workflow-page">
        <WorkflowBackHeader inspection={inspection} />
        <section className="processing-state" aria-live="polite">
          <div className="processing-state__visual" aria-hidden="true"><ScanSearch size={36} /></div>
          <p className="eyebrow">Processamento assíncrono</p>
          <h1>Análise da IA em andamento</h1>
          <p>As fotos já estão salvas. É seguro sair e voltar: esta página consulta o estado real, sem estimar porcentagens.</p>
          <Link className="button button--secondary" href="/client">Voltar ao início</Link>
        </section>
      </div>
    );
  }

  const editable = ["EM_RASCUNHO", "DEVOLVIDA_CLIENTE"].includes(inspection.status);
  const progress = calculateProgress(inspection.imagens);

  return (
    <div className="workflow-page">
      <div className="workflow-titlebar">
        <div>
          <Link className="back-link" href="/client"><ArrowLeft size={17} />Voltar ao início</Link>
          <p className="eyebrow">Captura guiada</p>
          <h1>Documente o imóvel, ambiente por ambiente.</h1>
          <p>O roteiro organiza as fotos; você decide quanto contexto registrar antes da análise.</p>
        </div>
        <StatusBadge status={inspection.status} />
      </div>

      <Journey status={inspection.status} />

      {inspection.status === "FALHA_IA" ? (
        <section className="tracking-panel tracking-panel--error" role="alert">
          <AlertTriangle size={28} />
          <div><p className="eyebrow">Falha no processamento</p><h2>A análise da IA não foi concluída</h2><p>Suas fotos continuam salvas. Você pode conferir as evidências e tentar novamente.</p></div>
          <button className="button button--primary" disabled={submitting} onClick={() => void sendInspection()}>{submitting ? "Reenviando..." : "Reenviar para análise"}</button>
        </section>
      ) : null}

      <div className="guided-workspace">
        <aside className="protocol-sidebar">
          <div className="property-summary">
            <p className="eyebrow">Imóvel</p>
            <strong>{inspection.endereco}</strong>
            <span className="mono">VISTORIA {String(inspection.id).padStart(4, "0")}</span>
          </div>
          <section className="workflow-progress" aria-label="Progresso da documentação">
            <div><strong>{progress} de {PROTOCOL_ITEM_TOTAL} itens documentados</strong><span>Progresso calculado apenas pelas fotos salvas.</span></div>
            <progress value={progress} max={PROTOCOL_ITEM_TOTAL}>{progress} de {PROTOCOL_ITEM_TOTAL}</progress>
          </section>
          <nav className="protocol-navigation" aria-label="Itens do roteiro">
            {PROTOCOL_GROUPS.map((group, groupIndex) => {
              const completed = group.items.filter((item) => inspection.imagens.some((entry) => entry.protocoloItem === item.code)).length;
              return (
                <section data-testid="protocol-group" key={group.name}>
                  <header><span className="mono">{String(groupIndex + 1).padStart(2, "0")}</span><strong>{group.name}</strong><small>{completed} de {group.items.length}</small></header>
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
          {protocolItems.map(({ group, item }, index) => {
            const evidence = inspection.imagens.filter((entry) => entry.protocoloItem === item.code);
            const isBusy = uploading === item.code;
            const active = selectedCode === item.code;
            const previous = protocolItems[index - 1]?.item;
            const next = protocolItems[index + 1]?.item;
            return (
              <article className="protocol-item" data-testid={`protocol-item-${item.code}`} hidden={!active} key={item.code}>
                <header className="focused-protocol__heading">
                  <p className="eyebrow">{group.name} · item {index + 1} de {PROTOCOL_ITEM_TOTAL}</p>
                  <h2>{item.label}</h2>
                  <p>{item.guidance}</p>
                </header>

                <aside className="photo-guidance"><Info size={21} /><div><strong>Uma boa evidência</strong><span>Use luz uniforme, mantenha referência do ambiente e faça uma aproximação somente quando houver algo a destacar.</span></div></aside>

                {editable ? (
                  <div className={`capture-actions ${isBusy ? "is-disabled" : ""}`}>
                    <label className="capture-action capture-action--primary">
                      <Camera aria-hidden="true" size={23} />
                      <span><strong>{isBusy ? "Enviando foto..." : "Tirar foto"}</strong><small>Abra a câmera traseira</small></span>
                      <input className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" capture="environment" disabled={isBusy || uploading !== null} aria-label={`Tirar foto de ${group.name} — ${item.label}`} onChange={(event) => chooseFile(item.code, event)} />
                    </label>
                    <label className="capture-action">
                      <Images aria-hidden="true" size={23} />
                      <span><strong>Escolher da galeria</strong><small>JPEG, PNG ou WebP · até 10 MB</small></span>
                      <input className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" disabled={isBusy || uploading !== null} aria-label={`Escolher da galeria para ${group.name} — ${item.label}`} onChange={(event) => chooseFile(item.code, event)} />
                    </label>
                    {retryFiles[item.code] ? <button className="retry-link" disabled={isBusy} onClick={() => void sendFile(item.code, retryFiles[item.code]!)}><RefreshCw size={15} />Tentar novamente</button> : null}
                  </div>
                ) : null}

                {evidence.length ? (
                  <section className="evidence-section" aria-label={`Fotos salvas de ${group.name} — ${item.label}`}>
                    <div className="evidence-section__heading"><CheckCircle2 size={18} /><strong>{evidence.length} {evidence.length === 1 ? "foto salva" : "fotos salvas"}</strong></div>
                    <div className="evidence-strip">{evidence.map((photo, evidenceIndex) => <EvidenceImage evidence={photo} alt={`${group.name} — ${item.label}, foto ${evidenceIndex + 1}`} key={photo.id} />)}</div>
                  </section>
                ) : <p className="empty-evidence">Nenhuma foto salva para este item.</p>}
                {itemErrors[item.code] ? <p className="item-error" role="alert">{itemErrors[item.code]}</p> : null}

                <footer className="protocol-pager">
                  <button className="button button--secondary" type="button" disabled={!previous} onClick={() => selectRelative(-1)}><ArrowLeft size={17} />{previous ? "Anterior" : "Primeiro item"}</button>
                  <span className="mono">{String(index + 1).padStart(2, "0")} / {PROTOCOL_ITEM_TOTAL}</span>
                  <button className="button button--primary" type="button" disabled={!next} onClick={() => selectRelative(1)}>{next ? "Próximo item" : "Roteiro percorrido"}<ArrowRight size={17} /></button>
                </footer>
              </article>
            );
          })}
        </section>

        <aside className="submission-guide" aria-label="Orientações antes do envio">
          <section><FileCheck2 size={24} /><div><h2>Antes de analisar</h2><p>Você não precisa completar os 12 itens, mas fotos variadas deixam o registro mais útil.</p></div></section>
          <ul className="submission-checklist">
            <li className={inspection.imagens.length > 0 ? "is-complete" : ""}><CheckCircle2 size={18} />{inspection.imagens.length} {inspection.imagens.length === 1 ? "foto salva" : "fotos salvas"}</li>
            <li className={progress > 1 ? "is-complete" : ""}><span>{progress}</span>{progress === 1 ? "item coberto" : "itens cobertos"}</li>
          </ul>
          <section className="next-steps"><Info size={22} /><div><h2>Depois do envio</h2><ol><li><strong>A IA organiza indícios</strong><span>Você pode sair durante a análise.</span></li><li><strong>Você confirma o contexto</strong><span>Nenhum achado vira conclusão silenciosamente.</span></li><li><strong>O relatório é montado</strong><span>Foto e decisão permanecem ligadas.</span></li></ol></div></section>
          <div className="professional-warning"><AlertTriangle size={19} /><p><strong>Limite claro</strong>A análise visual não constitui laudo técnico ou diagnóstico estrutural.</p></div>
          {editable ? (
            <div className="workflow-submit">
              <p>Envie quando as evidências representarem bem o imóvel.</p>
              {submitError ? <p className="item-error" role="alert">{submitError}</p> : null}
              <button className="button button--primary" disabled={submitting || uploading !== null} onClick={() => void sendInspection()}><Send size={17} />{submitting ? "Enviando..." : "Enviar para análise da IA"}</button>
              <small>A resposta é assíncrona; não feche esta tela esperando uma porcentagem.</small>
            </div>
          ) : null}
        </aside>
      </div>
    </div>
  );
}

function Journey({ status }: { status: InspectionStatus }) {
  return (
    <nav className="journey-steps" aria-label="Etapas da vistoria">
      <ol>{journeySteps.map(([title, description], index) => {
        const state = getJourneyStepState(status, index);
        return (
          <li className={state === "complete" ? "is-complete" : state === "active" ? "is-active" : ""} aria-current={state === "active" ? "step" : undefined} key={title}>
            <span>{state === "complete" ? <Check size={16} /> : index + 1}</span>
            <div><strong>{title}</strong><small>{description}</small></div>
          </li>
        );
      })}</ol>
    </nav>
  );
}

function WorkflowBackHeader({ inspection }: { inspection: Inspection }) {
  return (
    <div className="workflow-titlebar workflow-titlebar--compact">
      <Link className="back-link" href="/client"><ArrowLeft size={17} />Voltar ao início</Link>
      <StatusBadge status={inspection.status} />
    </div>
  );
}
