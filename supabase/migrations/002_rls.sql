-- 002_rls.sql — Row Level Security + RPCs that never leak WhatsApp URLs incorrectly.

alter table public.profiles enable row level security;
alter table public.trips enable row level security;
alter table public.trip_members enable row level security;

-- ---------- profiles ----------
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles
  for select using (
    auth.uid() = id
    or exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'owner')
    or exists (
      select 1
      from public.trip_members tm
      join public.trips t on t.id = tm.trip_id
      where tm.user_id = profiles.id
        and t.lead_manager_id = auth.uid()
    )
  );

drop policy if exists profiles_insert_owner on public.profiles;
create policy profiles_insert_owner on public.profiles
  for insert with check (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'owner')
  );

drop policy if exists profiles_update_self on public.profiles;
create policy profiles_update_self on public.profiles
  for update
  using (auth.uid() = id)
  with check (auth.uid() = id);

drop policy if exists profiles_update_owner on public.profiles;
create policy profiles_update_owner on public.profiles
  for update using (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'owner')
  );

drop policy if exists profiles_delete_owner on public.profiles;
create policy profiles_delete_owner on public.profiles
  for delete using (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'owner')
  );

-- ---------- trips (travelers must NOT select rows directly — use RPC for masked URL) ----------
drop policy if exists trips_owner_all on public.trips;
create policy trips_owner_all on public.trips
  for all using (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'owner')
  )
  with check (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'owner')
  );

drop policy if exists trips_manager_select on public.trips;
create policy trips_manager_select on public.trips
  for select using (lead_manager_id = auth.uid());

drop policy if exists trips_manager_update on public.trips;
create policy trips_manager_update on public.trips
  for update
  using (lead_manager_id = auth.uid())
  with check (lead_manager_id = auth.uid());

-- ---------- trip_members ----------
drop policy if exists trip_members_owner_all on public.trip_members;
create policy trip_members_owner_all on public.trip_members
  for all using (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'owner')
  )
  with check (
    exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'owner')
  );

drop policy if exists trip_members_manager_select on public.trip_members;
create policy trip_members_manager_select on public.trip_members
  for select using (
    exists (
      select 1 from public.trips t
      where t.id = trip_members.trip_id
        and t.lead_manager_id = auth.uid()
    )
  );

drop policy if exists trip_members_traveler_own on public.trip_members;
create policy trip_members_traveler_own on public.trip_members
  for select using (user_id = auth.uid());

-- ---------- RPC: public completed trips (no WhatsApp column) ----------
create or replace function public.list_completed_trips_public()
returns table (
  id uuid,
  title text,
  description text,
  start_date date,
  end_date date,
  hero_image_url text,
  created_at timestamptz
)
language sql
security definer
set search_path = public
as $$
  select
    t.id,
    t.title,
    t.description,
    t.start_date,
    t.end_date,
    t.hero_image_url,
    t.created_at
  from public.trips t
  where t.status = 'completed'
  order by t.end_date desc nulls last;
$$;

grant execute on function public.list_completed_trips_public() to anon, authenticated;

-- ---------- RPC: single trip with masked WhatsApp for travelers ----------
create or replace function public.get_trip_for_viewer(p_trip_id uuid)
returns table (
  id uuid,
  title text,
  description text,
  start_date date,
  end_date date,
  status text,
  hero_image_url text,
  lead_manager_id uuid,
  whatsapp_url text,
  whatsapp_visible_to_members boolean,
  created_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  r text;
  is_member boolean;
  is_manager boolean;
  is_company_owner boolean;
begin
  if uid is null then
    raise exception 'not authenticated';
  end if;

  select p.role into r from public.profiles p where p.id = uid;
  if r is null then
    raise exception 'no profile';
  end if;

  is_company_owner := r = 'owner';
  is_manager := exists (
    select 1 from public.trips t where t.id = p_trip_id and t.lead_manager_id = uid
  );
  is_member := exists (
    select 1 from public.trip_members m where m.trip_id = p_trip_id and m.user_id = uid
  );

  if is_company_owner or is_manager then
    return query
      select
        t.id,
        t.title,
        t.description,
        t.start_date,
        t.end_date,
        t.status,
        t.hero_image_url,
        t.lead_manager_id,
        t.whatsapp_url,
        t.whatsapp_visible_to_members,
        t.created_at
      from public.trips t
      where t.id = p_trip_id;
    return;
  end if;

  if r = 'traveler' and is_member then
    return query
      select
        t.id,
        t.title,
        t.description,
        t.start_date,
        t.end_date,
        t.status,
        t.hero_image_url,
        t.lead_manager_id,
        case when t.whatsapp_visible_to_members then t.whatsapp_url else null end as whatsapp_url,
        t.whatsapp_visible_to_members,
        t.created_at
      from public.trips t
      where t.id = p_trip_id;
    return;
  end if;

  raise exception 'forbidden';
end;
$$;

grant execute on function public.get_trip_for_viewer(uuid) to authenticated;

-- ---------- RPC: traveler landing after login ----------
create or replace function public.get_primary_trip_for_traveler()
returns uuid
language sql
security definer
set search_path = public
as $$
  select tm.trip_id
  from public.trip_members tm
  join public.trips t on t.id = tm.trip_id
  where tm.user_id = auth.uid()
    and t.status in ('draft', 'scheduled')
  order by t.start_date asc nulls last
  limit 1;
$$;

grant execute on function public.get_primary_trip_for_traveler() to authenticated;
