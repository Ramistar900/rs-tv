# RS TV Global Internet Catalogue

RS TV is an internet-only catalogue and player. Satellite dishes, DVB tuners, orbital positions, and satellite frequencies are outside the product.

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
