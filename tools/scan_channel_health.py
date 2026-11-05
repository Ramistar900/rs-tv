#!/usr/bin/env python3
"""Rotate a bounded health scan across globally deduplicated playlist streams.

Each run downloads configured playlists, canonicalizes direct HTTPS URLs, and
tests one deterministic batch. A daily batch rotation prevents a single long
run from hammering thousands of third-party endpoints. Raw URLs and query
tokens are deliberately omitted from the report.
"""
from __future__ import annotations

import argparse
import json
import math
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from pathlib import Path

from check_playlist_quality import SOURCES, canonical_url, fetch_playlist, is_page_only_platform
from probe_channel_streams import probe_stream

DEFAULT_BATCH_SIZE = 600
MAX_BATCH_SIZE = 1200
MAX_WORKERS = 24


def playlist_entries(content: str):
    lines = [line.strip() for line in content.lstrip("\ufeff").splitlines() if line.strip()]
    if not lines or not lines[0].upper().startswith("#EXTM3U"):
        raise ValueError("Response is not an M3U playlist")
    pending = None
    for line in lines[1:]:
        if line.upper().startswith("#EXTINF:"):
            pending = line.rsplit(",", 1)[-1].strip() if "," in line else ""
            continue
        if line.startswith("#"):
            continue
        if pending is not None:
            yield pending, line
            pending = None


def collect_unique_streams(playlist_fetcher=fetch_playlist, sources=None):
    source_list = SOURCES if sources is None else sources
    unique = {}
    source_reports = []
    for source_id, source_url in source_list:
        try:
            playlist = playlist_fetcher(source_url)
            entries = list(playlist_entries(playlist))
            eligible = 0
            for name, url in entries:
                key = canonical_url(url)
                if key is None or is_page_only_platform(url):
                    continue
                eligible += 1
                record = unique.get(key)
                if record is None:
                    unique[key] = {
                        "canonical_key": key,
                        "url": url,
                        "name": name or "قناة بدون اسم",
                        "source_ids": {source_id},
                    }
                else:
                    record["source_ids"].add(source_id)
                    if record["name"] == "قناة بدون اسم" and name:
                        record["name"] = name
            source_reports.append({
                "source_id": source_id,
                "status": "ok",
                "entries": len(entries),
                "eligible_https_entries": eligible,
            })
        except Exception as exc:
            source_reports.append({
                "source_id": source_id,
                "status": "error",
                "error": str(exc)[:180],
            })
    records = [unique[key] for key in sorted(unique)]
    for record in records:
        record["source_ids"] = sorted(record["source_ids"])
    return records, source_reports


def run_scan(
    playlist_fetcher=fetch_playlist,
    stream_probe=probe_stream,
    sources=None,
    limit: int = DEFAULT_BATCH_SIZE,
    batch_index: int | None = None,
    workers: int = MAX_WORKERS,
):
    if limit < 1 or limit > MAX_BATCH_SIZE:
        raise ValueError(f"limit must be between 1 and {MAX_BATCH_SIZE}")
    if workers < 1 or workers > MAX_WORKERS:
        raise ValueError(f"workers must be between 1 and {MAX_WORKERS}")

    records, source_reports = collect_unique_streams(playlist_fetcher, sources)
    total_unique = len(records)
    batch_count = max(1, math.ceil(total_unique / limit))
    if batch_index is None:
        day_number = datetime.now(timezone.utc).date().toordinal()
        selected_batch = day_number % batch_count
    else:
        selected_batch = batch_index % batch_count

    start = selected_batch * limit
    selected = records[start:start + limit]

    def check(record):
        result = stream_probe(record["url"])
        return {
            "channel": record["name"][:120],
            "source_ids": record["source_ids"],
            **result,
        }

    with ThreadPoolExecutor(max_workers=workers) as executor:
        checked = list(executor.map(check, selected))

    status_totals = {}
    for item in checked:
        status = item.get("status", "unknown")
        status_totals[status] = status_totals.get(status, 0) + 1

    now = datetime.now(timezone.utc).replace(microsecond=0)
    segment_checked = status_totals.get("playback_segment_checked", 0)
    media_checked = status_totals.get("media_bytes_checked", 0)
    return {
        "schema_version": 1,
        "generated_at_utc": now.isoformat().replace("+00:00", "Z"),
        "purpose": "Rotating bounded health scan of globally deduplicated HTTPS stream URLs",
        "total_unique_https_streams_discovered": total_unique,
        "batch_size": limit,
        "batch_index": selected_batch,
        "batch_count": batch_count,
        "streams_checked_this_batch": len(checked),
        "workers": workers,
        "status_totals": status_totals,
        "hls_first_segments_checked": segment_checked,
        "direct_media_bytes_checked": media_checked,
        "full_device_playback_tested": False,
        "rights_review_performed": False,
        "approval_effect": "none; endpoint or first-segment reachability does not prove full playback or broadcast rights",
        "sources": source_reports,
        "results": checked,
    }


def main():
    parser = argparse.ArgumentParser(description="Run one rotating batch of RS TV stream checks.")
    parser.add_argument("--limit", type=int, default=DEFAULT_BATCH_SIZE)
    parser.add_argument("--batch-index", type=int, default=None)
    parser.add_argument("--workers", type=int, default=MAX_WORKERS)
    parser.add_argument("--output", default="channel-health-report.json")
    args = parser.parse_args()

    report = run_scan(limit=args.limit, batch_index=args.batch_index, workers=args.workers)
    Path(args.output).write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    summary = {key: value for key, value in report.items() if key not in {"sources", "results"}}
    print(json.dumps(summary, ensure_ascii=False))
    for source in report["sources"]:
        if source["status"] == "ok":
            print(f'{source["source_id"]}: {source["entries"]} entries; {source["eligible_https_entries"]} eligible HTTPS')
        else:
            print(f'{source["source_id"]}: ERROR {source["error"]}')
    for status, count in sorted(report["status_totals"].items()):
        print(f"{status}: {count}")
    if not report["sources"] or not any(source["status"] == "ok" for source in report["sources"]):
        raise SystemExit("No playlist source could be scanned")


if __name__ == "__main__":
    main()
