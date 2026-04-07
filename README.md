# 宝宝儿歌（BabySongs）

本地儿歌播放器：**不联网**，通过系统「文档树」授权读取你自选文件夹中的 **mp3 / mp4**（仅播音频轨），按目录组织专辑并播放。

## 环境要求

- Android Studio（推荐最新稳定版）
- **JDK 17**（与 Android Gradle Plugin 要求一致）
- 调试/安装设备：**Android 12（API 31）及以上**

## 构建与安装

```bash
./gradlew assembleDebug
```

或在 Android Studio 中打开工程根目录后点击 **Run**。

## 使用说明摘要

1. 首次在应用内 **添加儿歌目录**（系统文件夹选择器），可多次添加；每个授权根目录对应一张专辑，子文件夹内音频会递归归入。
2. 授予 **媒体读取** 与（Android 13+）**通知** 等权限，以便扫描与媒体控制通知。
3. 详细联调、权限与功能清单见仓库内 **[RUNBOOK.md](./RUNBOOK.md)**。

## 版本说明

当前版本见 **应用内关于 / `app/build.gradle.kts` 中 `versionName`**，完整变更记录见 **[CHANGELOG.md](./CHANGELOG.md)**。

## 许可证

若未另行声明，以仓库内文件为准；用于个人/家庭学习娱乐请遵守当地版权法，仅播放你有权使用的音频文件。
