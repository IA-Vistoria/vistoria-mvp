"use client";

import { ImageOff } from "lucide-react";
import Image from "next/image";
import { useEffect, useState } from "react";

import { loadEvidence } from "../api";
import type { Evidence } from "../types";

export function EvidenceImage({ evidence, alt }: { evidence: Evidence; alt: string }) {
  const [source, setSource] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    let objectUrl: string | null = null;
    loadEvidence(evidence.conteudoUrl)
      .then((blob) => {
        if (!active || typeof URL.createObjectURL !== "function") return;
        objectUrl = URL.createObjectURL(blob);
        setSource(objectUrl);
      })
      .catch(() => {
        if (active) setFailed(true);
      });
    return () => {
      active = false;
      if (objectUrl && typeof URL.revokeObjectURL === "function") URL.revokeObjectURL(objectUrl);
    };
  }, [evidence.conteudoUrl]);

  if (failed) {
    return <span className="evidence-thumb evidence-thumb--failed"><ImageOff size={18} />Foto indisponível</span>;
  }
  if (!source) return <span className="evidence-thumb evidence-thumb--loading">Foto confirmada</span>;
  return <Image className="evidence-thumb" src={source} alt={alt} width={100} height={68} unoptimized />;
}
