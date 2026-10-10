# 零语 ZeroTalk · 非官方第三方客户端

> 面向「零语 / ZeroTalk」(app.zerotalk.cn) 的 **非官方** Android 客户端，基于 Kotlin Multiplatform + Compose Multiplatform。

**中文** | [English](./README.en.md)

---

## ⚠️ 非官方声明

本仓库是面向「零语 / ZeroTalk」的 **非官方第三方客户端**，由社区开发者独立编写，**与 ZeroTalk 官方及其运营方无任何关联**，
未获得其授权、认可或支持。相关服务名称、商标、接口与内容均归其各自权利人所有。
本项目仅供学习与技术交流使用，使用者请自行遵守目标服务的使用条款。

This is an **UNOFFICIAL** third-party client. It is not affiliated with, authorized, endorsed, or supported by the ZeroTalk service or its operators.

---

## 功能

- **动态**：精选 / 关注 / 我的，支持发布、点赞、评论、分享、举报、置顶、可见性
- **捞动态**：捞取与历史记录（同样支持举报）
- **暗号房（群聊）**：成员网格列表、禁止加入名单、管理员权限、移出成员
- **私聊 / 匹配**：文本、图片、语音消息
- **语音通话**：基于 WebRTC
- **对端备注**：本地 `uid → 备注` 映射，会话名与群聊气泡同步显示
- **会话背景**：仅存本机、按会话独立，可设为全局默认背景，支持离线显示
- **系统通知**：消息通知 + 可选的后台保活（可在「我的 → 消息与通知」关闭）
- **液态玻璃视觉**：支持「兼容渲染模式」降级到低性能设备

---

## 构建

### 环境要求

| 项 | 版本 |
| --- | --- |
| JDK | 17 |
| Android SDK | compileSdk / targetSdk 36，minSdk 23 |
| Gradle | 使用仓库自带的 Wrapper |

### 命令

```bash
# 调试包（产物：composeApp/build/outputs/apk/debug/composeApp-debug.apk）
./gradlew :composeApp:assembleDebug

# 发布包（需自行配置签名）
./gradlew :composeApp:assembleRelease
```

> Windows 下把 `./gradlew` 换成 `gradlew.bat`。首次构建需要联网拉取依赖。

---

## 项目结构

```text
composeApp/             Android 应用（KMP：commonMain 共享逻辑 / androidMain 平台实现）
libs/KMPLiquidGlass/    内嵌的液态玻璃库，仅保留 :backdrop 模块（见「第三方组件」）
gradle/                 版本目录（libs.versions.toml）
```

---

## 第三方组件

| 组件 | 协议 | 说明 |
| --- | --- | --- |
| [KMPLiquidGlass](https://github.com/Kashif-E/KMPLiquidGlass) | **Apache-2.0** | 液态玻璃（Backdrop）实现，Copyright 2025 Kashif-E。本仓库内嵌其 `:backdrop` 模块源码，并含本地修改（兼容渲染运行时总闸、Gradle 9 适配）；其原始 `LICENSE` 已原样保留在 `libs/KMPLiquidGlass/LICENSE` |
| Compose Multiplatform / androidx.* | Apache-2.0 | UI 框架 |
| kotlinx.coroutines | Apache-2.0 | 协程 |
| OkHttp | Apache-2.0 | 网络 |
| Gson | Apache-2.0 | JSON |
| webrtc-sdk (android) | BSD-3-Clause | 语音通话 |

完整署名见 [`NOTICE`](./NOTICE)。

---

## 下载

从 [Releases](https://github.com/keuio/zerotalk-android/releases) 下载**已签名的 APK** 直接安装（Android 6.0 / API 23 及以上）。

> 首次安装前请确认来源为本仓库；APK 签名证书 SHA-256：`576adcaafea97432944c4817e1feba9baaa4b81f325ac43dcaafd129c7241892`

## 贡献

欢迎提交 Issue 与 Pull Request。**代码贡献需先签署 [贡献者许可协议（CLA）](./CLA.md)** ——
本项目采用双授权模式，需要完整的再授权权利才能向商业用户提供许可，因此不接受未签署 CLA 的代码贡献。

详细流程见 [CONTRIBUTING.md](./CONTRIBUTING.md)。

## 授权

本项目采用 **双授权** 模式：

- **非商用免费**：本仓库中由本项目作者原创的代码，按 [PolyForm Noncommercial License 1.0.0](./LICENSE) 授权，
  任何**非商业目的**均可自由使用、修改与分发。
- **商用需授权**：如需**商业用途**（含在公司或组织内部使用），请先取得作者的书面授权。

> Required Notice: Copyright 2026 kelo

**联系方式**：`kelo@lanxint.top`

> **重要**：`libs/KMPLiquidGlass/` 属第三方组件，其授权为 **Apache-2.0**（明确允许商用），
> **不受**本项目 PolyForm Noncommercial 条款约束。其余第三方依赖同样遵循各自的原始协议。

---

## 免责声明

- 本项目仅供学习与技术交流，使用者需自行承担使用风险。
- 与目标服务交互时请遵守其服务条款；本项目不对因使用本软件产生的任何后果负责。
- 软件按「现状」提供，不提供任何明示或暗示的担保。
