# 贡献指南

感谢你愿意为「零语 ZeroTalk 非官方第三方客户端」做贡献。

## 提交前必读：签署 CLA

本项目采用**双授权模式**（原创代码为 PolyForm Noncommercial License 1.0.0，商业用途需另行授权）。
为了保留向商业用户授权的权利，**所有代码贡献必须先签署 [贡献者许可协议（CLA）](./CLA.md)**。

签署方式：在 Pull Request 下回复：

> 我已阅读并同意 CLA.md，并在此签署本协议。

**未签署 CLA 的 Pull Request 不会被合并。** 只提交 Issue（反馈 bug、提建议）无需签署。

## 流程

1. Fork 本仓库，从 `main` 切出分支（建议命名：`fix/xxx`、`feat/xxx`）
2. 改动尽量小而聚焦，一个 PR 只做一件事
3. 提交前先本地构建通过（见下）
4. 向 `main` 发起 Pull Request，并在 PR 描述里说明**改了什么、为什么改、怎么验证**
5. 在 PR 下回复 CLA 签署语句

## 本地构建

需要 JDK 17 与 Android SDK（compileSdk / targetSdk 36，minSdk 23）：

```bash
./gradlew :composeApp:assembleDebug
```

> 发布包签名所需的 `keystore.properties` 与密钥库**不在仓库中**（安全原因）。
> 文件不存在时构建仍可正常进行，只是产出未签名的 release 包。

## 代码风格

- 跟随现有代码风格（Kotlin 官方风格），注释用中文
- 新增 UI 尽量复用 `ui/components` 下的现有组件，不要重复造轮子
- 涉及交互/视觉的改动，请在 PR 里附**截图或录屏**

## 不接受的内容

- 未签署 CLA 的代码
- 来源不明的代码（含从别处复制但未标注协议的片段）
- 与「非官方客户端」定位冲突的改动（例如声称代表官方）
