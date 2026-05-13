import Link from "next/link";
import { Button } from "@/components/ui/button";

/** Marketing-style home: quick links to login, signup, and public archive. */
export default function HomePage() {
  return (
    <main className="mx-auto flex min-h-screen max-w-3xl flex-col justify-center gap-8 px-6 py-16">
      <div>
        <h1 className="text-4xl font-bold tracking-tight">Tours & Travelers</h1>
        <p className="mt-2 text-muted-foreground">
          Sign in to your dashboard or browse past trips.
        </p>
      </div>
      <div className="flex flex-wrap gap-3">
        <Button asChild>
          <Link href="/login">Log in</Link>
        </Button>
        <Button variant="secondary" asChild>
          <Link href="/signup">Create traveler account</Link>
        </Button>
        <Button variant="outline" asChild>
          <Link href="/trips/archive">Past trips</Link>
        </Button>
      </div>
    </main>
  );
}
