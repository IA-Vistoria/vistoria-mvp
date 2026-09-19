"use client";

import Link from "next/link";
import { ArrowLeft, ArrowRight, ClipboardCheck, MapPin } from "lucide-react";
import { useEffect, useState } from "react";

import { AsyncState } from "@/components/ui/async-state";
import { StatusBadge } from "@/components/ui/status-badge";
import { listPendingInspections } from "../api";
import type { Inspection, PageResponse } from "../types";

const byNewest = (a: Inspection, b: Inspection) => {
  const difference = Date.parse(b.dataCriacao) - Date.parse(a.dataCriacao);
  return difference || b.id - a.id;
};

export function EngineerDashboard() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<Inspection> | null>(null);
  const [failed, setFailed] = useState(false);

  async function load(targetPage: number) {
    try {
      const loaded = await listPendingInspections(targetPage);
      setData(loaded);
      setFailed(false);
    } catch {
      setFailed(true);
    }
  }

  useEffect(() => {
    let active = true;
    listPendingInspections(page)
      .then((loaded) => {
        if (!active) return;
        setData(loaded);
        setFailed(false);
      })
      .catch(() => {
        if (active) setFailed(true);
      });
    return () => {
      active = false;
    };
  }, [page]);

  if (failed) return <AsyncState role="alert" title="Não foi possível carregar a fila" description="Tente consultar novamente. Nenhuma decisão técnica foi alterada." action={<button className="button button--secondary" onClick={() => void load(page)}>Tentar novamente</button>} />;
  if (data === null) return <AsyncState title="Carregando fila técnica" description="Buscando os casos que aguardam revisão profissional." />;
  if (data.totalElementos === 0) return <AsyncState eyebrow="Fila atualizada" title="Nenhuma vistoria aguarda revisão" description="Novos casos aparecerão aqui depois da pré-análise." />;

  const queue = [...data.content].sort(byNewest);

  return (
    <main className="engineer-dashboard">
      <header className="engineer-heading">
        <div><p className="eyebrow">Central técnica</p><h1>Fila de revisão</h1><p>Casos prontos para decisão do engenheiro civil.</p></div>
        <div className="queue-count"><ClipboardCheck size={21} /><span><strong>{data.totalElementos}</strong> {data.totalElementos === 1 ? "caso pendente" : "casos pendentes"}</span></div>
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

      {data.totalPaginas > 1 ? (
        <nav className="pagination" aria-label="Paginação da fila">
          <button className="button button--secondary" disabled={data.pagina === 0} onClick={() => setPage((current) => current - 1)}>
            <ArrowLeft size={17} />Anterior
          </button>
          <span>Página {data.pagina + 1} de {data.totalPaginas}</span>
          <button className="button button--secondary" disabled={data.pagina + 1 >= data.totalPaginas} onClick={() => setPage((current) => current + 1)}>
            Próxima<ArrowRight size={17} />
          </button>
        </nav>
      ) : null}
    </main>
  );
}

