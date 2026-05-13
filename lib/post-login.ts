"use server";

import { createClient } from "@/lib/supabase/server";

/**
 * Computes where to send a user immediately after a successful login.
 * Travelers without a trip assignment land on /pending.
 */
export async function getPostLoginPath(): Promise<string> {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return "/login";

  const { data: profile } = await supabase.from("profiles").select("role").eq("id", user.id).maybeSingle();
  if (!profile?.role) return "/pending";

  if (profile.role === "owner") return "/dashboard/owner";
  if (profile.role === "manager") return "/dashboard/manager";

  const { data: tripId } = await supabase.rpc("get_primary_trip_for_traveler");
  if (tripId) return `/trips/${tripId}`;
  return "/pending";
}
