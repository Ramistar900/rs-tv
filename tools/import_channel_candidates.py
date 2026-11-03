#!/usr/bin/env python3
"""Import IPTV-org metadata into a review-only RS TV candidate catalogue.

This tool does not establish broadcast rights or stream reliability. Every
record is emitted with status=candidate and is excluded from the playable
catalogue until separately reviewed and approved.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from collections import defaultdict
from datetime import datetime, timezone
from urllib.error import URLError
from urllib.request import Request, urlopen

BASE = "https://iptv-org.github.io/api"
ENDPOINTS = {
    "channels": f"{BASE}/channels.json",
    "streams": f"{BASE}/streams.json",
    "countries": f"{BASE}/countries.json",
    "languages": f"{BASE}/languages.json",
    "categories": f"{BASE}/categories.json",
    "logos": f"{BASE}/logos.json",
    "feeds": f"{BASE}/feeds.json",
    "blocklist": f"{BASE}/blocklist.json",
}
USER_AGENT = "RS-TV-CatalogueImporter/1.0 (+https://github.com/Ramistar900/rs-tv)"


def fetch_json(url: str, timeout: int = 45):
    request = Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/json"})
    with urlopen(request, timeout=timeout) as response:
        if response.status != 200:
            raise RuntimeError(f"HTTP {response.status} while fetching {url}")
        payload = response.read()
    try:
        value = json.loads(payload)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise RuntimeError(f"Invalid JSON from {url}: {exc}") from exc
    if not isinstance(value, list):
        raise RuntimeError(f"Expected a JSON array from {url}")
    return value


def valid_stream_url(value):
    if not isinstance(value, str):
        return False
    value = value.strip()
    return value.startswith("https://") or value.startswith("http://")


def build_catalog(data, generated_at):
    channels = data["channels"]
    countries = {x.get("code"): x for x in data["countries"] if x.get("code")}
    languages = {x.get("code"): x.get("name") for x in data["languages"] if x.get("code")}
    blocked = {x.get("channel") for x in data["blocklist"] if x.get("channel")}
    feeds_by_channel = defaultdict(list)
    for feed in data["feeds"]:
        if feed.get("channel"):
            feeds_by_channel[feed["channel"]].append(feed)
    logos_by_channel = defaultdict(list)
    for logo in data["logos"]:
        if logo.get("channel") and logo.get("url"):
            logos_by_channel[logo["channel"]].append(logo)
    streams_by_channel = defaultdict(list)
    for stream in data["streams"]:
        channel_id = stream.get("channel")
        url = stream.get("url")
        if channel_id and valid_stream_url(url):
            labels = stream.get("labels") or stream.get("label") or []
            if isinstance(labels, str):
                labels = [labels]
            streams_by_channel[channel_id].append({
                "title": stream.get("title"),
                "url": url.strip(),
                "quality": stream.get("quality"),
                "labels": sorted(set(x for x in labels if isinstance(x, str))),
                "feed_id": stream.get("feed"),
                "referrer": stream.get("referrer"),
                "user_agent": stream.get("user_agent"),
            })

    records = []
    skipped = {"nsfw": 0, "closed_or_replaced": 0, "blocklisted": 0, "no_country": 0, "no_stream": 0}
    seen = set()
    for channel in channels:
        channel_id = channel.get("id")
        if not channel_id or channel_id in seen:
            continue
        seen.add(channel_id)
        if channel.get("is_nsfw") is True:
            skipped["nsfw"] += 1
            continue
        if channel.get("closed") or channel.get("replaced_by"):
            skipped["closed_or_replaced"] += 1
            continue
        if channel_id in blocked:
            skipped["blocklisted"] += 1
            continue
        country_code = channel.get("country")
        country = countries.get(country_code, {})
        if not country_code:
            skipped["no_country"] += 1
            continue
        streams = streams_by_channel.get(channel_id, [])
        if not streams:
            skipped["no_stream"] += 1
            continue
        feed_rows = feeds_by_channel.get(channel_id, [])
        feed_languages = sorted({code for feed in feed_rows for code in (feed.get("languages") or []) if code})
        logo_rows = sorted(
            logos_by_channel.get(channel_id, []),
            key=lambda item: (not item.get("in_use", False), item.get("width") or 0),
            reverse=True,
        )
        records.append({
            "id": channel_id,
            "name": channel.get("name") or channel_id,
            "alternative_names": channel.get("alt_names") or [],
            "country": {
                "code": country_code,
                "name": country.get("name") or country_code,
                "flag": country.get("flag") or "🌐",
            },
            "languages": [{"code": code, "name": languages.get(code, code)} for code in feed_languages],
            "categories": channel.get("categories") or [],
            "website": channel.get("website"),
            "logo_url": logo_rows[0].get("url") if logo_rows else None,
            "streams": streams,
            "source": "iptv-org-api",
            "status": "candidate",
            "rights_review_status": "not_reviewed",
            "reliability_grade": "unrated",
            "last_stream_check_utc": None,
            "imported_at_utc": generated_at,
        })

    records.sort(key=lambda item: (item["country"]["name"].casefold(), item["name"].casefold(), item["id"]))
    return records, skipped


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="catalog/generated/candidates.json")
    parser.add_argument("--timeout", type=int, default=45)
    args = parser.parse_args()

    data = {}
    for key, url in ENDPOINTS.items():
        print(f"Fetching {key}...", flush=True)
        data[key] = fetch_json(url, timeout=args.timeout)

    generated_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    records, skipped = build_catalog(data, generated_at)
    output = {
        "schema_version": 1,
        "catalogue": "RS TV internet channel candidates",
        "generated_at_utc": generated_at,
        "source": "https://github.com/iptv-org/api",
        "source_notice": "Discovery metadata only. Public reachability is not proof of free-to-watch status, redistribution rights, or stream stability.",
        "playback_policy": "Only separately rights-reviewed and technically approved records may enter the playable catalogue.",
        "country_filter": {
            "all_value": "ALL",
            "all_behavior": "Combine all approved countries in one list; retain each channel country flag and name.",
            "country_behavior": "Filter by exact ISO 3166-1 alpha-2 country code."
        },
        "counts": {
            "channels_source": len(data["channels"]),
            "streams_source": len(data["streams"]),
            "candidate_channels_with_streams": len(records),
            "countries_source": len(data["countries"]),
            "skipped": skipped
        },
        "records": records
    }
    directory = os.path.dirname(args.output)
    if directory:
        os.makedirs(directory, exist_ok=True)
    temp_path = args.output + ".tmp"
    with open(temp_path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(output, handle, ensure_ascii=False, separators=(",", ":"))
        handle.write("\n")
    os.replace(temp_path, args.output)
    print(json.dumps(output["counts"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except (OSError, URLError, RuntimeError, ValueError) as exc:
        print(f"Importer failed: {exc}", file=sys.stderr)
        sys.exit(1)
