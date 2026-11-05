#!/usr/bin/env python3
"""Bounded, read-only HTTP probes of a few public channel endpoints per playlist.

A passing probe means only that a URL returned a plausible media/manifest response.
It is not a playback test, availability guarantee, or rights determination.
"""
from __future__ import annotations

import argparse
import json
import socket
import sys
from datetime import datetime, timezone
from ipaddress import ip_address
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener

from check_playlist_quality import SOURCES, fetch_playlist, inspect_playlist, canonical_url, is_page_only_platform

MAX_SAMPLES_PER_SOURCE = 6
MAX_RESPONSE_BYTES = 64 * 1024
TIMEOUT_SECONDS = 8
USER_AGENT = "RS-TV-Bounded-Stream-Probe/1.0 (+https://github.com/Ramistar900/rs-tv)"
MEDIA_TYPES = (
    "application/vnd.apple.mpegurl", "application/x-mpegurl", "application/dash+xml",
    "video/", "audio/", "application/octet-stream",
)


def public_host(host: str) -> bool:
    try:
        addresses = {item[4][0] for item in socket.getaddrinfo(host, 443, type=socket.SOCK_STREAM)}
        return bool(addresses) and all(ip_address(value.split("%", 1)[0]).is_global for value in addresses)
    except (OSError, ValueError):
        return False


class SameHostHttpsRedirectHandler(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        old, new = urlsplit(req.full_url), urlsplit(newurl)
        if new.scheme.lower() != "https" or (new.hostname or "").lower() != (old.hostname or "").lower():
            raise HTTPError(newurl, code, "Redirect rejected: HTTPS same-host redirects only", headers, fp)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def probe_stream(url: str) -> dict:
    parsed = urlsplit(url)
    host = parsed.hostname or ""
    if parsed.scheme.lower() != "https" or not host or parsed.username or parsed.password:
        return {"status": "unsafe_url"}
    if not public_host(host):
        return {"status": "host_not_public_or_unresolvable"}
    request = Request(url, headers={
        "User-Agent": USER_AGENT,
        "Accept": "application/vnd.apple.mpegurl,application/dash+xml,video/*,audio/*,*/*;q=0.5",
        "Range": f"bytes=0-{MAX_RESPONSE_BYTES - 1}",
    })
    opener = build_opener(SameHostHttpsRedirectHandler())
    try:
        with opener.open(request, timeout=TIMEOUT_SECONDS) as response:
            status = response.status
            content_type = response.headers.get("Content-Type", "").split(";", 1)[0].strip().lower()
            sample = response.read(MAX_RESPONSE_BYTES)
        if status not in (200, 206):
            return {"status": "http_unexpected", "http_status": status, "content_type": content_type}
        looks_hls = sample.lstrip(b"\xef\xbb\xbf \t\r\n").startswith(b"#EXTM3U")
        looks_dash = b"<MPD" in sample[:4096] or b":MPD" in sample[:4096]
        looks_media = any(content_type.startswith(prefix) for prefix in MEDIA_TYPES)
        if looks_hls or looks_dash:
            kind = "hls_manifest" if looks_hls else "dash_manifest"
        elif looks_media and not content_type.startswith("text/html"):
            kind = "media_response"
        else:
            return {"status": "unexpected_content", "http_status": status, "content_type": content_type}
        return {"status": "responded", "http_status": status, "content_type": content_type, "response_kind": kind}
    except HTTPError as exc:
        code = exc.code
        category = "forbidden_or_geo_restricted" if code in (401, 403, 451) else "not_found" if code == 404 else "upstream_error" if code >= 500 else "http_error"
        return {"status": category, "http_status": code}
    except (TimeoutError, URLError, OSError) as exc:
        reason = getattr(exc, "reason", exc)
        if isinstance(reason, (TimeoutError, socket.timeout)):
            return {"status": "timeout"}
        return {"status": "network_error", "detail": str(reason)[:120]}


def run_audit(playlist_fetcher=fetch_playlist, stream_probe=probe_stream) -> dict:
    source_results = []
    for source_id, source_url in SOURCES:
        try:
            playlist = playlist_fetcher(source_url)
            stats = inspect_playlist(playlist)
            lines = [line.strip() for line in playlist.lstrip("\ufeff").splitlines() if line.strip()]
            pairs, pending = [], None
            for line in lines[1:]:
                if line.upper().startswith("#EXTINF:"):
                    pending = line.rsplit(",", 1)[-1].strip() if "," in line else ""
                elif line.startswith("#"):
                    continue
                elif pending is not None:
                    pairs.append((pending, line))
                    pending = None
            eligible, seen = [], set()
            for name, url in pairs:
                key = canonical_url(url)
                if not key or key in seen or is_page_only_platform(url):
                    continue
                seen.add(key)
                eligible.append((name, url))
            sample_count = min(MAX_SAMPLES_PER_SOURCE, len(eligible))
            if sample_count == 1:
                selected = [eligible[0]]
            elif sample_count > 1:
                indexes = [round(i * (len(eligible) - 1) / (sample_count - 1)) for i in range(sample_count)]
                selected = [eligible[index] for index in indexes]
            else:
                selected = []
            samples = []
            for name, url in selected:
                result = stream_probe(url)
                samples.append({"channel": name[:120], **result})
            source_results.append({
                "source_id": source_id,
                "playlist_entries": stats["entries"],
                "samples_attempted": len(samples),
                "samples": samples,
            })
        except Exception as exc:
            source_results.append({"source_id": source_id, "source_error": str(exc)[:180], "samples_attempted": 0, "samples": []})
    totals = {}
    for source in source_results:
        for sample in source["samples"]:
            status = sample["status"]
            totals[status] = totals.get(status, 0) + 1
    return {
        "schema_version": 1,
        "generated_at_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "purpose": "Bounded HTTP response probe of at most three channel URLs per public playlist",
        "max_samples_per_source": MAX_SAMPLES_PER_SOURCE,
        "max_response_bytes_per_probe": MAX_RESPONSE_BYTES,
        "channel_playback_tested": False,
        "rights_review_performed": False,
        "approval_effect": "none; HTTP response does not prove playback or redistribution rights",
        "samples_attempted": sum(x["samples_attempted"] for x in source_results),
        "status_totals": totals,
        "sources": source_results,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="stream-probe-report.json")
    args = parser.parse_args()
    report = run_audit()
    with open(args.output, "w", encoding="utf-8") as handle:
        json.dump(report, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print(json.dumps({k: v for k, v in report.items() if k != "sources"}, ensure_ascii=False))
    for source in report["sources"]:
        print(f'{source["source_id"]}: {source["samples_attempted"]} samples; ' +
              ", ".join(f'{s["channel"]}: {s["status"]}' for s in source["samples"]))
    if report["samples_attempted"] == 0:
        raise SystemExit("No channel URLs could be sampled")


if __name__ == "__main__":
    main()
