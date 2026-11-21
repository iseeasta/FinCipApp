-- FinCip optional cloud sync + live household budgets
-- Run this in the Supabase SQL Editor for the project you control.
-- All personal app data is scoped to auth.uid() with row-level security.

create extension if not exists pgcrypto;

create table if not exists public.user_finance_data (
  user_id uuid primary key references auth.users(id) on delete cascade,
  payload jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);
alter table public.user_finance_data enable row level security;
drop policy if exists "Users can access their own FinCip data" on public.user_finance_data;
create policy "Users can access their own FinCip data"
  on public.user_finance_data for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

grant select, insert, update, delete on public.user_finance_data to authenticated;

create table if not exists public.households (
  id uuid primary key default gen_random_uuid(),
  name text not null check (length(trim(name)) between 1 and 80),
  invite_code text not null unique default upper(substr(encode(gen_random_bytes(16), 'hex'), 1, 16)),
  owner_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create table if not exists public.household_members (
  household_id uuid not null references public.households(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'member' check (role in ('owner', 'member')),
  joined_at timestamptz not null default now(),
  primary key (household_id, user_id)
);

create table if not exists public.shared_budgets (
  id uuid primary key default gen_random_uuid(),
  household_id uuid not null references public.households(id) on delete cascade,
  category text not null check (length(trim(category)) between 1 and 80),
  monthly_limit numeric(14,2) not null check (monthly_limit > 0),
  currency text not null default 'INR' check (currency in ('INR', 'USD', 'EUR')),
  created_by uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (household_id, category)
);

create index if not exists household_members_user_idx on public.household_members(user_id);
create unique index if not exists household_members_single_household_user_idx on public.household_members(user_id);
create index if not exists shared_budgets_household_idx on public.shared_budgets(household_id);

-- SECURITY DEFINER avoids recursive RLS evaluation on household_members.
create or replace function public.fincip_is_household_member(p_household_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select exists (
    select 1 from public.household_members hm
    where hm.household_id = p_household_id and hm.user_id = auth.uid()
  );
$$;
revoke all on function public.fincip_is_household_member(uuid) from public;
grant execute on function public.fincip_is_household_member(uuid) to authenticated;

alter table public.households enable row level security;
alter table public.household_members enable row level security;
alter table public.shared_budgets enable row level security;

drop policy if exists "Members can read their household" on public.households;
create policy "Members can read their household" on public.households
  for select using (public.fincip_is_household_member(id));
drop policy if exists "Authenticated users can create their own household" on public.households;
create policy "Authenticated users can create their own household" on public.households
  for insert with check (auth.uid() = owner_id);
drop policy if exists "Owners can update their household" on public.households;
create policy "Owners can update their household" on public.households
  for update using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
drop policy if exists "Owners can delete their household" on public.households;
create policy "Owners can delete their household" on public.households
  for delete using (auth.uid() = owner_id);

drop policy if exists "Members can read household memberships" on public.household_members;
create policy "Members can read household memberships" on public.household_members
  for select using (public.fincip_is_household_member(household_id));
drop policy if exists "Users can leave their own household" on public.household_members;
create policy "Users can leave their own household" on public.household_members
  for delete using (auth.uid() = user_id);

drop policy if exists "Members can read shared budgets" on public.shared_budgets;
create policy "Members can read shared budgets" on public.shared_budgets
  for select using (public.fincip_is_household_member(household_id));
drop policy if exists "Members can add shared budgets as themselves" on public.shared_budgets;
create policy "Members can add shared budgets as themselves" on public.shared_budgets
  for insert with check (public.fincip_is_household_member(household_id) and auth.uid() = created_by);
drop policy if exists "Members can update shared budgets" on public.shared_budgets;
create policy "Members can update shared budgets" on public.shared_budgets
  for update using (public.fincip_is_household_member(household_id))
  with check (public.fincip_is_household_member(household_id));
drop policy if exists "Members can delete shared budgets" on public.shared_budgets;
create policy "Members can delete shared budgets" on public.shared_budgets
  for delete using (public.fincip_is_household_member(household_id));

grant select, update, delete on public.households to authenticated;
grant select, delete on public.household_members to authenticated;
grant select, insert, update, delete on public.shared_budgets to authenticated;

create or replace function public.create_fincip_household(p_name text)
returns table(id uuid, name text, invite_code text)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  new_household public.households%rowtype;
begin
  if auth.uid() is null then raise exception 'Sign in before creating a household.'; end if;
  if exists (select 1 from public.household_members hm where hm.user_id = auth.uid()) then raise exception 'This FinCip account already belongs to a household.'; end if;
  if length(trim(coalesce(p_name, ''))) not between 1 and 80 then raise exception 'Household name must be 1–80 characters.'; end if;
  insert into public.households(name, owner_id)
    values (trim(p_name), auth.uid()) returning * into new_household;
  insert into public.household_members(household_id, user_id, role)
    values (new_household.id, auth.uid(), 'owner');
  return query select new_household.id, new_household.name, new_household.invite_code;
end;
$$;

create or replace function public.join_fincip_household(p_invite_code text)
returns uuid
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  target_id uuid;
begin
  if auth.uid() is null then raise exception 'Sign in before joining a household.'; end if;
  select h.id into target_id
    from public.households h
    where h.invite_code = upper(trim(coalesce(p_invite_code, '')))
    limit 1;
  if target_id is null then raise exception 'That invite code was not found.'; end if;
  if exists (select 1 from public.household_members hm where hm.user_id = auth.uid() and hm.household_id <> target_id) then raise exception 'This FinCip account already belongs to another household.'; end if;
  insert into public.household_members(household_id, user_id, role)
    values (target_id, auth.uid(), 'member')
    on conflict (household_id, user_id) do nothing;
  return target_id;
end;
$$;

revoke all on function public.create_fincip_household(text) from public;
revoke all on function public.join_fincip_household(text) from public;
grant execute on function public.create_fincip_household(text) to authenticated;
grant execute on function public.join_fincip_household(text) to authenticated;

-- Enable live updates for private cloud rows and shared budgets. Safe to re-run.
do $$
begin
  alter publication supabase_realtime add table public.user_finance_data;
exception when duplicate_object then
  null;
end $$;

do $$
begin
  alter publication supabase_realtime add table public.shared_budgets;
exception when duplicate_object then
  null;
end $$;
