import unittest

from import_channel_candidates import build_catalog, normalize_quality, valid_stream_url


class ImporterTests(unittest.TestCase):
    def setUp(self):
        self.data = {
            "channels": [
                {"id": "A.tr", "name": "A", "country": "TR", "categories": ["news"]},
                {"id": "B.us", "name": "B", "country": "US", "is_nsfw": True},
                {"id": "C.fr", "name": "C", "country": "FR", "closed": "2020-01-01"},
                {"id": "D.de", "name": "D", "country": "DE"},
                {"id": "E.it", "name": "E", "country": "IT"},
                {"id": "F.global", "name": "F", "categories": ["general"]},
            ],
            "streams": [
                {"channel": "A.tr", "url": "https://example.org/live.m3u8", "quality": "720p"},
                {"channel": "B.us", "url": "https://example.org/adult.m3u8"},
                {"channel": "C.fr", "url": "https://example.org/closed.m3u8"},
                {"channel": "D.de", "url": "file:///etc/passwd"},
                {"channel": "E.it", "url": "https://example.org/e.m3u8"},
                {"channel": "F.global", "url": "https://global.example.org/live.m3u8"},
            ],
            "countries": [
                {"code": "TR", "name": "Türkiye", "flag": "🇹🇷"},
                {"code": "US", "name": "United States", "flag": "🇺🇸"},
                {"code": "FR", "name": "France", "flag": "🇫🇷"},
                {"code": "DE", "name": "Germany", "flag": "🇩🇪"},
                {"code": "IT", "name": "Italy", "flag": "🇮🇹"},
            ],
            "languages": [{"code": "tur", "name": "Turkish"}],
            "categories": [{"id": "news", "name": "News"}],
            "logos": [{"channel": "A.tr", "url": "https://example.org/a.png", "in_use": True}],
            "feeds": [{"channel": "A.tr", "languages": ["tur"]}],
            "blocklist": [{"channel": "E.it", "reason": "dmca"}],
        }

    def test_worldwide_candidates_keep_unknown_country_without_guessing(self):
        records, skipped, country_codes = build_catalog(self.data, "2026-11-03T00:00:00Z")
        self.assertEqual({item["id"] for item in records}, {"A.tr", "F.global"})
        global_record = next(item for item in records if item["id"] == "F.global")
        self.assertEqual(global_record["country"]["code"], "ZZ")
        self.assertEqual(global_record["country"]["flag"], "🌐")
        self.assertEqual(global_record["country"]["mapping_status"], "unknown")
        self.assertEqual(country_codes, ["TR"])
        turkey_record = next(item for item in records if item["id"] == "A.tr")
        self.assertEqual(turkey_record["category_names"], ["News"])
        self.assertEqual(turkey_record["languages"][0]["code"], "tur")
        self.assertEqual(skipped["nsfw"], 1)
        self.assertEqual(skipped["closed_or_replaced"], 1)
        self.assertEqual(skipped["blocklisted"], 1)
        self.assertEqual(skipped["no_stream"], 1)
        self.assertEqual(skipped["invalid_or_insecure_stream"], 1)

    def test_only_https_urls_with_host_and_without_credentials_are_accepted(self):
        self.assertTrue(valid_stream_url("https://example.org/live.m3u8"))
        self.assertFalse(valid_stream_url("http://example.org/live.m3u8"))
        self.assertFalse(valid_stream_url("file:///etc/passwd"))
        self.assertFalse(valid_stream_url("https:///missing-host"))
        self.assertFalse(valid_stream_url("https://user:pass@example.org/live.m3u8"))

    def test_duplicate_streams_are_removed_per_channel(self):
        self.data["streams"].append({"channel": "A.tr", "url": "https://example.org/live.m3u8"})
        records, skipped, _ = build_catalog(self.data, "2026-11-03T00:00:00Z")
        channel = next(item for item in records if item["id"] == "A.tr")
        self.assertEqual(len(channel["streams"]), 1)
        self.assertEqual(skipped["duplicate_stream"], 1)

    def test_quality_labels_are_source_hints_only(self):
        self.assertEqual(normalize_quality("2160p"), "UHD_4K")
        self.assertEqual(normalize_quality("4K UHD"), "UHD_4K")
        self.assertEqual(normalize_quality("1080p"), "FHD")
        self.assertEqual(normalize_quality("720p"), "HD")
        self.assertEqual(normalize_quality("576p"), "SD")
        self.assertEqual(normalize_quality("unknown"), "UNKNOWN")

    def test_no_candidate_is_approved_automatically(self):
        records, _, _ = build_catalog(self.data, "2026-11-03T00:00:00Z")
        self.assertTrue(all(item["status"] == "candidate" for item in records))
        self.assertTrue(all(item["rights_review_status"] == "not_reviewed" for item in records))


if __name__ == "__main__":
    unittest.main()
