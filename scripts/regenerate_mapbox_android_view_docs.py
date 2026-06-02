#!/usr/bin/env python3
"""Regenerate markdown from scraped JSON with GitHub source code and proper Chinese."""

from __future__ import annotations

import json
import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
URLS_FILE = ROOT / "scripts" / "mapbox_examples_urls.json"
DATA_FILE = ROOT / "scripts" / "mapbox_examples_data.json"
OUT_DIR = ROOT / "docs" / "mapbox" / "examples" / "android-view"
IMG_DIR = OUT_DIR / "images"

TITLE_ZH = {
    "display-a-map-view": "显示地图",
    "work-with-the-standard-style": "使用 Standard 样式",
    "display-the-users-location": "显示用户位置",
    "draw-a-polygon": "绘制多边形",
    "dynamic-view-annotations": "动态 View Annotation",
    "change-the-maps-style": "切换地图样式",
    "creating-a-map-view": "创建 MapView",
    "offline-map": "离线地图",
    "using-camera-animations": "相机动画",
    "view-annotations-basic-example": "View Annotation 基础示例",
    "add-interaction-to-custom-featuresets": "自定义 Featureset 交互",
    "add-interaction-to-featuresets": "预定义 Featureset 交互",
    "raster-particles": "栅格粒子图层",
    "add-a-marker-symbol": "添加 Symbol 标记",
    "add-point-annotations": "添加点标注",
    "display-a-globe": "显示地球投影",
    "display-3d-buildings-with-3d-lights": "3D 建筑与 3D 光照",
    "adjust-layer-order": "调整图层顺序",
    "3D-model-layer": "3D 模型图层",
    "patch-image": "9-patch 图片",
    "add-a-fill-polygon-annotation": "添加面标注",
    "add-line-3D-terrain": "3D 地形上的路线",
    "add-a-sky-layer": "添加天空图层",
    "add-animated-weather-data": "动画天气数据",
    "add-circle-annotations": "添加圆标注",
    "add-cluster-symbol-annotations": "Symbol 聚合标注",
    "add-markers-to-map": "添加多种图标标记",
    "add-polylines-annotations": "添加折线标注",
    "advanced-viewport-with-gestures": "高级 Viewport 与手势",
    "animate-a-line": "折线动画",
    "animate-marker-position": "标记位置动画",
    "animate-point-annotation": "点标注动画",
    "animated-textureview": "TextureView 动画",
    "basic-pulsing-circle": "基础脉冲圆",
    "cluster-points-within-a-layer": "图层内点聚合",
    "continue-camera-animation-during-gestures": "手势期间继续相机动画",
    "create-a-rotating-globe": "旋转地球",
    "create-a-snapshot": "创建地图快照",
    "simple-geofencing": "基础地理围栏",
    "custom-attribution": "自定义归属信息",
    "custom-layer": "自定义图层（Kotlin）",
    "dds-mapsnapshotter": "DDS MapSnapshotter",
    "debug-mode": "调试模式",
    "display-3d-buildings": "显示 3D 建筑",
    "display-multiple-icon-images-in-a-symbol-layer": "Symbol 图层多图标",
    "distance-expression": "距离表达式",
    "draw-a-geojson-line": "绘制 GeoJSON 线",
    "draw-a-polygon-with-holes": "绘制带洞多边形",
    "draw-multiple-geometries": "绘制多种几何体",
    "dsl-styling": "DSL 样式",
    "extended-geofencing": "扩展地理围栏",
    "external-vector-source": "外部矢量源",
    "feature-state": "Feature State",
    "fly-to-camera-animation": "FlyTo 相机动画",
    "geojson-performance": "GeoJSON 性能",
    "gestures": "手势配置",
    "inset-map": "Inset 小地图",
    "interactive-3d-model-source": "交互式 3D 模型源",
    "legacy-offline-map": "旧版离线地图",
    "line-behind-moving-icon": "移动图标后的线",
    "line-gradient": "线渐变",
    "local-style-mapsnapshotter": "本地样式 MapSnapshotter",
    "localization-plugin": "本地化插件",
    "location-component": "定位组件",
    "location-component-animation": "定位组件动画",
    "map-fragment": "Map Fragment",
    "map-overlay": "地图 Overlay",
    "mapbox-recorder": "Mapbox Recorder",
    "mapbox-studio-style": "Mapbox Studio 样式",
    "mapsurface": "MapSurface",
    "mapview-snapshot": "MapView 快照",
    "multi-display": "多屏显示",
    "multi-map-fragments": "多 Map Fragment",
    "viewpager": "ViewPager 多地图",
    "native-custom-layer": "Native 自定义图层（C++）",
    "ornaments": "装饰控件（Ornaments）",
    "raster-colorization": "栅格着色",
    "raw-expression": "Raw Expression",
    "raw-geojson": "Raw GeoJSON",
    "raw-source-layer": "Raw Source/Layer",
    "recyclerview": "RecyclerView 中的 MapView",
    "restrict-map-panning": "限制地图平移",
    "runtime-styling": "运行时样式",
    "scale-bar": "比例尺",
    "using-color-theme": "设置地图颜色主题",
    "show-and-hide-layers": "显示/隐藏图层",
    "simulate-a-navigation-route": "模拟导航路线",
    "sky-snapshotter": "Sky Snapshotter",
    "space-station-current-location": "空间站实时位置",
    "style-circles-categorically": "分类着色圆点",
    "textureview": "TextureView 渲染",
    "tilejson": "TileJSON 栅格源",
    "tint-a-fill-layer": "Fill 图层着色",
    "transparent-background": "透明背景",
    "triangle-custom-layer": "三角形自定义图层",
    "use-3d-terrain": "3D 地形",
    "use-an-image-source": "Image Source",
    "using-custom-camera-animations": "自定义相机动画",
    "vector-tile-source": "矢量瓦片源",
    "view-annotation-as-infowindow": "View Annotation 作为 InfoWindow",
    "view-annotation-with-point-annotation": "View Annotation 与点标注",
    "view-annotations-advanced-example": "View Annotation 高级示例",
    "view-annotations-animation": "View Annotation 动画",
    "viewport-camera": "Viewport 相机",
    "visualize-data-as-a-heatmap": "热力图",
    "within-expression": "Within 表达式",
    "wms-source": "WMS 源",
}

SUMMARY_ZH = {
    "display-a-map-view": "创建并显示使用 Mapbox Standard 默认样式的地图。初始化 `MapView` 作为 Activity 内容，并通过 `setCamera()` 设置中心点与缩放级别。",
    "work-with-the-standard-style": "演示如何使用 Standard 样式导入，并在运行时动态修改主题、光照预设、标签与 3D 对象等配置。",
    "display-the-users-location": "在地图上显示用户当前位置，使用默认的定位 puck（LocationComponent）。",
    "draw-a-polygon": "在地图上绘制矢量多边形（FillLayer + GeoJSON 数据源）。",
    "dynamic-view-annotations": "向线图层和固定坐标添加动态 View Annotation。",
    "change-the-maps-style": "在同一 `MapView` 上切换自定义样式与 Mapbox 默认样式。",
    "creating-a-map-view": "演示如何通过 XML 或代码自定义 `MapView` 的创建与配置。",
    "offline-map": "使用 `OfflineManager` 下载地图瓦片，实现离线地图。",
    "using-camera-animations": "使用 `setCamera()` 实现相机位置动画。",
    "view-annotations-basic-example": "在地图点击位置添加 View Annotation。",
    "add-interaction-to-custom-featuresets": "在 Mapbox Standard 样式中为自定义 Featureset 添加点击等交互。",
    "add-interaction-to-featuresets": "高亮 Mapbox Standard 样式中预定义的 POI（兴趣点）并添加交互。",
    "raster-particles": "向地图添加栅格粒子图层（Raster Particle Layer）。",
    "add-a-marker-symbol": "向样式添加蓝色水滴形 marker 图片，并通过 `SymbolLayer` 显示在地图上。",
    "add-point-annotations": "在地图上显示点标注（Point Annotation）。",
    "display-a-globe": "使用 globe 投影创建地球视图地图。",
    "display-3d-buildings-with-3d-lights": "在 Standard 样式中挤出 3D 建筑层，并配置 3D 光照位置。",
    "adjust-layer-order": "将指定图层插入到其他图层的上方或下方。",
    "3D-model-layer": "演示 3D 模型图层（Model Layer）的用法。",
    "patch-image": "向样式添加 9-patch 拉伸图片。",
    "add-a-fill-polygon-annotation": "在地图上显示面标注（Fill/Polygon Annotation）。",
    "add-line-3D-terrain": "在 3D 地形上展示路线，并播放路线动画。",
    "add-a-sky-layer": "添加可定制的天空图层，配合 Terrain 模拟自然光照。",
    "add-animated-weather-data": "使用 ImageSource 加载栅格图像，通过 RasterLayer 显示动画天气数据。",
    "add-circle-annotations": "在地图上显示圆标注（Circle Annotation）。",
    "add-cluster-symbol-annotations": "以聚合方式显示华盛顿特区消防栓等 Symbol 标注。",
    "add-markers-to-map": "添加使用不同图标的 marker。",
    "add-polylines-annotations": "在地图上显示折线标注（Polyline Annotation）。",
    "advanced-viewport-with-gestures": "高级 Viewport 与手势联动演示。",
    "animate-a-line": "对折线进行动态更新动画。",
    "animate-marker-position": "动画更新 marker/annotation 的位置。",
    "animate-point-annotation": "对地图上的 Point Annotation 进行动画。",
    "animated-textureview": "在 TextureView 上应用 View 动画。",
    "basic-pulsing-circle": "显示 LocationComponent 默认的脉冲圆效果。",
    "cluster-points-within-a-layer": "从 GeoJSON 源创建 CircleLayer 并对点进行聚合，聚合随相机变化更新。",
    "continue-camera-animation-during-gestures": "在用户手势操作期间继续播放相机动画。",
    "create-a-rotating-globe": "将地图显示为可交互、可旋转的地球。",
    "create-a-snapshot": "生成指定相机位置的静态地图快照（Bitmap）。",
    "simple-geofencing": "演示 Geofencing API 基础用法，在用户位置周围创建地理围栏。",
    "custom-attribution": "自定义地图归属（Attribution）显示。",
    "custom-layer": "使用 Kotlin 实现自定义图层（Custom Layer）。",
    "dds-mapsnapshotter": "结合数据驱动样式（DDS）生成静态地图图片。",
    "debug-mode": "切换地图的各种调试模式。",
    "display-3d-buildings": "在 Mapbox Light 样式中使用 FillExtrusionLayer 挤出 3D 建筑并设置光照。",
    "display-multiple-icon-images-in-a-symbol-layer": "向 SymbolLayer 添加多点与多图标，用 switchCase/get 表达式按属性选择图标。",
    "distance-expression": "使用 distance 表达式过滤并显示 POI。",
    "draw-a-geojson-line": "通过 GeoJsonSource 加载折线，用 LineLayer 显示在地图上。",
    "draw-a-polygon-with-holes": "添加带内洞的多边形图层。",
    "draw-multiple-geometries": "在同一地图上显示多种几何形状。",
    "dsl-styling": "使用 DSL 进行运行时样式设置。",
    "extended-geofencing": "结合 Isochrone API 与通知的扩展地理围栏示例。",
    "external-vector-source": "添加第三方矢量瓦片数据源。",
    "feature-state": "使用 Feature State 创建交互式 hover 效果。",
    "fly-to-camera-animation": "使用 flyTo 动画平滑移动相机。",
    "geojson-performance": "展示大型 GeoJSON 长路线的渲染性能。",
    "gestures": "配置与操控地图手势交互。",
    "inset-map": "显示联动的小地图 inset，适合游戏等双地图场景。",
    "interactive-3d-model-source": "通过更新模型源属性控制 3D 模型的车门、引擎盖、后备箱和颜色。",
    "legacy-offline-map": "使用旧版 OfflineManager API 下载瓦片。",
    "line-behind-moving-icon": "在移动图标后方绘制轨迹线。",
    "line-gradient": "为线条设置彩色渐变样式。",
    "local-style-mapsnapshotter": "使用本地样式文件生成静态地图图片。",
    "localization-plugin": "将地图标签自动本地化为设备当前语言。",
    "location-component": "在地图上显示定位 puck。",
    "location-component-animation": "通过自定义 LocationProvider 更新驱动 puck 动画。",
    "map-fragment": "在 Fragment 中使用 MapView，并配合 Fragment 回退栈。",
    "map-overlay": "使用 Map Overlay 功能。",
    "mapbox-recorder": "使用 MapboxMapRecorder 录制与回放地图会话。",
    "mapbox-studio-style": "加载在 Mapbox Studio 中设计的自定义样式。",
    "mapsurface": "使用 MapSurface + SurfaceView 宿主与 Widget。",
    "mapview-snapshot": "从 MapView 创建 Bitmap 快照。",
    "multi-display": "在副屏（第二显示器）上显示地图。",
    "multi-map-fragments": "在同一 Activity 中使用多个地图 Fragment。",
    "viewpager": "在 ViewPager 中嵌入多个 MapView。",
    "native-custom-layer": "使用 C++ 实现 Native Custom Layer。",
    "ornaments": "地图旋转时动态更新 logo、比例尺等装饰控件边距。",
    "raster-colorization": "使用 raster-color 对栅格图层着色。",
    "raw-expression": "通过 JSON 字符串定义 Expression。",
    "raw-geojson": "通过 Value API 使用 GeoJSON 字符串作为数据源。",
    "raw-source-layer": "通过 JSON 字符串定义 Source/Layer。",
    "recyclerview": "在 RecyclerView 列表项中集成 MapView。",
    "restrict-map-panning": "限制手势平移的可视范围（编程方式仍可改变 viewport）。",
    "runtime-styling": "运行时动态修改地图样式。",
    "scale-bar": "在自定义位置显示比例尺。",
    "using-color-theme": "为地图样式设置自定义颜色主题。",
    "show-and-hide-layers": "允许用户切换 CircleLayer 的可见性。",
    "simulate-a-navigation-route": "模拟带位置追踪的导航路线。",
    "sky-snapshotter": "将 Snapshotter 生成的图片叠加在 MapView 上。",
    "space-station-current-location": "实时更新 marker 位置（国际空间站示例）。",
    "style-circles-categorically": "从矢量瓦片集加载点数据，用 match/get 表达式按属性为 CircleLayer 着色。",
    "textureview": "使用 TextureView 作为地图渲染表面。",
    "tilejson": "使用 TileSet 类通过 raster source/layer 渲染 OSM 瓦片。",
    "tint-a-fill-layer": "向样式添加图片，并在 landuse FillLayer 中显示填充图案。",
    "transparent-background": "创建透明背景的 MapView，可在地图后方显示视频等内容。",
    "triangle-custom-layer": "使用 CustomLayer 向样式添加自定义三角形图层。",
    "use-3d-terrain": "添加 3D 地形与大气天空层，打造更真实的地图效果。",
    "use-an-image-source": "使用 Image Source 在地图上叠加图片。",
    "using-custom-camera-animations": "使用 Camera Animator 独立动画化 zoom、bearing、center 等属性。",
    "vector-tile-source": "添加矢量数据源与 LineLayer。",
    "view-annotation-as-infowindow": "使用 View Annotation 实现传统 InfoWindow 效果。",
    "view-annotation-with-point-annotation": "在 Point Annotation 上附加 View Annotation（类似带弹窗的 marker）。",
    "view-annotations-advanced-example": "将 View Annotation 锚定到 SymbolLayer 要素。",
    "view-annotations-animation": "通过持续更新坐标对 View Annotation 进行动画。",
    "viewport-camera": "Viewport 相机功能演示。",
    "visualize-data-as-a-heatmap": "从 GeoJSON 加载地震频率数据，用 HeatmapLayer 渲染热力图。",
    "within-expression": "在缓冲几何体上使用 within 表达式。",
    "wms-source": "通过 TileSet API 向地图添加外部 WMS（Web Map Service）栅格源。",
}


def github_to_raw(url: str) -> str | None:
    m = re.match(
        r"https://github\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.+)",
        url,
    )
    if not m:
        return None
    org, repo, ref, path = m.groups()
    return f"https://raw.githubusercontent.com/{org}/{repo}/{ref}/{path}"


def fetch_url(url: str) -> str | None:
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "AuraDocsBot/1.0"})
        with urllib.request.urlopen(req, timeout=90) as resp:
            return resp.read().decode("utf-8")
    except Exception:
        return None


def download_image(url: str, dest: Path) -> bool:
    if dest.exists() and dest.stat().st_size > 1000:
        return True
    content = fetch_url(url)
    if content is None:
        return False
    # binary fetch
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "AuraDocsBot/1.0"})
        with urllib.request.urlopen(req, timeout=90) as resp:
            dest.write_bytes(resp.read())
        return True
    except Exception:
        return False


def filter_paragraphs(paras: list[str]) -> str:
    skip = ("Examples App", "GitHub", "Run the Maps SDK", "step-by-step", "assets and res")
    kept = [p for p in paras if not any(s in p for s in skip)]
    return " ".join(kept)


def generate_markdown(meta: dict, fetched: dict, code: str, img_filename: str | None) -> str:
    slug = meta["slug"]
    title_en = fetched.get("h1") or meta["title_en"]
    title_zh = TITLE_ZH.get(slug, title_en)
    url = fetched["url"]
    desc_zh = SUMMARY_ZH.get(slug, meta.get("summary_en", ""))
    extra = filter_paragraphs(fetched.get("paras", []))
    desc_en = extra or meta.get("summary_en", "")
    activity = fetched.get("activity", "")
    github = fetched.get("github", "")

    lines = [
        f"# {title_zh}（{title_en}）",
        "",
        f"> 官方示例：[{slug}]({url})",
        "",
        "## 示例效果",
        "",
    ]
    if (IMG_DIR / f"{slug}.mp4").exists() and (IMG_DIR / f"{slug}.mp4").stat().st_size > 10_000:
        lines.extend(
            [
                f'<video controls width="100%" src="./images/{slug}.mp4"></video>',
                "",
                f"> 本示例为视频效果（本地文件 `./images/{slug}.mp4`）。",
                "",
            ]
        )
    elif (IMG_DIR / f"{slug}.png").exists() and (IMG_DIR / f"{slug}.png").stat().st_size > 5_000:
        lines.extend([f"![{title_zh}](./images/{slug}.png)", ""])
    else:
        lines.extend(["_（本地截图缺失，请运行 `python3 scripts/sync_example_images.py` 或重新抓取）_", ""])

    lines.extend(["## 功能说明", "", desc_zh, ""])
    if desc_en and desc_en != desc_zh:
        lines.extend(
            [
                "<details>",
                "<summary>英文原文</summary>",
                "",
                desc_en,
                "",
                "</details>",
                "",
            ]
        )

    if activity:
        lines.extend(["## 示例 Activity", "", f"- `{activity}.kt`", ""])

    if code:
        lines.extend(["## 示例代码", "", "```kotlin", code.rstrip(), "```", ""])

    lines.extend(
        [
            "## 在 Aura 项目中使用",
            "",
            "- UI 框架：**Android View**（与 Aura 当前 `MapFragment` + `MapView` 一致）",
            "- 包名请替换为 `com.catclaw.aura`",
            "- 需在 `local.properties` 配置 `MAPBOX_ACCESS_TOKEN`",
            "- 部分示例依赖 `assets/` 或额外布局文件，请参考 GitHub 示例工程",
            "",
            "## 参考链接",
            "",
            f"- [官方文档（英文）]({url})",
        ]
    )
    if github:
        lines.append(f"- [GitHub 源码]({github})")
    lines.extend(
        [
            "- [Android View 示例索引](./README.md)",
            "- [Mapbox 中文指南](../../README.md)",
            "",
        ]
    )
    return "\n".join(lines)


def build_readme(entries: list[tuple[int, dict, str]]) -> str:
    lines = [
        "# Mapbox Android View 示例 — 中文文档",
        "",
        "本目录为 [Mapbox Android View Examples](https://docs.mapbox.com/android/maps/examples/android-view/) 的中文译本，"
        "涵盖从 **Display a map view** 到 **WMS Source** 的全部 **107** 个官方示例。",
        "",
        "每个文档包含：**中文说明**、**效果截图**、**完整 Kotlin 源码**（来自 GitHub v11.24.3）、Aura 集成提示。",
        "",
        "**SDK 版本基准**：v11.24.3",
        "",
        "## 示例列表",
        "",
        "| # | 中文 | 英文 | 文档 |",
        "|:---:|:---|:---|:---|",
    ]
    for idx, meta, filename in entries:
        slug = meta["slug"]
        title_zh = TITLE_ZH.get(slug, meta["title_en"])
        title_en = meta["title_en"]
        lines.append(f"| {idx} | {title_zh} | {title_en} | [{title_zh}](./{filename}) |")
    lines.extend(
        [
            "",
            "## 运行官方 Examples App",
            "",
            "```bash",
            "git clone https://github.com/mapbox/mapbox-maps-android.git",
            "cd mapbox-maps-android",
            "# 在 Android Studio 中打开并运行 app 模块",
            "```",
            "",
            "详见 [Run the Maps SDK for Android Examples App](https://docs.mapbox.com/help/tutorials/android-examples-app/)。",
            "",
            "## 相关文档",
            "",
            "- [Mapbox 中文指南（Guides）](../../README.md)",
            "- [安装与 Mapbox 配置](../../01-安装与入门.md)",
            "",
        ]
    )
    return "\n".join(lines)


def main() -> None:
    meta_list = json.loads(URLS_FILE.read_text(encoding="utf-8"))
    fetched_all = json.loads(DATA_FILE.read_text(encoding="utf-8"))
    by_slug = {f["slug"]: f for f in fetched_all}

    IMG_DIR.mkdir(parents=True, exist_ok=True)
    readme_entries: list[tuple[int, dict, str]] = []

    for i, meta in enumerate(meta_list, start=1):
        slug = meta["slug"]
        fetched = by_slug.get(slug, {"slug": slug, "url": f"https://docs.mapbox.com/android/maps/examples/android-view/{slug}/"})
        print(f"regenerate [{i}/107] {slug}")

        code = ""
        if fetched.get("github"):
            raw = github_to_raw(fetched["github"])
            if raw:
                code = fetch_url(raw) or ""

        img_filename = None
        if fetched.get("image"):
            img_filename = f"{slug}.png"
            download_image(fetched["image"], IMG_DIR / img_filename)

        filename = f"{i:02d}-{slug}.md"
        md = generate_markdown(meta, fetched, code, img_filename)
        (OUT_DIR / filename).write_text(md, encoding="utf-8")
        readme_entries.append((i, meta, filename))

    (OUT_DIR / "README.md").write_text(build_readme(readme_entries), encoding="utf-8")
    print("Done regenerate")


if __name__ == "__main__":
    main()
