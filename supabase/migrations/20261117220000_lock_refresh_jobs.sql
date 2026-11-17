-- Close the scheduled refresh functions to the public internet.
--
-- refresh-news and refresh-events were deployed with Supabase's JWT check turned off,
-- because pg_cron calls them with no credentials. That left them callable by anyone who
-- knew the URL, and "?force=1" skips the "ran recently" guard: repeated calls would run up
-- the project's bill and fire scraping traffic at publishers' sites on demand.
--
-- They now require a key. It is generated here, kept in a schema the app's roles cannot
-- reach, and never written into the schedule or into an environment variable: the caller
-- (private.run_refresh) and the function that checks it both read it from this one row.

create schema if not exists private;
revoke all on schema private from anon, authenticated, public;

create table if not exists private.job_keys (
  name       text primary key,
  key        text not null,
  created_at timestamptz not null default now()
);
revoke all on private.job_keys from anon, authenticated, public;

insert into private.job_keys (name, key)
  values ('refresh', encode(extensions.gen_random_bytes(32), 'hex'))
  on conflict (name) do nothing;

-- The edge function calls this (as the service role) to check the key it was given.
create or replace function public.check_job_key(p_name text, p_key text) returns boolean
language sql security definer set search_path to 'private', 'public' as $$
  select exists (select 1 from private.job_keys k where k.name = p_name and k.key = p_key);
$$;
revoke execute on function public.check_job_key(text, text) from public, anon, authenticated;
grant execute on function public.check_job_key(text, text) to service_role;
comment on function public.check_job_key(text, text) is
  'Used by the scheduled edge functions to verify the caller. Not reachable by the app.';

-- What the schedule runs. Keeping the call here means the key stays out of cron.job.command.
create or replace function private.run_refresh(p_fn text, p_timeout_ms integer default 150000)
returns bigint
language plpgsql security definer set search_path to 'private', 'public', 'extensions' as $$
declare v_key text; v_request_id bigint;
begin
  if p_fn not in ('refresh-news', 'refresh-events') then
    raise exception 'Unknown refresh job: %', p_fn;
  end if;
  select key into strict v_key from private.job_keys where name = 'refresh';
  select net.http_get(
      url := 'https://vcdjdothkofhbyooecji.supabase.co/functions/v1/' || p_fn,
      headers := jsonb_build_object('x-refresh-key', v_key),
      timeout_milliseconds := p_timeout_ms)
    into v_request_id;
  return v_request_id;
end $$;
revoke execute on function private.run_refresh(text, integer) from public, anon, authenticated;

-- Point the existing schedules at it, keeping their times and timeouts.
select cron.alter_job(1, command := $cmd$select private.run_refresh('refresh-news', 120000)$cmd$);
select cron.alter_job(3, command := $cmd$select private.run_refresh('refresh-events', 150000)$cmd$);
