// place-search: address and place search for the app, tuned for Indian addresses.
// Uses Photon (OpenStreetMap data, type-as-you-go). Understands door numbers like
// "8-2-293/82/A", flat/plot prefixes, and "opposite/near <landmark>" directions, and
// says how precise each result is (door, building, street, area).
// GET ?q=...&lat=..&lng=..&city=hyderabad

const UA = "SheharSaathi-Search/1.0 (+https://sheharsaathi.com)";
const CITIES: Record<string, { bbox: [number, number, number, number]; lat: number; lng: number; name: string }> = {
  hyderabad: { bbox: [78.15, 17.2, 78.75, 17.65], lat: 17.4, lng: 78.45, name: "Hyderabad" },
};

type Hit = {
  id: string; name: string; sub: string; lat: number; lng: number;
  kind: "metro" | "venue" | "street" | "area" | "landmark";
  precision: "door" | "building" | "street" | "area";
  note: string | null; score: number; house: string | null; street: string | null;
};

// ---------- reading the address ----------
const UNIT = /^\s*(?:flat|apt|apartment|room|shop|office|unit|suite|floor|block|tower|wing|h\.?\s*no|d\.?\s*no|door\s*no|house\s*no|plot(?:\s*no)?|site\s*no|survey\s*no|no)\.?\s*[:#.-]?\s*[\w\/-]+\s*,?\s*/i;
const DOOR = /\b(\d{1,2}-\d{1,4}(?:-\d{1,5})?(?:\/[\w-]+)*)\b|#\s*(\d+[\w\/-]*)/i;
const LANDMARK = /\b(?:opp(?:osite)?\.?|near|nr\.?|beside|behind|next\s+to|adjacent\s+to|in\s+front\s+of|above|below|besides)\s+(.+?)(?:,|$)/i;

function tidy(q: string): string {
  return q.replace(/\s+/g, " ")
    .replace(/\brd\b\.?/gi, "road").replace(/\bst\b\.?/gi, "street").replace(/\bx\s*roads?\b/gi, "X Roads")
    .replace(/\broad\s*no\.?\s*/gi, "Road No ").replace(/\bno\.\s*/gi, "No ")
    .replace(/\bhyd\b/gi, "Hyderabad").replace(/\bsec'?bad\b/gi, "Secunderabad")
    .trim();
}

function parse(raw: string) {
  let rest = tidy(raw);
  const units: string[] = [];
  // Peel off "Flat 302,", "Plot 45,", "H.No 1-2-3," etc. from the front (there can be a few).
  for (let i = 0; i < 3; i++) {
    const m = rest.match(UNIT);
    if (!m || m[0].length === rest.length) break;
    units.push(m[0].replace(/[,\s]+$/, ""));
    rest = rest.slice(m[0].length);
  }
  const dm = (units.join(" ") + " " + rest).match(DOOR);
  const door = dm ? (dm[1] ?? dm[2]) : null;
  if (door) rest = rest.replace(door, "").replace(/^[\s,#-]+/, "");
  const lm = rest.match(LANDMARK);
  const landmark = lm ? lm[1].trim() : null;
  const withoutLandmark = lm ? rest.replace(lm[0], "").replace(/^[\s,]+|[\s,]+$/g, "") : rest;
  const parts = withoutLandmark.split(",")
    .map((s) => s.replace(/\b\d{6}\b/g, "").replace(/\b(hyderabad|telangana|india)\b/gi, "").replace(/\s+/g, " ").trim())
    .filter(Boolean);
  // The area the person named: the last piece, or what's left beside a landmark ("lane opposite X, Kukatpally").
  const locality = parts.length > 1 ? parts[parts.length - 1]
    : lm && parts.length ? parts[0].replace(/^(lane|street|road|gali|cross)\s+/i, "").trim() || null : null;
  return { cleaned: tidy(raw), rest: withoutLandmark, parts, door, units, landmark, locality };
}

// ---------- Photon ----------
let diag: string[] = [];
async function photon(q: string, city: typeof CITIES[string], biasLat: number, biasLng: number): Promise<any[]> {
  if (q.trim().length < 2) return [];
  const [w, s, e, n] = city.bbox;
  const u = `https://photon.komoot.io/api/?limit=10&lang=en&lat=${biasLat}&lon=${biasLng}&bbox=${w},${s},${e},${n}&q=${encodeURIComponent(q)}`;
  for (let attempt = 0; attempt < 2; attempt++) {
    const t0 = Date.now();
    try {
      const r = await fetch(u, { headers: { "User-Agent": UA }, signal: AbortSignal.timeout(9000) });
      diag.push(`${q} → ${r.status} in ${Date.now() - t0}ms`);
      if (r.status === 429 || r.status >= 500) { await new Promise((ok) => setTimeout(ok, 700)); continue; } // busy: wait and retry once
      if (!r.ok) return [];
      const j = await r.json();
      return j.features ?? [];
    } catch (e) { diag.push(`${q} → ${String(e)} | ${String((e as { cause?: unknown }).cause ?? "")} after ${Date.now() - t0}ms`); }
  }
  return [];
}

function km(aLat: number, aLng: number, bLat: number, bLng: number) {
  const r = (d: number) => d * Math.PI / 180;
  const x = Math.sin(r(bLat - aLat) / 2) ** 2 + Math.cos(r(aLat)) * Math.cos(r(bLat)) * Math.sin(r(bLng - aLng) / 2) ** 2;
  return 12742 * Math.asin(Math.sqrt(x));
}
const norm = (s: string) => s.toLowerCase().replace(/[^a-z0-9]+/g, " ").trim();

// How much of the typed door number matches a mapped one: "8-2-293/82/A" vs "8-2-293/82/A/1125".
function doorMatch(typed: string, mapped: string | null): number {
  if (!mapped) return 0;
  const a = typed.toLowerCase().split(/[-\/]/), b = mapped.toLowerCase().split(/[-\/]/);
  let n = 0;
  while (n < a.length && n < b.length && a[n] === b[n]) n++;
  return n / a.length; // 1 = every part of what was typed matches
}

function toHit(f: any, bias: { lat: number; lng: number }): Hit | null {
  const p = f.properties ?? {};
  const [lng, lat] = f.geometry?.coordinates ?? [];
  if (typeof lat !== "number") return null;
  const area = p.district || p.locality || p.suburb || p.city || "";
  const cleanArea = String(area).replace(/^Ward \d+\s*/i, "");
  const isStreet = p.type === "street";
  const isArea = ["district", "locality", "city", "county", "state"].includes(p.type) || p.osm_key === "place";
  const house = p.housenumber ? String(p.housenumber) : null;
  const name = p.name || (p.street ? (house ? `${house}, ${p.street}` : p.street) : cleanArea);
  if (!name) return null;
  const subParts = [house && p.name ? house : null, p.street && p.street !== name ? p.street : null, cleanArea && cleanArea !== name ? cleanArea : null]
    .filter(Boolean) as string[];
  const kind: Hit["kind"] =
    /station|subway|halt|bus_station/.test(p.osm_value ?? "") ? "metro"
    : isStreet ? "street" : isArea ? "area"
    : ["amenity", "shop", "tourism", "leisure", "office", "craft", "healthcare"].includes(p.osm_key) ? "venue" : "landmark";
  return {
    id: `${p.osm_type ?? "x"}${p.osm_id ?? Math.round(lat * 1e6)}`,
    name, sub: subParts.join(", "), lat, lng, kind,
    precision: house ? "building" : isStreet ? "street" : isArea ? "area" : "building",
    note: null, score: -km(bias.lat, bias.lng, lat, lng) * 0.02, house, street: p.street ?? (isStreet ? p.name : null),
  };
}

// ---------- search ----------
const cache = new Map<string, { at: number; body: string }>();

async function search(raw: string, cityId: string, biasLat: number, biasLng: number) {
  const city = CITIES[cityId] ?? CITIES.hyderabad;
  const bias = { lat: biasLat, lng: biasLng };
  const a = parse(raw);
  const tries: { q: string; weight: number; why: string }[] = [];
  tries.push({ q: a.cleaned, weight: 3, why: "full" });
  if (a.door) tries.push({ q: [a.door, a.locality ?? a.parts.slice(-2).join(" ")].filter(Boolean).join(" "), weight: 3, why: "door" });
  if (a.landmark) tries.push({ q: [a.landmark, a.locality].filter(Boolean).join(" "), weight: 2, why: "landmark" });
  if (a.rest && norm(a.rest) !== norm(a.cleaned)) tries.push({ q: a.rest, weight: 2, why: "no-unit" });
  if (a.parts.length > 2) tries.push({ q: a.parts.slice(0, 2).join(" "), weight: 1.6, why: "first-two" });
  // Then broader: drop leading pieces one at a time ("Aparna Towers, Kondapur" -> "Kondapur").
  for (let i = 1; i < a.parts.length; i++) tries.push({ q: a.parts.slice(i).join(", "), weight: 1.5 - i * 0.2, why: "broader" });

  // Ask the map service step by step, most specific first, and stop as soon as the results
  // clearly match what was typed (usually after 1–2 requests; at most 4).
  const wantWords = norm(a.parts.join(" ")).split(" ").filter((w) => w.length > 2);
  const hits: Hit[] = [];
  const seenQ = new Set<string>();
  const queue = tries.filter((t) => { const k = norm(t.q); if (!k || seenQ.has(k)) return false; seenQ.add(k); return true; });
  const good = () => hits.some((h) => {
    const hay = norm(`${h.name} ${h.sub}`);
    const got = wantWords.filter((w) => hay.includes(w)).length;
    return h.precision === "door" || (wantWords.length > 0 && got / wantWords.length >= 0.6) || (wantWords.length === 0 && hits.length > 0);
  });
  const take = (t: { q: string; weight: number; why: string }, feats: any[]) => {
    feats.forEach((f: any, i: number) => {
      const h = toHit(f, bias);
      if (!h) return;
      h.score += t.weight - i * 0.15;
      // Reward results that contain more of the words the person typed.
      const hay = norm(`${h.name} ${h.sub}`);
      h.score += wantWords.filter((w) => hay.includes(w)).length * 0.4;
      if (a.door) {
        const m = doorMatch(a.door, h.house ?? (/\d+-\d+/.test(h.name) ? h.name : null));
        if (m === 1) { h.precision = "door"; h.score += 6; }
        else if (m >= 0.5) { h.score += 3 * m; h.note = `Door no. ${a.door} isn't mapped exactly; this is the nearest numbered building (${h.house ?? h.name}).`; }
      }
      if (t.why === "landmark" && a.landmark) h.note = `Near ${a.landmark} — the exact spot isn't named on the map. Drop a pin to mark it.`;
      hits.push(h);
    });
  };
  // First round: the whole text, plus the door number with its area when there is one.
  const first = queue.splice(0, a.door ? 2 : 1);
  (await Promise.all(first.map(async (t) => [t, await photon(t.q, city, biasLat, biasLng)] as const))).forEach(([t, f]) => take(t, f));
  let calls = first.length;
  while (queue.length && calls < 4 && !good()) {
    const t = queue.shift()!;
    take(t, await photon(t.q, city, biasLat, biasLng));
    calls++;
  }

  // One entry per place: OpenStreetMap stores long roads in pieces; keep the nearest piece.
  const byKey = new Map<string, Hit>();
  for (const h of hits) {
    const key = h.precision === "street" || h.kind === "street"
      ? `street|${norm(h.name)}|${norm(h.sub)}`
      : `${norm(h.name)}|${Math.round(h.lat * 400)}|${Math.round(h.lng * 400)}`;
    const cur = byKey.get(key);
    if (!cur || h.score > cur.score) byKey.set(key, cur ? { ...h, note: h.note ?? cur.note } : h);
  }
  let list = [...byKey.values()].sort((x, y) => y.score - x.score).slice(0, 12);

  // Be honest about what was found when the door or flat itself isn't on the map.
  const asked = a.door ?? (a.units.length ? a.units.join(" ") : null);
  if (asked) list = list.map((h) => h.precision === "door" || h.note ? h : {
    ...h, note: h.precision === "street" ? `${asked} isn't on the map — this takes you to the street.`
      : h.precision === "area" ? `${asked} isn't on the map — this is the area.`
      : `${asked} isn't on the map — this is the building or nearest match.`,
  });
  return { query: raw, door: a.door, results: list.map(({ score: _s, house: _h, street: _st, ...rest }) => rest) };
}

Deno.serve(async (req) => {
  const u = new URL(req.url);
  const q = (u.searchParams.get("q") ?? "").slice(0, 200);
  const city = u.searchParams.get("city") ?? "hyderabad";
  const lat = parseFloat(u.searchParams.get("lat") ?? "") || CITIES[city]?.lat || 17.4;
  const lng = parseFloat(u.searchParams.get("lng") ?? "") || CITIES[city]?.lng || 78.45;
  const headers = { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*", "Cache-Control": "public, max-age=300" };
  if (q.trim().length < 2) return new Response(JSON.stringify({ query: q, results: [] }), { headers });
  // Results near the same spot are shared for a while (≈1 km grid) to keep the search fast.
  const key = `${city}|${norm(q)}|${lat.toFixed(2)}|${lng.toFixed(2)}`;
  const hit = cache.get(key);
  if (hit && Date.now() - hit.at < 10 * 60_000 && u.searchParams.get("debug") !== "1") return new Response(hit.body, { headers });
  diag = [];
  const found = await search(q, city, lat, lng);
  const body = JSON.stringify(u.searchParams.get("debug") === "1" ? { ...found, diag } : found);
  if (cache.size > 500) cache.delete(cache.keys().next().value!);
  // Don't remember empty answers: they're usually a busy map service, not a real "nothing here".
  if (found.results.length) cache.set(key, { at: Date.now(), body });
  return new Response(body, { headers: { ...headers, "Cache-Control": found.results.length ? "public, max-age=300" : "no-store" } });
});
