# PhotoRelay 📸

[中文版 (Chinese Version)](#photorelay-中文说明)

PhotoRelay is an Android application designed to seamlessly and automatically synchronize your photos from an [Immich](https://immich.app/) server in the background.

## Key Features

- **Automated Background Sync:** Uses Android's `WorkManager` to routinely check for and download new photos from your Immich server, even when the app is closed.
- **Easy Authentication:** Secure login using Immich Server URL and credentials. Tokens and URLs are safely cached via `DataStore`.
- **Customizable Intervals:** Set your own sync timeframes and frequencies. You can choose to download photos from the last 7 days, 30 days, or everything.
- **Detailed Sync Logs:** A built-in log viewer allows you to track exactly when background syncs occur and see the results of each attempt.
- **Auto-Cleanup (Optional):** Define rules to automatically remove locally cached files after a certain number of days to save storage space.

## Architecture

This project is built with modern Android development standards:
- **UI:** Jetpack Compose (Material Design 3)
- **Architecture:** MVVM (Model-View-ViewModel) pattern
- **Concurrency & Background:** Kotlin Coroutines & Android WorkManager
- **Networking:** Retrofit + OkHttp
- **Local Storage:** Preferences DataStore

---

# PhotoRelay 中文说明 📸

PhotoRelay 是一款专为安卓设备打造的应用，它能在后台自动且无缝地将您的照片从 [Immich](https://immich.app/) 服务器同步到本地。

## 核心功能

- **后台自动同步：** 利用安卓原生的 `WorkManager`，即使在应用关闭的情况下，也能在后台自动检查并下载 Immich 服务器上的新照片。
- **便捷登录：** 通过 Immich 服务器地址和账号安全登录，Token 和常用设置均通过 `DataStore` 安全缓存，支持免密快速重登。
- **自定义同步范围：** 自由设置拉取频率及时间范围（如：拉取最近 7 天、30 天或是全量照片）。
- **详细的运行日志：** 内置日志查看器，方便您随时掌握后台同步任务的触发时间、下载数量以及报错详情。
- **自动空间清理：** 支持配置本地照片留存天数，超时照片自动清理，告别存储空间焦虑。

## 技术栈

本项目采用了现代化的安卓开发标准构建：
- **UI 界面:** Jetpack Compose (Material 3)
- **架构模式:** MVVM (Model-View-ViewModel)
- **异步与后台:** Kotlin 协程 (Coroutines) 与 Android WorkManager
- **网络请求:** Retrofit + OkHttp
- **本地存储:** Preferences DataStore
