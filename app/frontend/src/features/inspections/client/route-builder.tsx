"use client";

import { ArrowDown, ArrowUp, LockKeyhole, Plus, Trash2 } from "lucide-react";
import type { Ref } from "react";

import type {
  EnvironmentType,
  InspectionEnvironment,
  PropertyType,
} from "../types";

export interface EnvironmentDraft {
  key: string;
  id?: number | null;
  tipo: EnvironmentType;
  nome: string;
}

interface EnvironmentRouteEditorProps {
  environments: EnvironmentDraft[];
  onChange: (environments: EnvironmentDraft[]) => void;
  lockedEnvironmentIds?: ReadonlySet<number>;
  disabled?: boolean;
  errorId?: string;
  invalid?: boolean;
  firstNameRef?: Ref<HTMLInputElement>;
}

const SUGGESTIONS: Record<PropertyType, Array<Omit<EnvironmentDraft, "key">>> = {
  CASA: [
    { tipo: "ENTRADA", nome: "Entrada e fachada" },
    { tipo: "SALA", nome: "Sala" },
    { tipo: "COZINHA", nome: "Cozinha" },
    { tipo: "BANHEIRO", nome: "Banheiro" },
    { tipo: "QUARTO", nome: "Quarto" },
    { tipo: "AREA_SERVICO", nome: "Área de serviço" },
    { tipo: "AREA_EXTERNA", nome: "Área externa" },
  ],
  APARTAMENTO: [
    { tipo: "ENTRADA", nome: "Entrada" },
    { tipo: "SALA", nome: "Sala" },
    { tipo: "COZINHA", nome: "Cozinha" },
    { tipo: "BANHEIRO", nome: "Banheiro" },
    { tipo: "QUARTO", nome: "Quarto" },
    { tipo: "AREA_SERVICO", nome: "Área de serviço" },
  ],
  COMERCIAL: [
    { tipo: "ENTRADA", nome: "Entrada" },
    { tipo: "OUTRO", nome: "Área principal" },
    { tipo: "BANHEIRO", nome: "Banheiro" },
    { tipo: "OUTRO", nome: "Área de apoio" },
  ],
  OUTRO: [
    { tipo: "ENTRADA", nome: "Entrada" },
    { tipo: "OUTRO", nome: "Ambiente principal" },
  ],
};

export const ENVIRONMENT_TYPE_OPTIONS: Array<{ value: EnvironmentType; label: string }> = [
  { value: "ENTRADA", label: "Entrada / fachada" },
  { value: "SALA", label: "Sala" },
  { value: "COZINHA", label: "Cozinha" },
  { value: "BANHEIRO", label: "Banheiro" },
  { value: "QUARTO", label: "Quarto" },
  { value: "AREA_SERVICO", label: "Área de serviço" },
  { value: "VARANDA", label: "Varanda" },
  { value: "GARAGEM", label: "Garagem" },
  { value: "AREA_EXTERNA", label: "Área externa" },
  { value: "ESCRITORIO", label: "Escritório" },
  { value: "OUTRO", label: "Outro" },
];

let nextDraftId = 0;

export function createSuggestedEnvironments(type: PropertyType): EnvironmentDraft[] {
  return SUGGESTIONS[type].map((environment, index) => ({
    ...environment,
    key: `${type}-${index}-${nextDraftId++}`,
  }));
}

export function createEnvironmentDrafts(environments: InspectionEnvironment[]): EnvironmentDraft[] {
  return [...environments]
    .sort((left, right) => left.ordem - right.ordem)
    .map((environment) => ({ ...environment, key: `persisted-${environment.id}` }));
}

export function normalizeEnvironmentName(name: string): string {
  return name.trim().replace(/\s+/g, " ").toLocaleLowerCase("pt-BR");
}

export function validateEnvironmentDrafts(environments: EnvironmentDraft[]): string | null {
  if (environments.length === 0) return "Adicione ao menos um ambiente ao roteiro.";
  if (environments.length > 30) return "O roteiro pode ter no máximo 30 ambientes.";
  if (environments.some(({ nome }) => nome.trim().length < 2 || nome.trim().length > 60)) {
    return "Cada ambiente precisa de um nome entre 2 e 60 caracteres.";
  }
  const normalizedNames = environments.map(({ nome }) => normalizeEnvironmentName(nome));
  if (new Set(normalizedNames).size !== normalizedNames.length) {
    return "Use nomes diferentes para identificar cada ambiente.";
  }
  return null;
}

export function EnvironmentRouteEditor({
  environments,
  onChange,
  lockedEnvironmentIds = new Set<number>(),
  disabled = false,
  errorId,
  invalid = false,
  firstNameRef,
}: EnvironmentRouteEditorProps) {
  function update(index: number, patch: Partial<EnvironmentDraft>) {
    onChange(environments.map((environment, currentIndex) => (
      currentIndex === index ? { ...environment, ...patch } : environment
    )));
  }

  function move(index: number, offset: number) {
    const target = index + offset;
    if (target < 0 || target >= environments.length) return;
    const reordered = [...environments];
    [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
    onChange(reordered);
  }

  function remove(index: number) {
    onChange(environments.filter((_, currentIndex) => currentIndex !== index));
  }

  function add() {
    if (environments.length >= 30) return;
    onChange([
      ...environments,
      { key: `custom-${nextDraftId++}`, tipo: "OUTRO", nome: "Novo ambiente" },
    ]);
  }

  return (
    <fieldset className="route-editor" disabled={disabled} aria-describedby={invalid ? errorId : undefined}>
      <legend>Ambientes do imóvel</legend>
      <div className="route-editor__heading">
        <div>
          <strong>Monte o roteiro real</strong>
          <p>Renomeie, reorganize ou remova a sugestão. A ordem definida aqui orienta a captura.</p>
        </div>
        <span>{environments.length} {environments.length === 1 ? "ambiente" : "ambientes"}</span>
      </div>

      <ol className="route-editor__list">
        {environments.map((environment, index) => {
          const locked = environment.id != null && lockedEnvironmentIds.has(environment.id);
          return (
            <li className="route-editor__row" key={environment.key}>
              <span className="route-editor__order" aria-hidden="true">{String(index + 1).padStart(2, "0")}</span>
              <div className="route-editor__fields">
                <label>
                  <span className="sr-only">Tipo do ambiente {index + 1}</span>
                  <select
                    aria-label={`Tipo do ambiente ${index + 1}`}
                    aria-invalid={invalid || undefined}
                    aria-describedby={invalid ? errorId : undefined}
                    value={environment.tipo}
                    onChange={(event) => update(index, { tipo: event.target.value as EnvironmentType })}
                  >
                    {ENVIRONMENT_TYPE_OPTIONS.map((option) => (
                      <option value={option.value} key={option.value}>{option.label}</option>
                    ))}
                  </select>
                </label>
                <label>
                  <span className="sr-only">Nome do ambiente {index + 1}</span>
                  <input
                    ref={index === 0 ? firstNameRef : undefined}
                    aria-label={`Nome do ambiente ${index + 1}`}
                    aria-invalid={invalid || undefined}
                    aria-describedby={invalid ? errorId : undefined}
                    maxLength={60}
                    value={environment.nome}
                    onChange={(event) => update(index, { nome: event.target.value })}
                  />
                </label>
              </div>
              <div className="route-editor__actions">
                <button type="button" disabled={index === 0} onClick={() => move(index, -1)} aria-label={`Mover ${environment.nome} para cima`}><ArrowUp size={17} /></button>
                <button type="button" disabled={index === environments.length - 1} onClick={() => move(index, 1)} aria-label={`Mover ${environment.nome} para baixo`}><ArrowDown size={17} /></button>
                <button type="button" disabled={locked} onClick={() => remove(index)} aria-label={`Remover ${environment.nome}`}><Trash2 size={17} /></button>
              </div>
              {locked ? <small className="route-editor__locked"><LockKeyhole size={14} />Este ambiente possui fotos e não pode ser removido.</small> : null}
            </li>
          );
        })}
      </ol>

      <button className="route-editor__add" type="button" disabled={environments.length >= 30} onClick={add}>
        <Plus size={18} />Adicionar ambiente
      </button>
    </fieldset>
  );
}
