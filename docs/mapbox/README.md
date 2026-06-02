# Mapbox Maps SDK for Android — 中文指南

本目录为 [Mapbox Android Maps 官方指南](https://docs.mapbox.com/android/maps/guides/) 的中文译本，便于在 AuraFlow 项目中集成地图能力。

**当前 SDK 版本（文档基准）**：v11.24.3

## 阅读顺序建议

| 顺序 | 文档 | 官方链接 |
|------|------|----------|
| 1 | [概述](./00-概述.md) | [guides](https://docs.mapbox.com/android/maps/guides/) |
| 2 | [安装与入门](./01-安装与入门.md) | [install](https://docs.mapbox.com/android/maps/guides/install/) |
| 3 | [地图样式](./02-地图样式.md) | [styles](https://docs.mapbox.com/android/maps/guides/styles/) |
| 4 | [设置样式](./03-设置样式.md) | [set-a-style](https://docs.mapbox.com/android/maps/guides/styles/set-a-style/) |
| 5 | [源与图层](./04-源与图层.md) | [work-with-layers](https://docs.mapbox.com/android/maps/guides/styles/work-with-layers/) |
| 6 | [添加数据概述](./05-添加数据概述.md) | [add-your-data](https://docs.mapbox.com/android/maps/guides/add-your-data/) |
| 7 | [Markers 标记](./06-Markers标记.md) | [markers](https://docs.mapbox.com/android/maps/guides/add-your-data/markers/) |
| 8 | [Annotations 标注](./07-Annotations标注.md) | [annotations](https://docs.mapbox.com/android/maps/guides/add-your-data/annotations/) |
| 9 | [View Annotations](./08-View-Annotations视图标注.md) | [view-annotations](https://docs.mapbox.com/android/maps/guides/add-your-data/view-annotations/) |
| 10 | [Style Layers](./09-Style-Layers样式图层.md) | [style-layers](https://docs.mapbox.com/android/maps/guides/add-your-data/style-layers/) |
| 11 | [相机位置](./10-相机位置.md) | [camera](https://docs.mapbox.com/android/maps/guides/camera-and-animation/camera/) |
| 12 | [相机动画](./11-相机动画.md) | [animations](https://docs.mapbox.com/android/maps/guides/camera-and-animation/animations/) |
| 12 | [用户位置](./12-用户位置.md) | [user-location](https://docs.mapbox.com/android/maps/guides/user-location/) |
| 14 | [权限处理](./14-权限处理.md) | [permissions](https://docs.mapbox.com/android/maps/guides/user-location/permissions/) |
| 15 | [设备定位](./15-设备定位.md) | [device-location](https://docs.mapbox.com/android/maps/guides/user-location/device-location/) |
| 16 | [地图上显示位置](./16-地图上显示位置.md) | [location-on-map](https://docs.mapbox.com/android/maps/guides/user-location/location-on-map/) |
| 17 | [用户交互与手势](./17-用户交互与手势.md) | [gestures](https://docs.mapbox.com/android/maps/guides/gestures/) |

## 与 AuraFlow 的兼容性提示

| 项目现状 | Mapbox 要求 | 说明 |
|----------|-------------|------|
| `minSdk` 35 | minSdk ≥ 21 | 满足 |
| Java 11 | Java 8+ | 满足 |
| XML + `AppCompatActivity` | 支持 View 与 Compose | 可按 View 路径集成 |
| 无 Compose | Markers 仅 Compose | 需启用 Compose 或使用 Annotation / Style Layer |

## 必备账号与密钥

1. 注册 [Mapbox 账号](https://account.mapbox.com/)
2. 创建 **Public Access Token**（以 `pk.` 开头）
3. 在根目录 `local.properties` 中设置 `MAPBOX_ACCESS_TOKEN`（见 [安装指南](./01-安装与入门.md)）

## 免责声明

- 译文基于官方文档整理，如有 API 变更请以英文官网为准。
- 代码示例保留官方写法；包名需替换为 `com.catclaw.aura`。
- Mapbox 使用需遵守 [归属说明](https://docs.mapbox.com/help/getting-started/attribution/) 与计费条款。
