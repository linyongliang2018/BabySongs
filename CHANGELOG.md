# 变更日志

本文件记录 **宝宝儿歌** 各版本相对上一版的用户可见变更。版本号与 `app/build.gradle.kts` 中 `versionName`、git tag `v*` 一致。

## [1.0.0] - 2026-04-07

### 新增

- 通过 Android `ACTION_OPEN_DOCUMENT_TREE` 授权文档根目录，递归扫描 **mp3 / mp4**，每个授权根目录对应一张专辑。
- 专辑列表、歌曲列表、正在播放界面；迷你播放器条；播放模式：顺序、列表循环、单曲循环、随机。
- 基于 **Media3 / ExoPlayer** 的播放与 **MediaSession**；前台播放服务 `BabyPlaybackService` 在启动后 **立即** `startForeground`，符合系统时限，避免 `ForegroundServiceDidNotStartInTimeException` 闪退。
- **歌曲索引缓存**（应用私有目录 JSON）：二次进入大目录时秒开列表；专辑页先展示再后台刷新扫描；时长元数据分批补全，减轻卡顿。
- 播放进度与模式等持久化（DataStore）；侧滑菜单管理多个文档根目录。

### 说明

- 本仓库 **无 `INTERNET` 权限**，不联网传输媒体。
- 更细的联调步骤见 [RUNBOOK.md](./RUNBOOK.md)。

[1.0.0]: https://github.com/linyongliang2018/BabySongs/releases/tag/v1.0.0
