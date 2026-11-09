#!/usr/bin/env python3
"""Read-only quality audit for RS TV's public M3U discovery sources.

This checks playlist endpoint availability and M3U structure/URL hygiene.
It does not probe channel streams, grant rights, or approve channels.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit, urlunsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener

MAX_BYTES = 12 * 1024 * 1024
TIMEOUT_SECONDS = 20
USER_AGENT = "RS-TV-PlaylistQualityAudit/1.0 (+https://github.com/Ramistar900/rs-tv)"
PAGE_ONLY_HOSTS = {
    "youtube.com", "youtu.be", "googlevideo.com", "twitch.tv", "ttvnw.net",
    "dailymotion.com", "facebook.com", "fb.watch", "fbcdn.net",
    "instagram.com", "tiktok.com", "rumble.com",
}
SOURCES = [
    ("doubo-world", "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/index.m3u"),
    ("doubo-turkey", "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/countries/tr.m3u"),
    ("doubo-arabic", "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/languages/ara.m3u"),
    ("doubo-sports", "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/categories/sports.m3u"),
    ("iptv-org-world", "https://iptv-org.github.io/iptv/index.m3u"),
    ("iptv-org-arab", "https://iptv-org.github.io/iptv/regions/arab.m3u"),
    ("iptv-org-turkey", "https://iptv-org.github.io/iptv/countries/tr.m3u"),
    ("iptv-org-sports", "https://iptv-org.github.io/iptv/categories/sports.m3u"),
    ("iptv-org-movies", "https://iptv-org.github.io/iptv/categories/movies.m3u"),
    ("iptv-org-series", "https://iptv-org.github.io/iptv/categories/series.m3u"),
    ("iptv-org-animation", "https://iptv-org.github.io/iptv/categories/animation.m3u"),
    ("iptv-org-auto", "https://iptv-org.github.io/iptv/categories/auto.m3u"),
    ("iptv-org-business", "https://iptv-org.github.io/iptv/categories/business.m3u"),
    ("iptv-org-classic", "https://iptv-org.github.io/iptv/categories/classic.m3u"),
    ("iptv-org-comedy", "https://iptv-org.github.io/iptv/categories/comedy.m3u"),
    ("iptv-org-cooking", "https://iptv-org.github.io/iptv/categories/cooking.m3u"),
    ("iptv-org-culture", "https://iptv-org.github.io/iptv/categories/culture.m3u"),
    ("iptv-org-documentary", "https://iptv-org.github.io/iptv/categories/documentary.m3u"),
    ("iptv-org-education", "https://iptv-org.github.io/iptv/categories/education.m3u"),
    ("iptv-org-entertainment", "https://iptv-org.github.io/iptv/categories/entertainment.m3u"),
    ("iptv-org-family", "https://iptv-org.github.io/iptv/categories/family.m3u"),
    ("iptv-org-general", "https://iptv-org.github.io/iptv/categories/general.m3u"),
    ("iptv-org-kids", "https://iptv-org.github.io/iptv/categories/kids.m3u"),
    ("iptv-org-legislative", "https://iptv-org.github.io/iptv/categories/legislative.m3u"),
    ("iptv-org-lifestyle", "https://iptv-org.github.io/iptv/categories/lifestyle.m3u"),
    ("iptv-org-music", "https://iptv-org.github.io/iptv/categories/music.m3u"),
    ("iptv-org-news", "https://iptv-org.github.io/iptv/categories/news.m3u"),
    ("iptv-org-outdoor", "https://iptv-org.github.io/iptv/categories/outdoor.m3u"),
    ("iptv-org-public", "https://iptv-org.github.io/iptv/categories/public.m3u"),
    ("iptv-org-relax", "https://iptv-org.github.io/iptv/categories/relax.m3u"),
    ("iptv-org-religious", "https://iptv-org.github.io/iptv/categories/religious.m3u"),
    ("iptv-org-science", "https://iptv-org.github.io/iptv/categories/science.m3u"),
    ("iptv-org-travel", "https://iptv-org.github.io/iptv/categories/travel.m3u"),
    ("iptv-org-weather", "https://iptv-org.github.io/iptv/categories/weather.m3u"),
    ("free-tv", "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"),
    ("freecasthub", "https://raw.githubusercontent.com/freecasthub/public-iptv/main/playlist.m3u"),
    ("m3upt-portuguese", "https://raw.githubusercontent.com/LITUATUI/M3UPT/main/M3U/M3UPT.m3u"),
    ("romaxa-world-auto-checked", "https://raw.githubusercontent.com/Romaxa55/world_ip_tv/master/output/index.m3u"),
]

# Doubo publishes one separate M3U file per country/region. The country lists overlap
# with its aggregate and category playlists; cross-source URLs are deduplicated in the audit.
DOUBO_COUNTRY_CODES = ["US","IN","DE","SE","GB","IT","ES","CA","NL","MX","FR","BR","KR","CL","ID","DO","PL","RO","AU","RU","TH","CO","CR","IQ","CN","TR","AR","HN","PR","AE","AT","BE","VE","VN","IR","MD","SG","DK","EE","GR","HK","IL","LU","MK","PE","PT","SA","UA","BO","JP","PH","PK","BH","CH","FI","HU","IE","LT","NG","NO","RW","SK","AL","BG","CZ","IS","KH","MN","MY","NZ","QA","SV","TW","UY","AZ","BA","GT","PY","AF","BJ","BQ","CM","CY","EC","EG","KW","KZ","LB","LK","MT","MZ","PS","RS","SD","SY","TN","TT","VG","ZA","AG","AM","AO","BI","BS","BY","CW","GE","GH","GM","GP","GQ","HR","HT","JO","LC","LV","LY","MA","MC","MM","MQ","MR","NA","NI","NP","OM","UG","XK"]
SOURCES.extend(
    (
        f"doubo-country-{code.lower()}",
        f"https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/countries/{'uk' if code == 'GB' else code.lower()}.m3u",
    )
    for code in DOUBO_COUNTRY_CODES
)



def canonical_url(value: str) -> str | None:
    try:
        parsed = urlsplit(value.strip())
        if parsed.scheme.lower() != "https" or not parsed.hostname:
            return None
        if parsed.username is not None or parsed.password is not None:
            return None
        host = parsed.hostname.lower()
        port = parsed.port
        netloc = host if port in (None, 443) else f"{host}:{port}"
        return urlunsplit(("https", netloc, parsed.path or "/", parsed.query, ""))
    except ValueError:
        return None


def is_page_only_platform(value: str) -> bool:
    try:
        host = (urlsplit(value).hostname or "").lower()
        return any(host == blocked or host.endswith("." + blocked) for blocked in PAGE_ONLY_HOSTS)
    except ValueError:
        return False


def inspect_playlist(content: str) -> dict:
    lines = [line.strip() for line in content.lstrip("\ufeff").splitlines() if line.strip()]
    if not lines or not lines[0].upper().startswith("#EXTM3U"):
        raise ValueError("Response is not an M3U playlist")

    channels = []
    pending = None
    orphan_urls = 0
    malformed_entries = 0
    for line in lines[1:]:
        if line.upper().startswith("#EXTINF:"):
            if pending is not None:
                malformed_entries += 1
            title = line.rsplit(",", 1)[-1].strip() if "," in line else ""
            pending = {"name": title, "has_tvg_id": "tvg-id" in line.lower(), "has_group": "group-title" in line.lower()}
            continue
        if line.startswith("#"):
            continue
        if pending is None:
            orphan_urls += 1
            continue
        channels.append({"url": line, **pending})
        pending = None
    if pending is not None:
        malformed_entries += 1

    seen = set()
    duplicates = 0
    unsafe = 0
    page_links = 0
    missing_names = 0
    for channel in channels:
        key = canonical_url(channel["url"])
        if key is None:
            unsafe += 1
        elif key in seen:
            duplicates += 1
        else:
            seen.add(key)
        if is_page_only_platform(channel["url"]):
            page_links += 1
        if not channel["name"] or channel["name"].lower() in {"unknown", "unnamed", "channel"}:
            missing_names += 1

    eligible_unique = {canonical_url(channel["url"]) for channel in channels
                       if canonical_url(channel["url"]) is not None and not is_page_only_platform(channel["url"])}
    return {
        "entries": len(channels),
        "unique_https_urls": len(seen),
        "eligible_unique_https_urls": len(eligible_unique),
        "duplicate_urls": duplicates,
        "unsafe_or_non_https_urls": unsafe,
        "platform_page_links": page_links,
        "missing_channel_names": missing_names,
        "orphan_stream_urls": orphan_urls,
        "incomplete_extinf_entries": malformed_entries,
    }


class SameHostHttpsRedirectHandler(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        old = urlsplit(req.full_url)
        new = urlsplit(newurl)
        if new.scheme.lower() != "https" or (new.hostname or "").lower() != (old.hostname or "").lower():
            raise HTTPError(newurl, code, "Redirect rejected: HTTPS same-host redirects only", headers, fp)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def fetch_playlist(url: str) -> str:
    parsed = urlsplit(url)
    if parsed.scheme.lower() != "https" or not parsed.hostname or parsed.username or parsed.password:
        raise ValueError("Configured source URL must be HTTPS without credentials")
    request = Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/vnd.apple.mpegurl,audio/x-mpegurl,text/plain"})
    opener = build_opener(SameHostHttpsRedirectHandler())
    with opener.open(request, timeout=TIMEOUT_SECONDS) as response:
        if response.status != 200:
            raise RuntimeError(f"HTTP {response.status}")
        payload = response.read(MAX_BYTES + 1)
    if len(payload) > MAX_BYTES:
        raise ValueError("Playlist exceeds 12 MiB audit limit")
    return payload.decode("utf-8-sig")


def audit(fetcher=fetch_playlist) -> dict:
    results = []
    source_unique_totals = 0
    global_unique_streams = set()
    total_entries = 0
    for source_id, url in SOURCES:
        try:
            content = fetcher(url)
            metrics = inspect_playlist(content)
            source_keys = {
                canonical_url(line) for line in content.lstrip("\ufeff").splitlines()
                if line.strip() and not line.lstrip().startswith("#")
                and canonical_url(line) is not None and not is_page_only_platform(line)
            }
            source_unique_totals += len(source_keys)
            global_unique_streams.update(source_keys)
            total_entries += metrics["entries"]
            results.append({"source_id": source_id, "url": url, "status": "ok", **metrics})
        except Exception as exc:  # report each source independently; one outage must not hide others
            results.append({"source_id": source_id, "url": url, "status": "error", "error": str(exc)[:240]})
    successful = sum(item["status"] == "ok" for item in results)
    return {
        "schema_version": 1,
        "generated_at_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "purpose": "Read-only playlist source and M3U structure audit",
        "channel_stream_probes_performed": 0,
        "rights_review_performed": False,
        "approval_effect": "none; all entries remain candidates until separate technical and rights review",
        "sources_checked": len(results),
        "sources_available": successful,
        "total_playlist_entries": total_entries,
        "unique_eligible_stream_urls_across_sources": len(global_unique_streams),
        "duplicate_stream_urls_across_sources": source_unique_totals - len(global_unique_streams),
        "sources": results,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="quality-report.json")
    args = parser.parse_args()
    report = audit()
    with open(args.output, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(report, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print(json.dumps({k: v for k, v in report.items() if k != "sources"}, ensure_ascii=False))
    for item in report["sources"]:
        if item["status"] == "ok":
            print(f'{item["source_id"]}: {item["entries"]} entries, {item["unique_https_urls"]} unique HTTPS, {item["duplicate_urls"]} duplicates, {item["unsafe_or_non_https_urls"]} unsafe')
        else:
            print(f'{item["source_id"]}: ERROR {item["error"]}', file=sys.stderr)
    if report["sources_available"] == 0:
        raise SystemExit("No playlist source could be audited")


if __name__ == "__main__":
    main()
