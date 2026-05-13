-- 003_trip_members_manager_write.sql — managers may mutate roster rows for trips they lead (per plan RLS summary).

drop policy if exists trip_members_manager_insert on public.trip_members;
create policy trip_members_manager_insert on public.trip_members
  for insert with check (
    exists (
      select 1 from public.trips t
      where t.id = trip_members.trip_id
        and t.lead_manager_id = auth.uid()
    )
  );

drop policy if exists trip_members_manager_update on public.trip_members;
create policy trip_members_manager_update on public.trip_members
  for update
  using (
    exists (
      select 1 from public.trips t
      where t.id = trip_members.trip_id
        and t.lead_manager_id = auth.uid()
    )
  )
  with check (
    exists (
      select 1 from public.trips t
      where t.id = trip_members.trip_id
        and t.lead_manager_id = auth.uid()
    )
  );

drop policy if exists trip_members_manager_delete on public.trip_members;
create policy trip_members_manager_delete on public.trip_members
  for delete using (
    exists (
      select 1 from public.trips t
      where t.id = trip_members.trip_id
        and t.lead_manager_id = auth.uid()
    )
  );
