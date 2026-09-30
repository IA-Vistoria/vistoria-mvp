"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Camera, CheckCircle2, MapPin } from "lucide-react";
import { FormEvent, useEffect, useRef, useState } from "react";

import { ApiError } from "@/lib/api";
import { createInspection } from "../api";
import type { PropertyType } from "../types";
import {
  EnvironmentQuantityPlanner,
  createSuggestedEnvironments,
  validateEnvironmentDrafts,
} from "./route-builder";

const PROPERTY_TYPES: Array<{ value: PropertyType; label: string; description: string }> = [
  { value: "CASA", label: "Casa", description: "Com áreas internas e externas" },
  { value: "APARTAMENTO", label: "Apartamento", description: "Ambientes internos da unidade" },
  { value: "COMERCIAL", label: "Comercial", description: "Loja, sala ou escritório" },
  { value: "OUTRO", label: "Outro", description: "Você monta do zero" },
];

const FORM_ERROR_ID = "new-inspection-error";

interface FormError {
  message: string;
  target: "address" | "route" | "form";
}

export function NewInspectionForm() {
  const router = useRouter();
  const [address, setAddress] = useState("");
  const [propertyType, setPropertyType] = useState<PropertyType>("CASA");
  const [environments, setEnvironments] = useState(() => createSuggestedEnvironments("CASA"));
  const [error, setError] = useState<FormError | null>(null);
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  const addressRef = useRef<HTMLInputElement>(null);
  const routeRef = useRef<HTMLFieldSetElement>(null);
  const errorRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (error?.target === "form") errorRef.current?.focus();
  }, [error]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;
    if (!address.trim()) {
      setError({ message: "Informe o endereço do imóvel.", target: "address" });
      addressRef.current?.focus();
      return;
    }
    const routeError = validateEnvironmentDrafts(environments);
    if (routeError) {
      setError({ message: routeError, target: "route" });
      routeRef.current?.focus();
      return;
    }

    submitting.current = true;
    setBusy(true);
    setError(null);
    try {
      const created = await createInspection({
        endereco: address,
        tipoImovel: propertyType,
        ambientes: environments.map(({ id, tipo, nome }) => ({ id, tipo, nome })),
      });
      router.replace(`/client/vistorias/${created.id}`);
    } catch (cause) {
      setError({
        message: cause instanceof ApiError ? cause.problem.detail : "Não foi possível criar o rascunho. Tente novamente.",
        target: "form",
      });
      submitting.current = false;
      setBusy(false);
    }
  }

  return (
    <div className="new-inspection-page new-inspection-page--adaptive">
      <Link className="back-link" href="/client"><ArrowLeft size={17} />Voltar ao início</Link>
      <div className="new-inspection-layout">
        <section className="new-inspection-copy">
          <p className="eyebrow">Nova vistoria · configure antes de fotografar</p>
          <h1>O roteiro acompanha o imóvel real.</h1>
          <p>Escolha uma base, informe quantos ambientes existem e ajuste apenas o que for necessário.</p>
          <div className="route-rule-card">
            <Camera size={22} />
            <span><strong>Uma visão geral por ambiente.</strong> Essa é a única regra mínima. Fotos de detalhe são opcionais e podem ser adicionadas quando houver algo a destacar.</span>
          </div>
          <ul className="route-benefits">
            <li><CheckCircle2 size={17} />O tipo do imóvel só cria uma sugestão inicial.</li>
            <li><CheckCircle2 size={17} />Cada quantidade vira uma etapa própria de fotos.</li>
            <li><CheckCircle2 size={17} />Nada é enviado à IA nesta etapa.</li>
          </ul>
        </section>

        <form className="inspection-form" onSubmit={handleSubmit}>
          <div className="inspection-form__heading">
            <div className="inspection-form__icon"><MapPin size={24} /></div>
            <div><p className="eyebrow">Identificação e roteiro</p><h2>Prepare a vistoria</h2></div>
          </div>
          {error ? <div className="form-alert" id={FORM_ERROR_ID} ref={errorRef} role="alert" tabIndex={error.target === "form" ? -1 : undefined}>{error.message}</div> : null}
          <label htmlFor="inspection-address">Endereço do imóvel</label>
          <input
            ref={addressRef}
            id="inspection-address"
            autoComplete="street-address"
            value={address}
            aria-invalid={error?.target === "address" || undefined}
            aria-describedby={error?.target === "address" ? FORM_ERROR_ID : undefined}
            onChange={(event) => {
              setAddress(event.target.value);
              if (error?.target === "address") setError(null);
            }}
            placeholder="Rua, número, complemento, cidade"
            disabled={busy}
          />

          <fieldset className="property-type-picker" disabled={busy}>
            <legend>Tipo do imóvel</legend>
            <p>Só usamos essa escolha para sugerir um ponto de partida.</p>
            <div>
              {PROPERTY_TYPES.map((option) => (
                <label className={propertyType === option.value ? "is-selected" : ""} key={option.value}>
                  <input
                    type="radio"
                    aria-label={option.label}
                    name="property-type"
                    value={option.value}
                    checked={propertyType === option.value}
                    onChange={() => {
                      setPropertyType(option.value);
                      setEnvironments(createSuggestedEnvironments(option.value));
                      setError(null);
                    }}
                  />
                  <strong>{option.label}</strong>
                  <small>{option.description}</small>
                </label>
              ))}
            </div>
          </fieldset>

          <p className="route-suggestion-note"><strong>Sugestão inicial:</strong> você pode adaptar tudo antes de continuar.</p>
          <EnvironmentQuantityPlanner
            environments={environments}
            onChange={(updated) => {
              setEnvironments(updated);
              if (error?.target === "route") setError(null);
            }}
            disabled={busy}
            errorId={FORM_ERROR_ID}
            invalid={error?.target === "route"}
            containerRef={routeRef}
          />

          <button className="button button--primary" disabled={busy} type="submit">
            {busy ? "Preparando roteiro..." : "Começar a registrar fotos"}
          </button>
          <small>O rascunho é criado uma única vez, já com o roteiro que você definiu.</small>
        </form>
      </div>
    </div>
  );
}

