import { createClient } from "@/lib/supabase/server";

export type AppRole = "owner" | "manager" | "traveler";

export type Profile = {
  id: string;
  role: AppRole;
  display_name: string;
  email: string;
  created_at: string;
};

/** Loads the current Supabase user and their profile row (if any). */
export async function getSessionProfile() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return { supabase, user: null as null, profile: null as null };

  const { data: profile } = await supabase.from("profiles").select("*").eq("id", user.id).maybeSingle();
  return { supabase, user, profile: profile as Profile | null };
}
