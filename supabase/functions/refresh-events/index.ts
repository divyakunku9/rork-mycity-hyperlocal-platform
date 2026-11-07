// refresh-events: gathers upcoming city events from public listing sites (District, Eventbrite,
// Meetup, AllEvents) using the structured event data (schema.org JSON-LD) those pages publish
// for search engines. Keeps title, date, venue, price, picture and the link; people book on the
// source site. Runs on a schedule; skips if it ran in the last 30 minutes unless ?force=1.
import { createClient } from "jsr:@supabase/supabase-js@2";

const UA = "SheharSaathi-EventsBot/1.0 (+https://sheharsaathi.com)";
const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, { auth: { persistSession: false } });

// City centres and the words that place an event inside the city.
const CITIES: Record<string, { lat: number; lng: number; radiusKm: number; words: RegExp; tz: string }> = {
  hyderabad: {
    lat: 17.385, lng: 78.4867, radiusKm: 45, tz: "+05:30",
    words: /hyderabad|secunderabad|cyberabad|telangana|gachibowli|hitec|madhapur|jubilee hills|banjara hills|kondapur|kukatpally|begumpet|shamshabad/i,
  },
};

type Source = { id: string; city_id: string; name: string; list_urls: string[]; priority: number };
type Ev = {
  source_id: string; priority: number; city_id: string; dedupe_key: string; url: string; title: string; summary: string | null;
  image_url: string | null; starts_at: string; ends_at: string | null; has_time: boolean; venue: string | null; address: string | null;
  lat: number | null; lng: number | null; price_min: number | null; price_max: number | null; is_free: boolean | null; category: string;
};

// ---------- text helpers ----------
const ENT: Record<string, string> = { amp: "&", lt: "<", gt: ">", quot: '"', apos: "'", nbsp: " ", rsquo: "’", lsquo: "‘", rdquo: "”", ldquo: "“", ndash: "–", mdash: "—", hellip: "…" };
function decode(s: string): string {
  return s.replace(/&#x([0-9a-f]+);/gi, (_, h) => String.fromCodePoint(parseInt(h, 16)))
    .replace(/&#(\d+);/g, (_, d) => String.fromCodePoint(parseInt(d, 10)))
    .replace(/&([a-z]+);/gi, (m, n) => ENT[n.toLowerCase()] ?? m);
}
function clean(s: unknown): string {
  if (typeof s !== "string") return "";
  return decode(decode(s).replace(/<[^>]+>/g, " ").replace(/[*_#`]+/g, "")).replace(/\s+/g, " ").trim();
}
function snippet(t: string, max = 200): string | null {
  const s = t.replace(/https?:\/\/\S+/g, "").replace(/\s+/g, " ").trim();
  if (s.length < 30) return null;
  if (s.length <= max) return s;
  const cut = s.slice(0, max);
  return cut.slice(0, Math.max(cut.lastIndexOf(" "), max - 30)).replace(/[,;:.\s]+$/, "") + "…";
}
function norm(s: string): string { return s.toLowerCase().replace(/[^a-z0-9]+/g, " ").trim(); }
function km(aLat: number, aLng: number, bLat: number, bLng: number): number {
  const r = (d: number) => d * Math.PI / 180, R = 6371;
  const x = Math.sin(r(bLat - aLat) / 2) ** 2 + Math.cos(r(aLat)) * Math.cos(r(bLat)) * Math.sin(r(bLng - aLng) / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(x));
}
function num(v: unknown): number | null {
  const n = typeof v === "number" ? v : typeof v === "string" ? parseFloat(v.replace(/[^0-9.]/g, "")) : NaN;
  return isFinite(n) ? n : null;
}
function firstImage(v: unknown): string | null {
  const pick = (x: unknown): string | null =>
    typeof x === "string" ? x : (x && typeof x === "object" && typeof (x as Record<string, unknown>).url === "string") ? (x as Record<string, string>).url : null;
  const arr = Array.isArray(v) ? v : [v];
  // Prefer a landscape picture when the site gives several.
  const urls = arr.map(pick).filter((u): u is string => !!u && /^https?:/.test(u));
  const u = urls.find((x) => /horizontal|landscape|1200|banner/i.test(x)) ?? urls[0] ?? null;
  // AVIF pictures don't display on Android 11 and older; AllEvents' image server also serves JPEG.
  return u ? u.replace(/\.avif(\?.*)?$/i, ".jpg$1") : null;
}

// ---------- dates ----------
// "2026-11-28" (no time) is a whole day in the city's time zone; full timestamps are used as given.
function when(v: unknown, tz: string): { iso: string; hasTime: boolean } | null {
  if (typeof v !== "string" || !v) return null;
  const s = v.trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return { iso: new Date(`${s}T00:00:00${tz}`).toISOString(), hasTime: false };
  const local = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(s) ? `${s}${tz}` : s; // no zone given → city time
  const t = Date.parse(local);
  return isNaN(t) ? null : { iso: new Date(t).toISOString(), hasTime: true };
}

// ---------- categories ----------
const RULES: [string, RegExp][] = [
  ["comedy", /\b(comedy|comedian|stand[- ]?up|standup|open mic|improv|roast|jokes?)\b/i],
  ["music", /\b(concert|live music|music|gig|dj|band|singer|karaoke|sufi|qawwali|ghazal|edm|unplugged|symphony|orchestra|rapper|hip[- ]?hop|tour|jam(ming)?)\b/i],
  ["theatre", /\b(theatre|theater|play|drama|musical|dance|ballet|kuchipudi|bharatanatyam|kathak|mime|storytelling|poetry|screening|film|movie|cinema)\b/i],
  ["art", /\b(exhibition|art|painting|pottery|craft|gallery|photography|sketch\w*|illustration|museum)\b/i],
  ["tech", /\b(ai|tech|technology|startup|developer|hackathon|data|cloud|product|founders?|summit|conference|networking|career|devops|python|agentic|llm|blockchain|web3|saas|expo)\b/i],
  ["workshop", /\b(workshop|masterclass|class|course|training|bootcamp|webinar|seminar|learn\w*|session)\b/i],
  ["sports", /\b(run|marathon|cycling|cyclothon|yoga|fitness|cricket|football|badminton|trek|trekking|hike|hiking|sports?|zumba|pickleball|tournament|match|swim\w*)\b/i],
  ["food", /\b(food|brunch|wine|beer|tasting|culinary|cooking|dinner|biryani|feast|cafe|baking)\b/i],
  ["kids", /\b(kids?|children|family|toddlers?|parents?)\b/i],
  ["nightlife", /\b(party|nightlife|club|pub|bollywood night|techno|ladies night|after ?party|night|lounge|rooftop)\b/i],
  ["spiritual", /\b(satsang|meditation|spiritual|bhajan|kirtan|devotional|retreat|mindfulness)\b/i],
  ["festival", /\b(festival|fest|mela|carnival|fair|flea|bazaar|market|pop[- ]?up|celebration|diwali|christmas|new year|convention|anime|comic con|cosplay|sneakers?|expo|exhibitors?)\b/i],
];
function categorise(title: string, extra: string): string {
  let best = "other", score = 0;
  for (const [cat, re] of RULES) {
    const g = new RegExp(re.source, "gi");
    const s = (title.match(g)?.length ?? 0) * 3 + (extra.match(g)?.length ?? 0);
    if (s > score) { best = cat; score = s; }
  }
  return best;
}

// The site's own event type, when it's specific, beats keyword guessing.
const TYPE_CAT: Record<string, string> = {
  MusicEvent: "music", ComedyEvent: "comedy", TheaterEvent: "theatre", DanceEvent: "theatre", ScreeningEvent: "theatre",
  SportsEvent: "sports", FoodEvent: "food", ChildrensEvent: "kids", ExhibitionEvent: "art", VisualArtsEvent: "art",
  Festival: "festival", EducationEvent: "workshop", BusinessEvent: "tech", LiteraryEvent: "art",
};
function typeCategory(t: unknown): string | null {
  for (const x of Array.isArray(t) ? t : [t]) if (typeof x === "string" && TYPE_CAT[x]) return TYPE_CAT[x];
  return null;
}
// Only when the words say nothing, fall back to the topic of the listing page it came from.
function fallbackCategory(cat: string, pageUrl: string): string {
  if (cat !== "other") return cat;
  const hint = categorise("", pageUrl.replace(/[-/]/g, " "));
  return hint;
}

// ---------- JSON-LD ----------
function ldObjects(html: string): unknown[] {
  const out: unknown[] = [];
  for (const m of html.matchAll(/<script[^>]*type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi)) {
    try { out.push(JSON.parse(m[1].trim())); } catch { /* skip broken block */ }
  }
  return out;
}
function isEventType(t: unknown): boolean {
  const types = Array.isArray(t) ? t : [t];
  return types.some((x) => typeof x === "string" && /Event$/.test(x) && x !== "EventSeries");
}
function collectEvents(node: unknown, out: Record<string, unknown>[], depth = 0): void {
  if (!node || depth > 8) return;
  if (Array.isArray(node)) { node.forEach((n) => collectEvents(n, out, depth + 1)); return; }
  if (typeof node !== "object") return;
  const o = node as Record<string, unknown>;
  if (isEventType(o["@type"]) && o.name && o.startDate) { out.push(o); return; }
  for (const k of ["@graph", "itemListElement", "item", "mainEntity", "subEvent", "event", "events"]) if (o[k]) collectEvents(o[k], out, depth + 1);
}

function toEvent(o: Record<string, unknown>, src: Source, pageUrl: string): Ev | null {
  const city = CITIES[src.city_id];
  if (!city) return null;
  const status = String(o.eventStatus ?? "");
  if (/Cancelled|Postponed/i.test(status)) return null;
  const mode = String(o.eventAttendanceMode ?? "");
  const loc = (Array.isArray(o.location) ? o.location[0] : o.location) as Record<string, unknown> | undefined;
  if (/Online/i.test(mode) && !/Mixed/i.test(mode)) return null;
  if (loc && /VirtualLocation/i.test(String(loc["@type"] ?? ""))) return null;

  const title = clean(o.name);
  if (!title) return null;
  const start = when(o.startDate, city.tz);
  if (!start) return null;
  const end = when(o.endDate, city.tz);
  const now = Date.now();
  const endMs = end ? Date.parse(end.iso) + (end.hasTime ? 0 : 86_400_000) : Date.parse(start.iso) + (start.hasTime ? 4 * 3_600_000 : 86_400_000);
  if (endMs < now) return null;                                   // already over
  if (Date.parse(start.iso) > now + 90 * 86_400_000) return null; // more than 3 months away

  // Where: must be inside the city.
  const addr = loc?.address as Record<string, unknown> | string | undefined;
  const addrText = typeof addr === "string" ? addr
    : addr ? [addr.streetAddress, addr.addressLocality, addr.addressRegion].filter((x) => typeof x === "string").join(", ") : "";
  const venue = clean(loc?.name) || null;
  const geo = loc?.geo as Record<string, unknown> | undefined;
  const lat = num(geo?.latitude), lng = num(geo?.longitude);
  const inCity = lat != null && lng != null && Math.abs(lat) > 0.01
    ? km(city.lat, city.lng, lat, lng) <= city.radiusKm
    : city.words.test(`${venue ?? ""} ${addrText}`);
  if (!inCity) return null;

  // Price: Offer or AggregateOffer, single or list.
  const offers = (Array.isArray(o.offers) ? o.offers : o.offers ? [o.offers] : []) as Record<string, unknown>[];
  const prices = offers.flatMap((f) => [num(f.price), num(f.lowPrice), num(f.highPrice)]).filter((p): p is number => p != null);
  const isFree = o.isAccessibleForFree === true || /free/i.test(String(o.isAccessibleForFree ?? "")) || (prices.length > 0 && Math.max(...prices) === 0) ? true
    : prices.length > 0 ? false : null;
  const offerUrl = offers.map((f) => f.url).find((u) => typeof u === "string" && /^https?:/.test(u)) as string | undefined;

  let url = (typeof o.url === "string" && /^https?:/.test(o.url) ? o.url : offerUrl) ?? "";
  if (!url) return null;
  try { const u = new URL(url, pageUrl); for (const k of [...u.searchParams.keys()]) if (/^utm_|^aff|^ref$/i.test(k)) u.searchParams.delete(k); url = u.toString(); } catch { return null; }

  const desc = clean(o.description);
  const summary = desc && norm(desc) !== norm(title) ? snippet(desc) : null;
  const day = start.iso.slice(0, 10);
  return {
    source_id: src.id, priority: src.priority, city_id: src.city_id,
    dedupe_key: `${src.city_id}|${norm(title).slice(0, 48)}|${day}`,
    url, title: title.slice(0, 200), summary, image_url: firstImage(o.image),
    starts_at: start.iso, ends_at: end?.iso ?? null, has_time: start.hasTime,
    venue: venue?.slice(0, 160) ?? null, address: addrText ? clean(addrText).slice(0, 240) : null, lat, lng,
    price_min: prices.length ? Math.min(...prices) : null, price_max: prices.length ? Math.max(...prices) : null, is_free: isFree,
    category: typeCategory(o["@type"]) ?? fallbackCategory(categorise(title, summary ?? ""), pageUrl),
  };
}

async function get(url: string, ms = 20000): Promise<string> {
  const r = await fetch(url, { headers: { "User-Agent": UA, Accept: "text/html,application/xhtml+xml" }, signal: AbortSignal.timeout(ms), redirect: "follow" });
  if (!r.ok) throw new Error(`HTTP ${r.status}`);
  return await r.text();
}

// District lists categories on separate pages; find the Hyderabad ones linked from the main list.
function districtCategoryPages(html: string): string[] {
  const set = new Set<string>();
  for (const m of html.matchAll(/https:\/\/www\.district\.in\/events\/[a-z0-9-]+-in-hyderabad-book-tickets/g)) set.add(m[0]);
  for (const m of html.matchAll(/["'](\/events\/[a-z0-9-]+-in-hyderabad-book-tickets)["']/g)) set.add(`https://www.district.in${m[1]}`);
  return [...set].slice(0, 12);
}

// Fill a missing picture or summary from the event page's own preview tags.
async function enrich(e: Ev): Promise<void> {
  try {
    const html = (await get(e.url, 8000)).slice(0, 250_000);
    const meta = (p: string) => {
      const a = html.match(new RegExp(`<meta[^>]+(?:property|name)=["']${p}["'][^>]+content=["']([^"']+)["']`, "i"));
      const b = html.match(new RegExp(`<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["']${p}["']`, "i"));
      return (a ?? b)?.[1] ?? null;
    };
    if (!e.image_url) { const i = meta("og:image"); if (i && /^https?:/.test(i)) e.image_url = decode(i).replace(/\.avif(\?.*)?$/i, ".jpg$1"); }
    if (!e.summary) {
      const d = meta("og:description") ?? meta("description");
      const s = d ? snippet(clean(d)) : null;
      if (s && !norm(s).startsWith(norm(e.title).slice(0, 40))) { e.summary = s; if (e.category === "other") e.category = categorise(e.title, s); }
    }
  } catch { /* keep as is */ }
}

Deno.serve(async (req) => {
  const force = new URL(req.url).searchParams.get("force") === "1";
  const { data: sources, error } = await db.from("event_sources").select("id,city_id,name,list_urls,priority,last_fetched_at").eq("enabled", true);
  if (error) return Response.json({ error: error.message }, { status: 500 });
  const latest = Math.max(0, ...(sources ?? []).map((s: { last_fetched_at: string | null }) => (s.last_fetched_at ? Date.parse(s.last_fetched_at) : 0)));
  if (!force && Date.now() - latest < 30 * 60_000) return Response.json({ skipped: "ran less than 30 minutes ago" });

  const report: Record<string, string> = {};
  const all: Ev[] = [];
  await Promise.all((sources as Source[]).map(async (src) => {
    let pages = 0, found = 0, kept = 0, failed = 0;
    const queue = [...src.list_urls];
    const seen = new Set<string>();
    while (queue.length) {
      const u = queue.shift()!;
      if (seen.has(u)) continue;
      seen.add(u);
      try {
        const html = await get(u);
        pages++;
        if (src.id.endsWith("district") && pages === 1) queue.push(...districtCategoryPages(html));
        const raw: Record<string, unknown>[] = [];
        ldObjects(html).forEach((n) => collectEvents(n, raw));
        found += raw.length;
        for (const o of raw) { const e = toEvent(o, src, u); if (e) { all.push(e); kept++; } }
      } catch { failed++; }
    }
    report[src.id] = `${pages} pages read${failed ? `, ${failed} failed` : ""}; ${found} listed, ${kept} in the city`;
    await db.from("event_sources").update({ last_fetched_at: new Date().toISOString(), last_status: report[src.id] }).eq("id", src.id);
  }));

  // One row per event: when several sites (or several listing pages) carry it, keep the direct
  // ticket seller, and borrow the picture, summary and topic from the others where it lacks them.
  const merge = (keep: Ev, other: Ev): Ev => ({
    ...keep,
    image_url: keep.image_url ?? other.image_url, summary: keep.summary ?? other.summary,
    category: keep.category === "other" ? other.category : keep.category,
    price_min: keep.price_min ?? other.price_min, price_max: keep.price_max ?? other.price_max, is_free: keep.is_free ?? other.is_free,
  });
  const best = new Map<string, Ev>();
  for (const e of all) {
    const cur = best.get(e.dedupe_key);
    best.set(e.dedupe_key, !cur ? e : e.priority < cur.priority ? merge(e, cur) : merge(cur, e));
  }
  // Same venue at the same minute is the same event even when sites word the title differently.
  const mergedAway: string[] = [];
  const bySlot = new Map<string, Ev>();
  for (const e of [...best.values()]) {
    if (!best.has(e.dedupe_key) || !e.has_time || !e.venue) continue;
    const slot = `${e.starts_at}|${norm(e.venue).slice(0, 10)}`;
    const cur = bySlot.get(slot);
    if (!cur) { bySlot.set(slot, e); continue; }
    const [keep, drop] = e.priority < cur.priority ? [e, cur] : [cur, e];
    best.set(keep.dedupe_key, merge(keep, drop));
    best.delete(drop.dedupe_key);
    mergedAway.push(drop.dedupe_key);
    bySlot.set(slot, best.get(keep.dedupe_key)!);
  }
  const rows = [...best.values()];

  // Keep pictures and summaries filled in on earlier runs.
  const known = new Map<string, { image_url: string | null; summary: string | null }>();
  for (let i = 0; i < rows.length; i += 200) {
    const { data } = await db.from("external_events").select("dedupe_key,image_url,summary").in("dedupe_key", rows.slice(i, i + 200).map((r) => r.dedupe_key));
    (data ?? []).forEach((d: { dedupe_key: string; image_url: string | null; summary: string | null }) => known.set(d.dedupe_key, d));
  }
  for (const r of rows) { const k = known.get(r.dedupe_key); if (k) { r.image_url ??= k.image_url; r.summary ??= k.summary; } }
  const needs = rows.filter((r) => !known.has(r.dedupe_key) && (!r.image_url || !r.summary)).slice(0, 40);
  for (let k = 0; k < needs.length; k += 5) await Promise.all(needs.slice(k, k + 5).map(enrich));

  const now = new Date().toISOString();
  let saved = 0;
  for (let i = 0; i < rows.length; i += 100) {
    const chunk = rows.slice(i, i + 100).map(({ priority: _p, ...r }) => ({ ...r, last_seen_at: now }));
    const { error: e } = await db.from("external_events").upsert(chunk, { onConflict: "dedupe_key" });
    if (e) report.save_error = e.message; else saved += chunk.length;
  }
  if (mergedAway.length) await db.from("external_events").delete().in("dedupe_key", mergedAway);
  // Drop events that are over, and listings that have disappeared from their site for two days.
  await db.from("external_events").delete().lt("starts_at", new Date(Date.now() - 2 * 86_400_000).toISOString()).or(`ends_at.is.null,ends_at.lt.${now}`);
  await db.from("external_events").delete().lt("last_seen_at", new Date(Date.now() - 2 * 86_400_000).toISOString());
  return Response.json({ saved, merged: mergedAway.length, new: rows.filter((r) => !known.has(r.dedupe_key)).length, report });
});
