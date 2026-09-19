"use client";

import Link from "next/link";
import { ArrowLeft, ArrowRight, Building2, Plus } from "lucide-react";
import { useEffect, useState } from "react";

import { AsyncState } from "@/components/ui/async-state";
import { StatusBadge } from "@/components/ui/status-badge";
import { listMyInspections } from "../api";
import { inspectionStatus } from "../status";
import type { Inspection, PageResponse } from "../types";

const byNewest = (a: Inspection, b: Inspection) => {
  const dateDifference = Date.parse(b.dataCriacao) - Date.parse(a.dataCriacao);
  return dateDifference || b.id - a.id;
};

export function ClientDashboard() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<Inspection> | null>(null);
  const [error, setError] = useState(false);

  async function load(targetPage: number) {
    try {
      const loaded = await listMyInspections(targetPage);
      setData(loaded);
      setError(false);
    } catch {
      setData(null);
      setError(true);
    }
  }

  useEffect(() => {
    let active = true;
    listMyInspections(page)
      .then((loaded) => {
        if (!active) return;
        setData(loaded);
        setError(false);
      })
      .catch(() => {
        if (!active) return;
        setData(null);
        setError(true);
      });
    return () => {
      active = false;
    };
  }, [page]);

  if (error) {
    return (
      <AsyncState
        role="alert"
        eyebrow="Conexão interrompida"
        title="Não foi possível carregar suas vistorias"
        description="Seus dados continuam seguros. Tente consultar novamente."
        action={<button className="button button--secondary" onClick={() => void load(page)}>Tentar novamente</button>}
      />
    );
  }

  if (data === null) {
    return <AsyncState title="Carregando vistorias" description="Estamos organizando seus imóveis e seus próximos passos." />;
  }

  if (data.totalElementos === 0) {
    return (
      <AsyncState
        eyebrow="Sua primeira vistoria"
        title="Nenhuma vistoria iniciada"
        description="Cadastre o endereço do imóvel para abrir um rascunho e registrar as evidências com calma."
        action={<Link className="button button--primary" href="/client/vistorias/nova"><Plus size={18} />Iniciar primeira vistoria</Link>}
      />
    );
  }

  const inspections = [...data.content].sort(byNewest);

  return (
    <main className="dashboard-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Visão do proprietário</p>
          <h1>Suas vistorias, sem perder o fio da obra.</h1>
          <p>Acompanhe cada imóvel e avance somente no passo que o processo realmente exige.</p>
        </div>
        <Link className="button button--primary" href="/client/vistorias/nova"><Plus size={18} />Nova vistoria</Link>
      </header>

      <section className="inspection-summary" aria-label="Resumo das vistorias">
        <div><Building2 size={22} /><span><strong>{data.totalElementos}</strong> {data.totalElementos === 1 ? "imóvel cadastrado" : "imóveis cadastrados"}</span></div>
      </section>

      <section className="inspection-grid" aria-label="Minhas vistorias">
        {inspections.map((inspection) => {
          const status = inspectionStatus[inspection.status];
          return (
            <article className="inspection-card" data-testid="inspection-card" key={inspection.id}>
              <div className="inspection-card__top">
                <span>Vistoria #{inspection.id}</span>
                <StatusBadge status={inspection.status} />
              </div>
              <h2>{inspection.endereco}</h2>
              <p>Iniciada em {new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" }).format(new Date(inspection.dataCriacao))}</p>
              <Link href={`/client/vistorias/${inspection.id}`}>{status.action}<ArrowRight size={17} /></Link>
            </article>
          );
        })}
      </section>

      {data.totalPaginas > 1 ? (
        <nav className="pagination" aria-label="Paginação de vistorias">
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
