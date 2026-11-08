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
- **FreeCastHub public IPTV**: an experimental supplementary list; its category files are subsets of the same catalogue and are not counted as new sources.
- **M3UPT**: a Portuguese community playlist with international entries; non-HTTPS, restricted, encrypted, and unsupported entries are filtered by the app parser and still require technical and rights review.
- **Romaxa55 World IPTV**: a large community aggregation that states it checks its collected channel URLs every six hours. It is treated as one additional discovery feed, not as an independent origin for every channel; the RS TV library deduplicates its URLs against all other feeds. Its own automated checks do not establish playback on every device, geographic availability, or redistribution rights.

The app also exposes IPTV-org category slices for animation, auto, business, classic, comedy, cooking, culture, documentary, education, entertainment, family, general, kids, legislative, lifestyle, music, news, outdoor, public, relax, religious, science, travel, and weather. These are official category paths documented by the upstream project. They are alternate views of the same global dataset, not 24 independent channel inventories; exact stream URLs are deduplicated by the RS TV catalogue and health scanner. Adult and undefined categories are intentionally excluded.

The **مصادر IPTV** screen is a package downloader only: it downloads the selected M3U list, checks HTTPS and a size limit, validates redirects against the source's exact host, parses HTTPS channel entries, and stores the package in the separate imported-channel library. It does not start playback and does not replace the user's local playlist. Selecting a source again refreshes that package. To watch imported channels, open **البث المباشر** and choose the imported source library; fullscreen playback opens a numbered channel selector with search and a receiver-style channel information panel. Official broadcaster destinations (Al Jazeera Arabic/English, CGTN, Euronews and NASA+) open the broadcaster's own website/player instead of extracting or proxying its media.

These playlists include traditional linear television brands and channels that may also be carried by satellite, but the app receives them through their online streams. It does not tune a satellite dish or emulate a satellite receiver.

## Official sports and film destinations

RS TV includes a separate official-service directory grouped into sports and films. These entries open the rights holder or licensed platform; they are not converted into direct M3U links and are not scraped or re-hosted.

### Sports

The app's **مصادر IPTV** list now also includes two direct category playlists for sports discovery:
- **Doubo Sports**: a separate daily-updated M3U. Doubo currently lists 100 sports channels in its directory; this is a publisher-reported count, not 100 independently verified working streams.
- **IPTV-org Sports**: a worldwide category playlist. It overlaps the main IPTV-org index and country lists, so the RS TV source merger removes exact duplicate stream URLs.

The app also exposes dedicated **IPTV-org Movies** and **IPTV-org Series** linear-channel lists. These are scheduled TV channels, not on-demand movie files. The health scan includes these category lists, but deduplicates their exact stream URLs across all worldwide feeds.

- **FIFA+** — selected live football matches, archive matches and original programming. The live schedule and rights vary by competition and territory.
- **Eurovision Sport** — free live events, replays and highlights across sports such as athletics, aquatics, cycling and combat sports. Individual event rights and availability can vary by country.
- **Red Bull TV** — selected live events, replays, sports films and documentaries, including motorsport and action sports; event coverage varies by region.
- **TRT Spor** — official Turkish broadcaster page. Particular match videos or feeds may be geo-restricted according to rights.
- **Fubo Sports Network** — free ad-supported sports programming through selected distribution partners; it is not a universal free feed of premium leagues and its supported platforms/territories are limited.

These links are useful starting points, not promises that every event is available free in Türkiye. Check the event schedule and territory notice on the provider itself. The unverified “Alwan Sports” name is not added as a source until an official operator website or rights-holder confirmation can be identified.

### Films and series

- **Rakuten TV Free / Live TV** — free ad-supported films, series and live channels in supported countries and on supported devices.
- **Pluto TV** — free ad-supported live channels and on-demand films; the catalogue differs by country.
- **Tubi** — free ad-supported films and selected live channels in markets listed by Tubi (including the US, Canada, Mexico, parts of LATAM, the UK and Australia); Türkiye is not listed as a supported market.
- **Plex** — free films and live channels in supported countries. Plex currently lists Türkiye among countries where its free Live TV service is unavailable.
- **Internet Archive Moving Image Archive** — a discovery collection for films and archival material. Rights must be checked for each item and each version before describing it as public-domain or reusing it.

The app opens these providers through their official player/app or website. It does not expose premium channels as free, bypass DRM, or assume a publicly accessible archive item is automatically in the public domain.

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

A separate read-only GitHub Actions workflow checks the configured public M3U source endpoints daily and on manual request. It reports source availability, M3U structure, unique HTTPS URL counts, eligible direct-stream URL counts, global unique stream URLs across all sources, cross-source duplicates, duplicate URLs, unsafe/non-HTTPS URLs, known platform page links, missing names, orphan URLs, and incomplete entries. The report is published in the GitHub Actions run summary; an artifact ZIP is also attempted when repository artifact storage is available.

This audit does **not** request individual channel streams, measure playback uptime/bitrate/resolution, or review copyright. It never changes the candidate catalogue or promotes entries to approved. Every channel remains a candidate until separate technical playback and rights review are completed.


## Bounded channel endpoint sampling

The daily quality workflow also samples up to six HTTPS channel endpoints from each configured playlist, selected evenly across the eligible entries rather than only from the beginning. It requests at most 64 KiB per endpoint with an eight-second timeout, rejects non-public/unresolvable hosts, rejects cross-host or non-HTTPS redirects, and does not fetch HLS segments or play media. Results distinguish HTTP response, HLS/DASH manifest, media response, timeout, network error, forbidden/geo-restricted, not found, and unexpected content. This is a lightweight endpoint check only: it does not prove that a viewer can play the channel, verify video quality, or establish redistribution rights. Reports remain diagnostic and do not modify or approve catalogue entries.


## Rotating global stream-health scan

The daily audit now collects the direct HTTPS entries from all configured playlists, removes duplicate canonical stream URLs across the complete source set, and checks a rotating batch of up to 600 unique URLs using bounded concurrency. The batch rotates by UTC day so that a catalogue of many thousands of links is not hammered in one workflow run. The report shows the total unique candidate URLs, batch number, source attribution, per-result status, and how many HLS first segments or direct media bytes were actually received. Raw stream URLs (including query tokens) are omitted from the report.

For HLS, the checker fetches a bounded manifest and attempts to retrieve the first media segment. It skips encrypted HLS playlists rather than trying to obtain keys. For direct media, it requests only a bounded byte range. DASH manifests are structurally checked, and manifests advertising content protection are identified; this is not a complete DASH playback test. Temporary failures, geo-blocks and server errors remain diagnostic statuses and are not a basis for instantly deleting a channel. Every batch is a snapshot; the full catalogue is revisited over successive scheduled runs.

This still is not playback on a real phone/TV box, a sustained uptime/bitrate test, or a legal review. A successful first segment does not grant streaming rights. Only separately rights-reviewed entries may ever be approved for the default catalogue.
