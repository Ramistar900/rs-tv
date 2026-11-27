# Sinjar TV Core

This Android library is the shared, UI-independent implementation used by the SINJAR TV phone/tablet app and ROYAL+ TV integration.

It owns:
- M3U parsing and HTTPS/header validation
- Channel and source models
- Duplicate-safe source merging and source catalog/downloading
- Per-installation source and favourites persistence
- Mature-content classification used to keep restricted entries out of public browsing

The host app owns its native interface and Media3 playback surface. The shared library intentionally has no Compose or player UI dependency.

Keep this module's package/API stable for both host apps. Validate changes in both repositories before release.
