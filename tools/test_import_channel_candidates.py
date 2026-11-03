import unittest

from import_channel_candidates import build_catalog, normalize_quality


class ImporterTests(unittest.TestCase):
    def setUp(self):
        self.data = {
            "channels": [
                {"id": "A.tr", "name": "A", "country": "TR", "categories": ["news"]},
                {"id": "B.us", "name": "B", "country": "US", "is_nsfw": True},
                {"id": "C.fr", "name": "C", "country": "FR", "closed": "2020-01-01"},
                {"id": "D.de", "name": "D", "country": "DE"},
                {"id": "E.it", "name": "E", "country": "IT"}
            ],
            "streams": [
                {"channel": "A.tr", "url": "https://example.org/live.m3u8", "quality": "720p"},
                {"channel": "B.us", "url": "https://example.org/adult.m3u8"},
                {"channel": "C.fr", "url": "https://example.org/closed.m3u8"},
                {"channel": "D.de", "url": "file:///etc/passwd"},
                {"channel": "E.it", "url": "https://example.org/e.m3u8"}
            ],
            "countries": [
                {"code": "TR", "name": "Türkiye", "flag": "🇹🇷"},
                {"code": "US", "name": "United States", "flag": "🇺🇸"},
                {"code": "FR", "name": "France", "flag": "🇫🇷"},
                {"code": "DE", "name": "Germany", "flag": "🇩🇪"},
                {"code": "IT", "name": "Italy", "flag": "🇮🇹"}
            ],
            "languages": [{"code": "tur", "name": "Turkish"}],
            "categories": [],
            "logos": [{"channel": "A.tr", "url": "https://example.org/a.png", "in_use": True}],
            "feeds": [{"channel": "A.tr", "languages": ["tur"]}],
            "blocklist": [{"channel": "E.it", "reason": "dmca"}]
        }

    def test_only_review_candidates_with_http_streams_are_included(self):
        records, skipped = build_catalog(self.data, "2026-11-03T00:00:00Z")
        self.assertEqual([item["id"] for item in records], ["A.tr"])
        self.assertEqual(records[0]["status"], "candidate")
        self.assertEqual(records[0]["rights_review_status"], "not_reviewed")
        self.assertEqual(records[0]["country"]["flag"], "🇹🇷")
        self.assertEqual(records[0]["languages"][0]["code"], "tur")
        self.assertEqual(skipped["nsfw"], 1)
        self.assertEqual(skipped["closed_or_replaced"], 1)
        self.assertEqual(skipped["blocklisted"], 1)
        self.assertEqual(skipped["no_stream"], 1)

    def test_quality_labels_are_normalized_as_hints(self):
        self.assertEqual(normalize_quality("2160p"), "UHD_4K")
        self.assertEqual(normalize_quality("4K UHD"), "UHD_4K")
        self.assertEqual(normalize_quality("1080p"), "FHD")
        self.assertEqual(normalize_quality("720p"), "HD")
        self.assertEqual(normalize_quality("576p"), "SD")
        self.assertEqual(normalize_quality("unknown"), "UNKNOWN")

    def test_no_stream_is_approved_automatically(self):
        records, _ = build_catalog(self.data, "2026-11-03T00:00:00Z")
        self.assertTrue(all(item["status"] != "approved" for item in records))


if __name__ == "__main__":
    unittest.main()
