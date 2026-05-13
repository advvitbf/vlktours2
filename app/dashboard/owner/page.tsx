import type { Metadata } from "next";
import Link from "next/link";
import { RoleGuard } from "@/components/RoleGuard";
import { createClient } from "@/lib/supabase/server";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  addTripMember,
  createTrip,
  removeTripMember,
  updateTripCore,
  updateTripManager,
  updateTripWhatsAppOwner,
  updateUserRole,
} from "./actions";

export const metadata: Metadata = { title: "Owner dashboard" };

type Trip = {
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
};

type ProfileRow = { id: string; display_name: string; email: string; role: string };

async function OwnerDashboard() {
  const supabase = await createClient();

  const { count: totalTrips } = await supabase.from("trips").select("*", { count: "exact", head: true });
  const { count: activeTrips } = await supabase
    .from("trips")
    .select("*", { count: "exact", head: true })
    .eq("status", "scheduled");
  const { count: travelerRows } = await supabase
    .from("trip_members")
    .select("*", { count: "exact", head: true })
    .eq("role_on_trip", "guest");

  const { data: trips } = await supabase
    .from("trips")
    .select(
      "id, title, description, start_date, end_date, status, hero_image_url, lead_manager_id, whatsapp_url, whatsapp_visible_to_members"
    )
    .order("start_date", { ascending: false });

  const { data: profiles } = await supabase
    .from("profiles")
    .select("id, display_name, email, role")
    .order("created_at", { ascending: false });

  const { data: managers } = await supabase.from("profiles").select("id, display_name, email").eq("role", "manager");

  const tripRows = (trips ?? []) as Trip[];
  const profileRows = (profiles ?? []) as ProfileRow[];
  const managerRows = (managers ?? []) as ProfileRow[];

  return (
    <div className="space-y-10">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Owner dashboard</h1>
        <p className="text-muted-foreground">Manage trips, rosters, WhatsApp links, and staff roles.</p>
      </div>

      <section className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium text-muted-foreground">Total trips</CardTitle>
          </CardHeader>
          <CardContent className="text-3xl font-bold">{totalTrips ?? 0}</CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium text-muted-foreground">Scheduled trips</CardTitle>
          </CardHeader>
          <CardContent className="text-3xl font-bold">{activeTrips ?? 0}</CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium text-muted-foreground">Guest roster rows</CardTitle>
          </CardHeader>
          <CardContent className="text-3xl font-bold">{travelerRows ?? 0}</CardContent>
        </Card>
      </section>

      <Card>
        <CardHeader>
          <CardTitle>Create trip</CardTitle>
          <CardDescription>Add a new itinerary shell — you can assign a manager and members afterwards.</CardDescription>
        </CardHeader>
        <CardContent>
          <form action={createTrip} className="grid gap-3 md:grid-cols-2">
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="title">Title</Label>
              <Input id="title" name="title" required placeholder="Himalayan spring trek" />
            </div>
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="description">Description</Label>
              <Input id="description" name="description" placeholder="Short summary for dashboards" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="start_date">Start date</Label>
              <Input id="start_date" name="start_date" type="date" required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="end_date">End date</Label>
              <Input id="end_date" name="end_date" type="date" required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="status">Status</Label>
              <select
                id="status"
                name="status"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                defaultValue="draft"
              >
                <option value="draft">draft</option>
                <option value="scheduled">scheduled</option>
                <option value="completed">completed</option>
                <option value="cancelled">cancelled</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="hero_image_url">Hero image URL</Label>
              <Input id="hero_image_url" name="hero_image_url" placeholder="https://..." />
            </div>
            <div className="md:col-span-2">
              <Button type="submit">Create trip</Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <section className="space-y-4">
        <h2 className="text-lg font-semibold">Trips</h2>
        {tripRows.length === 0 ? (
          <p className="text-sm text-muted-foreground">No trips yet.</p>
        ) : (
          <div className="space-y-6">
            {tripRows.map((trip) => (
              <Card key={trip.id}>
                <CardHeader className="flex flex-row flex-wrap items-start justify-between gap-3">
                  <div>
                    <CardTitle className="text-lg">{trip.title}</CardTitle>
                    <CardDescription className="flex flex-wrap gap-2">
                      <span>
                        {trip.start_date} → {trip.end_date}
                      </span>
                      <Badge className="capitalize">{trip.status}</Badge>
                    </CardDescription>
                  </div>
                  <Button variant="outline" size="sm" asChild>
                    <Link href={`/trips/${trip.id}`}>Open trip page</Link>
                  </Button>
                </CardHeader>
                <CardContent className="space-y-6">
                  <form action={updateTripCore} className="grid gap-3 md:grid-cols-2">
                    <input type="hidden" name="trip_id" value={trip.id} />
                    <div className="space-y-2 md:col-span-2">
                      <Label>Title</Label>
                      <Input name="title" defaultValue={trip.title} required />
                    </div>
                    <div className="space-y-2 md:col-span-2">
                      <Label>Description</Label>
                      <Input name="description" defaultValue={trip.description ?? ""} />
                    </div>
                    <div className="space-y-2">
                      <Label>Start date</Label>
                      <Input name="start_date" type="date" defaultValue={trip.start_date} required />
                    </div>
                    <div className="space-y-2">
                      <Label>End date</Label>
                      <Input name="end_date" type="date" defaultValue={trip.end_date} required />
                    </div>
                    <div className="space-y-2">
                      <Label>Status</Label>
                      <select
                        name="status"
                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        defaultValue={trip.status}
                      >
                        {["draft", "scheduled", "completed", "cancelled"].map((s) => (
                          <option key={s} value={s}>
                            {s}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="space-y-2">
                      <Label>Hero image URL</Label>
                      <Input name="hero_image_url" defaultValue={trip.hero_image_url ?? ""} />
                    </div>
                    <div className="md:col-span-2">
                      <Button type="submit" size="sm">
                        Save trip details
                      </Button>
                    </div>
                  </form>

                  <form action={updateTripManager} className="flex flex-wrap items-end gap-3">
                    <input type="hidden" name="trip_id" value={trip.id} />
                    <div className="space-y-2">
                      <Label>Lead manager</Label>
                      <select
                        name="lead_manager_id"
                        className="flex h-10 min-w-[220px] rounded-md border border-input bg-background px-3 py-2 text-sm"
                        defaultValue={trip.lead_manager_id ?? ""}
                      >
                        <option value="">— None —</option>
                        {managerRows.map((m) => (
                          <option key={m.id} value={m.id}>
                            {m.display_name} ({m.email})
                          </option>
                        ))}
                      </select>
                    </div>
                    <Button type="submit" size="sm">
                      Save manager
                    </Button>
                  </form>

                  <form action={updateTripWhatsAppOwner} className="grid gap-3 md:grid-cols-2">
                    <input type="hidden" name="trip_id" value={trip.id} />
                    <div className="space-y-2 md:col-span-2">
                      <Label>WhatsApp group link</Label>
                      <Input name="whatsapp_url" defaultValue={trip.whatsapp_url ?? ""} placeholder="https://chat.whatsapp.com/..." />
                    </div>
                    <label className="flex items-center gap-2 text-sm md:col-span-2">
                      <input
                        type="checkbox"
                        name="whatsapp_visible_to_members"
                        value="on"
                        defaultChecked={trip.whatsapp_visible_to_members}
                      />
                      Visible to travelers
                    </label>
                    <Button type="submit" size="sm">
                      Save WhatsApp
                    </Button>
                  </form>

                  <TripMembersOwner tripId={trip.id} />
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </section>

      <Card>
        <CardHeader>
          <CardTitle>Users & roles</CardTitle>
          <CardDescription>Promote managers or owners carefully — only owners may change roles.</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Role</TableHead>
                <TableHead className="w-[200px]">Change role</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {profileRows.map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{p.display_name}</TableCell>
                  <TableCell>{p.email}</TableCell>
                  <TableCell className="capitalize">{p.role}</TableCell>
                  <TableCell>
                    <form action={updateUserRole} className="flex flex-wrap items-center gap-2">
                      <input type="hidden" name="user_id" value={p.id} />
                      <select
                        name="role"
                        className="flex h-9 rounded-md border border-input bg-background px-2 text-sm"
                        defaultValue={p.role}
                      >
                        <option value="traveler">traveler</option>
                        <option value="manager">manager</option>
                        <option value="owner">owner</option>
                      </select>
                      <Button type="submit" size="sm" variant="secondary">
                        Save
                      </Button>
                    </form>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}

async function TripMembersOwner({ tripId }: { tripId: string }) {
  const supabase = await createClient();
  const { data } = await supabase
    .from("trip_members")
    .select("id, role_on_trip, profiles(display_name, email)")
    .eq("trip_id", tripId);

  const rows = data ?? [];

  return (
    <div className="space-y-3">
      <h3 className="text-sm font-medium">Trip roster</h3>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Name</TableHead>
            <TableHead>Email</TableHead>
            <TableHead>Role</TableHead>
            <TableHead className="w-[120px]" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.length === 0 ? (
            <TableRow>
              <TableCell colSpan={4} className="text-muted-foreground">
                No members yet.
              </TableCell>
            </TableRow>
          ) : (
            rows.map((row: any) => (
              <TableRow key={row.id}>
                <TableCell>{row.profiles?.display_name}</TableCell>
                <TableCell>{row.profiles?.email}</TableCell>
                <TableCell className="capitalize">{row.role_on_trip}</TableCell>
                <TableCell className="text-right">
                  <form action={removeTripMember}>
                    <input type="hidden" name="member_id" value={row.id} />
                    <Button type="submit" variant="destructive" size="sm">
                      Remove
                    </Button>
                  </form>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>

      <form action={addTripMember} className="grid gap-2 md:grid-cols-4 md:items-end">
        <input type="hidden" name="trip_id" value={tripId} />
        <div className="space-y-2 md:col-span-2">
          <Label>Member email (must already have an account)</Label>
          <Input name="email" type="email" required placeholder="traveler@example.com" />
        </div>
        <div className="space-y-2">
          <Label>Role on trip</Label>
          <select name="role_on_trip" className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm">
            <option value="guest">guest</option>
            <option value="guide">guide</option>
          </select>
        </div>
        <Button type="submit" className="md:justify-self-start">
          Add member
        </Button>
      </form>
    </div>
  );
}

export default function OwnerDashboardPage() {
  return (
    <RoleGuard allow="owner">
      <OwnerDashboard />
    </RoleGuard>
  );
}
