## [1.0.3] - 2026-10-29

### Fixed
- Signing in to a self-hosted backend failed with "No API key found in request": every Nuvio
  backend — official or self-hosted — needs its public anon key sent along. Flix now reads the
  key from the backend's own config.js (per backend) and uses it for all calls.

# Changelog

All notable changes to Flix Launcher are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [1.0.2] - 2026-10-29

### Fixed
- Holding the remote's volume button repeats again in every app: the Home-key service no longer filters
  all key events (an accessibility service that filters keys silently disables Android's key repeat, so
  holding volume changed it once instead of continuously). The Home button is caught by watching for the
  stock home screen instead — a press from another app may flash it for a moment before Flix comes back.
- Titles open in your own NuvioTV build (`com.nuvio.tv.jellyfintv`) when it's installed, instead of only
  ever the official Nuvio app — everywhere Nuvio is opened, gated or shown (rows, All apps, Settings).

### Added
- **My backend**: point Flix at your own NuvioTV server (Settings → Nuvio → My backend). Catalogues,
  addons and Continue watching are then fetched from your backend instead of Nuvio's official service,
  and sign-in happens against it. Switching backends signs you out once; "Use Nuvio's official service"
  switches back.

## [1.0.1] - 2026-10-27

### Added
- Back up and restore all settings, favourites and addon rows (Settings → About)

### Fixed
- Phone remote now needs the code from the TV's QR link, accepts changes only as form posts, and limits connections
- Home key service no longer touches other apps' accessibility services, and stays off when you turn it off
- Trailers no longer stop or swap when moving quickly between titles; trailer ids from addons are validated
- Poster cards no longer pick up another row's details or size; clicked posters spring back
- Background video and photos stop fully when you leave the home screen
- Cached files are saved safely and downloads have size limits
- Nuvio: a rejected sign-in now shows as signed out; tokens are stored encrypted
- Plain-HTTP traffic is limited to Apple's aerial video server; addons must use https

## [1.0.0] - 2026-10-27

First public release.

### Added
- Favourites row and an apps grid on the home screen itself
- Film and show rows from Stremio-compatible addons, with Top 10, genre filter and a featured carousel (can be switched off)
- Continue watching from a Nuvio account, with progress on each card and on the selected card
- Trailers behind the selected title, with start delay and sound options
- About 270 aerial video backgrounds in five categories, and photo backgrounds from the Bing archive
- Clock, greeting, date, sunrise/sunset arc, weather and 3-day forecast
- Searchable settings
- Home key handling for Google TV

### Performance
- Icons redrawn once into GPU bitmaps: about 54 MB of memory on a 2 GB TV
- Favourite apps appear before the rest of the app list is drawn

[1.0.1]: https://github.com/HatriGt/flix-launcher/releases/tag/v1.0.1
[1.0.0]: https://github.com/HatriGt/flix-launcher/releases/tag/v1.0.0
