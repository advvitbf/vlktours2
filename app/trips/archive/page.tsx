import type { Metadata } from "next";
import { createClient } from "@/lib/supabase/server";
import { TripCard } from "@/components/TripCard";

export const metadata: Metadata = {
  title: "Past trips",
  description: "Browse completed tours and adventures.",
  openGraph: {
    title: "Past trips | Tours & Travelers",
    description: "Browse completed tours and adventures.",
  },
};

type PublicTrip = {
  id: string;
  title: string;
  description: string | null;
  start_date: string;
  end_date: string;
  hero_image_url: string | null;
  created_at: string;
};

/** Public gallery of completed trips — no WhatsApp data is queried or shown. */
export default async function ArchivePage() {
  const supabase = await createClient();
  const { data, error } = await supabase.rpc("list_completed_trips_public");

  const trips = (data ?? []) as PublicTrip[];

  return (
    <main className="mx-auto max-w-6xl px-4 py-12">
      <div className="mb-10 space-y-2">
        <h1 className="text-3xl font-bold tracking-tight">Past trips</h1>
        <p className="text-muted-foreground">Completed journeys (public gallery).</p>
      </div>
      {error ? (
        <p className="text-sm text-destructive">Could not load trips. Did you run SQL migrations and grant RPC to anon?</p>
      ) : trips.length === 0 ? (
        <p className="text-sm text-muted-foreground">No completed trips yet.</p>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {trips.map((t) => (
            <TripCard
              key={t.id}
              trip={{
                ...t,
                status: "completed",
              }}
            />
          ))}
        </div>
      )}
    </main>
  );
}
