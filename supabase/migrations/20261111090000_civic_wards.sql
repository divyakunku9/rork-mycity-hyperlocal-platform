create table if not exists public.civic_bodies (
  code text primary key,            -- GHMC / CMC / MMC
  name text not null,
  head_title text not null,         -- who runs it now
  head_name text,
  council_note text not null,       -- why there is / isn't an elected council
  website text,
  helpline_display text, helpline_dial text,
  special_officer text,             -- the state-appointed officer standing in for the council
  as_of text,                       -- when these names were last confirmed
  updated_at timestamptz not null default now()
);
create table if not exists public.civic_wards (
  no int primary key,
  name text not null,
  corporation text not null references public.civic_bodies(code),
  zone text not null,
  circle text not null,
  lat double precision, lng double precision,
  geo_note text,                    -- how the centre point was found
  corporator text,                  -- null while no council is elected
  corporator_party text,
  updated_at timestamptz not null default now()
);
alter table public.civic_bodies enable row level security;
alter table public.civic_wards enable row level security;
create policy "civic bodies are public" on public.civic_bodies for select using (true);
create policy "civic wards are public" on public.civic_wards for select using (true);

insert into public.civic_bodies (code, name, head_title, head_name, council_note, website, helpline_display, helpline_dial, special_officer, as_of) values
('GHMC','Greater Hyderabad Municipal Corporation','Commissioner','R. V. Karnan, IAS','The elected council''s term ended in February 2026. A state-appointed special officer and the commissioner run the corporation until the next ward elections.','https://www.ghmc.gov.in','040-21111111','04021111111','Jayesh Ranjan, IAS','2026'),
('CMC','Cyberabad Municipal Corporation','Commissioner','Srijana Gummalla, IAS','Formed in February 2026 from the GHMC split. No council has been elected yet; the commissioner runs the corporation until ward elections.','https://cmc.telangana.gov.in',null,null,'Jayesh Ranjan, IAS','2026'),
('MMC','Malkajgiri Municipal Corporation','Commissioner','T. Vinay Krishna Reddy, IAS','Formed in February 2026 from the GHMC split. No council has been elected yet; the commissioner runs the corporation until ward elections.','https://mmc.telangana.gov.in',null,null,'Jayesh Ranjan, IAS','2026')
on conflict (code) do nothing;

-- The 300 wards themselves are loaded by supabase/functions/seed-wards (it also finds each ward's centre).
