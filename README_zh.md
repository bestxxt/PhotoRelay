# PhotoRelay 📸

[English Version](README.md)

**PhotoRelay** 是一款专门为了打通照片备份工作流而诞生的“桥接”应用。

## 为什么开发这个软件？

开发的初衷非常简单：我手头有一台闲置的 Pixel 3 手机，它享有 **无限容量的 Google Photos 原画质备份特权**。但我日常使用的是 iPhone，所有的照片都产生并保存在 iPhone 上。

为了数据安全和方便管理，我平时使用自己部署的 [Immich](https://immich.app/) 作为热备中心（第一道备份）。但我同时也希望把这些照片白嫖到 Google Photos 里作为异地灾备。为了能自动把照片从 iPhone 流转到 Immich，然后再流转到 Google Photos 里，我开发了这款软件。

**整个照片流转链路如下：**
`iPhone` ➔ `Immich 服务器` ➔ **`[Pixel 3 上的 PhotoRelay]`** ➔ `Google Photos 云端`

PhotoRelay 安装在 Pixel 3 上，它会在后台默默运行，自动去我的 Immich 服务器上拉取最新上传的照片，并下载到 Pixel 3 的本地相册里。接着，Pixel 3 系统自带的 Google Photos 就会接管剩下的工作，把照片以原画质免费上传到谷歌云盘中。

## 核心工作原理
- **全自动同步：** 在后台定期从 Immich 拉取新照片，完全不需要人工干预。
- **专为备机打造：** 非常适合装在一直插着电放在家里的老款 Pixel 手机上，默默工作。
- **自动清理空间：** 支持配置本地存留天数，等照片被安全上传到 Google Photos 后，应用会自动清理手机本地的老照片，防止 Pixel 存储空间被撑爆。

简单来说，如果您也有一台可以免流备份的初代 Pixel 设备，并且同样在使用 Immich，那这就是帮您全自动化白嫖谷歌羊毛的绝佳拼图！
