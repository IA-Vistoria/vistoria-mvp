"use client";

import Link from "next/link";
import { ArrowRight, ClipboardCheck, MapPin } from "lucide-react";
import { useEffect, useState } from "react";

import { AsyncState } from "@/components/ui/async-state";
import { StatusBadge } from "@/components/ui/status-badge";
import { listPendingInspections } from "../api";
import type { Inspection } from "../types";

const byNewest = (a: Inspection, b: Inspection) => {
  const difference = Date.parse(b.dataCriacao) - Date.parse(a.dataCriacao);
  return difference || b.id - a.id;
};

export function EngineerDashboard() {
  const [queue, setQueue] = useState<Inspection[] | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    listPendingInspections()
      .then((items) => {
        if (!active) return;
        setQueue([...items].sort(byNewest));
        setFailed(false);
      })
      .catch(() => {
        if (active) setFailed(true);
      });
    return () => {
      active = false;
    };
  }, []);

  async function retry() {
    try {
      setQueue([...(await listPendingInspections())].sort(byNewest));
      setFailed(false);
    } catch {
      setFailed(true);
    }
  }

  if (failed) return <AsyncState role="alert" title="Não foi possível carregar a fila" description="Tente consultar novamente. Nenhuma decisão técnica foi alterada." action={<button className="button button--secondary" onClick={() => void retry()}>Tentar novamente</button>} />;
  if (queue === null) return <AsyncState title="Carregando fila técnica" description="Buscando os casos que aguardam revisão profissional." />;
  if (queue.length === 0) return <AsyncState eyebrow="Fila atualizada" title="Nenhuma vistoria aguarda revisão" description="Novos casos aparecerão aqui depois da pré-análise." />;

  return (
    <main className="engineer-dashboard">
      <header className="engineer-heading">
        <div><p className="eyebrow">Central técnica</p><h1>Fila de revisão</h1><p>Casos prontos para decisão do engenheiro civil.</p></div>
        <div className="queue-count"><ClipboardCheck size={21} /><span><strong>{queue.length}</strong> {queue.length === 1 ? "caso pendente" : "casos pendentes"}</span></div>
      </header>
      <section className="review-list" aria-label="Vistorias pendentes">
        {queue.map((inspection) => (
          <article className="review-card" data-testid="review-card" key={inspection.id}>
            <div className="review-card__identity"><span>#{inspection.id}</span><div><h2>{inspection.endereco}</h2><p><MapPin size={14} />Cliente #{inspection.clienteId} · Recebida em {new Intl.DateTimeFormat("pt-BR").format(new Date(inspection.dataCriacao))}</p></div></div>
            <StatusBadge status={inspection.status} />
            <span className="review-card__evidence">{inspection.imagens.length} {inspection.imagens.length === 1 ? "evidência" : "evidências"}</span>
            <Link href={`/engineer/vistorias/${inspection.id}`} aria-label={`Revisar vistoria #${inspection.id}`}>Revisar caso<ArrowRight size={17} /></Link>
          </article>
        ))}
      </section>
    </main>
  );
}

