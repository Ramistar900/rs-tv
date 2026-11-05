#!/usr/bin/env python3
"""Bounded HTTPS probes for public playlist stream endpoints.

HLS checks fetch the first media segment (not just the playlist manifest).
Direct media URLs are checked for a bounded byte response. DASH manifests are
validated structurally; protected or dynamic DASH streams are never bypassed.
A passing result is not a full player/device test and is not a rights decision.
"""
from __future__ import annotations

import argparse
import json
import re
import socket
from datetime import datetime, timezone
from ipaddress import ip_address
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin, urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener
import xml.etree.ElementTree as ET

from check_playlist_quality import SOURCES, canonical_url, fetch_playlist, inspect_playlist, is_page_only_platform

MAX_SAMPLES_PER_SOURCE = 6
MAX_RESPONSE_BYTES = 64 * 1024
SEGMENT_SAMPLE_BYTES = 8 * 1024
TIMEOUT_SECONDS = 7
USER_AGENT = "RS-TV-Bounded-Stream-Probe/2.0 (+https://github.com/Ramistar900/rs-tv)"
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


def fetch_resource(url: str, max_bytes: int = MAX_RESPONSE_BYTES) -> dict:
    parsed = urlsplit(url)
    host = parsed.hostname or ""
    if parsed.scheme.lower() != "https" or not host or parsed.username or parsed.password:
        raise ValueError("unsafe_url")
    if not public_host(host):
        raise ValueError("host_not_public_or_unresolvable")
    request = Request(url, headers={
        "User-Agent": USER_AGENT,
        "Accept": "application/vnd.apple.mpegurl,application/dash+xml,video/*,audio/*,*/*;q=0.5",
        "Range": f"bytes=0-{max_bytes - 1}",
    })
    opener = build_opener(SameHostHttpsRedirectHandler())
    with opener.open(request, timeout=TIMEOUT_SECONDS) as response:
        status = response.status
        content_type = response.headers.get("Content-Type", "").split(";", 1)[0].strip().lower()
        sample = response.read(max_bytes)
        final_url = response.geturl()
    if status not in (200, 206):
        raise HTTPError(url, status, "Unexpected HTTP status", {}, None)
    return {
        "http_status": status,
        "content_type": content_type,
        "sample": sample,
        "final_url": final_url,
    }


def _http_failure(exc: Exception, prefix: str = "") -> dict:
    if isinstance(exc, ValueError):
        status = str(exc)
    elif isinstance(exc, HTTPError):
        code = exc.code
        status = "forbidden_or_geo_restricted" if code in (401, 403, 451) else \
            "not_found" if code == 404 else "upstream_error" if code >= 500 else "http_error"
        return {"status": f"{prefix}{status}", "http_status": code}
    elif isinstance(exc, (TimeoutError, URLError, OSError)):
        reason = getattr(exc, "reason", exc)
        status = "timeout" if isinstance(reason, (TimeoutError, socket.timeout)) else "network_error"
    else:
        status = "probe_error"
    return {"status": f"{prefix}{status}", "detail": str(exc)[:120]}


def _hls_segment_url(manifest_url: str, manifest: str) -> tuple[str | None, bool]:
    lines = [line.strip() for line in manifest.lstrip("\ufeff").splitlines() if line.strip()]
    if not lines or not lines[0].startswith("#EXTM3U"):
        return None, False

    for line in lines:
        if line.startswith("#EXT-X-KEY:"):
            method = re.search(r"METHOD=([^,]+)", line)
            if method and method.group(1).strip().upper() != "NONE":
                return None, True

    # A master playlist points to variant media playlists after EXT-X-STREAM-INF.
    for index, line in enumerate(lines[:-1]):
        if line.startswith("#EXT-X-STREAM-INF:"):
            for candidate in lines[index + 1:]:
                if not candidate.startswith("#"):
                    return urljoin(manifest_url, candidate), False

    # Test the first media segment. For fMP4, fetching only EXT-X-MAP would
    # verify initialization bytes but not an actual media segment.
    for line in lines:
        if not line.startswith("#"):
            return urljoin(manifest_url, line), False
    return None, False


def _probe_hls(url: str, first: dict) -> dict:
    manifest_url = first["final_url"]
    sample = first["sample"]
    if not sample.lstrip(b"\xef\xbb\xbf \t\r\n").startswith(b"#EXTM3U"):
        return {"status": "unexpected_hls_content", "http_status": first["http_status"]}

    manifest = sample.decode("utf-8", errors="replace")
    segment_url, encrypted = _hls_segment_url(manifest_url, manifest)
    if encrypted:
        return {"status": "encrypted_stream_not_decoded", "response_kind": "hls_manifest"}
    if segment_url is None:
        return {"status": "manifest_valid_no_segment", "response_kind": "hls_manifest"}

    # A master URL can resolve to a variant playlist. Read one additional manifest only.
    try:
        segment_path = urlsplit(segment_url).path.lower()
        if segment_path.endswith((".m3u8", ".m3u")):
            variant = fetch_resource(segment_url, MAX_RESPONSE_BYTES)
            if variant["sample"].lstrip(b"\xef\xbb\xbf \t\r\n").startswith(b"#EXTM3U"):
                next_url, variant_encrypted = _hls_segment_url(variant["final_url"],
                    variant["sample"].decode("utf-8", errors="replace"))
                if variant_encrypted:
                    return {"status": "encrypted_stream_not_decoded", "response_kind": "hls_manifest"}
                if next_url is None:
                    return {"status": "manifest_valid_no_segment", "response_kind": "hls_manifest"}
                segment_url = next_url

        segment = fetch_resource(segment_url, SEGMENT_SAMPLE_BYTES)
        if not segment["sample"] or segment["content_type"].startswith("text/html"):
            return {"status": "segment_empty_or_html", "http_status": segment["http_status"]}
        return {
            "status": "playback_segment_checked",
            "http_status": segment["http_status"],
            "response_kind": "hls_manifest_and_segment",
            "segment_bytes_received": len(segment["sample"]),
            "content_type": segment["content_type"],
        }
    except Exception as exc:
        return _http_failure(exc, prefix="segment_")


def _probe_dash(first: dict) -> dict:
    sample = first["sample"]
    try:
        root = ET.fromstring(sample)
    except ET.ParseError:
        return {"status": "invalid_dash_manifest", "http_status": first["http_status"]}
    tag_names = {node.tag.rsplit("}", 1)[-1].lower() for node in root.iter()}
    if "contentprotection" in tag_names:
        return {"status": "protected_dash_manifest", "response_kind": "dash_manifest"}
    return {
        "status": "manifest_valid_no_segment",
        "http_status": first["http_status"],
        "response_kind": "dash_manifest",
    }


def probe_stream(url: str) -> dict:
    try:
        first = fetch_resource(url)
        sample = first["sample"]
        content_type = first["content_type"]
        if sample.lstrip(b"\xef\xbb\xbf \t\r\n").startswith(b"#EXTM3U") or \
                "mpegurl" in content_type:
            return _probe_hls(url, first)
        if b"<MPD" in sample[:4096] or "application/dash+xml" in content_type:
            return _probe_dash(first)
        looks_media = any(content_type.startswith(prefix) for prefix in MEDIA_TYPES)
        if looks_media and not content_type.startswith("text/html") and sample:
            return {
                "status": "media_bytes_checked",
                "http_status": first["http_status"],
                "response_kind": "media_response",
                "media_bytes_received": len(sample),
                "content_type": content_type,
            }
        return {
            "status": "unexpected_content",
            "http_status": first["http_status"],
            "content_type": content_type,
        }
    except Exception as exc:
        return _http_failure(exc)



def run_audit(playlist_fetcher=fetch_playlist, stream_probe=probe_stream) -> dict:
    """Compatibility source sampler retained alongside the rotating full scan."""
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
            indexes = [round(i * (len(eligible) - 1) / (sample_count - 1))
                       for i in range(sample_count)] if sample_count > 1 else ([0] if sample_count else [])
            samples = []
            for index in indexes:
                name, url = eligible[index]
                samples.append({"channel": name[:120], **stream_probe(url)})
            source_results.append({
                "source_id": source_id,
                "playlist_entries": stats["entries"],
                "samples_attempted": len(samples),
                "samples": samples,
            })
        except Exception as exc:
            source_results.append({
                "source_id": source_id,
                "source_error": str(exc)[:180],
                "samples_attempted": 0,
                "samples": [],
            })
    totals = {}
    for source in source_results:
        for sample in source["samples"]:
            status = sample["status"]
            totals[status] = totals.get(status, 0) + 1
    return {
        "schema_version": 2,
        "generated_at_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "purpose": "Small evenly distributed source sample; see channel health scan for rotating batch checks",
        "max_samples_per_source": MAX_SAMPLES_PER_SOURCE,
        "max_response_bytes_per_probe": MAX_RESPONSE_BYTES,
        "channel_playback_tested": False,
        "rights_review_performed": False,
        "approval_effect": "none; endpoint or first-segment reachability does not prove full playback or redistribution rights",
        "samples_attempted": sum(item["samples_attempted"] for item in source_results),
        "status_totals": totals,
        "sources": source_results,
    }


def main():
    parser = argparse.ArgumentParser(description="Probe a stream URL or write the small source-sampling report.")
    parser.add_argument("url", nargs="?")
    parser.add_argument("--output", default=None)
    args = parser.parse_args()
    if args.url:
        print(json.dumps(probe_stream(args.url), ensure_ascii=False))
        return
    report = run_audit()
    if args.output:
        with open(args.output, "w", encoding="utf-8") as handle:
            json.dump(report, handle, ensure_ascii=False, indent=2)
            handle.write("\n")
    print(json.dumps({key: value for key, value in report.items() if key != "sources"}, ensure_ascii=False))
    for source in report["sources"]:
        print(f'{source["source_id"]}: {source["samples_attempted"]} samples; ' +
              ", ".join(f'{sample["channel"]}: {sample["status"]}' for sample in source["samples"]))
    if report["samples_attempted"] == 0:
        raise SystemExit("No channel URLs could be sampled")


if __name__ == "__main__":
    main()
