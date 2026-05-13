import { createClient } from "@/lib/supabase/server";
import { NextResponse } from "next/server";

/**
 * Lightweight health check for cron-job.org (or uptime monitors).
 * Touches Supabase so a paused free project wakes up when hit.
 */
export async function GET() {
  try {
    const supabase = await createClient();
    // Use an anon-safe RPC (see supabase/migrations/002_rls.sql) — profiles are not readable by anon.
    const { error } = await supabase.rpc("list_completed_trips_public");
    if (error) {
      return NextResponse.json({ ok: false, error: error.message }, { status: 500 });
    }
    return NextResponse.json({ ok: true, at: new Date().toISOString() });
  } catch (e) {
    const message = e instanceof Error ? e.message : "unknown error";
    return NextResponse.json({ ok: false, error: message }, { status: 500 });
  }
}
