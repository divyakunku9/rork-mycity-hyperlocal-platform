-- Public responses to civic reports.
--
-- While looking at a report, a signed-in resident can say one of four things about it:
--   seen         "I see this too"
--   still_there  "Still not fixed"
--   worse        "It's getting worse"
--   fixed        "It's fixed now"
-- One response per person per report; it can be changed or removed at any time.
--
-- Everyone can read the totals (kept on civic_issues). Who said what is private: a resident
-- can read only their own response. Responses never change a report's official status.

create type public.issue_response_kind as enum ('seen', 'still_there', 'worse', 'fixed');

create table public.issue_responses (
  issue_id   uuid not null references public.civic_issues (id) on delete cascade,
  user_id    uuid not null references public.profiles (id) on delete cascade,
  kind       public.issue_response_kind not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (issue_id, user_id)
);
create index issue_responses_user_idx on public.issue_responses (user_id);
comment on table public.issue_responses is
  'What each resident currently says about a civic report (one row per person per report).';

alter table public.civic_issues
  add column seen_count  integer not null default 0,
  add column still_count integer not null default 0,
  add column worse_count integer not null default 0,
  add column fixed_count integer not null default 0,
  add column last_response_kind public.issue_response_kind,
  add column last_response_at   timestamptz;
comment on column public.civic_issues.upvote_count is
  'Residents other than the reporter who currently say the problem is there (seen, still_there or worse).';

-- ---------------------------------------------------------------- Access
alter table public.issue_responses enable row level security;
revoke all on public.issue_responses from anon, authenticated;
grant select, insert, update, delete on public.issue_responses to authenticated;

create policy "issue_responses: read own" on public.issue_responses
  for select to authenticated using (user_id = (select auth.uid()) or (select public.is_staff()));
create policy "issue_responses: add own" on public.issue_responses
  for insert to authenticated with check (user_id = (select auth.uid()));
create policy "issue_responses: change own" on public.issue_responses
  for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "issue_responses: remove own" on public.issue_responses
  for delete to authenticated using (user_id = (select auth.uid()));

-- ---------------------------------------------------------------- Rules
create function public.issue_response_rules() returns trigger
language plpgsql security definer set search_path to 'public' as $$
declare
  uid uuid := auth.uid();
  v_reporter uuid;
  n_hour integer;
begin
  if tg_op = 'UPDATE' then
    -- A response stays attached to its report and its author.
    new.issue_id := old.issue_id;
    new.user_id := old.user_id;
    new.created_at := old.created_at;
  end if;
  new.updated_at := now();

  select reporter_id into v_reporter from public.civic_issues where id = new.issue_id;
  if new.kind = 'seen' and v_reporter = new.user_id then
    raise exception 'You reported this one. You can say if it is still there, getting worse or fixed.';
  end if;

  if uid is not null and not public.is_trusted_backend() and not public.is_staff() then
    select count(*) into n_hour from public.issue_responses
      where user_id = uid and updated_at > now() - interval '1 hour';
    if n_hour >= 60 then
      raise exception 'Too many responses in a short time. Please try again later.';
    end if;
  end if;
  return new;
end $$;

create trigger issue_responses_rules before insert or update on public.issue_responses
  for each row execute function public.issue_response_rules();

-- ---------------------------------------------------------------- Totals
create function public.issue_response_recount() returns trigger
language plpgsql security definer set search_path to 'public' as $$
declare
  v_issue uuid := case when tg_op = 'DELETE' then old.issue_id else new.issue_id end;
begin
  perform public.internal_write(true);
  update public.civic_issues i set
    seen_count  = c.seen,
    still_count = c.still,
    worse_count = c.worse,
    fixed_count = c.fixed,
    upvote_count = c.confirming,
    last_response_kind = l.kind,
    last_response_at   = l.updated_at
  from (
    select count(*) filter (where r.kind = 'seen')::int        as seen,
           count(*) filter (where r.kind = 'still_there')::int as still,
           count(*) filter (where r.kind = 'worse')::int       as worse,
           count(*) filter (where r.kind = 'fixed')::int       as fixed,
           count(*) filter (where r.kind <> 'fixed' and r.user_id is distinct from
             (select reporter_id from public.civic_issues where id = v_issue))::int as confirming
    from public.issue_responses r where r.issue_id = v_issue
  ) c
  left join lateral (
    select r.kind, r.updated_at from public.issue_responses r
    where r.issue_id = v_issue order by r.updated_at desc limit 1
  ) l on true
  where i.id = v_issue;
  perform public.internal_write(false);
  return null;
end $$;

create trigger issue_responses_count after insert or update or delete on public.issue_responses
  for each row execute function public.issue_response_recount();

-- The new totals are protected the same way as the existing confirmation count.
create or replace function public.guard_issue_update() returns trigger
language plpgsql security definer set search_path to 'public' as $$
begin
  if public.is_trusted_backend() or public.is_staff() or public.in_internal_write() then
    if new.status = 'resolved' and old.status <> 'resolved' then
      new.resolved_at := now();
    end if;
    return new;
  end if;
  if new.status is distinct from old.status
     or new.authority_id is distinct from old.authority_id
     or new.upvote_count is distinct from old.upvote_count
     or new.seen_count is distinct from old.seen_count
     or new.still_count is distinct from old.still_count
     or new.worse_count is distinct from old.worse_count
     or new.fixed_count is distinct from old.fixed_count
     or new.last_response_kind is distinct from old.last_response_kind
     or new.last_response_at is distinct from old.last_response_at
     or new.reporter_id is distinct from old.reporter_id
     or new.city_id is distinct from old.city_id then
    raise exception 'Only staff can change status, routing or counts';
  end if;
  if old.status <> 'reported' then
    raise exception 'A report can only be edited before review starts';
  end if;
  return new;
end $$;

-- ---------------------------------------------------------------- Older app builds
-- Builds from before this change write "I see this too" to issue_upvotes. Keep those working by
-- copying them into issue_responses, which is now the only place totals are counted from.
create function public.mirror_issue_upvote() returns trigger
language plpgsql security definer set search_path to 'public' as $$
begin
  if tg_op = 'INSERT' then
    insert into public.issue_responses (issue_id, user_id, kind)
      values (new.issue_id, new.user_id, 'seen')
      on conflict (issue_id, user_id) do nothing;
  else
    delete from public.issue_responses
      where issue_id = old.issue_id and user_id = old.user_id and kind = 'seen';
  end if;
  return null;
end $$;

drop trigger issue_upvotes_count on public.issue_upvotes;
create trigger issue_upvotes_mirror after insert or delete on public.issue_upvotes
  for each row execute function public.mirror_issue_upvote();

insert into public.issue_responses (issue_id, user_id, kind, created_at)
  select issue_id, user_id, 'seen', created_at from public.issue_upvotes
  on conflict (issue_id, user_id) do nothing;

-- ---------------------------------------------------------------- What the app calls
-- Sets, changes or (with p_kind null) removes the caller's response, and returns the fresh totals.
create function public.set_issue_response(p_issue_id uuid, p_kind text default null) returns jsonb
language plpgsql security invoker set search_path to 'public' as $$
declare
  uid uuid := auth.uid();
  v jsonb;
begin
  if uid is null then
    raise exception 'Please sign in to respond to a report';
  end if;
  if not exists (select 1 from public.civic_issues where id = p_issue_id) then
    raise exception 'This report is no longer available';
  end if;
  if p_kind is null then
    delete from public.issue_responses where issue_id = p_issue_id and user_id = uid;
  elsif p_kind not in ('seen', 'still_there', 'worse', 'fixed') then
    raise exception 'Unknown response';
  else
    insert into public.issue_responses (issue_id, user_id, kind)
      values (p_issue_id, uid, p_kind::public.issue_response_kind)
      on conflict (issue_id, user_id) do update set kind = excluded.kind;
  end if;
  select jsonb_build_object(
      'issue_id', i.id, 'my_kind', p_kind,
      'upvote_count', i.upvote_count, 'seen_count', i.seen_count, 'still_count', i.still_count,
      'worse_count', i.worse_count, 'fixed_count', i.fixed_count,
      'last_response_kind', i.last_response_kind, 'last_response_at', i.last_response_at)
    into v from public.civic_issues i where i.id = p_issue_id;
  return v;
end $$;

revoke execute on function public.set_issue_response(uuid, text) from public, anon;
grant execute on function public.set_issue_response(uuid, text) to authenticated;
revoke execute on function public.issue_response_rules() from public, anon, authenticated;
revoke execute on function public.issue_response_recount() from public, anon, authenticated;
revoke execute on function public.mirror_issue_upvote() from public, anon, authenticated;
