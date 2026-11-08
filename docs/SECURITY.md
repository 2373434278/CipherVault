# CipherVault 安全模型

本文档描述密匣 v1.0.0 的加密设计、密钥生命周期、防护边界与已知限制。当前版本未经过第三方安全审计。

## 1. 威胁模型

### 防护目标

- 设备丢失或离线被他人获得时，保险库内容不可读
- 应用进程被杀死、设备重启后，密钥不残留于磁盘
- 应用切后台、熄屏后按策略自动锁定
- 截屏、最近任务预览、剪贴板泄露风险可控
- 备份文件即使被获取，在不知道备份口令时不可读

### 不在防护范围

- 已 root、被解锁 Bootloader 或被植入恶意软件/键盘记录器的设备
- 能实时观察屏幕或输入的攻击者
- 弱主密码被离线暴力破解
- 云同步、Passkey/WebAuthn、泄露监控（v1 不提供）
- 通过其他应用无障碍服务读取界面的攻击

## 2. 密钥体系

```
主密码 (Char[])
  + 随机 salt (16B)
  → Argon2id (t=3, m=64MiB, p=2, 输出 32B) = KEK
      → AES-256-GCM 包裹随机 vaultKey (32B)
          → 包裹结果存入 DataStore (vault_meta)

vaultKey (32B, 仅解锁期间驻留内存)
  ├─ 作为 SQLCipher 数据库口令
  └─ 可选：由 Android Keystore 中要求生物识别的 AES-256-GCM 密钥再次包裹
```

关键约束：

- 不存储主密码、KEK 或明文 vaultKey
- 锁定时关闭 Room/SQLCipher 连接并清零内存中的 vaultKey 与 KEK
- 低内存设备新建库使用 Argon2id m=32MiB；参数随库持久化
- 若 Argon2 native 库不可用，仅对新建库回退 PBKDF2-HMAC-SHA256 600k；已有库按记录的 `kdf.id` 解锁，不会静默换算法
- 错误主密码在 AES-GCM 解包阶段失败；SQLCipher 打不开时提示“密码错误或数据损坏”，不会删除数据库

## 3. 生物识别

- Keystore 别名：`ciphervault_biometric_v1`
- AES-256-GCM，`setUserAuthenticationRequired(true)`
- API 30+：`setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`
- API 26–29：`setUserAuthenticationValidityDurationSeconds(-1)`，每次认证
- `setInvalidatedByBiometricEnrollment(true)`：录入新指纹/面容后旧密钥失效
- 认证通过后使用 `CryptoObject` 中的 Cipher 解包 vaultKey
- 密钥失效、认证取消或锁定超限时回退主密码；包裹密钥会被清除并需要重新启用

## 4. 数据库

- Room + SQLCipher for Android 4.19.1
- 数据库文件 `ciphervault.db`
- 口令为 vaultKey 字节数组；SQLCipher 内部生成数据库密钥
- 无明文索引；名称、用户名、密码、URL、备注、TOTP 密钥均在加密库内
- `allowBackup=false`，`dataExtractionRules` 排除云备份与设备迁移

## 5. TOTP

- RFC 6238：HMAC-SHA1/256/512，动态截断，6/8 位，30/60 秒
- 仅接受 `otpauth://totp/...`，拒绝 HOTP 与其他 scheme
- 二维码扫描使用 ZXing embedded，只解析 otpauth 内容
- TOTP 密钥随数据库加密存储，不单独落盘

## 6. 导入 / 导出

- 加密备份：独立备份口令 → 新 salt → Argon2id → AES-256-GCM；信封包含 KDF 参数与密文
- Bitwarden JSON、CSV 导入不写入外部服务
- CSV 明文导出前二次确认；CSV 包含未加密密码，应视为敏感文件
- 备份口令与主密码相互独立；遗失备份口令无法恢复

## 7. Autofill

- 仅填充用户名、密码与 OTP 字段
- 优先匹配 `webDomain` / 包名对应的条目；无匹配时回退到收藏条目
- 保险库锁定时，Dataset 携带认证 PendingIntent；认证成功后由系统重新发起填充请求。若系统未自动刷新面板，用户再次点击输入框即可重新请求
- 保存请求按域名与用户名去重
- 应用显式设置 `importantForAutofill=no` 时无法填充
- Autofill 下拉卡片由本应用 RemoteViews 渲染，仅包含条目名称与用户名，不展示密码

## 8. 运行时防护

- 敏感页面使用 `FLAG_SECURE`（默认开启，可在设置关闭）
- 剪贴板写入敏感标记（API 33+），默认 30 秒后仅当内容仍匹配时清除
- 离开应用后按配置自动锁定（默认 1 分钟；可设为立即/5/15/30 分钟/永不）
- 日志不输出主密码、vaultKey、TOTP 密钥或条目密码
- 明文备份与 CSV 导出前需要用户确认

## 9. 已知限制

- 主密码强度是离线暴力破解的唯一屏障；建议使用 4 个以上随机单词组成的长口令
- Argon2id 在低端设备上可能带来 1–2 秒解锁延迟
- 生物识别失效后必须使用主密码重新启用
- Autofill 的字段识别依赖应用提供的 hints/inputType，部分应用或 WebView 可能无法填充
- 截屏保护不能阻止相机拍摄屏幕
- 剪贴板清除是尽力而为，其他应用可能在此之前读取
- debug 构建未开启 R8，且使用默认 debug 签名；release 构建已开启 R8 与独立签名（见第 10 节）
- 仓库内的 release 签名密钥由本机 `keytool` 生成，属于临时密钥；正式分发前应替换为你自己保管的密钥，否则升级链路的信任锚点不受你控制
- 当前版本不提供云同步、Passkey、泄露检测与紧急访问

## 10. Release 构建与验证记录

### 构建配置

| 项 | 值 |
| --- | --- |
| `isMinifyEnabled` / `isShrinkResources` | true / true（R8 全模式） |
| 签名方案 | APK Signature Scheme v2 + v3（v1 已按 minSdk 26 关闭） |
| 证书 | `CN=CipherVault Release Key, OU=Mobile, O=CipherVault`，RSA 2048，SHA384withRSA，有效期 9125 天 |
| 证书 SHA-256 | `eb3e62feebe2b85ab75648e4561e1c96ddec9faf4dd998eb98756c1a0bdcb45a` |
| 体积 | 11.4 MB（debug 为 31.5 MB） |
| 产物 flags | `HAS_CODE ALLOW_CLEAR_USER_DATA`，无 `DEBUGGABLE` |

`app/proguard-rules.pro` 为 JNI / 反射 / 系统入口保留：SQLCipher、argon2kt、声明 native 方法的类、ZXing、Room `*_Impl`、`CipherVaultApp` / `MainActivity` / `VaultAutofillService` / `AutofillAuthActivity`、androidx.biometric。

### 离线核验

```
./gradlew :app:clean :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease   # exit 0
testReleaseUnitTest: 16 tests, 0 failures, 0 errors
lintRelease:         0 errors, 61 warnings, 4 hints
apksigner verify:    Verifies (v2=true, v3=true)
```

`mapping/release/resources.txt` 确认 `layout/autofill_dataset`、`xml/autofill_service` 与相关字符串未被资源收缩移除；APK 内 `lib/{arm64-v8a,armeabi-v7a,x86,x86_64}/libsqlcipher.so` 与 `libargon2*.so` 均在。

无 `keystore.properties` 时构建同样成功（exit 0），产物为 `app-release-unsigned.apk`，已实测。本机重复构建 `app-release.apk` 与交付包 SHA-256 一致（`a72bbd99…d4690e3`），不代表跨机器可复现。

### 实机验证（Android 16 / API 36 模拟器，release 包）

| 场景 | 结果 | 证据 |
| --- | --- | --- |
| 首次启动建库 | 通过 | Argon2id + Keystore 包裹 + SQLCipher 建库无异常 |
| 数据库确实加密 | 通过 | 首 16 字节为 `5a46b098c1445674…`，非 `SQLite format 3\0`；文件权限 `-rw-------` |
| 杀进程后主密码解锁 | 通过 | 重启进入锁屏，解锁后条目仍在 |
| TOTP 正确性 | 通过 | 设备出码 `317338`/`971850` 与独立实现的上一/当前时间窗逐位一致 |
| 条目新增·编辑·持久化 | 通过 | Room 读写经 SQLCipher，收藏分组生效 |
| 生成器 | 通过 | 随机密码与易记口令两种模式，特殊字符集未被裁剪 |
| 中英切换 | 通过 | `values-zh` 资源完整，Activity 重建后全界面为中文 |
| 自动锁定 | 通过 | 切后台超过设定时间后回到锁屏 |
| 截屏保护 | 通过 | `screencap` 得到纯色帧（约 20 KB / 1080×2400） |
| Autofill 端到端 | 通过 | `dumpsys autofill` 记录 `SERVICE=com.ciphervault.app, NUM_DATASETS=1`，响应含 `value=17_chars` 与 `value=11_chars`（与库中用户名/密码长度一致）及 `SaveInfo`；选择卡片后客户端两字段被正确填充 |
| 锁定态认证分支 | 通过 | 卡片「Unlock to autofill」渲染出自定义 `RemoteViews`，点击拉起 `AutofillAuthActivity` |
| 生物识别解锁 | **未在 release 包复测** | 见下 |

### 生物识别的验证缺口

release 包上只验证到 `BiometricManager.canAuthenticate()` 真实执行（`dumpsys` 侧出现 `BiometricService: canAuthenticate ... Package: com.ciphervault.app ... Status: 7`，即未注册指纹），未跑通「注册 → BiometricPrompt → CryptoObject 解包 vaultKey」全链路，原因是验证过程中清理测试锁屏 PIN（`locksettings clear`）连带删除了模拟器的指纹注册，重新注册需要走系统设置界面。

该链路在 debug 包上已完整验证过，release 侧的额外风险点是 `androidx.biometric` 被混淆，已通过 `-keep class androidx.biometric.** { *; }` 与上面 `canAuthenticate` 的实际调用记录间接排除。若需要严格结论，请在真机上完成一次生物识别注册与解锁。

### 环境侧发现

Android 16 模拟器默认让 Credential Manager 接管 Autofill 选择界面，表现为服务已返回数据集但 `Max visible datasets: 0`（面板不显示）。本次验证通过 `adb shell device_config put autofill autofill_credman_integration false` 临时关闭后复现了完整面板与填充。这是系统行为，debug 与 release 表现一致，不是 R8 造成的差异。

## 11. 报告问题

如发现安全问题，请不要公开披露漏洞细节。请通过项目维护者提供的私有渠道联系，并附上复现步骤、影响范围与设备信息。
