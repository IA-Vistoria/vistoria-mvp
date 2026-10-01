"use client";

import {
  AlertTriangle, ArrowLeft, ArrowRight, Camera, Check, CheckCircle2,
  FileCheck2, Images, Info, ListRestart, PencilLine, RefreshCw,
  ScanSearch, Send, Sparkles,
} from "lucide-react";
import Link from "next/link";
import { ChangeEvent, useEffect, useMemo, useRef, useState } from "react";

import { AsyncState } from "@/components/ui/async-state";
import { StatusBadge } from "@/components/ui/status-badge";
import { ApiError } from "@/lib/api";
import { getMyInspection, submitInspection, updateInspectionRoute, uploadEvidence } from "../api";
import { EvidenceImage } from "../shared/evidence-image";
import { validateEvidenceFile } from "../shared/protocol";
import type { EvidenceCategory, Inspection, InspectionEnvironment, InspectionStatus } from "../types";
import { InspectionReport } from "./inspection-report";
import { InspectionReview } from "./inspection-review";
import { InspectionResults } from "./inspection-results";
import {
  EnvironmentRouteEditor, createEnvironmentDrafts, validateEnvironmentDrafts,
  type EnvironmentDraft,
} from "./route-builder";

const POLLING_INTERVAL_MS = 4000;
const POLLED_STATUSES: ReadonlySet<InspectionStatus> = new Set(["AGUARDANDO_IA"]);
const journeySteps = [
  ["Imóvel", "Roteiro salvo"],
  ["Evidências", "Fotos por ambiente"],
  ["Revisão", "Contexto humano"],
  ["Relatório", "Documento final"],
] as const;

type JourneyStepState = "complete" | "active" | "pending";
type UploadKey = `${number}-${EvidenceCategory}`;

function getJourneyStepState(status: InspectionStatus, index: number): JourneyStepState {
  if (["EM_RASCUNHO", "DEVOLVIDA_CLIENTE", "FALHA_IA"].includes(status)) {
    if (index === 0) return "complete";
    return index === 1 ? "active" : "pending";
  }
  if (["AGUARDANDO_IA", "REVISAO_PENDENTE"].includes(status)) {
    return index < 2 ? "complete" : index === 2 ? "active" : "pending";
  }
  if (["RELATORIO_DISPONIVEL", "CONCLUIDA"].includes(status)) return "complete";
  return index < 2 ? "complete" : index === 2 ? "active" : "pending";
}

function orderedEnvironments(inspection: Inspection): InspectionEnvironment[] {
  return [...inspection.ambientes].sort((left, right) => left.ordem - right.ordem);
}

function overviewEnvironmentIds(inspection: Inspection): Set<number> {
  return new Set(inspection.imagens
    .filter((evidence) => evidence.categoria === "VISAO_GERAL" && evidence.ambienteId !== null)
    .map((evidence) => evidence.ambienteId!));
}

function firstIncompleteEnvironment(inspection: Inspection, afterId?: number): number | null {
  const environments = orderedEnvironments(inspection);
  if (environments.length === 0) return null;
  const covered = overviewEnvironmentIds(inspection);
  const start = afterId === undefined ? 0 : Math.max(0, environments.findIndex(({ id }) => id === afterId) + 1);
  const candidates = [...environments.slice(start), ...environments.slice(0, start)];
  return candidates.find(({ id }) => !covered.has(id))?.id ?? afterId ?? environments[0].id;
}

function missingEnvironmentNames(inspection: Inspection): string[] {
  const covered = overviewEnvironmentIds(inspection);
  return orderedEnvironments(inspection).filter(({ id }) => !covered.has(id)).map(({ nome }) => nome);
}

function formatMissingNames(names: string[]): string {
  if (names.length <= 1) return names[0] ?? "";
  return `${names.slice(0, -1).join(", ")} e ${names.at(-1)}`;
}

export function InspectionWorkflow({ inspectionId }: { inspectionId: number }) {
  const [inspection, setInspection] = useState<Inspection | null>(null);
  const [selectedEnvironmentId, setSelectedEnvironmentId] = useState<number | null>(null);
  const [loadError, setLoadError] = useState(false);
  const [uploading, setUploading] = useState<UploadKey | null>(null);
  const [roomErrors, setRoomErrors] = useState<Record<number, string | undefined>>({});
  const [retryFiles, setRetryFiles] = useState<Partial<Record<UploadKey, File>>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [editingRoute, setEditingRoute] = useState(false);
  const [routeDraft, setRouteDraft] = useState<EnvironmentDraft[]>([]);
  const [routeError, setRouteError] = useState<string | null>(null);
  const [savingRoute, setSavingRoute] = useState(false);
  const [reportView, setReportView] = useState<"report" | "analysis">("report");
  const submitLock = useRef(false);
  const routeFirstInputRef = useRef<HTMLInputElement>(null);
  const polledStatus = inspection?.status;

  useEffect(() => {
    const controller = new AbortController();
    getMyInspection(inspectionId, controller.signal)
      .then((loaded) => {
        setInspection(loaded);
        setSelectedEnvironmentId(firstIncompleteEnvironment(loaded));
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
        // Preserva o último estado confirmado e tenta novamente.
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

  const environments = useMemo(() => inspection ? orderedEnvironments(inspection) : [], [inspection]);
  const selectedIndex = Math.max(0, environments.findIndex(({ id }) => id === selectedEnvironmentId));

  async function reload() {
    try {
      const loaded = await getMyInspection(inspectionId);
      setInspection(loaded);
      setSelectedEnvironmentId(firstIncompleteEnvironment(loaded));
      setLoadError(false);
    } catch {
      setLoadError(true);
    }
  }

  async function sendFile(environmentId: number, category: EvidenceCategory, file: File) {
    const key: UploadKey = `${environmentId}-${category}`;
    setSelectedEnvironmentId(environmentId);
    const validationError = validateEvidenceFile(file);
    if (validationError) {
      setRoomErrors((current) => ({ ...current, [environmentId]: validationError }));
      return;
    }
    setUploading(key);
    setRoomErrors((current) => ({ ...current, [environmentId]: undefined }));
    try {
      const updated = await uploadEvidence(inspectionId, { ambienteId: environmentId, categoria: category, file });
      setInspection(updated);
      setRetryFiles((current) => ({ ...current, [key]: undefined }));
      if (category === "VISAO_GERAL") setSelectedEnvironmentId(firstIncompleteEnvironment(updated, environmentId));
    } catch (cause) {
      setRetryFiles((current) => ({ ...current, [key]: file }));
      setRoomErrors((current) => ({
        ...current,
        [environmentId]: cause instanceof ApiError ? cause.problem.detail : "Não foi possível enviar a foto.",
      }));
    } finally {
      setUploading(null);
    }
  }

  function chooseFile(environmentId: number, category: EvidenceCategory, event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (file) void sendFile(environmentId, category, file);
  }

  async function sendInspection() {
    if (!inspection || submitLock.current) return;
    const missing = missingEnvironmentNames(inspection);
    if (missing.length > 0) {
      const firstMissing = environments.find(({ nome }) => nome === missing[0]);
      setSelectedEnvironmentId(firstMissing?.id ?? selectedEnvironmentId);
      setSubmitError(`Ainda falta uma visão geral de ${formatMissingNames(missing)}.`);
      return;
    }
    if (inspection.ambientes.length === 0 && inspection.imagens.length === 0) {
      setSubmitError("Esta vistoria antiga ainda não possui evidências salvas.");
      return;
    }
    submitLock.current = true;
    setSubmitting(true);
    setSubmitError(null);
    try {
      setInspection(await submitInspection(inspection.id));
    } catch (cause) {
      if (cause instanceof ApiError && Array.isArray(cause.problem.ambientesAusentes)) {
        const absent = cause.problem.ambientesAusentes.filter((name): name is string => typeof name === "string");
        const firstMissing = environments.find(({ nome }) => nome === absent[0]);
        setSelectedEnvironmentId(firstMissing?.id ?? selectedEnvironmentId);
        setSubmitError(`Ainda falta uma visão geral de ${formatMissingNames(absent)}.`);
      } else {
        setSubmitError(cause instanceof ApiError ? cause.problem.detail : "Não foi possível enviar a vistoria.");
      }
      submitLock.current = false;
    } finally {
      setSubmitting(false);
    }
  }

  function startRouteEditing() {
    if (!inspection) return;
    setRouteDraft(createEnvironmentDrafts(inspection.ambientes));
    setRouteError(null);
    setEditingRoute(true);
  }

  async function saveRoute() {
    if (!inspection || inspection.tipoImovel === null) return;
    const validationError = validateEnvironmentDrafts(routeDraft);
    if (validationError) {
      setRouteError(validationError);
      routeFirstInputRef.current?.focus();
      return;
    }
    setSavingRoute(true);
    setRouteError(null);
    try {
      const updated = await updateInspectionRoute(inspection.id, {
        version: inspection.version,
        tipoImovel: inspection.tipoImovel,
        ambientes: routeDraft.map(({ id, tipo, nome }) => ({ id, tipo, nome })),
      });
      setInspection(updated);
      setSelectedEnvironmentId(firstIncompleteEnvironment(updated));
      setEditingRoute(false);
    } catch (cause) {
      setRouteError(cause instanceof ApiError ? cause.problem.detail : "Não foi possível salvar o roteiro.");
      routeFirstInputRef.current?.focus();
    } finally {
      setSavingRoute(false);
    }
  }

  function selectRelative(offset: number) {
    const next = environments[selectedIndex + offset];
    if (next) setSelectedEnvironmentId(next.id);
  }

  if (loadError) {
    return <AsyncState role="alert" title="Não foi possível abrir a vistoria" description="Tente carregar novamente sem criar outro rascunho." action={<button className="button button--secondary" onClick={() => void reload()}>Tentar novamente</button>} />;
  }
  if (!inspection) return <AsyncState title="Carregando roteiro" description="Buscando os ambientes e as evidências já salvas." />;
  if (inspection.status === "RELATORIO_DISPONIVEL" && reportView === "analysis") {
    return <div className="workflow-page"><WorkflowBackHeader inspection={inspection} /><InspectionReview inspection={inspection} onChange={setInspection} onRefresh={() => getMyInspection(inspection.id)} onOpenReport={() => setReportView("report")} /></div>;
  }
  if (inspection.status === "RELATORIO_DISPONIVEL") return <div className="workflow-page workflow-page--report"><WorkflowBackHeader inspection={inspection} /><InspectionReport inspection={inspection} onOpenAnalysis={() => setReportView("analysis")} /></div>;
  if (inspection.status === "CONCLUIDA") return <div className="workflow-page"><WorkflowBackHeader inspection={inspection} /><InspectionResults inspection={inspection} /></div>;
  if (inspection.status === "REVISAO_PENDENTE") return <div className="workflow-page"><WorkflowBackHeader inspection={inspection} /><InspectionReview inspection={inspection} onChange={setInspection} onRefresh={() => getMyInspection(inspection.id)} /></div>;
  if (inspection.status === "AGUARDANDO_IA") {
    return <ProcessingState inspection={inspection} />;
  }

  const editable = ["EM_RASCUNHO", "DEVOLVIDA_CLIENTE"].includes(inspection.status);
  const coveredEnvironmentIds = overviewEnvironmentIds(inspection);
  const completedCount = coveredEnvironmentIds.size;
  const lockedEnvironmentIds = new Set(inspection.imagens.filter((evidence) => evidence.ambienteId !== null).map((evidence) => evidence.ambienteId!));

  return (
    <div className="workflow-page workflow-page--adaptive">
      <div className="workflow-titlebar">
        <div>
          <Link className="back-link" href="/client"><ArrowLeft size={17} />Voltar ao início</Link>
          <p className="eyebrow">Captura orientada, roteiro flexível</p>
          <h1>Registre o imóvel ambiente por ambiente.</h1>
          <p>Uma visão geral é obrigatória em cada ambiente; detalhes são opcionais para o que merecer atenção.</p>
        </div>
        <StatusBadge status={inspection.status} />
      </div>
      <Journey status={inspection.status} />

      {inspection.status === "FALHA_IA" ? (
        <section className="tracking-panel tracking-panel--error" role="alert">
          <AlertTriangle size={28} />
          <div><p className="eyebrow">Falha no processamento</p><h2>A análise da IA não foi concluída</h2><p>Seu roteiro e suas fotos continuam salvos. Confira as evidências ou tente novamente.</p></div>
          <button className="button button--primary" disabled={submitting} onClick={() => void sendInspection()}>{submitting ? "Reenviando..." : "Reenviar para análise"}</button>
        </section>
      ) : null}

      {editingRoute ? (
        <section className="route-edit-panel" aria-labelledby="route-edit-title">
          <header>
            <div><p className="eyebrow">Ajuste em campo</p><h2 id="route-edit-title">Editar roteiro</h2><p>Ambientes com fotos podem ser renomeados ou movidos, mas não removidos.</p></div>
            <button className="button button--ghost" type="button" onClick={() => setEditingRoute(false)}>Cancelar</button>
          </header>
          {routeError ? <p className="form-alert" id="route-edit-error" role="alert">{routeError}</p> : null}
          <EnvironmentRouteEditor
            environments={routeDraft}
            onChange={(updated) => {
              setRouteDraft(updated);
              if (routeError) setRouteError(null);
            }}
            lockedEnvironmentIds={lockedEnvironmentIds}
            disabled={savingRoute}
            errorId="route-edit-error"
            invalid={routeError !== null}
            firstNameRef={routeFirstInputRef}
          />
          <button className="button button--primary" type="button" disabled={savingRoute} onClick={() => void saveRoute()}>{savingRoute ? "Salvando roteiro..." : "Salvar roteiro"}</button>
        </section>
      ) : inspection.ambientes.length === 0 ? (
        <LegacyInspection inspection={inspection} editable={editable} submitError={submitError} submitting={submitting} onSubmit={sendInspection} />
      ) : (
        <AdaptiveWorkspace
          inspection={inspection}
          environments={environments}
          selectedEnvironmentId={selectedEnvironmentId}
          coveredEnvironmentIds={coveredEnvironmentIds}
          completedCount={completedCount}
          editable={editable}
          uploading={uploading}
          roomErrors={roomErrors}
          retryFiles={retryFiles}
          submitError={submitError}
          submitting={submitting}
          onSelect={setSelectedEnvironmentId}
          onEditRoute={startRouteEditing}
          onChoose={chooseFile}
          onRetry={sendFile}
          onMove={selectRelative}
          onSubmit={sendInspection}
        />
      )}
    </div>
  );
}

interface AdaptiveWorkspaceProps {
  inspection: Inspection;
  environments: InspectionEnvironment[];
  selectedEnvironmentId: number | null;
  coveredEnvironmentIds: ReadonlySet<number>;
  completedCount: number;
  editable: boolean;
  uploading: UploadKey | null;
  roomErrors: Record<number, string | undefined>;
  retryFiles: Partial<Record<UploadKey, File>>;
  submitError: string | null;
  submitting: boolean;
  onSelect: (environmentId: number) => void;
  onEditRoute: () => void;
  onChoose: (environmentId: number, category: EvidenceCategory, event: ChangeEvent<HTMLInputElement>) => void;
  onRetry: (environmentId: number, category: EvidenceCategory, file: File) => Promise<void>;
  onMove: (offset: number) => void;
  onSubmit: () => Promise<void>;
}

function AdaptiveWorkspace(props: AdaptiveWorkspaceProps) {
  const {
    inspection, environments, selectedEnvironmentId,
    coveredEnvironmentIds, completedCount, editable, uploading,
    roomErrors, retryFiles, submitError, submitting,
    onSelect, onEditRoute, onChoose, onRetry, onMove, onSubmit,
  } = props;
  const detailCount = inspection.imagens.filter(({ categoria }) => categoria === "DETALHE").length;

  return (
    <div className="guided-workspace guided-workspace--adaptive">
      <aside className="environment-sidebar">
        <div className="property-summary">
          <p className="eyebrow">Imóvel</p>
          <strong>{inspection.endereco}</strong>
          <span className="mono">VISTORIA {String(inspection.id).padStart(4, "0")}</span>
        </div>
        <section className="workflow-progress" aria-label="Progresso da documentação">
          <div><strong>{completedCount} de {environments.length} ambientes com visão geral</strong><span>Somente visões gerais salvas contam como concluídas.</span></div>
          <progress value={completedCount} max={environments.length}>{completedCount} de {environments.length}</progress>
        </section>
        <nav className="environment-navigation" aria-label="Ambientes do roteiro">
          {environments.map((environment, index) => {
            const complete = coveredEnvironmentIds.has(environment.id);
            const details = inspection.imagens.filter((evidence) => evidence.ambienteId === environment.id && evidence.categoria === "DETALHE").length;
            return (
              <button
                className={selectedEnvironmentId === environment.id ? "is-active" : ""}
                type="button"
                aria-current={selectedEnvironmentId === environment.id ? "step" : undefined}
                onClick={() => onSelect(environment.id)}
                data-testid="environment-step"
                key={environment.id}
              >
                <span className={`protocol-check ${complete ? "is-complete" : ""}`} aria-hidden="true">{complete ? <Check size={14} /> : String(index + 1).padStart(2, "0")}</span>
                <span><strong>{environment.nome}</strong><small>{complete ? "Visão geral salva" : "Visão geral pendente"}{details > 0 ? ` · ${details} detalhe${details > 1 ? "s" : ""}` : ""}</small></span>
              </button>
            );
          })}
        </nav>
        {editable ? <button className="edit-route-button" type="button" onClick={onEditRoute}><PencilLine size={17} />Editar roteiro</button> : null}
      </aside>

      <section className="focused-environment" aria-live="polite">
        {environments.map((environment, index) => {
          const overviewEvidence = inspection.imagens.filter((evidence) => evidence.ambienteId === environment.id && evidence.categoria === "VISAO_GERAL");
          const detailEvidence = inspection.imagens.filter((evidence) => evidence.ambienteId === environment.id && evidence.categoria === "DETALHE");
          const overviewKey: UploadKey = `${environment.id}-VISAO_GERAL`;
          const detailKey: UploadKey = `${environment.id}-DETALHE`;
          const roomErrorId = `room-error-${environment.id}`;
          return (
            <article className="environment-capture" data-testid={`environment-capture-${environment.id}`} hidden={selectedEnvironmentId !== environment.id} key={environment.id}>
              <header className="focused-environment__heading">
                <p className="eyebrow">Ambiente {index + 1} de {environments.length}</p>
                <h2>{environment.nome}</h2>
                <p>Comece mostrando o espaço inteiro. Depois, aproxime apenas o que precisa de contexto adicional.</p>
              </header>
              <aside className="photo-guidance"><Info size={21} /><div><strong>Como enquadrar</strong><span>Use boa iluminação, fotografe a partir de um canto e evite cortar piso, teto ou aberturas importantes.</span></div></aside>
              <CaptureCategory environment={environment} category="VISAO_GERAL" title="Visão geral" description="Obrigatória · mostre o ambiente por completo" evidence={overviewEvidence} editable={editable} busy={uploading !== null} activeUpload={uploading === overviewKey} retryFile={retryFiles[overviewKey]} errorId={roomErrors[environment.id] ? roomErrorId : undefined} onChoose={onChoose} onRetry={onRetry} />
              <CaptureCategory environment={environment} category="DETALHE" title="Detalhes" description="Opcional · registre marcas, fissuras ou pontos relevantes" evidence={detailEvidence} editable={editable} busy={uploading !== null} activeUpload={uploading === detailKey} retryFile={retryFiles[detailKey]} errorId={roomErrors[environment.id] ? roomErrorId : undefined} onChoose={onChoose} onRetry={onRetry} />
              {roomErrors[environment.id] ? <p className="item-error" id={roomErrorId} role="alert">{roomErrors[environment.id]}</p> : null}
              <footer className="environment-pager">
                <button className="button button--secondary" type="button" disabled={index === 0} onClick={() => onMove(-1)}><ArrowLeft size={17} />Anterior</button>
                {!coveredEnvironmentIds.has(environment.id) && index < environments.length - 1
                  ? <button className="skip-button" type="button" onClick={() => onMove(1)}>Pular por agora</button>
                  : <span className="mono">{String(index + 1).padStart(2, "0")} / {String(environments.length).padStart(2, "0")}</span>}
                <button className="button button--primary" type="button" disabled={index === environments.length - 1} onClick={() => onMove(1)}>Próximo<ArrowRight size={17} /></button>
              </footer>
            </article>
          );
        })}
      </section>

      <aside className="submission-guide submission-guide--adaptive" aria-label="Orientações antes do envio">
        <section><FileCheck2 size={24} /><div><h2>Pronto para analisar?</h2><p>A IA só recebe as fotos quando todos os ambientes têm contexto mínimo.</p></div></section>
        <ul className="submission-checklist">
          <li className={completedCount === environments.length ? "is-complete" : ""}><CheckCircle2 size={18} />{completedCount} de {environments.length} visões gerais</li>
          <li className="is-informative"><Sparkles size={18} />{detailCount} fotos de detalhe opcionais</li>
        </ul>
        <section className="next-steps"><Info size={22} /><div><h2>Depois do envio</h2><ol><li><strong>A IA analisa as evidências</strong><span>Você pode sair durante o processamento.</span></li><li><strong>A IA define o resultado</strong><span>Cada ambiente recebe uma conclusão e um motivo.</span></li><li><strong>Você recebe o relatório</strong><span>Se discordar, registre uma manifestação sem alterar a análise original.</span></li></ol></div></section>
        <div className="professional-warning"><AlertTriangle size={19} /><p><strong>Limite claro</strong>A análise visual não substitui avaliação técnica profissional.</p></div>
        {editable ? <div className="workflow-submit">
          {submitError ? <p className="item-error" role="alert">{submitError}</p> : null}
          <button className="button button--primary" disabled={submitting || uploading !== null} onClick={() => void onSubmit()}><Send size={17} />{submitting ? "Enviando..." : "Enviar para análise da IA"}</button>
          <small>As evidências já salvas permanecem disponíveis mesmo se você sair.</small>
        </div> : null}
      </aside>
    </div>
  );
}

interface CaptureCategoryProps {
  environment: InspectionEnvironment;
  category: EvidenceCategory;
  title: string;
  description: string;
  evidence: Inspection["imagens"];
  editable: boolean;
  busy: boolean;
  activeUpload: boolean;
  retryFile?: File;
  errorId?: string;
  onChoose: AdaptiveWorkspaceProps["onChoose"];
  onRetry: AdaptiveWorkspaceProps["onRetry"];
}

function CaptureCategory({ environment, category, title, description, evidence, editable, busy, activeUpload, retryFile, errorId, onChoose, onRetry }: CaptureCategoryProps) {
  const categoryLabel = category === "VISAO_GERAL" ? "visão geral" : "detalhe";
  return (
    <section className={`capture-category capture-category--${category.toLocaleLowerCase()}`}>
      <header><div><h3>{title}</h3><p>{description}</p></div><span>{category === "VISAO_GERAL" ? "Essencial" : "Opcional"}</span></header>
      {editable ? <div className={`capture-actions ${busy ? "is-disabled" : ""}`}>
        <label className="capture-action capture-action--primary">
          <Camera aria-hidden="true" size={22} /><span><strong>{activeUpload ? "Enviando foto..." : `Tirar ${categoryLabel}`}</strong><small>Abra a câmera traseira</small></span>
          <input className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" capture="environment" disabled={busy} aria-label={`Tirar ${categoryLabel} de ${environment.nome}`} aria-invalid={errorId ? true : undefined} aria-describedby={errorId} onChange={(event) => onChoose(environment.id, category, event)} />
        </label>
        <label className="capture-action">
          <Images aria-hidden="true" size={22} /><span><strong>Escolher da galeria</strong><small>JPEG, PNG ou WebP · até 7 MB</small></span>
          <input className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" disabled={busy} aria-label={`Escolher ${categoryLabel} de ${environment.nome} da galeria`} aria-invalid={errorId ? true : undefined} aria-describedby={errorId} onChange={(event) => onChoose(environment.id, category, event)} />
        </label>
        {retryFile ? <button className="retry-link" type="button" disabled={busy} onClick={() => void onRetry(environment.id, category, retryFile)}><RefreshCw size={15} />Tentar novamente</button> : null}
      </div> : null}
      {evidence.length > 0 ? <div className="evidence-section">
        <div className="evidence-section__heading"><CheckCircle2 size={17} /><strong>{evidence.length} {evidence.length === 1 ? "foto salva" : "fotos salvas"}</strong></div>
        <div className="evidence-strip">{evidence.map((photo, index) => <EvidenceImage evidence={photo} alt={`${environment.nome} — ${categoryLabel}, foto ${index + 1}`} key={photo.id} />)}</div>
      </div> : <p className="empty-evidence">Nenhuma foto adicionada.</p>}
    </section>
  );
}

function ProcessingState({ inspection }: { inspection: Inspection }) {
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

function LegacyInspection({ inspection, editable, submitError, submitting, onSubmit }: {
  inspection: Inspection;
  editable: boolean;
  submitError: string | null;
  submitting: boolean;
  onSubmit: () => Promise<void>;
}) {
  return (
    <section className="legacy-inspection-state">
      <ListRestart size={30} />
      <p className="eyebrow">Registro anterior ao roteiro flexível</p>
      <h2>As evidências existentes continuam disponíveis.</h2>
      <p>Este registro não possui ambientes estruturados. Nada foi apagado; as fotos legadas continuam identificadas pelo protocolo original.</p>
      {inspection.imagens.length > 0 ? <div className="evidence-strip">{inspection.imagens.map((evidence, index) => <EvidenceImage evidence={evidence} alt={`Evidência legada ${index + 1}`} key={evidence.id} />)}</div> : null}
      {submitError ? <p className="item-error" role="alert">{submitError}</p> : null}
      {editable ? <button className="button button--primary" disabled={submitting} onClick={() => void onSubmit()}>{submitting ? "Enviando..." : "Enviar evidências existentes"}</button> : null}
    </section>
  );
}

function Journey({ status }: { status: InspectionStatus }) {
  return (
    <nav className="journey-steps" aria-label="Etapas da vistoria">
      <ol>{journeySteps.map(([title, description], index) => {
        const state = getJourneyStepState(status, index);
        return <li className={state === "complete" ? "is-complete" : state === "active" ? "is-active" : ""} aria-current={state === "active" ? "step" : undefined} key={title}>
          <span>{state === "complete" ? <Check size={16} /> : index + 1}</span>
          <div><strong>{title}</strong><small>{description}</small></div>
        </li>;
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
