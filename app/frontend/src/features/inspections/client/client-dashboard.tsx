"use client";

import {
  ArrowLeft,
  ArrowRight,
  Camera,
  FileCheck2,
  FileText,
  Plus,
  ScanSearch,
} from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

import { AsyncState } from "@/components/ui/async-state";
import { StatusBadge } from "@/components/ui/status-badge";
import { listMyInspections } from "../api";
import { inspectionStatus } from "../status";
import type { Inspection, InspectionStatus, PageResponse } from "../types";

const byNewest = (a: Inspection, b: Inspection) => {
  const dateDifference = Date.parse(b.dataCriacao) - Date.parse(a.dataCriacao);
  return dateDifference || b.id - a.id;
};

const nextActionCopy: Record<InspectionStatus, string> = {
  EM_RASCUNHO: "Continue pelas fotos. Seu progresso confirmado já está salvo.",
  DEVOLVIDA_CLIENTE: "Há evidências a complementar antes de uma nova análise.",
  AGUARDANDO_IA: "A IA está organizando os indícios visuais. Você pode sair e voltar depois.",
  FALHA_IA: "As fotos estão salvas. Reabra a vistoria para tentar a análise novamente.",
  REVISAO_PENDENTE: "A análise terminou. Agora confirme o contexto de cada achado.",
  RELATORIO_DISPONIVEL: "Seu relatório está organizado e pronto para consultar ou imprimir.",
  AGUARDANDO_ENGENHEIRO: "Este registro pertence ao fluxo anterior e continua disponível para consulta.",
  CONCLUIDA: "Este resultado histórico continua disponível para consulta.",
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium" }).format(new Date(value));
}

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
    return <AsyncState title="Carregando seu espaço" description="Organizando vistorias, revisões e relatórios." />;
  }

  if (data.totalElementos === 0) {
    return (
      <AsyncState
        eyebrow="Sua primeira vistoria"
        title="Nenhuma vistoria iniciada"
        description="Transforme fotos, contexto e relatório em um registro claro do imóvel. Você começa pelo endereço e avança no seu ritmo."
        action={<Link className="button button--primary" href="/client/vistorias/nova"><Plus size={18} />Iniciar primeira vistoria</Link>}
      />
    );
  }

  const inspections = [...data.content].sort(byNewest);
  const primary = inspections[0];
  const presentation = inspectionStatus[primary.status];
  const reports = inspections.filter((inspection) =>
    inspection.status === "RELATORIO_DISPONIVEL" && inspection.id !== primary.id,
  );
  const history = inspections.slice(1).filter((inspection) => inspection.status !== "RELATORIO_DISPONIVEL");

  return (
    <div className="dashboard-page">
      <header className="page-heading dashboard-heading">
        <div>
          <p className="eyebrow">Visão geral</p>
          <h1>O próximo passo, sem ruído.</h1>
          <p>Capture evidências, confirme o contexto e acompanhe seu Relatório de vistoria por IA.</p>
        </div>
        <Link className="button button--primary" href="/client/vistorias/nova"><Plus size={18} />Nova vistoria</Link>
      </header>

      <section className="next-action-card" data-testid="next-action" aria-labelledby="next-action-title">
        <div className="next-action-card__icon" aria-hidden="true">
          {primary.status === "EM_RASCUNHO" ? <Camera size={26} /> : <ScanSearch size={26} />}
        </div>
        <div className="next-action-card__content">
          <div className="next-action-card__meta">
            <span className="mono">VISTORIA {String(primary.id).padStart(4, "0")}</span>
            <StatusBadge status={primary.status} />
          </div>
          <p className="eyebrow">Sua próxima ação</p>
          <h2 id="next-action-title">{primary.endereco}</h2>
          <p>{nextActionCopy[primary.status]}</p>
          <span className="next-action-card__date">Iniciada em {formatDate(primary.dataCriacao)}</span>
        </div>
        <Link className="button button--primary" href={`/client/vistorias/${primary.id}`}>
          {presentation.action}<ArrowRight size={18} />
        </Link>
      </section>

      {reports.length > 0 ? (
        <section className="dashboard-section" aria-labelledby="available-reports-title">
          <header className="section-heading">
            <div><p className="eyebrow">Documentos recentes</p><h2 id="available-reports-title">Relatórios disponíveis</h2></div>
            <FileCheck2 aria-hidden="true" size={24} />
          </header>
          <div className="report-list">
            {reports.map((inspection) => (
              <article className="report-row" key={inspection.id}>
                <FileText aria-hidden="true" size={22} />
                <div><strong>{inspection.endereco}</strong><span className="mono">VISTORIA {String(inspection.id).padStart(4, "0")} · {formatDate(inspection.dataConclusao ?? inspection.dataCriacao)}</span></div>
                <Link href={`/client/vistorias/${inspection.id}`}>Ver relatório<ArrowRight size={17} /></Link>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {history.length > 0 ? (
        <section className="dashboard-section" aria-labelledby="history-title">
          <header className="section-heading"><div><p className="eyebrow">Outros registros</p><h2 id="history-title">Histórico recente</h2></div></header>
          <div className="inspection-grid" aria-label="Outras vistorias">
            {history.map((inspection) => {
              const status = inspectionStatus[inspection.status];
              return (
                <article className="inspection-card" data-testid="inspection-card" key={inspection.id}>
                  <div className="inspection-card__top"><span className="mono">VISTORIA {String(inspection.id).padStart(4, "0")}</span><StatusBadge status={inspection.status} /></div>
                  <h3>{inspection.endereco}</h3>
                  <p>Iniciada em {formatDate(inspection.dataCriacao)}</p>
                  <Link href={`/client/vistorias/${inspection.id}`}>{status.action}<ArrowRight size={17} /></Link>
                </article>
              );
            })}
          </div>
        </section>
      ) : null}

      {data.totalPaginas > 1 ? (
        <nav className="pagination" aria-label="Paginação de vistorias">
          <button className="button button--secondary" disabled={data.pagina === 0} onClick={() => setPage((current) => current - 1)}><ArrowLeft size={17} />Anterior</button>
          <span>Página {data.pagina + 1} de {data.totalPaginas}</span>
          <button className="button button--secondary" disabled={data.pagina + 1 >= data.totalPaginas} onClick={() => setPage((current) => current + 1)}>Próxima<ArrowRight size={17} /></button>
        </nav>
      ) : null}
    </div>
  );
}
