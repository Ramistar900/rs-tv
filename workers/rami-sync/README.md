# RAMİ TV Sync Worker — prototype

This isolated prototype is for the first technical test only. It does **not** change RS TV, ROYAL+, the shared IPTV catalogue, or any existing playback path.

## Endpoints

- `GET /health` — confirms that the Worker is deployed.
- `GET /clock` — returns the Worker UTC clock in milliseconds and ISO-8601.
- `GET /probe/archive-range` — performs HEAD and a 1 KB HTTP Range request against the exact Archive.org MP4 supplied for the test. It records response headers and cancels the response body immediately; it does not download, proxy, or store the video.
- `POST /simulate` — accepts a temporary channel schedule and returns the video index and offset that should be playing at the requested time. This is a calculation test, not persistent channel storage.

Example simulation body:

```json
{
  "nowMs": 1795824005000,
  "channel": {
    "id": "rami-tv-1",
    "startAtUtc": "2026-11-28T00:00:00.000Z",
    "loop": true,
    "videos": [
      {
        "id": "sample",
        "title": "Sample video",
        "url": "https://example.org/sample.mp4",
        "durationMs": 22245040
      }
    ]
  }
}
```

## Local tests

Run with Node.js 22 or later:

```bash
node --test workers/rami-sync/test/index.test.mjs
```

## Deploy from a phone browser

1. Open Cloudflare Dashboard → Workers & Pages.
2. Create a Worker and connect the GitHub repository `Ramistar900/rs-tv`.
3. Select branch `experiment/rami-sync-worker-prototype-2026-11-28`.
4. Set the root directory to `workers/rami-sync`, with no build command.
5. Deploy, then open the Worker URL followed by `/health` and `/probe/archive-range`.

The prototype has no KV namespace, no secrets, and no paid services. Do not connect it to either app yet. Do not add a billing method or enable a paid plan. The next stage is considered only after the range test and two-device playback test succeed.

## Important limits

A successful Range response only proves that the source can return byte ranges. It does not prove the file is licensed for redistribution, that Archive.org will sustain a large audience, or that two devices render the same frame at the same instant. The Worker returns timing data only; it does not relay video bytes.
