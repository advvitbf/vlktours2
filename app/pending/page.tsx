import type { Metadata } from "next";
import Link from "next/link";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export const metadata: Metadata = {
  title: "Pending assignment",
};

/**
 * Shown to signed-in travelers who are not yet on any active trip roster.
 */
export default function PendingPage() {
  return (
    <main className="mx-auto flex min-h-screen max-w-lg flex-col justify-center gap-6 px-4 py-12">
      <Card>
        <CardHeader>
          <CardTitle>Almost there</CardTitle>
          <CardDescription>
            Your account is active, but you are not on a trip roster yet. Ask your tour operator to add you to a trip in
            the owner dashboard.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex gap-3">
          <Link href="/login" className="text-sm text-primary underline-offset-4 hover:underline">
            Back to login
          </Link>
          <Link href="/trips/archive" className="text-sm text-muted-foreground underline-offset-4 hover:underline">
            Browse past trips
          </Link>
        </CardContent>
      </Card>
    </main>
  );
}
