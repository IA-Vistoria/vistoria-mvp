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
import { useEffect, useRef, useState } from "react";

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

function ReportList({ reports }: { reports: Inspection[] }) {
  return (
    <div className="report-list">
      {reports.map((inspection) => (
        <article className="report-row" key={inspection.id}>
          <FileText aria-hidden="true" size={22} />
          <div><strong>{inspection.endereco}</strong><span className="mono">VISTORIA {String(inspection.id).padStart(4, "0")} · {formatDate(inspection.dataConclusao ?? inspection.dataCriacao)}</span></div>
          <Link href={`/client/vistorias/${inspection.id}`}>Ver relatório<ArrowRight size={17} /></Link>
        </article>
      ))}
    </div>
  );
}

interface ClientDashboardProps {
  view?: "overview" | "reports";
}

const FLOW_STEPS = [
  { title: "Defina os ambientes", description: "Inclua somente os espaços que existem no imóvel." },
  { title: "Registre as fotos", description: "Envie uma visão geral por ambiente e detalhes quando precisar." },
  { title: "Receba a análise da IA", description: "Cada ambiente recebe uma conclusão, um motivo e os achados identificados." },
  { title: "Consulte ou se manifeste", description: "O relatório preserva a análise original e qualquer manifestação opcional." },
];

function DashboardIntroduction({ showAction = true }: { showAction?: boolean }) {
  return (
    <>
      <header className="page-heading dashboard-heading">
        <div>
          <h1>Acompanhe suas vistorias</h1>
          <p>Continue registros em andamento, acompanhe o resultado da IA e acesse relatórios concluídos.</p>
        </div>
        {showAction ? <Link className="button button--primary" href="/client/vistorias/nova"><Plus size={18} />Nova vistoria</Link> : null}
      </header>

      <section className="dashboard-flow" aria-labelledby="dashboard-flow-title">
        <div className="dashboard-flow__intro">
          <h2 id="dashboard-flow-title">Como o relatório é produzido</h2>
          <p>Você registra o imóvel e as evidências. A IA analisa as fotos, define o resultado e explica os motivos sem depender de aprovação humana.</p>
        </div>
        <ol>
          {FLOW_STEPS.map((step, index) => (
            <li key={step.title}>
              <span aria-hidden="true">{index + 1}</span>
              <div><strong>{step.title}</strong><small>{step.description}</small></div>
            </li>
          ))}
        </ol>
      </section>
    </>
  );
}

export function ClientDashboard({ view = "overview" }: ClientDashboardProps) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<Inspection> | null>(null);
  const [error, setError] = useState(false);
  const reportsHeadingRef = useRef<HTMLHeadingElement>(null);
  const statusFilter = view === "reports" ? "RELATORIO_DISPONIVEL" : undefined;

  async function load(targetPage: number) {
    try {
      const loaded = await listMyInspections(targetPage, 10, statusFilter);
      setData(loaded);
      setError(false);
    } catch {
      setData(null);
      setError(true);
    }
  }

  useEffect(() => {
    let active = true;
    listMyInspections(page, 10, statusFilter)
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
  }, [page, statusFilter]);

  useEffect(() => {
    if (view === "reports" && data !== null) {
      reportsHeadingRef.current?.focus();
    }
  }, [data, view]);

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
    if (view === "reports") {
      return (
        <AsyncState
          eyebrow="Documentos"
          title="Nenhum relatório disponível"
          description="Quando uma vistoria for concluída, o relatório organizado por ambiente aparecerá aqui."
          action={<Link className="button button--secondary" href="/client"><ArrowLeft size={17} />Voltar à visão geral</Link>}
        />
      );
    }
    return (
      <div className="dashboard-page">
        <DashboardIntroduction showAction={false} />
        <AsyncState
          eyebrow="Sua primeira vistoria"
          title="Nenhuma vistoria iniciada"
          description="Transforme fotos, contexto e relatório em um registro claro do imóvel. Você começa pelo endereço e avança no seu ritmo."
          action={<Link className="button button--primary" href="/client/vistorias/nova"><Plus size={18} />Iniciar primeira vistoria</Link>}
        />
      </div>
    );
  }

  const inspections = [...data.content].sort(byNewest);
  const primary = inspections[0];
  const presentation = inspectionStatus[primary.status];
  const allReports = inspections.filter((inspection) => inspection.status === "RELATORIO_DISPONIVEL");
  const reports = inspections.filter((inspection) =>
    inspection.status === "RELATORIO_DISPONIVEL" && inspection.id !== primary.id,
  );
  const history = inspections.slice(1).filter((inspection) => inspection.status !== "RELATORIO_DISPONIVEL");
  const environments = primary.ambientes ?? [];
  const environmentsWithOverview = new Set(
    primary.imagens
      .filter((evidence) => evidence.categoria === "VISAO_GERAL" && evidence.ambienteId !== null)
      .map((evidence) => evidence.ambienteId),
  ).size;

  if (view === "reports") {
    return (
      <div className="dashboard-page">
        <header className="page-heading dashboard-heading">
          <div>
            <Link className="back-link" href="/client"><ArrowLeft size={17} />Voltar à visão geral</Link>
            <p className="eyebrow">Documentos</p>
            <h1 ref={reportsHeadingRef} tabIndex={-1}>Seus relatórios</h1>
            <p>Consulte os registros concluídos, organizados pelos ambientes reais de cada imóvel.</p>
          </div>
          <Link className="button button--primary" href="/client/vistorias/nova"><Plus size={18} />Nova vistoria</Link>
        </header>

        {allReports.length > 0 ? (
          <section className="dashboard-section" aria-labelledby="all-reports-title">
            <header className="section-heading">
              <div><p className="eyebrow">Disponíveis agora</p><h2 id="all-reports-title">Relatórios concluídos</h2></div>
              <FileCheck2 aria-hidden="true" size={24} />
            </header>
            <ReportList reports={allReports} />
          </section>
        ) : (
          <AsyncState
            title="Nenhum relatório disponível"
            description="As vistorias em andamento continuam na visão geral."
            action={<Link className="button button--secondary" href="/client">Acompanhar vistorias</Link>}
          />
        )}

        {data.totalPaginas > 1 ? (
          <nav className="pagination" aria-label="Paginação de relatórios">
            <button className="button button--secondary" disabled={data.pagina === 0} onClick={() => setPage((current) => current - 1)}><ArrowLeft size={17} />Anterior</button>
            <span>Página {data.pagina + 1} de {data.totalPaginas}</span>
            <button className="button button--secondary" disabled={data.pagina + 1 >= data.totalPaginas} onClick={() => setPage((current) => current + 1)}>Próxima<ArrowRight size={17} /></button>
          </nav>
        ) : null}
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <DashboardIntroduction />

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
          {environments.length > 0 ? (
            <div className="next-action-card__progress">
              <span>{environmentsWithOverview} de {environments.length} ambientes com visão geral</span>
              <progress value={environmentsWithOverview} max={environments.length}>
                {environmentsWithOverview} de {environments.length}
              </progress>
            </div>
          ) : null}
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
          <ReportList reports={reports} />
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
