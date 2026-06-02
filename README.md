<div align="center">

# ✦ Aura

**地图驱动的 Android 体验 · MVVM · Mapbox · Retrofit**

<br/>

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Mapbox](https://img.shields.io/badge/Mapbox-000000?style=for-the-badge&logo=mapbox&logoColor=white)](https://www.mapbox.com/)
[![Retrofit](https://img.shields.io/badge/Retrofit-49C838?style=for-the-badge&logo=swagger&logoColor=white)](https://square.github.io/retrofit/)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)

<br/>

`com.catclaw.aura` · minSdk **35** · targetSdk **36** · AGP **9.2** · Gradle **9.4**

[快速开始](#-快速开始) · [架构](#-架构一览) · [文档](#-文档) · [命令](#-命令速查)

</div>

---

```text
     _                    _____ _
    / \   _ __ _   _  ___|  ___| | _____      __
   / _ \ | '__| | | |/ _ \ |_  | |/ _ \ \ /\ / /
  / ___ \| |  | |_| |  __/  _| | | (_) \ V  V /
 /_/   \_\_|   \__,_|\___|_|   |_|\___/ \_/\_/
```

> **Aura** — 地图驱动的 Android 客户端。  
> 单 Activity 多 Fragment · 全屏 Mapbox 地图 · 统一网络回调 · Room 就绪。

---

## ✨ 亮点

<table>
<tr>
<td width="50%">

### 🗺️ 沉浸式地图

Mapbox Maps SDK **11.x**  
`MapFragment` + 可配置相机  
Edge-to-edge 全屏体验

</td>
<td width="50%">

### ⚡ 统一网络层

Retrofit + OkHttp  
多 Base URL · 拦截器链  
`HttpCallback` 一行发起 GET / POST

</td>
</tr>
<tr>
<td>

### 🧱 MVVM 脚手架

`BaseViewModel` · `BaseFragment`  
`StateFlow` / `SharedFlow`  
按 Feature 分包，易扩展

</td>
<td>

### 📚 中文文档

Mapbox 官方指南译本  
NetworkClient 完整示例  
Agent 协作约定 `AGENTS.md`

</td>
</tr>
</table>

---

## 🛠 技术栈

| | |
|:---:|:---|
| **语言** | Kotlin |
| **UI** | XML · ViewBinding · Material |
| **架构** | MVVM · 单 Activity + Fragment |
| **地图** | Mapbox Maps SDK 11.x |
| **网络** | Retrofit 2 · OkHttp 4 · Coroutines |
| **存储** | Room（runtime 已接入） |
| **构建** | Version Catalog · AGP 9.2 |

---

## 🚀 快速开始

<details open>
<summary><b>展开 · 四步跑起来</b></summary>

<br/>

**① 克隆**

```bash
git clone <repo-url> && cd Aura
```

**② 配置密钥** — 编辑项目根目录 `local.properties`：

```properties
sdk.dir=/path/to/Android/Sdk
MAPBOX_ACCESS_TOKEN=pk.你的Mapbox公钥
```

> Token 经 Gradle `resValue` 注入，不会进 Git。  
> 获取地址：[Mapbox Access Tokens](https://account.mapbox.com/access-tokens/)

**③ 编译**

```bash
./gradlew assembleDebug
```

**④ 安装**

| 方式 | 路径 / 操作 |
|------|-------------|
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| IDE | Android Studio → Run **app** |
| 设备 | API **35+** 真机或模拟器 |

</details>

---

## 🏗 架构一览

```mermaid
flowchart TB
    subgraph UI["🖥 UI Layer"]
        MA[MainActivity<br/>Fragment 容器]
        MF[MapFragment]
        MVM[MapViewModel]
        MA --> MF --> MVM
    end

    subgraph Data["💾 Data Layer"]
        NC[NetworkClient<br/>Retrofit + OkHttp]
        RM[(Room<br/>待定义 Entity)]
    end

    subgraph App["⚙️ Application"]
        AA[AuraApplication]
    end

    AA -->|init| NC
    MVM -.->|Repository 扩展| NC
    MVM -.->|Repository 扩展| RM
    MF -->|MapView| MB[("Mapbox SDK")]
```

**扩展新页面**

```
ui/<feature>/
  ├── FeatureFragment.kt
  ├── FeatureViewModel.kt
  ├── FeatureUiState.kt
  └── FeatureUiEvent.kt

MainActivity.showFeatureFragment()  →  replaceFragment()
```

---

## 📁 项目结构

```
Aura/
├── 📱 app/
│   ├── 📄 docs/network.md
│   └── src/main/java/com/catclaw/aura/
│       ├── AuraApplication.kt
│       ├── MainActivity.kt
│       ├── data/network/      ← NetworkClient
│       ├── data/local/        ← Room
│       └── ui/
│           ├── base/
│           └── map/
├── 📖 docs/mapbox/            ← Mapbox 中文指南
├── gradle/libs.versions.toml
└── AGENTS.md
```

---

## 📚 文档

| | 链接 | 内容 |
|:---:|:---|:---|
| 🌐 | [**NetworkClient 手册**](./app/docs/network.md) | GET / POST · 多 Base URL · 测试 API |
| 🗺️ | [**Mapbox 指南**](./docs/mapbox/README.md) | SDK 安装 · 样式 · 标注 · 定位 |
| 🤖 | [**AGENTS.md**](./AGENTS.md) | 包结构 · 约定 · 构建命令 |

---

## ⌨️ 命令速查

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew test                   # 单元测试
./gradlew connectedAndroidTest   # 仪器化测试（需设备）
```

---

## 🔒 安全

```diff
- 切勿提交 local.properties（含 MAPBOX_ACCESS_TOKEN）
- 切勿提交 *.jks / *.keystore
+ Token 仅保存在本地 local.properties
+ 网络日志仅在 Debug 构建开启
```

---

## 🔗 外部资源

| 资源 | 用途 |
|------|------|
| [Mapbox Android Docs](https://docs.mapbox.com/android/maps/guides/) | 官方 SDK 文档 |
| [JSONPlaceholder](https://jsonplaceholder.typicode.com/) | 网络层零配置测试 API |

---

<div align="center">

<br/>

**Aura** — flow with the map.

<br/>

</div>
