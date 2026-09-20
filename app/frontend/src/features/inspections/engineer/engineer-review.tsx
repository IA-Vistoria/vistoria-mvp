"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  ChevronRight,
  MapPin,
  RotateCcw,
  ShieldCheck,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { AsyncState } from "@/components/ui/async-state";
import { StatusBadge } from "@/components/ui/status-badge";
import { ApiError } from "@/lib/api";
import { getPendingInspection, listPendingInspections, reviewInspection } from "../api";
import { EvidenceImage } from "../shared/evidence-image";
import { PROTOCOL_GROUPS } from "../shared/protocol";
import type { Evidence, Inspection, ProtocolItemCode } from "../types";
import { PreReport } from "./pre-report";

const protocolLabels = new Map<ProtocolItemCode, string>(
  PROTOCOL_GROUPS.flatMap((group) => group.items.map((item) => [item.code, `${group.name} — ${item.label}`] as const)),
);

const byNewest = (a: Inspection, b: Inspection) => {
  const difference = Date.parse(b.dataCriacao) - Date.parse(a.dataCriacao);
  return difference || b.id - a.id;
};

function evidenceLabel(evidence: Evidence) {
  return protocolLabels.get(evidence.protocoloItem) || evidence.protocoloItem;
}

export function EngineerReview({ inspectionId }: { inspectionId: number }) {
  const router = useRouter();
  const [inspection, setInspection] = useState<Inspection | null>(null);
  const [queue, setQueue] = useState<Inspection[]>([]);
  const [queueTotal, setQueueTotal] = useState(0);
  const [selectedEvidenceId, setSelectedEvidenceId] = useState<number | null>(null);
  const [opinion, setOpinion] = useState("");
  const [loadFailed, setLoadFailed] = useState(false);
  const [decisionError, setDecisionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [processed, setProcessed] = useState(false);
  const decisionLock = useRef(false);

  useEffect(() => {
    let active = true;
    getPendingInspection(inspectionId)
      .then((loaded) => {
        if (!active) return;
        setInspection(loaded);
        setSelectedEvidenceId(loaded.imagens[0]?.id ?? null);
      })
      .catch(() => {
        if (active) setLoadFailed(true);
      });
    listPendingInspections()
      .then((page) => {
        if (!active) return;
        setQueue([...page.content].sort(byNewest));
        setQueueTotal(page.totalElementos);
      })
      .catch(() => {
        if (active) setQueue([]);
      });
    return () => {
      active = false;
    };
  }, [inspectionId]);

  async function retry() {
    try {
      const loaded = await getPendingInspection(inspectionId);
      setInspection(loaded);
      setSelectedEvidenceId(loaded.imagens[0]?.id ?? null);
      setLoadFailed(false);
    } catch {
      setLoadFailed(true);
    }
  }

  async function decide(approved: boolean) {
    if (!inspection || !opinion.trim() || decisionLock.current) return;
    decisionLock.current = true;
    setBusy(true);
    setDecisionError(null);
    try {
      const updated = await reviewInspection(inspection.id, approved, opinion.trim());
      setInspection(updated);
      if (!approved) router.replace("/engineer");
    } catch (cause) {
      if (cause instanceof ApiError && cause.problem.status === 409) {
        await listPendingInspections().catch(() => undefined);
        setProcessed(true);
        setDecisionError("Este caso já foi processado.");
      } else {
        setDecisionError(cause instanceof ApiError ? cause.problem.detail : "Não foi possível registrar a decisão.");
        decisionLock.current = false;
      }
    } finally {
      setBusy(false);
    }
  }

  if (loadFailed) return <AsyncState role="alert" title="Caso indisponível" description="A vistoria pode ter saído da fila ou a conexão foi interrompida." action={<button className="button button--secondary" onClick={() => void retry()}>Atualizar fila</button>} />;
  if (!inspection) return <AsyncState title="Abrindo revisão técnica" description="Carregando evidências e pré-laudo do caso." />;

  const editable = inspection.status === "AGUARDANDO_ENGENHEIRO" && !processed;
  const selectedEvidence = inspection.imagens.find((item) => item.id === selectedEvidenceId) ?? inspection.imagens[0];

  return (
    <main className="engineer-review-page">
      <div className="review-breadcrumb"><Link href="/engineer">Fila de revisão</Link><ChevronRight size={15} /><span>Vistoria #{inspection.id}</span></div>
      <header className="review-heading">
        <div><p className="eyebrow">Revisão técnica · Cliente #{inspection.clienteId}</p><h1>Revisão da vistoria #{inspection.id}</h1><p>Analise as evidências, confira o pré-laudo e registre sua decisão profissional.</p></div>
        <StatusBadge status={inspection.status} />
      </header>

      {processed ? <div className="processed-notice" role="alert"><AlertTriangle size={20} /><span>{decisionError}</span></div> : null}
      {inspection.status === "CONCLUIDA" ? <div className="processed-notice processed-notice--success" aria-live="polite"><CheckCircle2 size={20} /><span>Vistoria concluída</span></div> : null}

      <div className="technical-workspace">
        <section className="review-queue" role="region" aria-label="Fila de revisão">
          <header><div><span>Fila de revisão</span><strong>{queueTotal}</strong></div><small>Mais recentes</small></header>
          <div>
            {queue.map((item) => (
              <Link className={item.id === inspection.id ? "is-active" : ""} href={`/engineer/vistorias/${item.id}`} aria-current={item.id === inspection.id ? "page" : undefined} aria-label={`Vistoria #${item.id} — ${item.endereco}`} key={item.id}>
                <strong>VP-{String(item.id).padStart(4, "0")}</strong>
                <span>{item.endereco}</span>
                <small>Cliente #{item.clienteId} · {new Intl.DateTimeFormat("pt-BR").format(new Date(item.dataCriacao))}</small>
              </Link>
            ))}
          </div>
        </section>

        <section className="review-case" aria-label="Conteúdo da vistoria">
          <header className="case-summary">
            <div><h2>{inspection.endereco}</h2><p><MapPin size={15} />Recebida em {new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" }).format(new Date(inspection.dataCriacao))}</p></div>
            <span>{inspection.imagens.length} {inspection.imagens.length === 1 ? "evidência" : "evidências"}</span>
          </header>

          <section className="evidence-panel" aria-labelledby="evidence-title">
            <div className="panel-heading"><p className="eyebrow">Protocolo fotográfico</p><h2 id="evidence-title">Evidências do imóvel</h2></div>
            {selectedEvidence ? (
              <figure className="evidence-main">
                <EvidenceImage evidence={selectedEvidence} alt={`Evidência em destaque: ${evidenceLabel(selectedEvidence)}`} />
                <figcaption>{evidenceLabel(selectedEvidence)}</figcaption>
              </figure>
            ) : <p className="empty-evidence">Nenhuma evidência disponível.</p>}
            <div className="evidence-gallery" aria-label="Selecionar evidência">
              {inspection.imagens.map((evidence, index) => {
                const label = evidenceLabel(evidence);
                return (
                  <button className={selectedEvidence?.id === evidence.id ? "is-active" : ""} type="button" aria-label={`Visualizar ${label}, evidência ${index + 1}`} onClick={() => setSelectedEvidenceId(evidence.id)} key={evidence.id}>
                    <EvidenceImage evidence={evidence} alt={`${label}, evidência ${index + 1}`} />
                  </button>
                );
              })}
            </div>
          </section>

          <PreReport value={inspection.preLaudoIa} />
        </section>

        <section className="decision-panel" aria-labelledby="decision-title">
          <aside className="professional-note"><ShieldCheck size={24} /><div><strong>A IA sugere. O engenheiro decide.</strong><span>Você responde pela validação técnica e pela decisão final.</span></div></aside>
          <p className="eyebrow">Responsabilidade profissional</p>
          <h2 id="decision-title">Decisão técnica</h2>
          <p>Registre a justificativa que ficará vinculada à vistoria.</p>
          <label htmlFor="technical-opinion">Parecer técnico</label>
          <textarea id="technical-opinion" rows={10} value={opinion} disabled={!editable || busy} onChange={(event) => setOpinion(event.target.value)} placeholder="Descreva sua avaliação técnica, pontos de atenção e orientações..." />
          {decisionError && !processed ? <p className="item-error" role="alert">{decisionError}</p> : null}
          {editable ? <div className="decision-actions"><button className="button button--primary" disabled={!opinion.trim() || busy} onClick={() => void decide(true)}><CheckCircle2 size={17} />{busy ? "Registrando..." : "Aprovar vistoria"}</button><button className="button button--secondary button--danger" disabled={!opinion.trim() || busy} onClick={() => void decide(false)}><RotateCcw size={17} />Devolver ao cliente</button></div> : null}
          <Link className="return-queue" href="/engineer"><ArrowLeft size={16} />Voltar para a fila completa</Link>
        </section>
      </div>
    </main>
  );
}
