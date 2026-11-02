# 🎨 PixivPatched

A collection of toggleable [Morphe](https://morphe.software) patches for Pixiv on Android (`jp.pxv.android`):
AI artwork flagging, ad blocking, artwork downloading, premium client features, an OLED dark theme,
analytics blocking and a faster fullscreen viewer.

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL%203.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Target App](https://img.shields.io/badge/Target%20App-Pixiv%20v6.202.1-0096FA.svg)](https://play.google.com/store/apps/details?id=jp.pxv.android)

---

## 📲 Add to Morphe Manager

<a href="https://morphe.software/add-source?github=Piracola/PixivPatched">
  <img src=".github/assets/add-to-morphe.svg" alt="Add to Morphe" height="48">
</a>

Or manually: **Morphe Manager** ➔ **Settings** ➔ **Sources** ➔ **Add Source** ➔ `https://github.com/Piracola/PixivPatched`

---

## ✨ Features

| Feature | What it does |
| :--- | :--- |
| 🤖 **AI Work Flagger** | Reads Pixiv's own `illustAiType` flag (plus an optional tag blocklist), dims thumbnails with an `[AI]` badge or hides them, shows a warning banner on artwork detail, adds an `[AI]` pill next to titles, can auto-block AI artists, and ships its own settings screen. |
| 🚫 **Adblocker** | Collapses the bottom banner ad containers, blocks AppLovin interstitials, suppresses review prompts, and hides sponsored works in feeds and on detail pages. |
| 📥 **Downloader** | Adds a Save button to the artwork detail bottom bar and writes the original full-quality image to your gallery with a clean file name. |
| ⭐ **Premium Features** | Emulates client-side premium status, removes the mute list limits, and restores the browsing-history list for accounts the server returns an empty page for. |
| 🔥 **Popular Search Unlock** | Opens the "popular" (`popular_desc`) search tab without a subscription or trial countdown. |
| 🌑 **OLED Dark Theme** | Rewrites the dark theme surface/background colours to pure black. |
| 🕵️ **Analytics Blocker** | Neutralizes Firebase Analytics screen tracking. |
| 🖼️ **Enhanced Viewer & Instant Zoom** | Shows the standard-resolution image as an instant placeholder in the fullscreen viewer, keeps pinch-zoom/pan while the full-resolution image loads, and shows a discreet loading indicator. |

All patches are independent and can be toggled individually. De-select **Popular Search Unlock** if you
prefer to keep the original trial behaviour.

---

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->

<!-- Do not modify this section by hand. The patch list is generated when release.yml creates a new release. -->

<!-- PATCHES_END -->

---

## 🛠️ Building

Requirements: JDK 21, Android SDK (compileSdk 35).

```bash
./gradlew buildAndroid
```

The bundle is written to `patches/build/libs/patches-<version>.mpp`.

---

## ⚠️ Honest limitations

- **Premium is client-side only.** Server-side entitlements (premium-only downloads, higher
  resolution tiers, and so on) are unaffected.
- **Popular sort** only changes which search fragment and sort parameter the client requests. The
  ranking and result depth still come from Pixiv's servers.
- **Ads are hidden in the UI.** The ad SDKs still run and may still make requests.
- **Browsing history** falls back to a local cache of the last 100 artworks opened in the app. It is
  not Pixiv's server-side history.
- **AI flagging** depends on Pixiv's own `illustAiType` metadata, which is only populated for works
  the uploader tagged as AI-generated. A tag blocklist is provided as a fallback.

---

## 🙏 Credits

- [Piracola/PixivPatches](https://github.com/Piracola/PixivPatches) — original Pixiv patch set this
  repository is based on (GPL-3.0).
- [RookieEnough/De-Vanced](https://github.com/RookieEnough/De-Vanced) — popular search unlock and
  interstitial ad blocking (GPL-3.0).
- [MorpheApp/morphe-patches-template](https://github.com/MorpheApp/morphe-patches-template) — project
  scaffolding (GPL-3.0).

Ported files keep a note about their origin in the file header.

---

## 📄 License

GNU General Public License v3.0 — see [LICENSE](LICENSE).
