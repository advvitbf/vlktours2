-- 001_schema.sql — tables, triggers, and helper functions for a tours company app.
-- Run in Supabase SQL Editor or via `supabase db push` after linking the project.

-- Profiles mirror auth users (staff created in dashboard get role updated by owner).
create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  role text not null check (role in ('owner', 'manager', 'traveler')),
  display_name text not null default '',
  email text not null default '',
  created_at timestamptz not null default now()
);

create table if not exists public.trips (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  description text,
  start_date date not null,
  end_date date not null,
  status text not null check (status in ('draft', 'scheduled', 'completed', 'cancelled')),
  hero_image_url text,
  lead_manager_id uuid references public.profiles (id) on delete set null,
  whatsapp_url text default '',
  whatsapp_visible_to_members boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.trip_members (
  id uuid primary key default gen_random_uuid(),
  trip_id uuid not null references public.trips (id) on delete cascade,
  user_id uuid not null references public.profiles (id) on delete cascade,
  role_on_trip text not null check (role_on_trip in ('guide', 'guest')),
  unique (trip_id, user_id)
);

create index if not exists trips_lead_manager_id_idx on public.trips (lead_manager_id);
create index if not exists trip_members_trip_id_idx on public.trip_members (trip_id);
create index if not exists trip_members_user_id_idx on public.trip_members (user_id);

-- Auto-create a traveler profile when a new auth user is created (self-serve /signup).
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, display_name, role)
  values (
    new.id,
    coalesce(new.email, ''),
    coalesce(new.raw_user_meta_data->>'display_name', split_part(coalesce(new.email, 'user'), '@', 1)),
    'traveler'
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- Only company owners may change profile roles (travelers cannot promote themselves).
create or replace function public.enforce_profile_role_change()
returns trigger
language plpgsql
as $$
begin
  if new.role is distinct from old.role then
    if not exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'owner') then
      raise exception 'Only an owner may change roles';
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists profiles_role_guard on public.profiles;
create trigger profiles_role_guard
  before update on public.profiles
  for each row execute procedure public.enforce_profile_role_change();
