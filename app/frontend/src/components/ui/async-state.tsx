import type { ReactNode } from "react";

interface AsyncStateProps {
  eyebrow?: string;
  title: string;
  description: string;
  action?: ReactNode;
  role?: "status" | "alert";
}

export function AsyncState({ eyebrow, title, description, action, role = "status" }: AsyncStateProps) {
  return (
    <section className="async-state" role={role}>
      {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
      <h2>{title}</h2>
      <p>{description}</p>
      {action ? <div className="async-state__action">{action}</div> : null}
    </section>
  );
}

