"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { AlertTriangle, ArrowLeft, CheckCircle2, RotateCcw } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { AsyncState } from "@/components/ui/async-state";
import { StatusBadge } from "@/components/ui/status-badge";
import { ApiError } from "@/lib/api";
import { getPendingInspection, listPendingInspections, reviewInspection } from "../api";
import { EvidenceImage } from "../shared/evidence-image";
import { PROTOCOL_GROUPS } from "../shared/protocol";
import type { Inspection, ProtocolItemCode } from "../types";
import { PreReport } from "./pre-report";

const protocolLabels = new Map<ProtocolItemCode, string>(
  PROTOCOL_GROUPS.flatMap((group) => group.items.map((item) => [item.code, `${group.name} — ${item.label}`] as const)),
);

export function EngineerReview({ inspectionId }: { inspectionId: number }) {
  const router = useRouter();
  const [inspection, setInspection] = useState<Inspection | null>(null);
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
        if (active) setInspection(loaded);
      })
      .catch(() => {
        if (active) setLoadFailed(true);
      });
    return () => {
      active = false;
    };
  }, [inspectionId]);

  async function retry() {
    try {
      setInspection(await getPendingInspection(inspectionId));
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
        await listPendingInspections().catch(() => []);
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

  return (
    <main className="engineer-review-page">
      <Link className="back-link" href="/engineer"><ArrowLeft size={17} />Voltar para a fila</Link>
      <header className="review-heading">
        <div><p className="eyebrow">Caso #{inspection.id} · Cliente #{inspection.clienteId}</p><h1>{inspection.endereco}</h1><p>Recebida em {new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" }).format(new Date(inspection.dataCriacao))}</p></div>
        <StatusBadge status={inspection.status} />
      </header>

      {processed ? <div className="processed-notice" role="alert"><AlertTriangle size={20} /><span>{decisionError}</span></div> : null}
      {inspection.status === "CONCLUIDA" ? <div className="processed-notice processed-notice--success" aria-live="polite"><CheckCircle2 size={20} /><span>Vistoria concluída</span></div> : null}

      <div className="review-workspace">
        <section className="evidence-panel" aria-labelledby="evidence-title">
          <div className="panel-heading"><p className="eyebrow">Protocolo fotográfico</p><h2 id="evidence-title">Evidências</h2><span>{inspection.imagens.length} fotos</span></div>
          <div className="evidence-gallery">
            {inspection.imagens.map((evidence, index) => {
              const label = protocolLabels.get(evidence.protocoloItem) || evidence.protocoloItem;
              return <figure key={evidence.id}><EvidenceImage evidence={evidence} alt={`${label}, evidência ${index + 1}`} /><figcaption>{label}</figcaption></figure>;
            })}
          </div>
        </section>

        <PreReport value={inspection.preLaudoIa} />

        <section className="decision-panel" aria-labelledby="decision-title">
          <p className="eyebrow">Responsabilidade profissional</p>
          <h2 id="decision-title">Decisão técnica</h2>
          <p>Registre a justificativa que ficará vinculada à vistoria.</p>
          <label htmlFor="technical-opinion">Parecer técnico</label>
          <textarea id="technical-opinion" rows={9} value={opinion} disabled={!editable || busy} onChange={(event) => setOpinion(event.target.value)} placeholder="Descreva sua avaliação técnica..." />
          {decisionError && !processed ? <p className="item-error" role="alert">{decisionError}</p> : null}
          {editable ? <div className="decision-actions"><button className="button button--secondary" disabled={!opinion.trim() || busy} onClick={() => void decide(false)}><RotateCcw size={17} />Devolver ao cliente</button><button className="button button--primary" disabled={!opinion.trim() || busy} onClick={() => void decide(true)}><CheckCircle2 size={17} />{busy ? "Registrando..." : "Aprovar vistoria"}</button></div> : null}
        </section>
      </div>
    </main>
  );
}

