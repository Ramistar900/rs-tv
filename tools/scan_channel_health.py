#!/usr/bin/env python3
"""Rotate a bounded health scan across globally deduplicated playlist streams.

Each run downloads configured playlists, canonicalizes direct HTTPS URLs, and
checks a limited batch. Stream fingerprints, rather than raw URLs, are stored
in the persistent state file so query-string access tokens are not committed.
A passing first-segment check is not a full player/device test or a rights decision.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from pathlib import Path

from check_playlist_quality import SOURCES, canonical_url, fetch_playlist, is_page_only_platform
from probe_channel_streams import probe_stream

DEFAULT_BATCH_SIZE = 600
MAX_BATCH_SIZE = 1200
MAX_WORKERS = 24
RETRY_SLOTS_MAX = 100
SUCCESS_STATUSES = {"playback_segment_checked", "media_bytes_checked"}
MANIFEST_STATUSES = {"manifest_valid_no_segment"}
NON_RETRY_STATUSES = SUCCESS_STATUSES | MANIFEST_STATUSES | {"encrypted_stream_not_decoded", "protected_dash_manifest"}


def stream_fingerprint(canonical_key: str) -> str:
    return hashlib.sha256(canonical_key.encode("utf-8")).hexdigest()


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
        record["fingerprint"] = stream_fingerprint(record["canonical_key"])
    return records, source_reports


def _last_checked(entry: dict) -> datetime:
    raw = entry.get("last_checked_utc", "")
    try:
        parsed = datetime.fromisoformat(raw.replace("Z", "+00:00"))
        return parsed.astimezone(timezone.utc)
    except (ValueError, TypeError, AttributeError):
        return datetime.min.replace(tzinfo=timezone.utc)


def _state_classification(technical_status: str) -> str:
    if technical_status in SUCCESS_STATUSES:
        return "stream_checked"
    if technical_status in MANIFEST_STATUSES:
        return "manifest_valid"
    return "candidate"


def run_scan(
    playlist_fetcher=fetch_playlist,
    stream_probe=probe_stream,
    sources=None,
    limit: int = DEFAULT_BATCH_SIZE,
    batch_index: int | None = None,
    workers: int = MAX_WORKERS,
    state: dict | None = None,
):
    if limit < 1 or limit > MAX_BATCH_SIZE:
        raise ValueError(f"limit must be between 1 and {MAX_BATCH_SIZE}")
    if workers < 1 or workers > MAX_WORKERS:
        raise ValueError(f"workers must be between 1 and {MAX_WORKERS}")

    records, source_reports = collect_unique_streams(playlist_fetcher, sources)
    total_unique = len(records)
    prior_state = state if isinstance(state, dict) else {}
    streams_state = dict(prior_state.get("streams", {}))
    cursor = max(0, int(prior_state.get("next_cursor", 0) or 0))
    if total_unique:
        cursor %= total_unique

    now = datetime.now(timezone.utc).replace(microsecond=0)
    now_iso = now.isoformat().replace("+00:00", "Z")

    # Revisit previous transient failures daily, but reserve no more than one sixth
    # of the run for retries so new URLs continue to receive their first check.
    retry_due = []
    for record in records:
        previous = streams_state.get(record["fingerprint"])
        if not previous:
            continue
        technical_status = previous.get("technical_status", "")
        if technical_status in NON_RETRY_STATUSES:
            continue
        checked_at = _last_checked(previous)
        consecutive = int(previous.get("consecutive_failures", 0) or 0)
        cooldown = timedelta(days=7) if consecutive >= 3 else timedelta(hours=24)
        if now - checked_at >= cooldown:
            retry_due.append((checked_at, record))
    retry_due.sort(key=lambda item: item[0])
    retry_slots = min(RETRY_SLOTS_MAX, max(1, limit // 6)) if limit else 0
    retry_records = [record for _, record in retry_due[:retry_slots]]
    retry_fingerprints = {record["fingerprint"] for record in retry_records}

    selected = list(retry_records)
    if total_unique and len(selected) < limit:
        needed = limit - len(selected)
        walked = 0
        while walked < total_unique and len(selected) < limit:
            record = records[(cursor + walked) % total_unique]
            walked += 1
            if record["fingerprint"] in retry_fingerprints:
                continue
            selected.append(record)
        next_cursor = (cursor + walked) % total_unique
    else:
        next_cursor = cursor if total_unique else 0

    def check(record):
        result = stream_probe(record["url"])
        return record, result

    with ThreadPoolExecutor(max_workers=workers) as executor:
        checked_pairs = list(executor.map(check, selected))

    checked = []
    status_totals = {}
    for record, result in checked_pairs:
        technical_status = result.get("status", "unknown")
        status_totals[technical_status] = status_totals.get(technical_status, 0) + 1
        previous = streams_state.get(record["fingerprint"], {})
        prior_failures = int(previous.get("consecutive_failures", 0) or 0)
        if technical_status in SUCCESS_STATUSES or technical_status in MANIFEST_STATUSES:
            consecutive_failures = 0
        elif technical_status in NON_RETRY_STATUSES:
            consecutive_failures = prior_failures
        else:
            consecutive_failures = prior_failures + 1

        streams_state[record["fingerprint"]] = {
            "channel": record["name"][:120],
            "source_ids": record["source_ids"],
            "classification": _state_classification(technical_status),
            "technical_status": technical_status,
            "last_checked_utc": now_iso,
            "check_count": int(previous.get("check_count", 0) or 0) + 1,
            "consecutive_failures": consecutive_failures,
            "http_status": result.get("http_status"),
            "response_kind": result.get("response_kind"),
            "rights_review_status": "not_reviewed",
        }
        checked.append({
            "channel": record["name"][:120],
            "source_ids": record["source_ids"],
            **result,
        })

    state_summary = {
        "stream_checked": 0,
        "manifest_valid": 0,
        "candidate": 0,
        "rights_reviewed": 0,
        "approved": 0,
        "rejected": 0,
    }
    for record in records:
        entry = streams_state.get(record["fingerprint"], {})
        classification = entry.get("classification", "candidate")
        state_summary[classification] = state_summary.get(classification, 0) + 1
        if entry.get("rights_review_status") == "reviewed":
            state_summary["rights_reviewed"] += 1
        if classification == "approved":
            state_summary["approved"] += 1
        if classification == "rejected":
            state_summary["rejected"] += 1

    updated_state = {
        "schema_version": 1,
        "last_updated_utc": now_iso,
        "next_cursor": next_cursor,
        "streams": streams_state,
    }
    selected_batch = cursor // limit if total_unique else 0
    batch_count = max(1, math.ceil(total_unique / limit))
    return {
        "schema_version": 2,
        "generated_at_utc": now_iso,
        "purpose": "Rotating bounded health scan of globally deduplicated HTTPS stream URLs with persistent fingerprinted status",
        "total_unique_https_streams_discovered": total_unique,
        "batch_size": limit,
        "batch_index": selected_batch,
        "batch_count": batch_count,
        "retry_slots_used": len(retry_records),
        "streams_checked_this_batch": len(checked),
        "workers": workers,
        "status_totals": status_totals,
        "catalogue_status_totals": state_summary,
        "next_cursor": next_cursor,
        "hls_first_segments_checked": status_totals.get("playback_segment_checked", 0),
        "direct_media_bytes_checked": status_totals.get("media_bytes_checked", 0),
        "full_device_playback_tested": False,
        "rights_review_performed": False,
        "approval_effect": "none; endpoint or first-segment reachability does not prove full playback or broadcast rights",
        "sources": source_reports,
        "results": checked,
        "updated_state": updated_state,
    }


def main():
    parser = argparse.ArgumentParser(description="Run one rotating batch of RS TV stream checks.")
    parser.add_argument("--limit", type=int, default=DEFAULT_BATCH_SIZE)
    parser.add_argument("--batch-index", type=int, default=None, help="Compatibility override; the persistent cursor is used by default.")
    parser.add_argument("--workers", type=int, default=MAX_WORKERS)
    parser.add_argument("--state", default="catalog/generated/stream-health.json")
    parser.add_argument("--output", default="channel-health-report.json")
    args = parser.parse_args()

    state_path = Path(args.state)
    if state_path.exists():
        try:
            previous_state = json.loads(state_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise SystemExit(f"Cannot read stream health state: {exc}")
    else:
        previous_state = {}

    report = run_scan(limit=args.limit, batch_index=args.batch_index, workers=args.workers, state=previous_state)
    updated_state = report.pop("updated_state")
    state_path.parent.mkdir(parents=True, exist_ok=True)
    state_path.write_text(json.dumps(updated_state, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
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
