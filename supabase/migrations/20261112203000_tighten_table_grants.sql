-- Defence in depth for table permissions.
--
-- Supabase's default is to grant every public table to the 'anon' and 'authenticated' roles,
-- leaving row-level security as the only thing between the app's public key and the data.
-- RLS is working (verified), but a single mistaken policy on a single table would then be
-- enough to lose data. TRUNCATE is worse: it ignores RLS entirely.
--
-- So each role now holds only the permissions the app actually uses:
--   anon           reads public content, writes nothing
--   authenticated  reads, and writes only the tables a resident owns rows in
-- Writes that run inside SECURITY DEFINER functions (book_event, cancel_booking,
-- set_issue_response, handle_new_user) are unaffected: those run as the function's owner.

-- 1. Nobody using the app's key needs these. TRUNCATE in particular bypasses RLS.
do $$
declare t record;
begin
  for t in select c.relname from pg_class c join pg_namespace n on n.oid = c.relnamespace
           where n.nspname = 'public' and c.relkind = 'r'
  loop
    execute format('revoke truncate, references, trigger on public.%I from anon, authenticated', t.relname);
  end loop;
end $$;

-- 2. An anonymous visitor only ever reads. No policy grants anon a write, so this takes
--    nothing away today; it means a future policy slip cannot be written to anonymously.
do $$
declare t record;
begin
  for t in select c.relname from pg_class c join pg_namespace n on n.oid = c.relnamespace
           where n.nspname = 'public' and c.relkind = 'r'
  loop
    execute format('revoke insert, update, delete on public.%I from anon', t.relname);
  end loop;
end $$;

-- 3. Reference and editorial content: readable by everyone, written only by staff tools and
--    the scheduled jobs (which use the service key and are not limited by these grants).
do $$
declare t text;
begin
  foreach t in array array[
    'cities', 'areas', 'authorities', 'city_portals', 'civic_bodies', 'civic_wards',
    'helplines', 'fare_rules', 'transit_lines', 'transit_stations', 'line_stations',
    'service_status', 'city_alerts', 'polls', 'poll_options',
    'news_items', 'news_sources', 'news_articles', 'event_sources', 'external_events',
    'public_profiles', 'event_bookings'
  ]
  loop
    execute format('revoke insert, update, delete on public.%I from authenticated', t);
  end loop;
end $$;

-- 4. A resident's own rows: only the operations the app performs.
revoke insert, delete on public.profiles from authenticated;           -- rows are created by handle_new_user
revoke insert, delete on public.notification_prefs from authenticated; -- created with the profile; the app only toggles
revoke update on public.poll_votes from authenticated;                 -- a vote is cast once, never edited
revoke update on public.issue_upvotes from authenticated;              -- added or withdrawn, never edited

-- 5. New tables should start closed rather than open, so this cannot drift back.
alter default privileges in schema public revoke all on tables from anon, authenticated;

comment on schema public is
  'Shehar Saathi. Table permissions are kept to what the app uses; row-level security decides which rows. Writes needing more run in SECURITY DEFINER functions.';
