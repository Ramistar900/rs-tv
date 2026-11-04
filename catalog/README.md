# RS TV Global Internet Catalogue

RS TV plays television delivered over the internet. A broadcaster may also be a traditional satellite channel; RS TV can play its official/public online feed when available. Direct dish reception, DVB tuners, orbital tuning and satellite frequencies are not performed by a phone/TV Box app.

## Worldwide live-channel discovery

The scheduled importer uses the IPTV-org structured API datasets for channels, streams, countries, languages, categories, logos, feeds, and the upstream blocklist. It joins each stream to its stable channel ID and retains country/language/category attribution.

- **الكل** combines candidates from every country represented in the source.
- Country filters use exact ISO 3166-1 alpha-2 metadata.
- Channels without a source country are retained as `ZZ` with a neutral globe icon; no flag is guessed.
- Categories and language labels are taken from source metadata.
- Stream candidates must use HTTPS, have a host, and contain no URL credentials.
- Duplicate stream URLs are removed per channel.
- Adult, closed/replaced, blocklisted, and no-HTTPS-stream records are excluded.
- Every generated record remains `candidate`, `not_reviewed`, and `unrated`.

See [sources.json](sources.json) for the source registry and [import-plan.json](import-plan.json) for the source/field contract.

Run the importer locally:

```sh
python -m unittest discover -s tools -p 'test_*.py' -v
python tools/import_channel_candidates.py
```

GitHub Actions refreshes `catalog/generated/candidates.json` daily and when the importer changes. The generated file is a discovery/review queue, not a ready-to-play or rights-cleared playlist. The importer does not guarantee that every country or broadcaster is represented; it is limited to upstream source coverage.


## Additional online channel sources in the Android app

The **مصادر IPTV** screen offers user-initiated M3U downloads from several independent publishing projects:

- **Doubo Public Channel Directory**: worldwide, Türkiye, and Arabic-language lists. Its publisher describes a daily build with an official-domain/allowlisted-host policy and first-media-segment checks. The link selection/index data is CC BY 4.0; RS TV credits the directory. This is not a licence to redistribute the underlying programmes.
- **IPTV-org playlists**: worldwide, Arab-region, and Türkiye lists. This is a large community-maintained discovery catalogue; availability and rights vary by channel.
- **Free-TV/IPTV**: a smaller quality-focused community list whose README says it targets channels officially offered free in at least one market; regional blocks can apply.
- **FreeCastHub public IPTV**: an experimental small supplementary list, never treated as independently rights-cleared.

The app downloads only the selected playlist, checks HTTPS and a size limit, validates redirects against the source's exact host, parses HTTPS entries, and stores the playlist locally. Selecting another source **replaces the active local playlist**. Selecting a source again fetches its latest published list. Official broadcaster destinations (Al Jazeera Arabic/English, CGTN, Euronews and NASA+) open the broadcaster's own website/player instead of extracting or proxying its media.

These playlists include traditional linear television brands and channels that may also be carried by satellite, but the app receives them through their online streams. It does not tune a satellite dish or emulate a satellite receiver.

## YouTube live broadcasts

YouTube live discovery is designed around the official YouTube Data API v3 search endpoint with `type=video` and `eventType=live`. It requires a protected `YOUTUBE_API_KEY`. Results depend on search terms, supported languages/regions, quota, and availability; they are not a complete index of all YouTube live channels. Playback must use the official YouTube watch route or approved official player integration. RS TV must not extract or rehost YouTube media streams.

## Official broadcaster websites and internet services

A universal index of every live broadcaster website does not exist. Add verified official website entries by country and use the broadcaster's permitted embed/player. When embedding is unavailable or not permitted, open the broadcaster website or official service app. Do not scrape protected players or reverse-engineer private APIs.

FAST services such as Pluto TV, Rakuten TV Live, and Samsung TV Plus remain official-service links. Their channel availability, geographic restrictions, and device support are controlled by their operators; RS TV will not extract their streams.

## Films and series catalogue

TMDB can provide international movie/TV metadata, including titles, descriptions, artwork, seasons, episodes, and availability metadata where offered. It requires a `TMDB_API_KEY`, attribution, and compliance with its current terms. TMDB metadata is not a film/episode stream and does not grant playback or redistribution rights. Commercial licensing must be reviewed separately.

Internet Archive can be searched for collection metadata, but every item needs an item-level rights check before being described as free or public-domain content. A listing alone is not sufficient rights evidence.

For every film/series record, keep metadata provider, country availability, official watch provider or item-level rights evidence, attribution, and last review time. Never add pirated streams or bypass DRM/subscriptions.

## Admission states and reliability

- `candidate`: imported from a discovery source; not reviewed.
- `stream_checked`: technical URL/playback probe passed; rights remain unverified.
- `rights_reviewed`: evidence supports the intended catalogue use.
- `approved`: passes both technical and rights review.
- `rejected`: unavailable, unauthorized, paid/encrypted without entitlement, or rights unclear.

Only `approved` sources can appear in the default playable catalogue. Technical reachability is not copyright permission. Prefer official broadcaster/CDN endpoints, preserve source attribution, show last checked time, and hide stale or repeatedly failing streams. Never promise zero buffering.

## Quality labels

Imported quality labels are hints, not verified resolution. Show 4K UHD (2160p), Full HD (1080p), HD (720p), or SD only when the stream actually provides that rendition. Auto is the default; never upscale and label as native 4K.


## Automated playlist quality audit

A separate read-only GitHub Actions workflow checks the eight configured public M3U source endpoints daily and on manual request. It reports source availability, M3U structure, unique HTTPS URL counts, duplicate URLs, unsafe/non-HTTPS URLs, known platform page links, missing names, orphan URLs, and incomplete entries. The report is uploaded as the `rs-tv-playlist-quality-report` artifact.

This audit does **not** request individual channel streams, measure playback uptime/bitrate/resolution, or review copyright. It never changes the candidate catalogue or promotes entries to approved. Every channel remains a candidate until separate technical playback and rights review are completed.


## Bounded channel endpoint sampling

The daily quality workflow also samples at most three HTTPS channel endpoints from each configured playlist. It requests at most 64 KiB per endpoint with an eight-second timeout, rejects non-public/unresolvable hosts, rejects cross-host or non-HTTPS redirects, and does not fetch HLS segments or play media. Results distinguish HTTP response, HLS/DASH manifest, media response, timeout, network error, forbidden/geo-restricted, not found, and unexpected content. This is a lightweight endpoint check only: it does not prove that a viewer can play the channel, verify video quality, or establish redistribution rights. Reports remain diagnostic and do not modify or approve catalogue entries.
