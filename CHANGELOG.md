# 变更日志

本文件记录 **宝宝儿歌** 各版本相对上一版的用户可见变更。版本号与 `app/build.gradle.kts` 中 `versionName`、git tag `v*` 一致。

## [1.0.3] - 2026-04-08

### 新增

- **桌面小组件**：迷你播放控制（显示当前曲目与专辑名、上一首 / 播放暂停 / 下一首）；点击标题区域进入应用。
- **小组件样式**：薄荷绿系卡片风格；自定义矢量图标，避免系统默认图标发糊；`AppWidgetProvider` 注册与 `BroadcastReceiver` 转发播放控制，复用现有 `PlaybackController`。

### 变更

- **多档尺寸**：提供 **小 / 中 / 大** 三套 `RemoteViews` 布局；根据桌面给出的 `AppWidgetOptions` 自动切换；拖拽调整尺寸时通过 `onAppWidgetOptionsChanged` 刷新。
- **播放状态联动**：在 `PlaybackController` 状态变化时刷新小组件（与现有 UI 状态流一致）。

## [1.0.2] - 2026-04-07

### 变更

- **界面与主题**：单一柔和青绿主色、灰白层次与 **12～24dp** 圆角；专辑卡片、歌曲列表、正在播放与 **迷你播放条** 样式统一，顶栏使用系统图标（菜单/返回）。
- **歌曲列表**：**当前播放曲目** 以浅主色底与描边高亮（同专辑且与播放状态一致）。
- **系统栏可读性**：浅色主题下 **状态栏 / 导航栏** 使用深色系统图标（`windowLight*` 与运行时 `WindowInsetsController` 同步）；**页面背景** 与卡片表面拉开层次，**导航栏底色** 与内容区对齐，避免底部按键与背景融在一起。
- **正在播放**：居中布局、更大主播放键与胶囊形播放模式切换；进度条与主题主色一致。

## [1.0.1] - 2026-04-07

### 变更

- **歌单为主**：点击歌曲仅开始播放，**不再自动进入**「正在播放」全屏；全屏入口改为歌单顶栏 **「全屏」**。
- **迷你播放器**：进度条下显示 **当前时间 / 总时长**（未知时长显示 `--:--`）；增加 **播放模式** 切换（顺序 / 列表循环 / 单曲循环 / 随机），与全屏页逻辑一致；标题区不再误触进入全屏。
- 抽取共享逻辑 [`PlayModeUi.kt`](app/src/main/java/com/swqsv/babysongs/ui/PlayModeUi.kt)（`cyclePlayMode`、`playModeLabel`），供迷你栏与全屏页共用。

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

[1.0.3]: https://github.com/linyongliang2018/BabySongs/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/linyongliang2018/BabySongs/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/linyongliang2018/BabySongs/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/linyongliang2018/BabySongs/releases/tag/v1.0.0
