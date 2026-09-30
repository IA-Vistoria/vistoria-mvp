import Image from "next/image";

interface BrandMarkProps {
  inverse?: boolean;
  compact?: boolean;
}

export function BrandMark({ inverse = false, compact = false }: BrandMarkProps) {
  return (
    <span
      className={`brand-mark${inverse ? " brand-mark--inverse" : ""}${compact ? " brand-mark--compact" : ""}`}
    >
      <Image
        className="brand-mark__symbol"
        src="/vistoria-logo.svg"
        width={52}
        height={52}
        alt="Vistor.IA — vistoria inteligente"
        priority
      />
      <span className="brand-mark__name" aria-hidden="true">
        Vistor.<strong>IA</strong>
        <small>vistoria inteligente</small>
      </span>
    </span>
  );
}
