// refresh-news: gathers the latest city headlines from publishers' public RSS feeds,
// sorts each into a category and stores headline + short summary + picture + link.
// Readers open the full story on the publisher's own site.
// Runs on a schedule (pg_cron); safe to call often — it skips if it ran in the last 10 minutes.
import { createClient } from "jsr:@supabase/supabase-js@2";

const UA = "SheharSaathi-NewsBot/1.0 (+https://sheharsaathi.com)";
const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
  auth: { persistSession: false },
});

type Source = { id: string; city_id: string; name: string; feed_url: string; keep_if: string[] | null; max_items: number };
type Item = { url: string; title: string; summary: string | null; image_url: string | null; published_at: string; category: string };

// ---------- text helpers ----------
const ENT: Record<string, string> = { amp: "&", lt: "<", gt: ">", quot: '"', apos: "'", nbsp: " ", rsquo: "’", lsquo: "‘", rdquo: "”", ldquo: "“", ndash: "–", mdash: "—", hellip: "…" };
function decode(s: string): string {
  return s
    .replace(/&#x([0-9a-f]+);/gi, (_, h) => String.fromCodePoint(parseInt(h, 16)))
    .replace(/&#(\d+);/g, (_, d) => String.fromCodePoint(parseInt(d, 10)))
    .replace(/&([a-z]+);/gi, (m, n) => ENT[n.toLowerCase()] ?? m);
}
function cdata(s: string): string {
  return s.replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, "$1");
}
function stripTags(html: string): string {
  return decode(decode(html).replace(/<script[\s\S]*?<\/script>/gi, " ").replace(/<style[\s\S]*?<\/style>/gi, " ").replace(/<[^>]+>/g, " "))
    .replace(/\s+/g, " ")
    .trim();
}
function tag(item: string, name: string): string | null {
  const m = item.match(new RegExp(`<${name}(?:\\s[^>]*)?>([\\s\\S]*?)</${name}>`, "i"));
  return m ? cdata(m[1]).trim() : null;
}
function attr(item: string, tagName: string, attrName: string): string | null {
  const re = new RegExp(`<${tagName}\\b[^>]*\\b${attrName}=["']([^"']+)["']`, "i");
  const m = item.match(re);
  return m ? decode(m[1]) : null;
}
function firstImg(html: string | null): string | null {
  if (!html) return null;
  const m = decode(html).match(/<img[^>]+src=["']([^"']+)["']/i);
  return m ? m[1] : null;
}
function snippet(text: string, max = 220): string | null {
  const t = text
    .replace(/\s*The post .* appeared first on .*$/i, "")
    .replace(/\s*Continue reading.*$/i, "")
    .replace(/\[…\]|\[\.\.\.\]/g, "…")
    .trim();
  if (t.length < 25) return null;
  if (t.length <= max) return t;
  const cut = t.slice(0, max);
  return cut.slice(0, Math.max(cut.lastIndexOf(" "), max - 30)).replace(/[,;:.\s]+$/, "") + "…";
}
function norm(s: string): string {
  return s.toLowerCase().replace(/[^a-z0-9]+/g, " ").trim();
}
function cleanUrl(u: string): string {
  try {
    const x = new URL(u.trim());
    for (const k of [...x.searchParams.keys()]) if (/^utm_|^fbclid$|^gclid$/i.test(k)) x.searchParams.delete(k);
    x.hash = "";
    return x.toString();
  } catch {
    return u.trim();
  }
}
function goodImage(u: string | null): string | null {
  if (!u) return null;
  const s = decode(u).trim();
  if (!/^https?:\/\//i.test(s)) return null;
  if (/gravatar|logo|placeholder|default[-_]?image|og-image|theme\/images|1x1|pixel|feedburner|msid-47529300/i.test(s)) return null;
  return s.replace(/^http:\/\//i, "https://");
}

// ---------- categories ----------
// The group with the most matching words wins (headline words count double); no match → general city news.
const RULES: [string, RegExp][] = [
  ["weather", /\b(rain|rains|rainfall|monsoon|imd|weather|heatwave|heat wave|temperature|cyclone|thunderstorm|flood|floods|flooding|waterlog\w*|cold wave|fog|humidity|downpour|drizzle)\b/i],
  ["traffic", /\b(traffic|road accident|metro|mmts|tsrtc|tgsrtc|rtc bus|buses|flyover|road closure|diversion|commut\w*|airport|rgia|railway|train|trains|south central railway|scr|cab|auto[- ]?rickshaw|parking|outer ring road|orr|signal|congestion|accident)\b/i],
  ["crime", /\b(police|arrest\w*|murder\w*|crime|theft|robbery|fraud|scam|cyber ?crime|cheat\w*|assault\w*|killed|stabb\w*|smuggl\w*|ganja|drugs?|narcotic\w*|abduct\w*|kidnap\w*|acb|bribe|custody|fir|raid\w*|seiz\w*|absconding|chain snatch\w*|suicide|dies|died|death|dead|body found|missing|fire|blaze|drown\w*|safety|cops?)\b/i],
  ["civic", /\b(ghmc|hmda|hmwssb|water board|water supply|drinking water|sewer\w*|drain\w*|garbage|sanitation|pothole\w*|power cut|power supply|tgspdcl|tsspdcl|electricity|streetlight\w*|municipal|corporator|property tax|encroach\w*|demolition|hydraa|lake|lakes|groundwater|stray dogs?|mosquito\w*|civic|colony|residents)\b/i],
  ["politics", /\b(brs|bjp|congress|aimim|mim|revanth|ktr|kcr|harish rao|owaisi|minister|ministers|mla|mlas|mp|mps|assembly|election\w*|poll|polls|cabinet|chief minister|cm|governor|party|opposition|by-?election|panchayat|legislat\w*|electoral|voters?|eci|high court|hc|supreme court|govt\.?|government)\b/i],
  ["health", /\b(hospital\w*|health|doctor\w*|patients?|disease|dengue|malaria|covid|virus|fever|vaccin\w*|medical|surgery|cancer|osmania general|gandhi hospital|niloufer|nims|aiims)\b/i],
  ["education", /\b(school\w*|college\w*|universit\w*|students?|exam\w*|osmania university|jntu\w*|iit|iiit|uoh|admission\w*|results|ssc|inter(mediate)?|eamcet|eapcet|teachers?|education|campus|scholarship\w*)\b/i],
  ["business", /\b(startup\w*|it firm|it sector|it company|tech|technology|investment\w*|invest|company|companies|industr\w*|business|market|realty|real estate|jobs|hiring|layoff\w*|economy|gdp|data cent\w*|ai\b|pharma|hitec city|gachibowli|financial district|t-hub|ipo|startup|sbi|bank|banks|exports?|gst|tax)\b/i],
  ["sports", /\b(cricket|ipl|sunrisers|srh|football|hockey|badminton|tennis|match|tournament|stadium|uppal|medal\w*|athlete\w*|sports?|chess|marathon|kabaddi|olympi\w*|league)\b/i],
  ["entertainment", /\b(film|films|movie\w*|tollywood|actor|actress|cinema|music|concert|festival\w*|exhibition|art|theatre|theater|dance|bathukamma|bonalu|ganesh|diwali|sankranti|ramzan|eid|christmas|new year|food fest|event|events|show|celebrat\w*|heritage|culture|cultural|book fair|numaish|bigg boss|ott|web series|box office|tv|television|celebrity|singer)\b/i],
];
function categorise(title: string, summary: string | null): string {
  let best = "city", bestScore = 0;
  for (const [cat, re] of RULES) {
    const g = new RegExp(re.source, "gi");
    const score = (title.match(g)?.length ?? 0) * 2 + ((summary ?? "").match(g)?.length ?? 0);
    if (score > bestScore) { best = cat; bestScore = score; }
  }
  return best;
}

// ---------- feed parsing ----------
function parseFeed(xml: string, src: Source): Item[] {
  const blocks = xml.match(/<item\b[\s\S]*?<\/item>/gi) ?? xml.match(/<entry\b[\s\S]*?<\/entry>/gi) ?? [];
  const out: Item[] = [];
  const now = Date.now();
  for (const b of blocks.slice(0, src.max_items * (src.keep_if ? 3 : 1))) {
    const title = stripTags(tag(b, "title") ?? "");
    let link = tag(b, "link");
    if (!link || !/^https?:/.test(link)) link = attr(b, "link", "href") ?? tag(b, "guid");
    if (!title || !link || !/^https?:/.test(link)) continue;
    const descHtml = tag(b, "description") ?? tag(b, "summary") ?? "";
    const contentHtml = tag(b, "content:encoded") ?? tag(b, "content") ?? "";
    const text = stripTags(descHtml) || stripTags(contentHtml);

    if (src.keep_if) {
      // The headline or the story's opening words (its dateline) must mention the city.
      const hay = `${title} ${text.slice(0, 60)}`.toLowerCase();
      if (!src.keep_if.some((w) => hay.includes(w))) continue;
    }

    const image = goodImage(
      attr(b, "media:content", "url") ?? attr(b, "media:thumbnail", "url") ??
      (/<enclosure[^>]+type=["']image/i.test(b) ? attr(b, "enclosure", "url") : null) ??
      firstImg(descHtml) ?? firstImg(contentHtml),
    );
    const dateRaw = tag(b, "pubDate") ?? tag(b, "dc:date") ?? tag(b, "published") ?? tag(b, "updated");
    let t = dateRaw ? Date.parse(dateRaw) : NaN;
    if (isNaN(t)) t = now;
    if (t > now + 5 * 60_000) t = now; // never in the future
    let summary = snippet(text);
    if (summary && norm(summary).startsWith(norm(title).slice(0, 60)) && summary.length < title.length + 25) summary = null; // just repeats the headline
    out.push({ url: cleanUrl(decode(link)), title, summary, image_url: image, published_at: new Date(t).toISOString(), category: categorise(title, summary) });
    if (out.length >= src.max_items) break;
  }
  return out;
}

async function get(url: string, ms: number): Promise<Response> {
  return await fetch(url, { headers: { "User-Agent": UA, Accept: "application/rss+xml, application/xml, text/xml, text/html;q=0.8, */*;q=0.5" }, signal: AbortSignal.timeout(ms), redirect: "follow" });
}

// Fills a missing picture or summary from the article page's own preview tags (og:image / og:description).
async function enrich(it: Item): Promise<void> {
  try {
    const r = await get(it.url, 6000);
    if (!r.ok) return;
    const html = (await r.text()).slice(0, 200_000);
    const meta = (p: string) => {
      const a = html.match(new RegExp(`<meta[^>]+(?:property|name)=["']${p}["'][^>]+content=["']([^"']+)["']`, "i"));
      const b = html.match(new RegExp(`<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["']${p}["']`, "i"));
      return (a ?? b)?.[1] ?? null;
    };
    if (!it.image_url) it.image_url = goodImage(meta("og:image") ?? meta("twitter:image"));
    if (!it.summary) {
      const d = meta("og:description") ?? meta("description");
      if (d && !norm(d).startsWith(norm(it.title).slice(0, 60))) { it.summary = snippet(stripTags(d)); it.category = categorise(it.title, it.summary); }
    }
  } catch { /* leave as is */ }
}

Deno.serve(async (req) => {
  const force = new URL(req.url).searchParams.get("force") === "1";
  const { data: sources, error } = await db.from("news_sources").select("id,city_id,name,feed_url,keep_if,max_items,last_fetched_at").eq("enabled", true);
  if (error) return Response.json({ error: error.message }, { status: 500 });

  const latest = Math.max(0, ...(sources ?? []).map((s: { last_fetched_at: string | null }) => (s.last_fetched_at ? Date.parse(s.last_fetched_at) : 0)));
  if (!force && Date.now() - latest < 10 * 60_000) return Response.json({ skipped: "ran less than 10 minutes ago" });

  const report: Record<string, string> = {};
  let added = 0;
  await Promise.all((sources as Source[]).map(async (src) => {
    let status = "ok";
    try {
      const r = await get(src.feed_url, 15000);
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      const items = parseFeed(await r.text(), src);
      const fresh = items.filter((i) => Date.now() - Date.parse(i.published_at) < 4 * 86_400_000);
      // Only look up preview tags for stories we don't already have.
      const { data: known } = fresh.length
        ? await db.from("news_articles").select("url").in("url", fresh.map((i) => i.url))
        : { data: [] };
      const have = new Set((known ?? []).map((k: { url: string }) => k.url));
      const newOnes = fresh.filter((i) => !have.has(i.url));
      const needs = newOnes.filter((i) => !i.image_url || !i.summary).slice(0, 25);
      for (let k = 0; k < needs.length; k += 4) await Promise.all(needs.slice(k, k + 4).map(enrich));
      if (newOnes.length) {
        const rows = newOnes.map((i) => ({ ...i, city_id: src.city_id, source_id: src.id }));
        const { error: e } = await db.from("news_articles").upsert(rows, { onConflict: "url", ignoreDuplicates: true });
        if (e) throw new Error(e.message);
        added += newOnes.length;
      }
      status = `ok: ${items.length} read, ${newOnes.length} new`;
    } catch (e) {
      status = `error: ${(e as Error).message}`.slice(0, 200);
    }
    report[src.id] = status;
    await db.from("news_sources").update({ last_fetched_at: new Date().toISOString(), last_status: status }).eq("id", src.id);
  }));

  // Keep a week of stories.
  await db.from("news_articles").delete().lt("published_at", new Date(Date.now() - 7 * 86_400_000).toISOString());
  return Response.json({ added, report });
});
