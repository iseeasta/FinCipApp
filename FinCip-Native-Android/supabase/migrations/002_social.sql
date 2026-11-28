-- FinCip social layer: QR-only friending, posts, followers, and a shared "signature" for the solar-system view.
-- Run in the Supabase SQL Editor AFTER schema.sql. Safe to re-run.
-- Nothing here exposes anyone who has not scanned or been scanned: profiles are only readable by yourself
-- and people you are connected to, and the QR token is never readable by anyone but its owner.

create extension if not exists pgcrypto;

create table if not exists public.social_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  handle text not null unique check (handle ~ '^[a-z0-9_.]{3,24}$'),
  display_name text not null check (length(trim(display_name)) between 1 and 60),
  avatar jsonb not null default '{}'::jsonb,
  level int not null default 1,
  signature jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

create table if not exists public.social_qr (
  user_id uuid primary key references auth.users(id) on delete cascade,
  token text not null unique default encode(gen_random_bytes(18), 'hex')
);

create table if not exists public.follows (
  follower uuid not null references auth.users(id) on delete cascade,
  followee uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower, followee),
  check (follower <> followee)
);

create table if not exists public.posts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  body text not null check (length(trim(body)) between 1 and 500),
  created_at timestamptz not null default now()
);
create index if not exists posts_user_created on public.posts (user_id, created_at desc);

alter table public.social_profiles enable row level security;
alter table public.social_qr enable row level security;
alter table public.follows enable row level security;
alter table public.posts enable row level security;

create or replace function public.is_connected(a uuid, b uuid) returns boolean
language sql security definer set search_path = public stable as $$
  select exists (select 1 from public.follows f where (f.follower = a and f.followee = b) or (f.follower = b and f.followee = a));
$$;

drop policy if exists "profiles readable by self and connections" on public.social_profiles;
create policy "profiles readable by self and connections" on public.social_profiles for select
  using (user_id = auth.uid() or public.is_connected(auth.uid(), user_id));
drop policy if exists "profiles writable by owner" on public.social_profiles;
create policy "profiles writable by owner" on public.social_profiles for all
  using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "qr token only for owner" on public.social_qr;
create policy "qr token only for owner" on public.social_qr for select using (user_id = auth.uid());

drop policy if exists "see own follow edges" on public.follows;
create policy "see own follow edges" on public.follows for select using (follower = auth.uid() or followee = auth.uid());
drop policy if exists "unfollow only yourself" on public.follows;
create policy "unfollow only yourself" on public.follows for delete using (follower = auth.uid());
-- no insert policy: following happens only through follow_by_qr()

drop policy if exists "posts visible to author and followers" on public.posts;
create policy "posts visible to author and followers" on public.posts for select
  using (user_id = auth.uid() or exists (select 1 from public.follows f where f.follower = auth.uid() and f.followee = posts.user_id));
drop policy if exists "post as yourself" on public.posts;
create policy "post as yourself" on public.posts for insert with check (user_id = auth.uid());
drop policy if exists "delete own posts" on public.posts;
create policy "delete own posts" on public.posts for delete using (user_id = auth.uid());

-- Create or refresh my social profile and return my QR token.
create or replace function public.ensure_social(p_handle text, p_name text, p_avatar jsonb, p_level int, p_signature jsonb)
returns text language plpgsql security definer set search_path = public as $$
declare t text;
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  insert into public.social_profiles (user_id, handle, display_name, avatar, level, signature, updated_at)
  values (auth.uid(), lower(p_handle), p_name, coalesce(p_avatar, '{}'), coalesce(p_level, 1), coalesce(p_signature, '{}'), now())
  on conflict (user_id) do update set display_name = excluded.display_name, avatar = excluded.avatar,
    level = excluded.level, signature = excluded.signature, updated_at = now();
  insert into public.social_qr (user_id) values (auth.uid()) on conflict do nothing;
  select token into t from public.social_qr where user_id = auth.uid();
  return t;
end $$;

-- Scanning someone's QR makes you follow them. Returns who you just followed.
create or replace function public.follow_by_qr(p_token text) returns jsonb
language plpgsql security definer set search_path = public as $$
declare target uuid; prof public.social_profiles;
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  select user_id into target from public.social_qr where token = p_token;
  if target is null then raise exception 'That QR code is not valid any more'; end if;
  if target = auth.uid() then raise exception 'That is your own QR code'; end if;
  insert into public.follows (follower, followee) values (auth.uid(), target) on conflict do nothing;
  select * into prof from public.social_profiles where user_id = target;
  return jsonb_build_object('user_id', target, 'handle', prof.handle, 'name', prof.display_name);
end $$;

-- Invalidate my old QR and get a new token (use if a code leaked).
create or replace function public.rotate_qr() returns text
language plpgsql security definer set search_path = public as $$
declare t text := encode(gen_random_bytes(18), 'hex');
begin
  update public.social_qr set token = t where user_id = auth.uid();
  return t;
end $$;

create or replace function public.social_summary() returns jsonb
language sql security definer set search_path = public stable as $$
  select jsonb_build_object(
    'followers', (select count(*) from public.follows where followee = auth.uid()),
    'following', (select count(*) from public.follows where follower = auth.uid()),
    'posts', (select count(*) from public.posts where user_id = auth.uid()));
$$;

grant select, delete on public.follows to authenticated;
grant select, insert, update on public.social_profiles to authenticated;
grant select on public.social_qr to authenticated;
grant select, insert, delete on public.posts to authenticated;
grant execute on function public.ensure_social(text, text, jsonb, int, jsonb) to authenticated;
grant execute on function public.follow_by_qr(text) to authenticated;
grant execute on function public.rotate_qr() to authenticated;
grant execute on function public.social_summary() to authenticated;
