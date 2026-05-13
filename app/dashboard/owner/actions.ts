"use server";

import { revalidatePath } from "next/cache";
import { getSessionProfile } from "@/lib/session-profile";

function assertOwner(profile: { role: string } | null) {
  if (!profile || profile.role !== "owner") {
    throw new Error("Forbidden");
  }
}

/** Creates a trip (owner only). */
export async function createTrip(formData: FormData) {
  const { supabase, user, profile } = await getSessionProfile();
  assertOwner(profile);
  if (!user) throw new Error("Unauthorized");

  const title = String(formData.get("title") ?? "").trim();
  const description = String(formData.get("description") ?? "").trim();
  const start_date = String(formData.get("start_date") ?? "");
  const end_date = String(formData.get("end_date") ?? "");
  const status = String(formData.get("status") ?? "draft");
  const hero_image_url = String(formData.get("hero_image_url") ?? "").trim();

  if (!title || !start_date || !end_date) throw new Error("Missing required fields");

  const { error } = await supabase.from("trips").insert({
    title,
    description: description || null,
    start_date,
    end_date,
    status,
    hero_image_url: hero_image_url || null,
  });
  if (error) throw new Error(error.message);
  revalidatePath("/dashboard/owner");
}

/** Updates core trip fields (owner only). */
export async function updateTripCore(formData: FormData) {
  const { supabase, profile } = await getSessionProfile();
  assertOwner(profile);

  const id = String(formData.get("trip_id") ?? "");
  if (!id) throw new Error("Missing trip");

  const patch = {
    title: String(formData.get("title") ?? "").trim(),
    description: String(formData.get("description") ?? "").trim() || null,
    start_date: String(formData.get("start_date") ?? ""),
    end_date: String(formData.get("end_date") ?? ""),
    status: String(formData.get("status") ?? "draft"),
    hero_image_url: String(formData.get("hero_image_url") ?? "").trim() || null,
  };

  const { error } = await supabase.from("trips").update(patch).eq("id", id);
  if (error) throw new Error(error.message);
  revalidatePath("/dashboard/owner");
}

/** Assigns or clears the lead manager for a trip (owner only). */
export async function updateTripManager(formData: FormData) {
  const { supabase, profile } = await getSessionProfile();
  assertOwner(profile);

  const id = String(formData.get("trip_id") ?? "");
  const lead_manager_id = String(formData.get("lead_manager_id") ?? "").trim();

  const { error } = await supabase
    .from("trips")
    .update({ lead_manager_id: lead_manager_id ? lead_manager_id : null })
    .eq("id", id);
  if (error) throw new Error(error.message);
  revalidatePath("/dashboard/owner");
}

/** Updates WhatsApp fields (owner only). */
export async function updateTripWhatsAppOwner(formData: FormData) {
  const { supabase, profile } = await getSessionProfile();
  assertOwner(profile);

  const id = String(formData.get("trip_id") ?? "");
  const whatsapp_url = String(formData.get("whatsapp_url") ?? "");
  const whatsapp_visible_to_members = String(formData.get("whatsapp_visible_to_members") ?? "") === "on";

  const { error } = await supabase.from("trips").update({ whatsapp_url, whatsapp_visible_to_members }).eq("id", id);
  if (error) throw new Error(error.message);
  revalidatePath("/dashboard/owner");
}

/** Adds a traveler/guide to a trip by profile email (owner only). */
export async function addTripMember(formData: FormData) {
  const { supabase, profile } = await getSessionProfile();
  assertOwner(profile);

  const trip_id = String(formData.get("trip_id") ?? "");
  const email = String(formData.get("email") ?? "").trim().toLowerCase();
  const role_on_trip = String(formData.get("role_on_trip") ?? "guest");

  if (!trip_id || !email) throw new Error("Missing fields");

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
  revalidatePath("/dashboard/owner");
}

/** Removes a roster row (owner only). */
export async function removeTripMember(formData: FormData) {
  const { supabase, profile } = await getSessionProfile();
  assertOwner(profile);

  const id = String(formData.get("member_id") ?? "");
  const { error } = await supabase.from("trip_members").delete().eq("id", id);
  if (error) throw new Error(error.message);
  revalidatePath("/dashboard/owner");
}

/** Updates a user role (owner only — enforced in DB too). */
export async function updateUserRole(formData: FormData) {
  const { supabase, profile } = await getSessionProfile();
  assertOwner(profile);

  const id = String(formData.get("user_id") ?? "");
  const role = String(formData.get("role") ?? "traveler");
  if (!["owner", "manager", "traveler"].includes(role)) throw new Error("Invalid role");

  const { error } = await supabase.from("profiles").update({ role }).eq("id", id);
  if (error) throw new Error(error.message);
  revalidatePath("/dashboard/owner");
}
