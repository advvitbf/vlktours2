"use server";

import { revalidatePath } from "next/cache";
import { getSessionProfile } from "@/lib/session-profile";

function assertManager(profile: { role: string } | null) {
  if (!profile || (profile.role !== "manager" && profile.role !== "owner")) {
    throw new Error("Forbidden");
  }
}

/** Managers (and owners) can update WhatsApp fields only for trips they lead. */
export async function updateTripWhatsAppManager(formData: FormData) {
  const { supabase, user, profile } = await getSessionProfile();
  assertManager(profile);
  if (!user) throw new Error("Unauthorized");

  const trip_id = String(formData.get("trip_id") ?? "");
  const whatsapp_url = String(formData.get("whatsapp_url") ?? "");
  const whatsapp_visible_to_members = formData.get("whatsapp_visible_to_members") === "on";

  if (!trip_id) throw new Error("Missing trip");

  if (profile!.role === "manager") {
    const { data: trip, error } = await supabase
      .from("trips")
      .select("id")
      .eq("id", trip_id)
      .eq("lead_manager_id", user.id)
      .maybeSingle();
    if (error) throw new Error(error.message);
    if (!trip) throw new Error("Not assigned to this trip");
  }

  const { error } = await supabase.from("trips").update({ whatsapp_url, whatsapp_visible_to_members }).eq("id", trip_id);
  if (error) throw new Error(error.message);
  revalidatePath("/dashboard/manager");
  revalidatePath("/dashboard/owner");
}

/** Lead managers may add roster rows for their assigned trips (RLS must allow insert). */
export async function addTripMemberManager(formData: FormData) {
  const { supabase, user, profile } = await getSessionProfile();
  assertManager(profile);
  if (!user) throw new Error("Unauthorized");

  const trip_id = String(formData.get("trip_id") ?? "");
  const email = String(formData.get("email") ?? "").trim().toLowerCase();
  const role_on_trip = String(formData.get("role_on_trip") ?? "guest");

  if (!trip_id || !email) throw new Error("Missing fields");

  if (profile!.role === "manager") {
    const { data: trip, error: te } = await supabase
      .from("trips")
      .select("id")
      .eq("id", trip_id)
      .eq("lead_manager_id", user.id)
      .maybeSingle();
    if (te) throw new Error(te.message);
    if (!trip) throw new Error("Not assigned to this trip");
  }

  const { data: memberProfile, error: pe } = await supabase
    .from("profiles")
    .select("id")
    .ilike("email", email)
    .maybeSingle();
  if (pe) throw new Error(pe.message);
  if (!memberProfile) throw new Error("No profile with that email");

  const { error } = await supabase.from("trip_members").insert({
    trip_id,
    user_id: memberProfile.id,
    role_on_trip: role_on_trip === "guide" ? "guide" : "guest",
  });
  if (error) throw new Error(error.message);
  revalidatePath("/dashboard/manager");
  revalidatePath("/dashboard/owner");
}

/** Lead managers may remove roster rows for their assigned trips. */
export async function removeTripMemberManager(formData: FormData) {
  const { supabase, user, profile } = await getSessionProfile();
  assertManager(profile);
  if (!user) throw new Error("Unauthorized");

  const member_id = String(formData.get("member_id") ?? "");
  const trip_id = String(formData.get("trip_id") ?? "");
  if (!member_id || !trip_id) throw new Error("Missing fields");

  if (profile!.role === "manager") {
    const { data: trip, error: te } = await supabase
      .from("trips")
      .select("id")
      .eq("id", trip_id)
      .eq("lead_manager_id", user.id)
      .maybeSingle();
    if (te) throw new Error(te.message);
    if (!trip) throw new Error("Not assigned to this trip");
  }

  const { error } = await supabase.from("trip_members").delete().eq("id", member_id).eq("trip_id", trip_id);
  if (error) throw new Error(error.message);
  revalidatePath("/dashboard/manager");
  revalidatePath("/dashboard/owner");
}
