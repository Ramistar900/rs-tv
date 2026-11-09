import unittest

from unittest.mock import patch

from check_playlist_quality import SOURCES, audit, canonical_url, inspect_playlist, is_page_only_platform


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
    def test_audit_reports_global_unique_streams_and_cross_source_duplicates(self):
        playlists = {
            "https://list.example/one": "#EXTM3U\n#EXTINF:-1,One\nhttps://news.example/live.m3u8\n#EXTINF:-1,Page\nhttps://youtube.com/watch?v=abc\n",
            "https://list.example/two": "#EXTM3U\n#EXTINF:-1,Duplicate\nhttps://news.example/live.m3u8\n#EXTINF:-1,Two\nhttps://sports.example/live.m3u8\n",
        }
        report = audit(lambda url: playlists[url])
        self.assertEqual(report["sources_checked"], 2)
        self.assertEqual(report["unique_eligible_stream_urls_across_sources"], 2)
        self.assertEqual(report["duplicate_stream_urls_across_sources"], 1)


    def test_audit_registry_includes_specialized_sports_and_film_sources(self):
        ids = {source_id for source_id, _ in SOURCES}
        self.assertTrue({"doubo-sports", "iptv-org-sports", "iptv-org-movies", "iptv-org-series"} <= ids)
        self.assertTrue({"iptv-org-animation", "iptv-org-auto", "iptv-org-business", "iptv-org-classic", "iptv-org-comedy", "iptv-org-cooking", "iptv-org-culture", "iptv-org-documentary", "iptv-org-education", "iptv-org-entertainment", "iptv-org-family", "iptv-org-general", "iptv-org-kids", "iptv-org-legislative", "iptv-org-lifestyle", "iptv-org-music", "iptv-org-news", "iptv-org-outdoor", "iptv-org-public", "iptv-org-relax", "iptv-org-religious", "iptv-org-science", "iptv-org-travel", "iptv-org-weather"} <= ids)

    def test_audit_includes_all_128_doubo_country_playlists(self):
        country_sources = [
            (source_id, url) for source_id, url in SOURCES
            if source_id.startswith("doubo-country-")
        ]
        self.assertEqual(len(country_sources), 128)
        self.assertEqual(len({source_id for source_id, _ in country_sources}), 128)
        self.assertEqual(len({url for _, url in country_sources}), 128)
        urls = dict(country_sources)
        self.assertIn("doubo-country-sy", urls)
        self.assertIn("doubo-country-lb", urls)
        self.assertIn("doubo-country-iq", urls)
        self.assertIn("doubo-country-us", urls)
        self.assertIn("doubo-country-ca", urls)
        self.assertTrue(urls["doubo-country-gb"].endswith("/countries/uk.m3u"))
        self.assertTrue(all(url.startswith("https://cdn.jsdelivr.net/") for _, url in country_sources))

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
