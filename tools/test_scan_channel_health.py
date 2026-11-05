import unittest
from unittest.mock import patch

from scan_channel_health import collect_unique_streams, run_scan, stream_fingerprint
from check_playlist_quality import canonical_url


PLAYLIST_ONE = """#EXTM3U
#EXTINF:-1,Alpha
https://a.example/live.m3u8
#EXTINF:-1,YouTube page
https://youtube.com/watch?v=video
#EXTINF:-1,Beta
https://b.example/live.m3u8
"""
PLAYLIST_TWO = """#EXTM3U
#EXTINF:-1,Alpha duplicate
https://a.example/live.m3u8#player
#EXTINF:-1,Gamma
https://c.example/live.m3u8
"""


class ChannelHealthScanTests(unittest.TestCase):
    def test_global_dedup_and_platform_page_exclusion(self):
        playlists = {
            "one": PLAYLIST_ONE,
            "two": PLAYLIST_TWO,
        }
        records, reports = collect_unique_streams(
            lambda url: playlists[url],
            [("one", "one"), ("two", "two")],
        )
        self.assertEqual(len(records), 3)
        self.assertEqual(reports[0]["eligible_https_entries"], 2)
        alpha = next(record for record in records if record["name"] == "Alpha")
        self.assertEqual(alpha["source_ids"], ["one", "two"])

    def test_batch_is_bounded_and_deterministic(self):
        playlists = {"one": PLAYLIST_ONE, "two": PLAYLIST_TWO}
        calls = []
        def fake_probe(url):
            calls.append(url)
            return {"status": "playback_segment_checked"}
        report = run_scan(
            lambda url: playlists[url],
            fake_probe,
            [("one", "one"), ("two", "two")],
            limit=2,
            batch_index=0,
            workers=2,
        )
        self.assertEqual(report["streams_checked_this_batch"], 2)
        self.assertEqual(len(calls), 2)
        self.assertEqual(report["total_unique_https_streams_discovered"], 3)
        self.assertEqual(report["batch_count"], 2)
        self.assertEqual(report["hls_first_segments_checked"], 2)

    def test_report_never_claims_full_playback_or_rights(self):
        with patch("scan_channel_health.SOURCES", []):
            report = run_scan(sources=[], limit=10, batch_index=0)
        self.assertFalse(report["full_device_playback_tested"])
        self.assertFalse(report["rights_review_performed"])
        self.assertEqual(report["streams_checked_this_batch"], 0)


    def test_persists_fingerprinted_status_without_raw_stream_urls(self):
        report = run_scan(
            lambda url: PLAYLIST_ONE,
            lambda url: {"status": "playback_segment_checked", "http_status": 206},
            [("one", "one")],
            limit=1,
            batch_index=0,
            workers=1,
        )
        state = report["updated_state"]
        key = stream_fingerprint(canonical_url("https://a.example/live.m3u8"))
        self.assertIn(key, state["streams"])
        self.assertEqual(state["streams"][key]["classification"], "stream_checked")
        self.assertEqual(state["streams"][key]["rights_review_status"], "not_reviewed")
        self.assertNotIn("url", state["streams"][key])
        self.assertNotIn("canonical_key", state["streams"][key])

    def test_retries_old_transient_failure_without_blocking_new_urls(self):
        key = stream_fingerprint(canonical_url("https://a.example/live.m3u8"))
        state = {
            "next_cursor": 0,
            "streams": {
                key: {
                    "technical_status": "timeout",
                    "classification": "candidate",
                    "last_checked_utc": "2020-01-01T00:00:00Z",
                    "consecutive_failures": 1,
                    "check_count": 1,
                }
            },
        }
        calls = []
        def fake_probe(url):
            calls.append(url)
            return {"status": "playback_segment_checked"}
        report = run_scan(
            lambda url: PLAYLIST_ONE,
            fake_probe,
            [("one", "one")],
            limit=2,
            batch_index=0,
            workers=2,
            state=state,
        )
        self.assertEqual(report["retry_slots_used"], 1)
        self.assertIn("https://a.example/live.m3u8", calls)
        self.assertEqual(report["streams_checked_this_batch"], 2)
        self.assertEqual(report["catalogue_status_totals"]["stream_checked"], 2)


    def test_invalid_batch_size_is_rejected(self):
        with self.assertRaises(ValueError):
            run_scan(sources=[], limit=1201)


if __name__ == "__main__":
    unittest.main()
