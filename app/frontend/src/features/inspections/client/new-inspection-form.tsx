"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, MapPin, ScanLine } from "lucide-react";
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
    <div className="new-inspection-page">
      <Link className="back-link" href="/client"><ArrowLeft size={17} />Voltar ao início</Link>
      <div className="new-inspection-layout">
        <section className="new-inspection-copy">
          <p className="eyebrow">Nova vistoria · etapa 1 de 4</p>
          <h1>Qual imóvel você quer documentar?</h1>
          <p>O endereço identifica o registro. Em seguida, um roteiro simples orienta as fotos de cada ambiente.</p>
          <div className="process-note"><ScanLine size={22} /><span><strong>Nada é enviado à IA agora.</strong> Primeiro criamos um rascunho para salvar seu progresso com segurança.</span></div>
        </section>

        <form className="inspection-form" onSubmit={handleSubmit}>
          <div className="inspection-form__icon"><MapPin size={26} /></div>
          <h2>Identifique o imóvel</h2>
          <p>Use um endereço que você reconheça facilmente na lista de vistorias.</p>
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
          <small>Ao continuar, você abre o roteiro de 12 itens para registrar as fotos.</small>
        </form>
      </div>
    </div>
  );
}

