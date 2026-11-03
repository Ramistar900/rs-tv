# RS TV Channel Catalog

This directory is the starting point for the RS TV channel library.

## Source registry

See [sources.json](sources.json) for the initial source registry.

### Internet channels

- IPTV-org is a discovery source for public playlists grouped by country, language, and category.
- Official free streaming services such as Pluto TV, Rakuten TV, and Samsung TV Plus should initially be linked as official services. Their streams must not be extracted or re-hosted without permission.
- A publicly reachable M3U/M3U8 URL is not, by itself, evidence of permission to redistribute it.

### Satellite FTA channels

Use a dedicated satellite catalogue for free-to-air channel names and transponder data. Keep satellite tuning data separate from internet stream URLs. A satellite frequency is not a stream URL and requires compatible reception hardware.

## Admission states

Every candidate channel must have an explicit state:

- `candidate`: discovered, not yet reviewed.
- `stream_checked`: URL was tested, rights are still unverified.
- `rights_reviewed`: reliable first-party evidence supports inclusion.
- `approved`: passes both technical and rights review.
- `rejected`: unavailable, paid/encrypted without authorization, or rights are unclear.

Only `approved` channels may appear in the default RS TV catalogue.

## Required review

For each channel, record its country, language, category, logo source, stream format, source attribution, rights evidence, last stream check, and last metadata review. Recheck links periodically and remove broken or disputed entries.
