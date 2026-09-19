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

export function ProtectedArea({ allowedRole, children }: ProtectedAreaProps) {
  const { replace } = useRouter();
  const session = useSyncExternalStore(subscribeSession, getSession, getServerSession);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (!session) {
      replace("/login");
      return;
    }
    if (session.perfil !== allowedRole) {
      replace(roleHome(session.perfil));
    }
  }, [allowedRole, replace, session]);

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
      {session?.perfil === allowedRole ? children : null}
    </>
  );
}
