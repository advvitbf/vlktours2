# Tours & Travelers (Next.js + Supabase)

Full-stack starter for a tours company: **email/password auth**, **owner / manager / traveler** roles, **WhatsApp link gating**, **public archive**, and a **`/api/ping`** route for [cron-job.org](https://cron-job.org) keep-alive pings.

## Prerequisites

- Node.js 18+
- A [Supabase](https://supabase.com/) project (free tier)

## 1. Configure Supabase

1. Create a project in Supabase.
2. In **SQL Editor**, run (in order):

   - `supabase/migrations/001_schema.sql`
   - `supabase/migrations/002_rls.sql`
   - `supabase/migrations/003_trip_members_manager_write.sql` (manager roster writes)

3. **Authentication → Providers**: enable **Email**; for local testing you may **disable “Confirm email”** so `/signup` returns a session immediately.

4. **Authentication → Users**: create your first staff user (or sign up then promote — see below).

5. **Bootstrap the first owner** (one-time):

   - If you self-registered at `/signup`, your `profiles.role` defaults to `traveler`. Run:

   ```sql
   update public.profiles set role = 'owner' where email = 'you@example.com';
   ```

6. **Managers**: promote a user to `manager` from the owner dashboard, or run SQL:

   ```sql
   update public.profiles set role = 'manager' where email = 'guide@example.com';
   ```

## 2. Configure the Next.js app

```bash
cd tours-company
cp .env.local.example .env.local
# fill NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_ANON_KEY
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## 3. Deploy (Vercel + GitHub)

- Push this folder to GitHub and import the repo in Vercel.
- Add the same `NEXT_PUBLIC_*` env vars in Vercel.
- Optional: add a cron job hitting `https://YOUR_DOMAIN/api/ping` every ~4 days.

## Project map

| Path | Purpose |
|------|---------|
| `app/(auth)/login` | Email login |
| `app/(auth)/signup` | Traveler self-signup |
| `app/dashboard/owner` | Full company controls |
| `app/dashboard/manager` | Assigned trips + WhatsApp |
| `app/trips/[tripId]` | Masked WhatsApp for travelers |
| `app/trips/archive` | Public completed trips (RPC) |
| `app/api/ping` | Keep-alive / monitoring |
| `supabase/migrations/003_trip_members_manager_write.sql` | Manager INSERT/UPDATE/DELETE on `trip_members` for led trips |

## Notes

- **WhatsApp URLs** for travelers are masked by the `get_trip_for_viewer` RPC, not only in the UI.
- **Staff** accounts can be created in the Supabase dashboard; ensure a matching `profiles` row exists (the `handle_new_user` trigger creates one on auth insert).
- If SQL errors on `EXECUTE PROCEDURE` vs `EXECUTE FUNCTION` for triggers, switch to the syntax your Postgres version expects (Supabase shows the error in the SQL editor).
