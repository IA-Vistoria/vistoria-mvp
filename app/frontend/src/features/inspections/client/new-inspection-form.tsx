"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Building2, MapPin } from "lucide-react";
import { FormEvent, useRef, useState } from "react";

import { ApiError } from "@/lib/api";
import { createInspection } from "../api";

export function NewInspectionForm() {
  const router = useRouter();
  const [address, setAddress] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;
    if (!address.trim()) {
      setError("Informe o endereço do imóvel.");
      return;
    }

    submitting.current = true;
    setBusy(true);
    setError(null);
    try {
      const created = await createInspection(address);
      router.replace(`/client/vistorias/${created.id}`);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.problem.detail : "Não foi possível criar o rascunho. Tente novamente.");
      submitting.current = false;
      setBusy(false);
    }
  }

  return (
    <main className="new-inspection-page">
      <Link className="back-link" href="/client"><ArrowLeft size={17} />Voltar para vistorias</Link>
      <div className="new-inspection-layout">
        <section className="new-inspection-copy">
          <p className="eyebrow">Nova vistoria</p>
          <h1>Comece pelo endereço. O restante fica salvo passo a passo.</h1>
          <p>Este primeiro registro cria apenas um rascunho. Nada será enviado para análise antes da sua confirmação.</p>
          <div className="process-note"><Building2 size={22} /><span><strong>Você mantém o controle.</strong> Fotos e progresso ficam vinculados a este imóvel.</span></div>
        </section>

        <form className="inspection-form" onSubmit={handleSubmit}>
          <div className="inspection-form__icon"><MapPin size={26} /></div>
          <h2>Identifique o imóvel</h2>
          <p>Use um endereço completo para facilitar a revisão técnica.</p>
          {error ? <div className="form-alert" role="alert">{error}</div> : null}
          <label htmlFor="inspection-address">Endereço do imóvel</label>
          <input
            id="inspection-address"
            autoComplete="street-address"
            value={address}
            onChange={(event) => setAddress(event.target.value)}
            placeholder="Rua, número, complemento, cidade"
            disabled={busy}
          />
          <button className="button button--primary" disabled={busy} type="submit">
            {busy ? "Criando rascunho..." : "Criar rascunho"}
          </button>
          <small>Você poderá revisar o endereço antes de enviar a vistoria.</small>
        </form>
      </div>
    </main>
  );
}

