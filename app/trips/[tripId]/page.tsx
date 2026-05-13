import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { WhatsAppBlock } from "@/components/WhatsAppBlock";
import Link from "next/link";
import { Button } from "@/components/ui/button";

type TripRow = {
  id: string;
  title: string;
  description: string | null;
  start_date: string;
  end_date: string;
  status: string;
  hero_image_url: string | null;
  lead_manager_id: string | null;
  whatsapp_url: string | null;
  whatsapp_visible_to_members: boolean;
  created_at: string;
};

type PageProps = { params: Promise<{ tripId: string }> };

export const dynamic = "force-dynamic";

/**
 * Traveler (or staff) trip detail — WhatsApp URL is loaded via RPC so it stays masked for travelers until released.
 */
export default async function TripDetailPage({ params }: PageProps) {
  const { tripId } = await params;
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) notFound();

  const { data, error } = await supabase.rpc("get_trip_for_viewer", { p_trip_id: tripId });
  if (error || !data?.length) notFound();

  const trip = data[0] as TripRow;

  return (
    <main className="mx-auto max-w-3xl space-y-8 px-4 py-10">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button variant="ghost" asChild>
          <Link href="/">Home</Link>
        </Button>
      </div>
      {trip.hero_image_url ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={trip.hero_image_url} alt="" className="h-56 w-full rounded-lg object-cover" />
      ) : (
        <div className="h-56 w-full rounded-lg bg-muted" />
      )}
      <header className="space-y-2">
        <p className="text-sm uppercase text-muted-foreground">{trip.status}</p>
        <h1 className="text-3xl font-bold tracking-tight">{trip.title}</h1>
        <p className="text-muted-foreground">
          {trip.start_date} → {trip.end_date}
        </p>
      </header>
      {trip.description ? (
        <article className="prose prose-neutral max-w-none dark:prose-invert">
          <p className="whitespace-pre-wrap leading-relaxed">{trip.description}</p>
        </article>
      ) : null}

      <WhatsAppBlock
        whatsappUrl={trip.whatsapp_url}
        whatsappVisibleToMembers={trip.whatsapp_visible_to_members}
      />
    </main>
  );
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { tripId } = await params;
  return { title: `Trip ${tripId.slice(0, 8)}…` };
}
