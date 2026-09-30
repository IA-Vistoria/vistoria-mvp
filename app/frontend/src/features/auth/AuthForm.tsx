"use client";

import { ArrowRight, Camera, Eye, EyeOff, FileCheck2, ListChecks, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useSyncExternalStore } from "react";

import { ApiError } from "@/lib/api";
import { setSession } from "@/lib/auth";
import { BrandMark } from "@/components/brand/BrandMark";
import { login, register } from "./auth-service";
import { roleHome } from "./role-home";

interface AuthFormProps {
  mode: "login" | "register";
}

const subscribeLocation = () => () => undefined;

export function AuthForm({ mode }: AuthFormProps) {
  const router = useRouter();
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [showSenha, setShowSenha] = useState(false);

  const isRegister = mode === "register";
  const expiredSession = useSyncExternalStore(
    subscribeLocation,
    () => new URLSearchParams(window.location.search).get("motivo") === "sessao-expirada",
    () => false,
  );
  const notice = !isRegister && expiredSession
    ? "Sua sessão expirou. Entre novamente para continuar."
    : "";

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;

    setBusy(true);
    setError("");
    try {
      const session = isRegister
        ? await register({
            nome: nome.trim(),
            email: email.trim(),
            senha,
            perfil: "ROLE_CLIENTE",
          })
        : await login({ email: email.trim(), senha });
      setSession(session);
      router.replace(roleHome(session.perfil));
    } catch (reason: unknown) {
      setSenha("");
      setError(
        reason instanceof ApiError
          ? reason.problem.detail
          : "Não foi possível concluir o acesso. Tente novamente.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-story" aria-label="Sobre a Vistor.IA">
        <Link className="auth-story__brand" href="/">
          <BrandMark inverse />
        </Link>
        <div className="auth-story__content">
          <p className="eyebrow">Da coleta ao documento</p>
          <h1>Relatório de vistoria por IA</h1>
          <p>
            Um fluxo claro para registrar o imóvel real, orientar as fotos e revisar cada indício
            antes de gerar o relatório.
          </p>
          <ol className="auth-journey" aria-label="Como funciona">
            <li><ListChecks aria-hidden="true" size={20} /><span><strong>Registre os ambientes reais</strong><small>O roteiro se adapta ao imóvel, não o contrário.</small></span></li>
            <li><Camera aria-hidden="true" size={20} /><span><strong>Envie fotos guiadas</strong><small>Uma visão geral por ambiente e detalhes quando necessário.</small></span></li>
            <li><FileCheck2 aria-hidden="true" size={20} /><span><strong>Revise e gere o relatório</strong><small>Você confirma o contexto antes do documento final.</small></span></li>
          </ol>
          <div className="auth-proof">
            <ShieldCheck aria-hidden="true" size={24} />
            <span>Cada conclusão permanece ligada à foto e ao contexto que você confirmou.</span>
          </div>
        </div>
        <p className="auth-story__foot">Organização visual. Decisões transparentes.</p>
      </section>

      <section className="auth-panel" aria-labelledby="auth-title">
        <div className="auth-card">
          <div className="auth-card__brand" aria-hidden="true"><BrandMark compact /></div>
          <p className="eyebrow">{isRegister ? "Comece sua jornada" : "Bem-vindo de volta"}</p>
          <h2 id="auth-title">{isRegister ? "Crie sua conta" : "Entre na sua conta"}</h2>
          <p className="auth-card__intro">
            {isRegister
              ? "Crie seu acesso para começar a primeira vistoria."
              : "Retome suas evidências, revisões e relatórios."}
          </p>

          {notice || error ? (
            <div className="form-alert" role="alert">
              {error || notice}
            </div>
          ) : null}

          <form className="auth-form" onSubmit={handleSubmit}>
            {isRegister ? (
              <label>
                <span>Nome completo</span>
                <input
                  autoComplete="name"
                  required
                  value={nome}
                  onChange={(event) => setNome(event.target.value)}
                />
              </label>
            ) : null}

            <label>
              <span>E-mail</span>
              <input
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="voce@exemplo.com"
              />
            </label>

            <div className="field-group">
              <label htmlFor="auth-senha">Senha</label>
              <div className="password-field">
                <input
                  id="auth-senha"
                  type={showSenha ? "text" : "password"}
                  autoComplete={isRegister ? "new-password" : "current-password"}
                  minLength={8}
                  required
                  value={senha}
                  onChange={(event) => setSenha(event.target.value)}
                />
                <button
                  type="button"
                  className="password-toggle"
                  aria-label={showSenha ? "Ocultar senha" : "Mostrar senha"}
                  aria-pressed={showSenha}
                  onClick={() => setShowSenha((current) => !current)}
                >
                  {showSenha ? <EyeOff aria-hidden="true" size={18} /> : <Eye aria-hidden="true" size={18} />}
                </button>
              </div>
              {isRegister ? <small>Use ao menos 8 caracteres.</small> : null}
            </div>

            <button className="button button--primary auth-submit" type="submit" disabled={busy}>
              {busy ? "Aguarde..." : isRegister ? "Criar conta" : "Entrar"}
              {!busy ? <ArrowRight aria-hidden="true" size={18} /> : null}
            </button>
          </form>

          <p className="auth-switch">
            {isRegister ? "Já possui uma conta?" : "Ainda não possui uma conta?"}{" "}
            <Link href={isRegister ? "/login" : "/register"}>
              {isRegister ? "Entrar" : "Criar conta"}
            </Link>
          </p>

          <div className="auth-professional-note">
            <Camera aria-hidden="true" size={19} />
            <span>A análise aponta indícios visuais e não substitui uma avaliação técnica presencial.</span>
          </div>
        </div>
      </section>
    </main>
  );
}
