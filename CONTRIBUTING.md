# Contributing to CipherVault

Thanks for your interest. CipherVault is a local-first Android password manager; security-sensitive changes are reviewed with extra care.

## 开发环境

- JDK 21（Temurin 21 已验证）
- Android SDK Platform 36 + Build Tools 36.0.0
- Gradle 由 `./gradlew` 自动分发

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

本地安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 提交前检查

```bash
./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:assembleRelease
```

- 所有单元测试必须通过；新增逻辑需要配套测试。
- 不要提交 `keystore/`、`keystore.properties`、`local.properties`、APK 或 `dist/` 产物。
- 数据库结构变更必须提供显式 Room 迁移，禁止 destructive migration。
- 新增依赖需为 Apache-2.0 / MIT / BSD 等宽松许可，并在 PR 中说明用途与许可证。

## 代码风格

- Kotlin 官方风格（`kotlin.code.style=official`），Compose 优先。
- 界面文案同时提供 `values/strings.xml`（中文）与 `values-en/strings.xml`（英文）。
- 面向安全相关改动（密钥派生、加解密、Autofill 数据暴露）请在 PR 描述里写明威胁模型影响。

## 分支与 PR

1. 从 `main` 切出功能分支。
2. PR 描述说明动机、改动范围与验证方式（测试/实机截图）。
3. 保持 PR 聚焦，避免顺带重构无关代码。

Commit message 建议使用 `feat:` / `fix:` / `docs:` / `build:` / `test:` / `chore:` 前缀。

## 发布流程

发布由 GitHub Actions 自动完成：

1. 更新版本相关说明并提交。
2. 打 tag 并推送：

   ```bash
   git tag v1.1.0
   git push origin v1.1.0
   ```

3. `.github/workflows/release.yml` 会用 tag 推导 `versionName`（`1.1.0`）与 `versionCode`（`major*10000 + minor*100 + patch`），构建 APK、生成 SHA-256 校验和、压缩 R8 mapping，并创建 GitHub Release。

### 发布签名（可选）

在没有配置 Secrets 时，Release 流程产出未签名 APK。要产出可覆盖安装的签名包，在仓库 Settings → Secrets and variables → Actions 中添加：

| Secret | 内容 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `base64 -i keystore/release.jks \| pbcopy` 的结果 |
| `ANDROID_KEYSTORE_PASSWORD` | 密钥库口令 |
| `ANDROID_KEY_ALIAS` | 密钥别名 |
| `ANDROID_KEY_PASSWORD` | 密钥口令 |

生成密钥：

```bash
keytool -genkeypair -keystore keystore/release.jks -alias ciphervault \
  -keyalg RSA -keysize 2048 -validity 9125 -dname "CN=CipherVault Release Key"
```

**务必备份密钥库与口令。** 丢失后无法用同一密钥发布升级包，已安装用户只能卸载重装（保险库数据会丢失）。

## 安全问题

请不要在公开 Issue 中披露未修复的漏洞，报告方式见 [docs/SECURITY.md](docs/SECURITY.md)。
