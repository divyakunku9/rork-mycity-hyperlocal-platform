"""Finds a centre point for each of Hyderabad's 300 wards (supabase/data/hyderabad_wards.json).

Ward boundaries aren't published as map data, so the app matches a spot to the nearest ward centre.
Each circle is located first and anchors its wards, so same-named places elsewhere in the city
(there are several "Gandhi Nagar"s) aren't picked. Uses OSM Nominatim at 1 request/second,
then Photon as a fallback. Writes out/wards_geo.json. Standard library only.
"""
import json, math, re, sys, time, urllib.parse, urllib.request

UA = "SheharSaathi/1.0 (ward centres; github.com/divyakunku9/rork-mycity-hyperlocal-platform)"
VIEWBOX = "78.10,17.75,78.85,17.15"  # lon1,lat1,lon2,lat2
PLACE = {"suburb", "neighbourhood", "quarter", "locality", "village", "hamlet", "town", "city_block", "residential", "isolated_dwelling", "city_district"}
CITY = (17.42, 78.47)


def km(a, b):
    x, y = math.radians(b[0] - a[0]), math.radians(b[1] - a[1])
    h = math.sin(x / 2) ** 2 + math.cos(math.radians(a[0])) * math.cos(math.radians(b[0])) * math.sin(y / 2) ** 2
    return 2 * 6371 * math.asin(math.sqrt(h))


def clean(n):
    n = re.sub(r"\(.*?\)", "", n)
    return re.sub(r"\s+", " ", re.sub(r"[–\-.]", " ", n)).strip()


def norm(s):
    return re.sub(r"[^a-z]", "", s.lower().replace("hitech", "hitec"))


def variants(n):
    c = clean(n)
    out = [c]
    for v in (re.sub(r"([a-z])(nagar|guda|pally|palli|pet|bagh|puram|gutta|pur|kunta|bowli|colony)\b", r"\1 \2", c, flags=re.I),
              re.sub(r"\s+(nagar|guda|pally|pet|bagh|puram|gutta|pur|kunta)\b", r"\1", c, flags=re.I)):
        if v not in out:
            out.append(v)
    m = re.search(r"\((.*?)\)", n)
    if m:
        out.append(m.group(1))
    return out


def get(url):
    for i in range(3):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept-Language": "en"})
            with urllib.request.urlopen(req, timeout=25) as r:
                return json.loads(r.read().decode())
        except Exception as e:  # noqa: BLE001
            print("  retry", i, e, file=sys.stderr)
            time.sleep(3 + 3 * i)
    return None


def nominatim(q):
    url = "https://nominatim.openstreetmap.org/search?" + urllib.parse.urlencode(
        {"q": q, "format": "jsonv2", "limit": 10, "viewbox": VIEWBOX, "bounded": 1, "countrycodes": "in"})
    time.sleep(1.1)
    res = get(url) or []
    return [{"lat": float(r["lat"]), "lng": float(r["lon"]), "label": r.get("name") or r.get("display_name", "").split(",")[0],
             "type": r.get("type", ""), "src": "nominatim"} for r in res]


def photon(q, near):
    url = "https://photon.komoot.io/api/?" + urllib.parse.urlencode(
        {"q": q, "limit": 10, "bbox": "78.10,17.15,78.85,17.75", "lat": near[0], "lon": near[1]})
    time.sleep(0.6)
    res = get(url) or {}
    return [{"lat": f["geometry"]["coordinates"][1], "lng": f["geometry"]["coordinates"][0],
             "label": f["properties"].get("name", ""), "type": f["properties"].get("osm_value", ""), "src": "photon"}
            for f in res.get("features", [])]


def best(hits, name, near, max_km):
    want = norm(clean(name))
    top, score = None, 1e9
    for h in hits:
        n = norm(h["label"])
        if not n or not (n == want or n.startswith(want) or (want.startswith(n) and len(n) >= 5)):
            continue
        d = km(near, (h["lat"], h["lng"]))
        if d > max_km:
            continue
        s = d + (0 if h["type"] in PLACE else 2.5) + (0 if n == want else 0.5)
        if s < score:
            top, score = h, s
    return top


def locate(name, near, max_km):
    for q in variants(name):
        h = best(nominatim(q + ", Hyderabad"), q, near, max_km) or best(nominatim(q), q, near, max_km)
        if h:
            return h
    for q in variants(name):
        h = best(photon(q, near), q, near, max_km)
        if h:
            return h
    return None


def main():
    wards = json.load(open(sys.argv[1]))
    circles = {}
    for c in sorted({w["circle"] for w in wards}):
        h = locate(c, CITY, 40)
        circles[c] = (h["lat"], h["lng"]) if h else None
        print("circle", c, circles[c], flush=True)
    out = []
    for w in wards:
        near = circles.get(w["circle"])
        h = locate(w["name"], near or CITY, 9 if near else 40)
        at = (h["lat"], h["lng"]) if h else near
        note = f"{h['src']} {h['type']}: {h['label']}" if h else (f"{w['circle']} circle centre (approx.)" if near else None)
        out.append({**w, "lat": at[0] if at else None, "lng": at[1] if at else None, "geo_note": note})
        print(w["no"], w["name"], at, note, flush=True)
    json.dump({"circles": circles, "wards": out}, open(sys.argv[2], "w"), indent=1, ensure_ascii=False)
    print("located", sum(1 for o in out if o["lat"]), "of", len(out), "; approx", sum(1 for o in out if o["geo_note"] and "approx" in o["geo_note"]))


if __name__ == "__main__":
    main()
