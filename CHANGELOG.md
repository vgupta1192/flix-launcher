## [1.1.5] - 2026-10-30

### Changed
- The featured panel (Play / More info) is now **part of the artwork**: its background and the
  text scrim take on a deep version of each featured title's dominant colour and crossfade to it
  on every slide, instead of sitting on a fixed dark-navy card that clashed with brighter
  artwork. If a colour can't be read, it falls back to the original navy.

## [1.1.4] - 2026-10-30

### Added
- **Self-healing home default**: when a Google TV app update (or a firmware reset) takes the
  home role back, Flix notices on the next open and shows the system's own "Set Flix Launcher as
  your home app?" dialog — one press restores it, at most once a day until fixed. Also visible
  under Settings → About → "Default home app".

## [1.1.3] - 2026-10-30

### Fixed
- Kids mode setup is no longer a dead end: with no kid apps tagged yet, the kids home shows the
  **All apps tile** — open it, long-press an app and choose "Kid app", and the kids home fills as
  you tag. Once at least one app is tagged, the tile disappears and the home locks behind the PIN.
  Kid-tagged apps now appear on the kids home even if they aren't favourites.

## [1.1.2] - 2026-10-30

### Changed
- Continue watching: the app resume cards now come **first** (most recently opened first) instead
  of hiding after every Nuvio title, and each card shows the **app's real icon** on its own
  coloured plate instead of generated art. Per-title progress from streaming apps (Netflix, Prime,
  YouTube…) remains impossible for any launcher — Android keeps it behind a signature permission;
  the card opens the app, which resumes where it left off.

## [1.1.1] - 2026-10-29

### Fixed
- The What's-new card now lists the 1.1.0 features (the card shipped in 1.1.0 was one release behind).

## [1.1.0] - 2026-10-29

### Added
- **Parental PIN** (Settings → Home screen → Parental): a hashed, salted code with a D-pad
  keypad. An unlock lasts for the current visit to the launcher — leaving for any app re-arms it.
- **App PIN lock**: long-press any app (home or All apps) → "Require PIN" — it asks for the PIN
  before opening, from the home, All apps, or a Continue watching card.
- **Kids mode**: a gated home that shows only apps tagged "Kid app" (long-press an app), hides
  the Apps/Search tabs, the billboard and the catalogue rows, and asks for the PIN before
  Settings, the All apps page or any non-kid app opens.
- **Photo-frame idle**: photo wallpapers now idle into a picture frame too — the rotation keeps
  going with the scene caption, like the aerial-video scenery; any keypress wakes it.
- **Ambient tint**: the wallpaper's own colour washes gently into the bottom of the home screen
  (and tints the video scenery's shading), changing with each photo. Setting: Wallpaper →
  Picture → "Ambient tint" (on by default).

## [1.0.8] - 2026-10-29

### Added
- **Wallpaper drift** (Ken Burns): wallpaper photos slowly pan and zoom while on screen, alternating
  direction each photo. Setting: Wallpaper → Picture → "Wallpaper drift" (on by default).
- **Night dimming**: from a chosen hour (22:00 / 23:00 / midnight) until 6:00 the scene dims
  further and the clock softens — no more blinding wallpaper during late-night TV. Setting:
  Wallpaper → Picture → "Night dimming" (off by default; pick your hour).
- **What's new card**: after each update, the first home open shows what changed; also under
  Settings → About → "What's new".
- **Surprise me**: long-press the billboard's Play button to jump into a random title from your
  rows (respects the current Movies/Shows tab and hidden titles).

## [1.0.7] - 2026-10-29

### Changed
- Performance audit: the "lite" rendering path — built for weak-GPU TVs, drops the ambient
  glow layer (~3 ms/frame) and crossfades hero backdrops on one layer instead of two — now
  engages on 3 GB-class TVs (threshold raised from 2.6 GB to 3.4 GB of total RAM). A 2.98 GB
  TV like ours was previously treated as high-end and ran the expensive path.
- Everything else the audit measured is already in good shape: 44 MB PSS with a single process
  (trailer process is killed when not in use), image caches trimmed on stop and on system
  memory pressure, clock/weather/wallpaper all on disciplined throttles, posters decoded at
  display width into GPU-side bitmaps, and a shared RecyclerView card pool across rows.

## [1.0.6] - 2026-10-29

### Added
- Continue watching now includes **resume cards for your other apps**, not just Nuvio: the video
  apps opened in the last week (YouTube, Prime Video, JioHotstar, Sony LIV, MovieBoxTV and any app
  that declares itself video) appear most-recent-first next to Nuvio's titles, and OK opens the app.
  Recency comes from the launcher's own launch notes plus Android's usage log (Usage access, granted
  once with adb — covers voice and in-app launches). A setting, "Apps in Continue watching", turns
  the cards off.

## [1.0.5] - 2026-10-29

### Added
- The updater logs every step to logcat under `FlixUpdater` (`adb logcat -s FlixUpdater` shows a
  whole update end to end): when auto/manual checks run and what they find, download size, the
  install-permission state the app sees, the install session commit and the installer's status
  callback — so a stalled update can be diagnosed from the Mac instead of guessed at.

## [1.0.4] - 2026-10-29

### Fixed
- Updates never finished installing: after downloading, the system's own "Install this update?"
  confirmation has to be launched by the app, and Flix only showed a toast — so nothing happened
  after the download. The confirmation now opens as it should.
- The "Install unknown apps" Settings page opened again on every check even after allowing it,
  and before anything was downloaded. The permission is checked first now, and the page is
  offered at most once every 30 minutes with a clear "press Check again" message.

## [1.0.3] - 2026-10-29

### Fixed
- Signing in to a self-hosted backend failed with "No API key found in request": every Nuvio
  backend — official or self-hosted — needs its public anon key sent along. Flix now reads the
  key from the backend's own config.js (per backend) and uses it for all calls.
- The QR code / phone sign-in link pointed at Nuvio's official login page, approving into the
  wrong (official) account. With a custom backend set, the link now opens the backend's own
  /tv-login page, so the approval signs in to YOUR backend.

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
