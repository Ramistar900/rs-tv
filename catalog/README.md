# RS TV IPTV Package Catalogue

RS TV discovers and imports live television through IPTV playlist sources (M3U/M3U8). The app does not tune satellite frequencies or require a dish: a satellite broadcaster is available only when an online stream is published and the source permits its use.

## IPTV packages are the channel-discovery entry point

The standalone «العالمية» tab and YouTube live-discovery tab are not part of the current navigation. The «مصادر IPTV» screen is the channel-package directory. It downloads a selected playlist; it does not play video inside the source page and does not replace the user's own local playlist. Imported channels are available from «البث المباشر».

The package directory is grouped by region and country. Arabic country packages are listed separately, including:
- سوريا، لبنان، العراق، فلسطين، الأردن، مصر، السعودية، الإمارات، البحرين، الكويت، قطر، عُمان، اليمن، السودان، ليبيا، تونس، الجزائر، المغرب، موريتانيا، الصومال، جيبوتي، وجزر القمر.
- Europe, North America (including the United States and Canada), Latin America and South America, East Asia, the rest of Asia, Africa, and Oceania.
- Additional category packages for movies, series, sports, news, documentary, music, children, and other classifications.

## Country-package sources

### Doubo Public Channel Directory

RS TV includes country M3U packages for the 128 countries and regions currently listed by Doubo. Each country has a separate package, including Syria, Lebanon, Iraq, the United States, Canada and Türkiye. The source directory says it is updated daily and lists channels whose broadcasters make them freely and publicly available. Its 2026-11-08 snapshot reported 2,982 listed channels across 128 countries and regions; this number changes and is not an RS TV verified-playback count.

Doubo describes its filtering policy as limiting entries to streams served from a broadcaster's own website, a broadcast-grade CDN or an official free platform; it says it excludes paid channels and channels subject to rights-holder objections. The directory/index data is CC BY 4.0 and RS TV displays attribution. This licence applies to the directory data, not the underlying programmes: stream/content rights remain with broadcasters, and geographic restrictions may apply. The directory also states that it does not provide or relay channel content.

- Directory and current counts: https://doubo.tv/en/playlists/
- Country data and methodology: https://doubo.tv/en/data/free-tv-channels/
- M3U country files are published under the directory's countries path.

### IPTV-org

The app also provides country and category playlists from IPTV-org. Its country package list is generated from country metadata in the bundled review catalogue and includes the Arab country codes even when the current snapshot has no candidate stream for that country. These are community-maintained discovery playlists. Their inclusion in an M3U index does not establish licensing, current availability, or successful playback.

The structured importer uses the upstream channels, streams, countries, languages, categories, logos, feeds and blocklist datasets. The bundled candidate snapshot in this branch was generated on 2026-11-08 and contained:
- 31,758 channel records in the upstream channel dataset;
- 18,637 stream records;
- 8,622 channel candidates with HTTPS streams;
- 180 represented countries with candidates.

These are candidate/metadata counts, not the number of unique working channels in the RS TV app. All imported records are marked candidate, not_reviewed, and unrated. The parser can index up to 50,000 candidate records, but that capacity limit is not a statement that 50,000 eligible channels currently exist.

## Deduplication and import behavior

- Each selected M3U source is downloaded separately, with HTTPS, size limits and redirect-host validation.
- Imported lists are stored in the IPTV source library, separate from the user's local playlist.
- When packages are merged, entries are deduplicated by canonical stream URL and HTTP-header profile. Identical URLs across country, language, category and global lists are not counted as additional channels.
- Country metadata is taken from the playlist entry when present; for country-specific packages, the country is inferred from the package identifier when the entry omits it.
- Selecting a package again refreshes that package. The user can clear the imported-source library without deleting their personal playlist or favourites.
- The screen is a package downloader, not a live player. To watch imported entries, open «البث المباشر» and choose the imported source library.

Different URLs for the same broadcaster may still represent separate feeds or mirrors. Deduplication by URL does not prove that differently addressed feeds are different broadcasters, nor does matching channel names prove that two streams are the same.

## Rights, safety and availability

An HTTPS URL, public M3U file, or successful HTTP response is not by itself evidence of permission to redistribute or watch a channel. Country lists may include geo-restricted feeds, streams may stop working, and broadcaster rights vary by competition, programme and territory. RS TV must not add pirated sources, paid channels without a valid entitlement, DRM/access-control bypasses, anonymous file hosts, or channels whose rights/source cannot be established.

The app filters malformed and non-HTTPS playlist entries. Technical checks are diagnostic only; passing an endpoint or manifest check does not establish playback on every device or legal permission. Only entries with separate source/rights evidence and technical verification should be described as approved or licensed. The app must not promise that every channel is available in Türkiye.

## Movies and series packages

The IPTV Movies and Series category playlists represent linear television channels with schedules; they are not a library of on-demand film files or episode downloads. The existing Movies library remains a separate app feature. Metadata from sources such as TMDB does not itself supply a video stream or grant playback/redistribution rights.

## Quality labels

Imported quality labels are hints, not verified resolution. Show 4K UHD (2160p), Full HD (1080p), HD (720p), or SD only when the stream actually provides that rendition. Auto is the default; never upscale and label it as native 4K.

## Automated playlist quality audit

The read-only GitHub Actions workflow audits configured M3U endpoints for availability, playlist structure, unique HTTPS URL counts, and duplicates. Additional channel endpoint/stream scans may be skipped for pull-request runs, and even a successful endpoint response does not prove real-device playback or rights.

The health workflow must not relay, store, transcode or publish channel media. Reports are diagnostic and must not automatically promote sources to legally approved or guaranteed-working status.

## Legacy private-section data

The old «خاص»/PIN interface has been removed from app navigation. The change does not automatically erase any legacy files that may already exist in a user's separate private-package storage directory; unrelated local data is left untouched by the update.
