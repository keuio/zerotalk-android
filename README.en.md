# ZeroTalk · Unofficial Third-Party Client

> An **unofficial** Android client for 「零语 / ZeroTalk」 (app.zerotalk.cn), built with Kotlin Multiplatform + Compose Multiplatform.

[中文说明](./README.md) | **English**

---

## ⚠️ Unofficial Notice

This repository is an **UNOFFICIAL third-party client** for the ZeroTalk service, independently developed by community developers.
It is **not affiliated with, authorized, endorsed, or supported by** the ZeroTalk service or its operators.
All service names, trademarks, APIs, and content belong to their respective owners.
This project is provided for learning and technical exchange only; users must comply with the target service's terms of use.

---

## Features

- **Moments**: Featured / Following / Mine; publish, like, comment, share, report, pin, visibility
- **Catch**: catch moments and browse catch history (report supported)
- **Secret rooms (group chats)**: member grid, join-ban list, admin permissions, remove member
- **Private chat / matching**: text, image and voice messages
- **Voice calls**: powered by WebRTC
- **Peer remarks**: local `uid → remark` map, reflected in conversation names and group chat bubbles
- **Chat backgrounds**: stored **locally only**, per-conversation, with an optional global default, works offline
- **System notifications & keep-alive**: can be turned off in 「Me → Messages & Notifications」
- **Liquid Glass visuals**: with a compatibility rendering mode for low-end devices

---

## Download

Get the **signed APK** from [Releases](https://github.com/keuio/zerotalk-android/releases) — Android 7.0 (API 24) or later.

> APK signing certificate SHA-256: `576adcaafea97432944c4817e1feba9baaa4b81f325ac43dcaafd129c7241892`

---

## Build

### Requirements

| Item | Version |
| --- | --- |
| JDK | 17 |
| Android SDK | compileSdk / targetSdk 36, minSdk 24 |
| Gradle | use the bundled Wrapper |

### Commands

```bash
# Debug build (output: composeApp/build/outputs/apk/debug/composeApp-debug.apk)
./gradlew :composeApp:assembleDebug

# Release build (requires your own signing config)
./gradlew :composeApp:assembleRelease
```

> On Windows use `gradlew.bat`. The first build needs network access to fetch dependencies.

---

## Project structure

```text
composeApp/            Android app (KMP: commonMain shared logic / androidMain platform code)
libs/KMPLiquidGlass/   Bundled Liquid Glass library; only the :backdrop module is kept
gradle/                Version catalog (libs.versions.toml)
```

---

## Third-party components

| Component | License | Notes |
| --- | --- | --- |
| [KMPLiquidGlass](https://github.com/Kashif-E/KMPLiquidGlass) | **Apache-2.0** | Liquid Glass (Backdrop), Copyright 2025 Kashif-E. This repo bundles the `:backdrop` module source together with local modifications; its original `LICENSE` is preserved at `libs/KMPLiquidGlass/LICENSE` |
| Compose Multiplatform / androidx.* | Apache-2.0 | UI framework |
| kotlinx.coroutines | Apache-2.0 | Coroutines |
| OkHttp | Apache-2.0 | Networking |
| Gson | Apache-2.0 | JSON |
| webrtc-sdk (android) | BSD-3-Clause | Voice calls |

See [`NOTICE`](./NOTICE) for full attributions.

---

## License

This project uses a **dual-licensing** model:

- **Free for noncommercial use**: code originally authored for this project is licensed under the
  [PolyForm Noncommercial License 1.0.0](./LICENSE). Any **noncommercial purpose** may use, modify and distribute it freely.
- **Commercial use requires a license**: for **commercial purposes** (including internal use within a company or organization),
  you must obtain written authorization from the author first.

> Required Notice: Copyright 2026 kelo

**Contact**: `kelo@lanxint.top`

> **Important**: `libs/KMPLiquidGlass/` is a third-party component licensed under **Apache-2.0** (which explicitly permits
> commercial use) and is **not** covered by this project's PolyForm Noncommercial terms. Other third-party dependencies
> follow their own original licenses.

---

## Contributing

Issues and pull requests are welcome. **Code contributions require signing the
[Contributor License Agreement (CLA)](./CLA.md) first** — because this project is dual-licensed, it needs full
relicensing rights in order to offer commercial licenses, so contributions without a signed CLA cannot be accepted.

(The CLA is currently available in Chinese; an English version can be provided on request.)

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details.

---

## Disclaimer

- This project is for learning and technical exchange only; use at your own risk.
- Please comply with the target service's terms of use; this project is not responsible for any consequences
  arising from the use of this software.
- The software is provided "as is", without warranty of any kind.
