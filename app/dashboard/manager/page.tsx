import type { Metadata } from "next";
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
import { addTripMemberManager, removeTripMemberManager, updateTripWhatsAppManager } from "./actions";

export const metadata: Metadata = { title: "Manager dashboard" };

type Trip = {
  id: string;
  title: string;
  status: string;
  start_date: string;
  end_date: string;
  whatsapp_url: string | null;
  whatsapp_visible_to_members: boolean;
};

/** Lists trips where the signed-in user is the lead manager and exposes WhatsApp controls. */
async function ManagerDashboard() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return null;

  const { data: myProfile } = await supabase.from("profiles").select("role").eq("id", user.id).maybeSingle();
  const canEditRoster = myProfile?.role === "manager" || myProfile?.role === "owner";

  const { data: trips } = await supabase
    .from("trips")
    .select("id, title, status, start_date, end_date, whatsapp_url, whatsapp_visible_to_members")
    .eq("lead_manager_id", user.id)
    .order("start_date", { ascending: true });

  const rows = (trips ?? []) as Trip[];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Manager dashboard</h1>
        <p className="text-muted-foreground">Trips where you are the assigned lead manager.</p>
      </div>

      {rows.length === 0 ? (
        <p className="text-sm text-muted-foreground">No assigned trips yet.</p>
      ) : (
        <div className="space-y-6">
          {rows.map((trip) => (
            <Card key={trip.id}>
              <CardHeader className="flex flex-row items-start justify-between gap-4">
                <div>
                  <CardTitle className="text-lg">{trip.title}</CardTitle>
                  <CardDescription>
                    {trip.start_date} → {trip.end_date}
                  </CardDescription>
                </div>
                <Badge className="capitalize">{trip.status}</Badge>
              </CardHeader>
              <CardContent className="space-y-4">
                <form action={updateTripWhatsAppManager} className="grid gap-3 md:grid-cols-2">
                  <input type="hidden" name="trip_id" value={trip.id} />
                  <div className="md:col-span-2 space-y-2">
                    <Label htmlFor={`wa-${trip.id}`}>WhatsApp group link</Label>
                    <Input
                      id={`wa-${trip.id}`}
                      name="whatsapp_url"
                      defaultValue={trip.whatsapp_url ?? ""}
                      placeholder="https://chat.whatsapp.com/..."
                    />
                  </div>
                  <label className="flex items-center gap-2 text-sm md:col-span-2">
                    <input type="checkbox" name="whatsapp_visible_to_members" value="on" defaultChecked={trip.whatsapp_visible_to_members} />
                    Visible to travelers on the trip page
                  </label>
                  <Button type="submit" size="sm">
                    Save WhatsApp settings
                  </Button>
                </form>

                <MembersPreview tripId={trip.id} canEdit={canEditRoster} />
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

/** Member list for a trip; managers/owners may edit roster when allowed by RLS (see migration 003). */
async function MembersPreview({ tripId, canEdit }: { tripId: string; canEdit: boolean }) {
  const supabase = await createClient();
  const { data } = await supabase
    .from("trip_members")
    .select("id, role_on_trip, profiles(display_name, email)")
    .eq("trip_id", tripId);

  const rows = data ?? [];
  const colCount = canEdit ? 4 : 3;

  return (
    <div className="space-y-3">
      <h3 className="mb-2 text-sm font-medium">Travelers & guides</h3>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Name</TableHead>
            <TableHead>Email</TableHead>
            <TableHead>Role on trip</TableHead>
            {canEdit ? <TableHead className="w-[100px]" /> : null}
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.length === 0 ? (
            <TableRow>
              <TableCell colSpan={colCount} className="text-muted-foreground">
                No members yet.
              </TableCell>
            </TableRow>
          ) : (
            rows.map((row: { id: string; role_on_trip: string; profiles?: { display_name?: string; email?: string } }) => (
              <TableRow key={row.id}>
                <TableCell>{row.profiles?.display_name}</TableCell>
                <TableCell>{row.profiles?.email}</TableCell>
                <TableCell className="capitalize">{row.role_on_trip}</TableCell>
                {canEdit ? (
                  <TableCell className="text-right">
                    <form action={removeTripMemberManager}>
                      <input type="hidden" name="trip_id" value={tripId} />
                      <input type="hidden" name="member_id" value={row.id} />
                      <Button type="submit" variant="destructive" size="sm">
                        Remove
                      </Button>
                    </form>
                  </TableCell>
                ) : null}
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>

      {canEdit ? (
        <form action={addTripMemberManager} className="grid gap-2 md:grid-cols-4 md:items-end">
          <input type="hidden" name="trip_id" value={tripId} />
          <div className="space-y-2 md:col-span-2">
            <Label>Member email</Label>
            <Input name="email" type="email" required placeholder="traveler@example.com" />
          </div>
          <div className="space-y-2">
            <Label>Role on trip</Label>
            <select
              name="role_on_trip"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="guest">guest</option>
              <option value="guide">guide</option>
            </select>
          </div>
          <Button type="submit" className="md:justify-self-start">
            Add member
          </Button>
        </form>
      ) : null}
    </div>
  );
}

export default function ManagerDashboardPage() {
  return (
    <RoleGuard allow={["manager", "owner"]}>
      <ManagerDashboard />
    </RoleGuard>
  );
}
