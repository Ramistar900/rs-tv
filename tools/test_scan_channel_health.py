import unittest
from unittest.mock import patch

from scan_channel_health import collect_unique_streams, run_scan


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

    def test_invalid_batch_size_is_rejected(self):
        with self.assertRaises(ValueError):
            run_scan(sources=[], limit=1201)


if __name__ == "__main__":
    unittest.main()
