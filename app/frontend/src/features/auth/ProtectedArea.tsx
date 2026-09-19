"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, useSyncExternalStore } from "react";

import {
  SESSION_EXPIRED_EVENT,
  getServerSession,
  getSession,
  removeSession,
  subscribeSession,
  type UserRole,
} from "@/lib/auth";
import { roleHome } from "./role-home";

interface ProtectedAreaProps {
  allowedRole: UserRole;
  children: React.ReactNode;
}

const subscribeHydration = () => () => undefined;

export function ProtectedArea({ allowedRole, children }: ProtectedAreaProps) {
  const { replace } = useRouter();
  const session = useSyncExternalStore(subscribeSession, getSession, getServerSession);
  const hydrated = useSyncExternalStore(subscribeHydration, () => true, () => false);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (!hydrated) {
      return;
    }
    if (!session) {
      replace("/login");
      return;
    }
    if (session.perfil !== allowedRole) {
      replace(roleHome(session.perfil));
    }
  }, [allowedRole, hydrated, replace, session]);

  useEffect(() => {
    function handleExpiredSession() {
      removeSession();
      setNotice("Sua sessão expirou. Entre novamente para continuar.");
      replace("/login");
    }

    window.addEventListener(SESSION_EXPIRED_EVENT, handleExpiredSession);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleExpiredSession);
  }, [replace]);

  return (
    <>
      <p className={notice ? "session-notice" : "sr-only"} aria-live="assertive">
        {notice}
      </p>
      {hydrated && session?.perfil === allowedRole ? children : null}
    </>
  );
}
