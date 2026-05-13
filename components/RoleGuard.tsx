import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

type AppRole = "owner" | "manager" | "traveler";

type RoleGuardProps = {
  /** Only these roles may view the wrapped content. */
  allow: AppRole | AppRole[];
  children: React.ReactNode;
};

/**
 * Server wrapper: loads the signed-in user's profile and redirects
 * if their role is not allowed.
 */
export async function RoleGuard({ allow, children }: RoleGuardProps) {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (!user) {
    redirect("/login");
  }

  const { data: profile } = await supabase.from("profiles").select("role").eq("id", user.id).single();

  const role = profile?.role as AppRole | undefined;
  const allowed = Array.isArray(allow) ? allow : [allow];

  if (!role || !allowed.includes(role)) {
    redirect("/");
  }

  return <>{children}</>;
}
