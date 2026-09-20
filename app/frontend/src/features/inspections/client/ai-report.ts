export interface AiAreaFinding {
  area: string | null;
  issueType: string | null;
  description: string | null;
  evidence: string | null;
  severity: string | null;
  confidence: string | null;
  recommendation: string | null;
  location: string | null;
}

export interface AiImageResult {
  storagePath: string;
  analysisId?: string | null;
  overallSummary: string | null;
  limitations: string[];
  imageQuality: {
    usable: boolean;
    issues: string[];
  };
  areas: AiAreaFinding[];
}

export interface AiReport {
  version: number;
  images: AiImageResult[];
}

export function parseAiReport(raw: string | null): AiReport | null {
  if (!raw?.trim()) return null;
  const trimmed = raw.trim();
  if (!trimmed.startsWith("{")) return null;
  try {
    const parsed = JSON.parse(trimmed) as AiReport;
    if (!parsed || !Array.isArray(parsed.images)) return null;
    return {
      version: parsed.version ?? 1,
      images: parsed.images.map((image) => ({
        storagePath: image.storagePath ?? "",
        analysisId: image.analysisId ?? null,
        overallSummary: image.overallSummary ?? null,
        limitations: Array.isArray(image.limitations) ? image.limitations : [],
        imageQuality: {
          usable: image.imageQuality?.usable ?? true,
          issues: Array.isArray(image.imageQuality?.issues) ? image.imageQuality.issues : [],
        },
        areas: Array.isArray(image.areas)
          ? image.areas.map((area) => ({
              area: area.area ?? null,
              issueType: area.issueType ?? null,
              description: area.description ?? null,
              evidence: area.evidence ?? null,
              severity: area.severity ?? null,
              confidence: area.confidence ?? null,
              recommendation: area.recommendation ?? null,
              location: area.location ?? null,
            }))
          : [],
      })),
    };
  } catch {
    return null;
  }
}

export function issueTypeLabel(issueType: string | null): string {
  if (!issueType) return "Achado visual";
  const labels: Record<string, string> = {
    possible_moisture: "Possível umidade",
    apparent_mold: "Mofo aparente",
    apparent_crack: "Fissura aparente",
    peeling: "Descascamento",
    stain: "Mancha",
    dirt: "Sujeira",
    bubble: "Bolhas",
    scratch: "Riscos",
    hole: "Furo",
    wear: "Desgaste",
    broken_piece: "Peça quebrada",
    detachment: "Descolamento",
    missing_part: "Parte faltante",
    insufficient_evidence: "Evidência insuficiente",
    other_visual_issue: "Outro problema visual",
  };
  return labels[issueType] ?? issueType;
}
