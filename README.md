# 🎨 PixivPatched

面向 Pixiv 安卓版（`jp.pxv.android`）的 [Morphe](https://morphe.software) 补丁合集，可逐条开关：
AI 作品标记、去广告、原图下载、客户端会员功能、热度搜索、OLED 纯黑主题、分析上报拦截、全屏查看器加速。

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL%203.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Target App](https://img.shields.io/badge/目标版本-Pixiv%20v6.202.1-0096FA.svg)](https://play.google.com/store/apps/details?id=jp.pxv.android)

支持的版本：**Pixiv 6.202.1（versionCode 69581）**。补丁直接绑定这个版本，其他版本会拒绝打补丁。

---

## 📲 添加到 Morphe Manager

<a href="https://morphe.software/add-source?github=Piracola/Pixiv-Patched">
  <img src=".github/assets/add-to-morphe.svg" alt="Add to Morphe" height="48">
</a>

手动添加：**Morphe Manager** ➔ **设置** ➔ **补丁源** ➔ **添加源** ➔ `https://github.com/Piracola/Pixiv-Patched`

---

## ✨ 功能说明

| 补丁 | 作用 |
| :--- | :--- |
| 🤖 **AI Work Flagger** | 读取 Pixiv 自带的 `illustAiType` 标记（外加可自定义的标签黑名单）判断 AI 作品：缩略图变暗并加 `[AI]` 角标（也可以直接隐藏）、详情页顶部显示提示横幅、标题旁加 `[AI]` 标签、可自动屏蔽 AI 作者，并自带独立的设置界面。 |
| 🚫 **Adblocker** | 去掉底部的横幅广告容器、拦截 AppLovin 插屏广告、屏蔽评分弹窗，并隐藏信息流和详情页里的推广作品。 |
| 📥 **Downloader** | 在作品详情页底栏加一个保存按钮，把原图下载到相册，文件名带作品信息。 |
| ⭐ **Premium Features** | 模拟客户端的会员状态、解除屏蔽标签的数量上限、在服务端返回空列表时补上插画/漫画和小说的本地浏览历史。 |
| 🔥 **Popular Search Unlock** | 无需订阅、也没有试用倒计时，直接解锁「按热度排序」（`popular_desc`）搜索标签页。 |
| 🌑 **OLED Dark Theme** | 把暗色主题的表面/背景色改写为纯黑。 |
| 🕵️ **Analytics Blocker** | 关闭 Firebase Analytics 的页面追踪。 |
| 🖼️ **Enhanced Viewer & Instant Zoom** | 全屏查看器先用标准分辨率图做即时占位，原图加载过程中仍可缩放拖动并保留缩放位置，同时显示一个低调的加载指示。 |

所有补丁互相独立，可自由开关。想保留原来的试用流程，把 **Popular Search Unlock** 关掉即可。

---

## 🩹 补丁列表

<!-- PATCHES_START EXPANDED -->
> **[v1.0.1](https://github.com/Piracola/Pixiv-Patched/releases/tag/v1.0.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;8 patches total
<details open>
<summary>📦 Pixiv&nbsp;&nbsp;•&nbsp;&nbsp;8 patches</summary>
<br>

**Package name:** `jp.pxv.android`

**🎯 Supported versions:**

| 6.202.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Pixiv AI Work Flagger](#pixiv-ai-work-flagger) | Identifies AI-generated works, dims thumbnails with an [AI] badge (or hides them completely), displays a full-width warning banner below the top toolbar on artwork detail, adds title [AI] pill, auto-blocks AI artists, and provides in-app settings with tag editor. |  |
| [Pixiv Adblocker](#pixiv-adblocker) | Eliminates bottom advertising banners and interstitials across all screens without layout padding, suppresses rate-this-app dialogs, and hides sponsored works in feeds. |  |
| [Pixiv Analytics Blocker](#pixiv-analytics-blocker) | Neutralizes Firebase Analytics screen tracking and Google Measurement telemetry for enhanced privacy. |  |
| [Pixiv Downloader](#pixiv-downloader) | Adds a button to the illustration and manga detail view to download original full-quality artwork directly to your Photos library with clean metadata filenames. |  |
| [Pixiv Enhanced Viewer & Instant Zoom](#pixiv-enhanced-viewer-instant-zoom) | Displays the standard-resolution artwork as an instant placeholder while the full-resolution image loads, allows immediate pinch-to-zoom/pan preserving zoom coordinates upon high-res load, and shows a discreet loading indicator. |  |
| [Pixiv OLED Dark Theme](#pixiv-oled-dark-theme) | Overrides dark gray surface and background colors with pure pitch black (#ff000000) for OLED battery savings and true high-contrast dark mode. |  |
| [Pixiv Popular Search Unlock](#pixiv-popular-search-unlock) | Unlocks sorting search results by popularity (popular_desc) without a premium subscription or trial countdown. |  |
| [Pixiv Premium Features](#pixiv-premium-features) | Emulates client-side Pixiv Premium membership status, removes the mute list limits, and restores the illustration/manga and novel browsing history lists. |  |

</details>

<!-- PATCHES_END -->

---

## 🛠️ 自行构建

环境：JDK 21、Android SDK（compileSdk 35）。

```bash
./gradlew buildAndroid
```

产物位于 `patches/build/libs/patches-<版本号>.mpp`。

---

## ⚠️ 需要先说清楚的限制

- **会员是纯客户端的。** 服务端权益（会员专属下载、更高分辨率档位等）不受影响。
- **热度排序**只改变客户端请求的搜索页面和排序参数，结果的排名和深度仍由 Pixiv 服务器决定。
- **广告只是界面层隐藏。** 广告 SDK 仍在运行，也仍可能发出请求。
- **浏览历史**分插画/漫画和小说两个列表，各自回退到本地缓存的最近 100 条（在 App 内打开过的作品），不是 Pixiv 的服务端历史；只有在开启 **Premium Features** 时才会记录和回填。
- **AI 标记**依赖 Pixiv 自己的 `illustAiType` 元数据，只有上传时勾选了 AI 的作品才有这个值；所以另外提供了标签黑名单作为兜底。

---

## 🙏 致谢

- [Piracola/PixivPatches](https://github.com/Piracola/PixivPatches) — 本仓库基于这套原始补丁改写（GPL-3.0）。
- [RookieEnough/De-Vanced](https://github.com/RookieEnough/De-Vanced) — 热度搜索解锁与插屏广告拦截（GPL-3.0）。
- [MorpheApp/morphe-patches-template](https://github.com/MorpheApp/morphe-patches-template) — 项目骨架（GPL-3.0）。

移植过来的文件在文件头保留了来源说明。

---

## 📄 许可

GNU General Public License v3.0 — 见 [LICENSE](LICENSE)。
