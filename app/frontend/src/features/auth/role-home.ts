import type { UserRole } from "@/lib/auth";

export function roleHome(role: UserRole): "/client" | "/engineer" {
  return role === "ROLE_ENGENHEIRO" ? "/engineer" : "/client";
}
