import unittest
from unittest.mock import patch

from probe_channel_streams import probe_stream, run_audit


class StreamProbeTests(unittest.TestCase):
    def test_rejects_non_https_before_network(self):
        self.assertEqual(probe_stream("http://example.org/live")["status"], "unsafe_url")

    @patch("probe_channel_streams.public_host", return_value=False)
    def test_rejects_non_public_or_unresolvable_host(self, _):
        self.assertEqual(probe_stream("https://example.org/live")["status"], "host_not_public_or_unresolvable")

    def test_caps_samples_and_does_not_probe_platform_pages(self):
        playlist = """#EXTM3U
#EXTINF:-1,One
https://one.example/live.m3u8
#EXTINF:-1,YouTube
https://youtube.com/watch?v=abc
#EXTINF:-1,Two
https://two.example/live.m3u8
#EXTINF:-1,Three
https://three.example/live.m3u8
#EXTINF:-1,Four
https://four.example/live.m3u8
"""
        calls = []
        def fake_probe(url):
            calls.append(url)
            return {"status": "responded", "response_kind": "hls_manifest"}
        with patch("probe_channel_streams.SOURCES", [("fixture", "https://playlist.example/list.m3u")]):
            report = run_audit(lambda _: playlist, fake_probe)
        self.assertEqual(report["samples_attempted"], 4)
        self.assertEqual(len(calls), 4)
        self.assertTrue(all("youtube.com" not in url for url in calls))


    def test_hls_probe_checks_first_segment_bytes(self):
        manifest = {
            "final_url": "https://example.org/live/index.m3u8",
            "sample": b"#EXTM3U\\n#EXTINF:6,segment\\nsegment001.ts\\n",
            "http_status": 200,
        }
        segment = {
            "sample": b"\\x47" * 1024,
            "http_status": 206,
            "content_type": "video/mp2t",
        }
        with patch("probe_channel_streams.public_host", return_value=True), \\
             patch("probe_channel_streams.fetch_resource", return_value=segment) as fetch:
            from probe_channel_streams import _probe_hls
            result = _probe_hls("https://example.org/live/index.m3u8", manifest)
        self.assertEqual(result["status"], "playback_segment_checked")
        self.assertEqual(result["segment_bytes_received"], 1024)
        fetch.assert_called_once_with("https://example.org/live/segment001.ts", 8192)

    def test_hls_probe_does_not_fetch_encrypted_segment(self):
        manifest = {
            "final_url": "https://example.org/live/index.m3u8",
            "sample": b'#EXTM3U\\n#EXT-X-KEY:METHOD=AES-128,URI="key.bin"\\n#EXTINF:6,segment\\nsegment001.ts\\n',
            "http_status": 200,
        }
        with patch("probe_channel_streams.fetch_resource") as fetch:
            from probe_channel_streams import _probe_hls
            result = _probe_hls("https://example.org/live/index.m3u8", manifest)
        self.assertEqual(result["status"], "encrypted_stream_not_decoded")
        fetch.assert_not_called()

    def test_report_never_claims_playback_or_rights(self):
        with patch("probe_channel_streams.SOURCES", []):
            report = run_audit()
        self.assertFalse(report["channel_playback_tested"])
        self.assertFalse(report["rights_review_performed"])
        self.assertEqual(report["samples_attempted"], 0)


if __name__ == "__main__":
    unittest.main()
