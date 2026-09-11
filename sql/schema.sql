-- ============================================================
-- Caisse / POS schema for Supabase (Postgres + Auth + RLS)
-- Run this in the Supabase SQL editor.
-- ============================================================

create extension if not exists "pgcrypto";

-- ------------------------------------------------------------
-- users: app profile linked 1:1 to auth.users, holds cashier code
-- ------------------------------------------------------------
create table if not exists public.users (
    id uuid primary key references auth.users(id) on delete cascade,
    email text not null,
    cashier_code text not null unique,
    created_at timestamptz not null default now()
);

-- ------------------------------------------------------------
-- products
-- ------------------------------------------------------------
create table if not exists public.products (
    id uuid primary key default gen_random_uuid(),
    code text not null unique,
    label text not null,
    stock_quantity integer not null default 0,
    normal_price numeric(12,3) not null default 0,
    discount_percent numeric(5,2) not null default 0,
    created_at timestamptz not null default now()
);

-- ------------------------------------------------------------
-- journeys (cashier sessions)
-- ------------------------------------------------------------
create table if not exists public.journeys (
    id text primary key,                 -- e.g. J-2026-0015
    user_id uuid not null references public.users(id),
    cashier_code text not null,
    opened_at timestamptz not null default now(),
    closed_at timestamptz,
    status text not null default 'OPEN' check (status in ('OPEN','CLOSED')),
    total_sales numeric(12,3) not null default 0,
    total_cash numeric(12,3) not null default 0,
    total_card numeric(12,3) not null default 0,
    total_returns numeric(12,3) not null default 0,
    net_total numeric(12,3) not null default 0
);

-- THE critical business rule, enforced in the database, not only in Java:
-- a cashier can have at most one OPEN journey at a time.
create unique index if not exists one_open_journey_per_cashier
    on public.journeys (user_id)
    where (status = 'OPEN');

-- ------------------------------------------------------------
-- tickets / ticket_items / payments
-- ------------------------------------------------------------
create table if not exists public.tickets (
    id uuid primary key default gen_random_uuid(),
    journey_id text not null references public.journeys(id),
    cashier_id uuid not null references public.users(id),
    total numeric(12,3) not null default 0,
    payment_method text not null check (payment_method in ('CASH','CARD')),
    created_at timestamptz not null default now()
);

create table if not exists public.ticket_items (
    id uuid primary key default gen_random_uuid(),
    ticket_id uuid not null references public.tickets(id) on delete cascade,
    product_id uuid not null references public.products(id),
    quantity integer not null check (quantity > 0),
    unit_price numeric(12,3) not null,
    discount_percent numeric(5,2) not null default 0,
    line_total numeric(12,3) not null
);

create table if not exists public.payments (
    id uuid primary key default gen_random_uuid(),
    ticket_id uuid not null references public.tickets(id) on delete cascade,
    method text not null check (method in ('CASH','CARD')),
    amount numeric(12,3) not null
);

-- ------------------------------------------------------------
-- returns / return_items
-- ------------------------------------------------------------
create table if not exists public.returns (
    id uuid primary key default gen_random_uuid(),
    original_ticket_id uuid not null references public.tickets(id),
    journey_id text not null references public.journeys(id),
    total numeric(12,3) not null default 0,
    created_at timestamptz not null default now()
);

create table if not exists public.return_items (
    id uuid primary key default gen_random_uuid(),
    return_id uuid not null references public.returns(id) on delete cascade,
    product_id uuid not null references public.products(id),
    quantity integer not null check (quantity > 0),
    unit_price numeric(12,3) not null,
    line_total numeric(12,3) not null
);

-- ------------------------------------------------------------
-- Business-rule trigger, enforced server-side, independent of the app:
-- a ticket or return cannot be inserted unless its journey is OPEN.
-- ------------------------------------------------------------
create or replace function public.enforce_open_journey()
returns trigger as $$
declare
    journey_status text;
begin
    select status into journey_status from public.journeys where id = new.journey_id;
    if journey_status is distinct from 'OPEN' then
        raise exception 'Cannot record a sale or return: journey % is not OPEN', new.journey_id;
    end if;
    return new;
end;
$$ language plpgsql;

drop trigger if exists trg_tickets_require_open_journey on public.tickets;
create trigger trg_tickets_require_open_journey
    before insert on public.tickets
    for each row execute function public.enforce_open_journey();

drop trigger if exists trg_returns_require_open_journey on public.returns;
create trigger trg_returns_require_open_journey
    before insert on public.returns
    for each row execute function public.enforce_open_journey();

-- ------------------------------------------------------------
-- Row Level Security
-- Simple policy set: any authenticated cashier can read/write
-- everything needed for the register to function. Tighten further
-- (e.g. restrict journeys/tickets to auth.uid() = user/cashier_id)
-- if multiple cashiers must not see each other's data.
-- ------------------------------------------------------------
alter table public.users enable row level security;
alter table public.products enable row level security;
alter table public.journeys enable row level security;
alter table public.tickets enable row level security;
alter table public.ticket_items enable row level security;
alter table public.payments enable row level security;
alter table public.returns enable row level security;
alter table public.return_items enable row level security;

create policy "authenticated read own profile" on public.users
    for select using (auth.uid() = id);

create policy "authenticated read products" on public.products
    for select using (auth.role() = 'authenticated');

create policy "authenticated manage own journeys" on public.journeys
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "authenticated read/write tickets" on public.tickets
    for all using (auth.role() = 'authenticated') with check (auth.role() = 'authenticated');

create policy "authenticated read/write ticket_items" on public.ticket_items
    for all using (auth.role() = 'authenticated') with check (auth.role() = 'authenticated');

create policy "authenticated read/write payments" on public.payments
    for all using (auth.role() = 'authenticated') with check (auth.role() = 'authenticated');

create policy "authenticated read/write returns" on public.returns
    for all using (auth.role() = 'authenticated') with check (auth.role() = 'authenticated');

create policy "authenticated read/write return_items" on public.return_items
    for all using (auth.role() = 'authenticated') with check (auth.role() = 'authenticated');

-- Stock also needs to be updatable by any authenticated cashier (sale/return adjustments)
create policy "authenticated update stock" on public.products
    for update using (auth.role() = 'authenticated');

-- ------------------------------------------------------------
-- Seed example (optional) — remove or edit before production use
-- ------------------------------------------------------------
-- insert into public.products (code, label, stock_quantity, normal_price, discount_percent)
-- values ('P001', 'Article exemple', 50, 25.000, 0);
