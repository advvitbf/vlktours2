import Link from "next/link";
import { SignOutButton } from "@/components/SignOutButton";

/** Shared chrome for authenticated dashboard areas. */
export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-muted/30">
      <header className="border-b bg-background">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <nav className="flex flex-wrap items-center gap-3 text-sm">
            <Link href="/" className="font-semibold">
              Tours & Travelers
            </Link>
            <Link href="/dashboard/owner" className="text-muted-foreground hover:text-foreground">
              Owner
            </Link>
            <Link href="/dashboard/manager" className="text-muted-foreground hover:text-foreground">
              Manager
            </Link>
            <Link href="/trips/archive" className="text-muted-foreground hover:text-foreground">
              Archive
            </Link>
          </nav>
          <SignOutButton />
        </div>
      </header>
      <div className="mx-auto max-w-6xl px-4 py-8">{children}</div>
    </div>
  );
}
