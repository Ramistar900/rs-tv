import unittest

from unittest.mock import patch

from check_playlist_quality import audit, canonical_url, inspect_playlist, is_page_only_platform


class PlaylistQualityTests(unittest.TestCase):
    def test_canonical_url_removes_fragment_and_default_port_but_keeps_query(self):
        self.assertEqual(
            canonical_url("HTTPS://Example.org:443/live.m3u8?token=one#player"),
            "https://example.org/live.m3u8?token=one",
        )
        self.assertNotEqual(
            canonical_url("https://example.org/live?token=one"),
            canonical_url("https://example.org/live?token=two"),
        )

    def test_rejects_non_https_and_credentials(self):
        self.assertIsNone(canonical_url("http://example.org/live"))
        self.assertIsNone(canonical_url("https://user:pass@example.org/live"))

    def test_eligible_unique_urls_exclude_platform_pages(self):
        result = inspect_playlist("""#EXTM3U\n#EXTINF:-1,Direct\nhttps://news.example/live.m3u8\n#EXTINF:-1,YouTube\nhttps://youtube.com/watch?v=abc\n""")
        self.assertEqual(result["eligible_unique_https_urls"], 1)

    @patch("check_playlist_quality.SOURCES", [("one", "https://list.example/one"), ("two", "https://list.example/two")])
    def test_audit_reports_global_unique_streams_and_cross_source_duplicates(self, _):
        playlists = {
            "https://list.example/one": "#EXTM3U\n#EXTINF:-1,One\nhttps://news.example/live.m3u8\n#EXTINF:-1,Page\nhttps://youtube.com/watch?v=abc\n",
            "https://list.example/two": "#EXTM3U\n#EXTINF:-1,Duplicate\nhttps://news.example/live.m3u8\n#EXTINF:-1,Two\nhttps://sports.example/live.m3u8\n",
        }
        report = audit(lambda url: playlists[url])
        self.assertEqual(report["sources_checked"], 2)
        self.assertEqual(report["unique_eligible_stream_urls_across_sources"], 2)
        self.assertEqual(report["duplicate_stream_urls_across_sources"], 1)

    def test_platform_pages_are_not_direct_streams(self):
        self.assertTrue(is_page_only_platform("https://www.youtube.com/watch?v=abc"))
        self.assertFalse(is_page_only_platform("https://news.example.org/live.m3u8"))

    def test_playlist_metrics_report_duplicates_unsafe_and_incomplete_rows(self):
        playlist = """#EXTM3U
#EXTINF:-1 tvg-id="one" group-title="News",Channel One
https://example.org/live.m3u8#first
#EXTINF:-1,Channel One duplicate
https://example.org/live.m3u8
#EXTINF:-1,YouTube page
https://youtube.com/watch?v=abc
#EXTINF:-1,Unsafe HTTP
http://example.org/live
#EXTINF:-1,Missing stream
"""
        result = inspect_playlist(playlist)
        self.assertEqual(result["entries"], 4)
        self.assertEqual(result["duplicate_urls"], 1)
        self.assertEqual(result["unsafe_or_non_https_urls"], 1)
        self.assertEqual(result["platform_page_links"], 1)
        self.assertEqual(result["incomplete_extinf_entries"], 1)


if __name__ == "__main__":
    unittest.main()
