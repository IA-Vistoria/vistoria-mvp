"use client";

import { ArrowRight, Building2, Eye, EyeOff, HardHat, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, useSyncExternalStore } from "react";

import { ApiError } from "@/lib/api";
import { setSession, type UserRole } from "@/lib/auth";
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
  const [perfil, setPerfil] = useState<UserRole>("ROLE_CLIENTE");
  const [crea, setCrea] = useState("");
  const [codigoConvite, setCodigoConvite] = useState("");
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
            perfil,
            ...(perfil === "ROLE_ENGENHEIRO"
              ? { crea: crea.trim(), codigoConvite: codigoConvite.trim() }
              : {}),
          })
        : await login({ email: email.trim(), senha });
      setSession(session);
      router.replace(roleHome(session.perfil));
    } catch (reason: unknown) {
      setSenha("");
      setCodigoConvite("");
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
        <div className="brand-lockup brand-lockup--light">
          <Building2 aria-hidden="true" size={34} strokeWidth={1.8} />
          <span>Vistor.IA</span>
        </div>
        <div className="auth-story__content">
          <p className="eyebrow">Vistoria predial inteligente</p>
          <h1>Mais clareza para cuidar do seu imóvel.</h1>
          <p>
            Registre evidências com um roteiro simples e receba uma análise técnica revisada por
            engenharia civil.
          </p>
          <div className="auth-proof">
            <ShieldCheck aria-hidden="true" size={24} />
            <span>A IA organiza o pré-laudo. O engenheiro valida a decisão.</span>
          </div>
        </div>
        <p className="auth-story__foot">Tecnologia com responsabilidade técnica.</p>
      </section>

      <section className="auth-panel" aria-labelledby="auth-title">
        <div className="auth-card">
          <div className="brand-lockup auth-card__brand">
            <Building2 aria-hidden="true" size={30} strokeWidth={1.8} />
            <span>Vistor.IA</span>
          </div>
          <p className="eyebrow">{isRegister ? "Comece sua jornada" : "Bem-vindo de volta"}</p>
          <h2 id="auth-title">{isRegister ? "Crie sua conta" : "Entre na sua conta"}</h2>
          <p className="auth-card__intro">
            {isRegister
              ? "Escolha seu perfil para acessar a experiência certa."
              : "Acesse suas vistorias e acompanhe cada etapa."}
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

            {isRegister ? (
              <label>
                <span>Perfil</span>
                <select
                  value={perfil}
                  onChange={(event) => {
                    const nextRole = event.target.value as UserRole;
                    setPerfil(nextRole);
                    if (nextRole === "ROLE_CLIENTE") {
                      setCrea("");
                      setCodigoConvite("");
                    }
                  }}
                >
                  <option value="ROLE_CLIENTE">Cliente / responsável pelo imóvel</option>
                  <option value="ROLE_ENGENHEIRO">Engenheiro civil</option>
                </select>
              </label>
            ) : null}

            {isRegister && perfil === "ROLE_ENGENHEIRO" ? (
              <>
                <label>
                  <span>CREA</span>
                  <input
                    autoComplete="off"
                    required
                    value={crea}
                    onChange={(event) => setCrea(event.target.value)}
                    placeholder="Número de registro profissional"
                  />
                </label>
                <label>
                  <span>Código de convite</span>
                  <input
                    type="password"
                    autoComplete="new-password"
                    required
                    value={codigoConvite}
                    onChange={(event) => setCodigoConvite(event.target.value)}
                    placeholder="Fornecido pela equipe Vistor.IA"
                  />
                </label>
              </>
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
            <HardHat aria-hidden="true" size={19} />
            <span>Ambiente preparado para clientes e profissionais de engenharia.</span>
          </div>
        </div>
      </section>
    </main>
  );
}
