# RAMİ TV Catalog + Sync Worker — experimental

This Worker reads the existing public `tv.json` catalog directly. Channel numbers are **not hard-coded**: adding a new numeric key (for example `"9"`) to the catalog makes that channel discoverable through the Worker after the short cache window. No Worker code change or APK update is needed just to add a channel.

This remains an isolated experiment. It does not change RS TV, ROYAL+, their playback code, or any production service. Do not merge or connect the apps until the API contract and real-device tests are approved.

## Live catalog source

The Worker reads the current raw Gist URL configured in `src/index.mjs` and caches the upstream response for about 20 seconds.

The expected catalog shape is the current object keyed by channel number:

```json
{
  "1": [
    { "url": "https://media.example/video-one.mp4", "duration": 600 }
  ],
  "9": [
    { "url": "https://media.example/video-nine.mp4", "duration": 900 }
  ]
}
```

Each channel value is a non-empty array of videos. Every entry needs an HTTPS media URL and a positive duration in seconds. The Worker validates the data and reports invalid channels rather than attempting to proxy arbitrary URLs.

## API

- `GET /health` — confirms the Worker deployment.
- `GET /clock` or `GET /api/clock` — returns server UTC time.
- `GET /api/channels` — lists every numeric channel key currently present in `tv.json`.
- `GET /api/channel/9` — returns channel 9's playlist and metadata. Replace 9 with any channel number present in the catalog.
- `GET /probe/archive-range` — checks HEAD and a 1 KB Range request against the fixed Archive.org test MP4; cancels the body immediately.
- `POST /simulate` — tests UTC playlist position calculation.

Example channel response includes the full playlist. If no `startAtUtc` exists at the top level of `tv.json`, it explicitly reports `schedule_not_configured`; it does not invent a shared broadcast start time. To enable common timeline offsets later, add a valid UTC ISO-8601 `startAtUtc` at the top level, then ensure every listed duration matches the actual media duration.

## Important playback distinction

This Worker returns JSON metadata and source URLs. It does **not** generate an HLS `.m3u8` playlist from MP4, does not relay video bytes, and does not guarantee frame-perfect synchronization. The apps must later be updated to read this API, open the returned media URL, and seek to the returned offset. That app integration is not part of this Worker-only experiment.

The current Gist contains only the entries actually present in that file. Channels 2 and 3 previously used placeholder URLs, and channel 1's M3U8 URL returned 404 in the earlier check. A channel appearing in the list means its catalog entry exists, not that its media source is playable or licensed for redistribution.

## Tests

Run with Node.js 22 or later:

```bash
node --test workers/rami-sync/test/index.test.mjs
```

Tests cover dynamic discovery, channel 9 appearing from catalog data, absent channels, URL/duration validation, UTC offsets, and disabled arbitrary proxy behavior.

## Deploy from a phone browser

1. Open Cloudflare Dashboard → Workers & Pages.
2. Connect `Ramistar900/rs-tv` to a new experimental Worker.
3. Select branch `experiment/rami-sync-worker-prototype-2026-11-28`.
4. Set the root directory to `workers/rami-sync`.
5. Deploy only after reviewing the experiment.
6. Test `/health`, `/api/channels`, and a channel that is actually present, such as `/api/channel/1`.

No KV, paid database, video proxy, billing method, or paid plan is required by this prototype. Deployment is a separate action and has not been performed by this code change.
