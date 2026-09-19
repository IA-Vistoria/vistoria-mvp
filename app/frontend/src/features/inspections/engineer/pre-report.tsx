export function splitPreReport(value: string | null): string[] {
  if (!value?.trim()) return [];
  return value
    .split(/\r?\n/)
    .map((line) => line.trim().replace(/^[-*•]\s*/, ""))
    .filter(Boolean);
}

export function PreReport({ value }: { value: string | null }) {
  const findings = splitPreReport(value);
  return (
    <section className="pre-report" aria-labelledby="pre-report-title">
      <p className="eyebrow">Análise preliminar</p>
      <h2 id="pre-report-title">Pré-laudo da IA</h2>
      {findings.length ? (
        <ol>{findings.map((finding, index) => <li key={`${index}-${finding}`}>{finding}</li>)}</ol>
      ) : (
        <p className="pre-report__empty">Pré-laudo ainda não disponível</p>
      )}
      <aside className="human-loop-note">
        <strong>A IA sugere. O engenheiro decide.</strong>
        <span>Este conteúdo não substitui a avaliação técnica profissional.</span>
      </aside>
    </section>
  );
}

