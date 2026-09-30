"use client";

import { ArrowDown, ArrowUp, LockKeyhole, Minus, Plus, Trash2 } from "lucide-react";
import { useState, type Ref } from "react";

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

interface EnvironmentQuantityPlannerProps {
  environments: EnvironmentDraft[];
  onChange: (environments: EnvironmentDraft[]) => void;
  disabled?: boolean;
  errorId?: string;
  invalid?: boolean;
  containerRef?: Ref<HTMLFieldSetElement>;
}

interface EnvironmentGroup {
  key: string;
  tipo: EnvironmentType;
  environments: EnvironmentDraft[];
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

const ENVIRONMENT_TYPE_LABELS = new Map(
  ENVIRONMENT_TYPE_OPTIONS.map(({ value, label }) => [value, label]),
);

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

function groupEnvironmentDrafts(environments: EnvironmentDraft[]): EnvironmentGroup[] {
  const groups: EnvironmentGroup[] = [];
  const knownGroups = new Map<EnvironmentType, EnvironmentGroup>();

  environments.forEach((environment) => {
    if (environment.tipo === "OUTRO") {
      groups.push({
        key: `custom:${environment.key}`,
        tipo: environment.tipo,
        environments: [environment],
      });
      return;
    }

    const existing = knownGroups.get(environment.tipo);
    if (existing) {
      existing.environments.push(environment);
      return;
    }

    const group = {
      key: `type:${environment.tipo}`,
      tipo: environment.tipo,
      environments: [environment],
    };
    knownGroups.set(environment.tipo, group);
    groups.push(group);
  });

  return groups;
}

function replaceEnvironmentGroup(
  environments: EnvironmentDraft[],
  groupKey: string,
  replacement: EnvironmentDraft[],
): EnvironmentDraft[] {
  return groupEnvironmentDrafts(environments).flatMap((group) => (
    group.key === groupKey ? replacement : group.environments
  ));
}

function environmentTypeLabel(type: EnvironmentType): string {
  return ENVIRONMENT_TYPE_LABELS.get(type) ?? "Ambiente";
}

function groupTitle(group: EnvironmentGroup): string {
  if (group.tipo !== "OUTRO") return environmentTypeLabel(group.tipo);
  return group.environments[0]?.nome.trim() || "Ambiente personalizado";
}

function automaticGroupBase(group: EnvironmentGroup): string {
  const typeLabel = environmentTypeLabel(group.tipo);
  const firstName = group.environments[0]?.nome.replace(/\s+\d+$/, "").trim();
  if (!firstName) return typeLabel;

  const normalizedFirstName = normalizeEnvironmentName(firstName);
  const automaticNames = group.tipo === "ENTRADA"
    ? ["entrada", "entrada e fachada", "entrada / fachada"]
    : [normalizeEnvironmentName(typeLabel)];
  return automaticNames.includes(normalizedFirstName) ? firstName : typeLabel;
}

function resizeEnvironmentGroup(group: EnvironmentGroup, nextQuantity: number): EnvironmentDraft[] {
  const current = group.environments;
  const baseName = automaticGroupBase(group);

  if (nextQuantity > current.length) {
    const numbered = current.map((environment, index) => ({
      ...environment,
      nome: current.length === 1
        && normalizeEnvironmentName(environment.nome) === normalizeEnvironmentName(baseName)
        ? `${baseName} ${index + 1}`
        : environment.nome,
    }));
    return [
      ...numbered,
      ...Array.from({ length: nextQuantity - current.length }, (_, offset) => ({
        key: `quantity-${nextDraftId++}`,
        tipo: group.tipo,
        nome: `${baseName} ${current.length + offset + 1}`,
      })),
    ];
  }

  const reduced = current.slice(0, nextQuantity);
  if (reduced.length === 1 && reduced[0].nome === `${baseName} 1`) {
    return [{ ...reduced[0], nome: baseName }];
  }
  return reduced;
}

export function EnvironmentQuantityPlanner({
  environments,
  onChange,
  disabled = false,
  errorId,
  invalid = false,
  containerRef,
}: EnvironmentQuantityPlannerProps) {
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(() => new Set());
  const [typeToAdd, setTypeToAdd] = useState<EnvironmentType | "">("");
  const groups = groupEnvironmentDrafts(environments);
  const selectedKnownTypes = new Set(
    groups.filter(({ tipo }) => tipo !== "OUTRO").map(({ tipo }) => tipo),
  );
  const availableTypes = ENVIRONMENT_TYPE_OPTIONS.filter(({ value }) => (
    value === "OUTRO" || !selectedKnownTypes.has(value)
  ));

  function changeQuantity(group: EnvironmentGroup, offset: number) {
    const nextQuantity = group.environments.length + offset;
    if (nextQuantity < 1 || environments.length + offset > 30) return;
    onChange(replaceEnvironmentGroup(
      environments,
      group.key,
      resizeEnvironmentGroup(group, nextQuantity),
    ));
  }

  function updateName(key: string, name: string) {
    onChange(environments.map((environment) => (
      environment.key === key ? { ...environment, nome: name } : environment
    )));
  }

  function moveGroup(index: number, offset: number) {
    const target = index + offset;
    if (target < 0 || target >= groups.length) return;
    const reordered = [...groups];
    [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
    onChange(reordered.flatMap(({ environments: grouped }) => grouped));
  }

  function removeGroup(group: EnvironmentGroup) {
    const keys = new Set(group.environments.map(({ key }) => key));
    onChange(environments.filter(({ key }) => !keys.has(key)));
  }

  function addType() {
    if (!typeToAdd || environments.length >= 30) return;
    const label = typeToAdd === "OUTRO" ? "Novo ambiente" : environmentTypeLabel(typeToAdd);
    onChange([
      ...environments,
      { key: `custom-${nextDraftId++}`, tipo: typeToAdd, nome: label },
    ]);
    setTypeToAdd("");
  }

  function toggleNames(groupKey: string) {
    setExpandedGroups((current) => {
      const next = new Set(current);
      if (next.has(groupKey)) next.delete(groupKey);
      else next.add(groupKey);
      return next;
    });
  }

  return (
    <fieldset
      className="route-editor route-planner"
      disabled={disabled}
      aria-describedby={invalid ? errorId : undefined}
      aria-invalid={invalid || undefined}
      ref={containerRef}
      tabIndex={-1}
    >
      <legend>Ambientes do imóvel</legend>
      <div className="route-editor__heading">
        <div>
          <strong>Defina os ambientes e as quantidades</strong>
          <p>A quantidade cria um registro separado para cada ambiente. Personalize os nomes somente quando precisar diferenciá-los.</p>
        </div>
        <span aria-live="polite">{environments.length} {environments.length === 1 ? "ambiente" : "ambientes"}</span>
      </div>

      <ol className="route-editor__list route-planner__list">
        {groups.map((group, groupIndex) => {
          const title = groupTitle(group);
          const custom = group.tipo === "OUTRO";
          const showNames = custom || expandedGroups.has(group.key);
          return (
            <li className={`route-planner__row${custom ? " route-planner__row--custom" : ""}`} key={group.key}>
              <span className="route-editor__order" aria-hidden="true">{String(groupIndex + 1).padStart(2, "0")}</span>
              <div className="route-planner__summary">
                <strong>{custom ? "Ambiente personalizado" : title}</strong>
                <span>{group.environments.map(({ nome }) => nome).join(" · ")}</span>
              </div>

              {!custom ? (
                <div className="route-planner__quantity" role="group" aria-label={`Quantidade de ${title}`}>
                  <span>Quantidade</span>
                  <div>
                    <button
                      type="button"
                      disabled={group.environments.length === 1}
                      onClick={() => changeQuantity(group, -1)}
                      aria-label={`Diminuir quantidade de ${title}`}
                    ><Minus size={16} /></button>
                    <output aria-live="polite">{group.environments.length}</output>
                    <button
                      type="button"
                      disabled={environments.length >= 30}
                      onClick={() => changeQuantity(group, 1)}
                      aria-label={`Aumentar quantidade de ${title}`}
                    ><Plus size={16} /></button>
                  </div>
                </div>
              ) : null}

              {!custom ? (
                <button
                  className="route-planner__customize"
                  type="button"
                  aria-expanded={showNames}
                  onClick={() => toggleNames(group.key)}
                  aria-label={`Personalizar nomes de ${title}`}
                >{showNames ? "Ocultar nomes" : "Personalizar nomes"}</button>
              ) : null}

              <div className="route-editor__actions route-planner__order-actions">
                <button type="button" disabled={groupIndex === 0} onClick={() => moveGroup(groupIndex, -1)} aria-label={`Mover ${title} para cima`}><ArrowUp size={17} /></button>
                <button type="button" disabled={groupIndex === groups.length - 1} onClick={() => moveGroup(groupIndex, 1)} aria-label={`Mover ${title} para baixo`}><ArrowDown size={17} /></button>
                <button type="button" onClick={() => removeGroup(group)} aria-label={`Remover ${title} do roteiro`}><Trash2 size={17} /></button>
              </div>

              {showNames ? (
                <div className="route-planner__names">
                  {group.environments.map((environment, environmentIndex) => {
                    const inputLabel = custom
                      ? "Nome do ambiente personalizado"
                      : group.environments.length === 1
                        ? `Nome de ${title}`
                        : `Nome de ${title} ${environmentIndex + 1}`;
                    return (
                      <label key={environment.key}>
                        <span>{inputLabel}</span>
                        <input
                          aria-label={inputLabel}
                          aria-invalid={invalid || undefined}
                          aria-describedby={invalid ? errorId : undefined}
                          maxLength={60}
                          value={environment.nome}
                          onChange={(event) => updateName(environment.key, event.target.value)}
                        />
                      </label>
                    );
                  })}
                </div>
              ) : null}
            </li>
          );
        })}
      </ol>

      <div className="route-planner__add">
        <label htmlFor="route-add-type">Adicionar outro tipo de ambiente</label>
        <div>
          <select
            id="route-add-type"
            value={typeToAdd}
            onChange={(event) => setTypeToAdd(event.target.value as EnvironmentType | "")}
          >
            <option value="">Selecione um tipo</option>
            {availableTypes.map(({ value, label }) => (
              <option value={value} key={value}>{value === "OUTRO" ? "Ambiente personalizado" : label}</option>
            ))}
          </select>
          <button type="button" disabled={!typeToAdd || environments.length >= 30} onClick={addType}>
            <Plus size={18} />Adicionar ao roteiro
          </button>
        </div>
      </div>
    </fieldset>
  );
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
