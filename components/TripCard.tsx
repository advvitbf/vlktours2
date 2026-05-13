import Link from "next/link";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export type TripCardTrip = {
  id: string;
  title: string;
  description: string | null;
  start_date: string;
  end_date: string;
  status: string;
  hero_image_url: string | null;
};

type TripCardProps = {
  trip: TripCardTrip;
  /** When set, the whole card links to this href (e.g. traveler trip page). */
  href?: string;
  className?: string;
};

/**
 * Reusable trip summary — dashboards and public archive.
 */
export function TripCard({ trip, href, className }: TripCardProps) {
  const inner = (
    <Card className={cn("overflow-hidden transition-shadow hover:shadow-md", href && "cursor-pointer", className)}>
      {trip.hero_image_url ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={trip.hero_image_url} alt="" className="h-40 w-full object-cover" />
      ) : (
        <div className="h-40 w-full bg-muted" />
      )}
      <CardHeader className="space-y-1">
        <div className="flex items-start justify-between gap-2">
          <CardTitle className="line-clamp-2 text-lg">{trip.title}</CardTitle>
          <Badge variant="secondary" className="shrink-0 capitalize">
            {trip.status}
          </Badge>
        </div>
        <CardDescription>
          {trip.start_date} → {trip.end_date}
        </CardDescription>
      </CardHeader>
      {trip.description ? (
        <CardContent>
          <p className="line-clamp-3 text-sm text-muted-foreground">{trip.description}</p>
        </CardContent>
      ) : null}
    </Card>
  );

  if (href) {
    return (
      <Link href={href} className="block focus:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-lg">
        {inner}
      </Link>
    );
  }

  return inner;
}
