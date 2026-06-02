#!/usr/bin/env python3
"""Fetch Mapbox Android View examples and generate Chinese markdown docs."""

from __future__ import annotations

import json
import re
import sys
import time
import urllib.request
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
URLS_FILE = ROOT / "scripts" / "mapbox_examples_urls.json"
OUT_DIR = ROOT / "docs" / "mapbox" / "examples" / "android-view"
IMG_DIR = OUT_DIR / "images"
DATA_FILE = ROOT / "scripts" / "mapbox_examples_data.json"
BASE = "https://docs.mapbox.com/android/maps/examples/android-view"

# Chinese titles keyed by slug
TITLE_ZH: dict[str, str] = {
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
    "inset-map": " inset 小地图",
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


def slug_to_image_prefix(slug: str) -> str:
    return "maps-examples-" + slug.replace("3D", "3d")


def fetch_page(page, slug: str) -> dict:
    url = f"{BASE}/{slug}/"
    page.goto(url, wait_until="networkidle", timeout=120_000)
    data = page.evaluate(
        """() => {
        const article = document.querySelector('article') || document.querySelector('main');
        const h1 = document.querySelector('h1')?.textContent?.trim() || '';
        const paras = Array.from(article.querySelectorAll('p'))
          .map(p => p.textContent.trim())
          .filter(t => t.length > 30 && !t.includes('Was this example') && !t.includes('Ready to get started'));
        const imgs = Array.from(article.querySelectorAll('img'))
          .map(i => i.src)
          .filter(s => s.includes('ideal-img') || s.includes('maps-examples'));
        const codeEl = document.querySelector('pre code');
        const code = codeEl ? codeEl.textContent : '';
        const ghLinks = Array.from(document.querySelectorAll('a[href*="github.com/mapbox/mapbox-maps-android"]'))
          .map(a => a.href);
        const activityMatch = code.match(/class\\s+(\\w+)\\s*:/);
        return {
          h1,
          paras,
          image: imgs[0] || '',
          code,
          github: ghLinks.find(u => u.includes('/blob/')) || ghLinks[0] || '',
          activity: activityMatch ? activityMatch[1] : ''
        };
    }"""
    )
    data["slug"] = slug
    data["url"] = url
    return data


def download_image(url: str, dest: Path) -> bool:
    if not url:
        return False
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "AuraDocsBot/1.0"})
        with urllib.request.urlopen(req, timeout=60) as resp:
            dest.write_bytes(resp.read())
        return True
    except Exception as exc:  # noqa: BLE001
        print(f"  image download failed: {exc}", file=sys.stderr)
        return False


def format_code(code: str) -> str:
    if not code:
        return ""
    # Fix missing newlines from scraped text
    code = re.sub(r"(?<=[;}])\s*(?=(?:import|package|class|fun|override|private|companion|val|var|const|if|for|while|return|\/\*|\/\/))", "\n", code)
    code = re.sub(r"\s{2,}", lambda m: "\n" + " " * 2 if "\n" not in m.group() else m.group(), code)
    return code.strip()


def translate_paragraphs(paras: list[str], summary_en: str) -> list[str]:
    """Use fetched paragraphs; filter boilerplate."""
    skip = (
        "Examples App",
        "GitHub",
        "Run the Maps SDK",
        "step-by-step",
        "assets and res folders",
    )
    result = []
    for p in paras:
        if any(s in p for s in skip):
            continue
        result.append(p)
    if not result and summary_en:
        result.append(summary_en)
    return result


def summarize_zh(slug: str, paras: list[str], summary_en: str) -> str:
    """Build Chinese description from English content."""
    body = " ".join(translate_paragraphs(paras, summary_en))
    # Simple heuristic translations for common patterns
    replacements = [
        (r"This example demonstrates how to (.+?)\.", r"本示例演示如何\1。"),
        (r"This example demonstrates (.+?)\.", r"本示例演示\1。"),
        (r"The code below (.+?)\.", r"下方代码\1。"),
        (r"display a map", "显示地图"),
        (r"Mapbox Maps SDK for Android", "Mapbox Maps SDK for Android"),
        (r"MapView", "`MapView`"),
        (r"setCamera\(\)", "`setCamera()`"),
        (r"loadStyle", "`loadStyle`"),
        (r"GeoJSON", "GeoJSON"),
        (r"SymbolLayer", "`SymbolLayer`"),
        (r"RasterLayer", "`RasterLayer`"),
        (r"LineLayer", "`LineLayer`"),
        (r"CircleLayer", "`CircleLayer`"),
        (r"FillLayer", "`FillLayer`"),
        (r"HeatmapLayer", "`HeatmapLayer`"),
        (r"View annotation", "View Annotation"),
        (r"OfflineManager", "`OfflineManager`"),
        (r"camera", "相机"),
        (r"style", "样式"),
        (r"layer", "图层"),
        (r"source", "数据源"),
        (r"annotation", "标注"),
        (r"gesture", "手势"),
        (r"location", "位置"),
        (r"terrain", "地形"),
        (r"building", "建筑"),
        (r"cluster", "聚合"),
        (r"animate", "动画"),
        (r"snapshot", "快照"),
        (r"offline", "离线"),
        (r"heatmap", "热力图"),
        (r"geofenc", "地理围栏"),
        (r"WMS", "WMS"),
    ]
    zh = body
    for en, cn in replacements:
        zh = re.sub(en, cn, zh, flags=re.IGNORECASE)
    if zh == body:
        zh = summary_en
    return zh


def generate_markdown(meta: dict, fetched: dict, img_filename: str | None) -> str:
    slug = meta["slug"]
    title_en = fetched.get("h1") or meta["title_en"]
    title_zh = TITLE_ZH.get(slug, title_en)
    url = fetched["url"]
    summary_en = meta.get("summary_en", "")
    paras = fetched.get("paras", [])
    desc_zh = summarize_zh(slug, paras, summary_en)
    desc_en = " ".join(translate_paragraphs(paras, summary_en))
    code = format_code(fetched.get("code", ""))
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
    if img_filename:
        lines.extend([f"![{title_zh}](./images/{img_filename})", ""])
    else:
        lines.extend(["_（截图暂未获取，请查看官方页面）_", ""])

    lines.extend(
        [
            "## 功能说明",
            "",
            desc_zh,
            "",
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
        lines.extend(["## 示例代码", "", "```kotlin", code, "```", ""])

    lines.extend(
        [
            "## 在 Aura 项目中使用",
            "",
            "- UI 框架：**Android View**（与 Aura 当前 `MapFragment` + `MapView` 一致）",
            "- 包名请替换为 `com.catclaw.aura`",
            "- 需在 `local.properties` 配置 `MAPBOX_ACCESS_TOKEN`",
            "- 若示例使用 ViewBinding，可在 Aura 中同样启用（已开启）",
            "",
            "## 参考链接",
            "",
            f"- [官方文档（英文）]({url})",
        ]
    )
    if github:
        lines.append(f"- [GitHub 示例工程]({github})")
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
        "涵盖从 **Display a map view** 到 **WMS Source** 的全部官方示例，便于开发时对照代码与效果图。",
        "",
        "**SDK 版本基准**：v11.24.3（与 Aura 项目一致）",
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
            "## 说明",
            "",
            "- 示例代码来自 Mapbox 官方 Examples App，运行完整交互效果请克隆 "
            "[mapbox-maps-android](https://github.com/mapbox/mapbox-maps-android)。",
            "- 部分示例需要额外 assets（GeoJSON、图片等），见官方仓库 `app/src/main/assets`。",
            "- 截图版权归 Mapbox 所有，仅供开发参考。",
            "",
            "## 相关文档",
            "",
            "- [Mapbox 中文指南（Guides）](../../README.md)",
            "- [Aura 安装与 Mapbox 配置](../../01-安装与入门.md)",
            "",
        ]
    )
    return "\n".join(lines)


def main() -> None:
    meta_list = json.loads(URLS_FILE.read_text(encoding="utf-8"))
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    IMG_DIR.mkdir(parents=True, exist_ok=True)

    fetched_all: list[dict] = []
    readme_entries: list[tuple[int, dict, str]] = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        total = len(meta_list)
        for i, meta in enumerate(meta_list, start=1):
            slug = meta["slug"]
            print(f"[{i}/{total}] {slug}")
            try:
                fetched = fetch_page(page, slug)
            except Exception as exc:  # noqa: BLE001
                print(f"  fetch failed: {exc}", file=sys.stderr)
                fetched = {"slug": slug, "url": f"{BASE}/{slug}/", "paras": [], "code": "", "image": ""}
            fetched_all.append(fetched)

            img_filename = None
            if fetched.get("image"):
                ext = ".png" if ".png" in fetched["image"] else ".jpg"
                img_filename = f"{slug}{ext}"
                download_image(fetched["image"], IMG_DIR / img_filename)

            filename = f"{i:02d}-{slug}.md"
            md = generate_markdown(meta, fetched, img_filename)
            (OUT_DIR / filename).write_text(md, encoding="utf-8")
            readme_entries.append((i, meta, filename))
            time.sleep(0.3)
        browser.close()

    DATA_FILE.write_text(json.dumps(fetched_all, ensure_ascii=False, indent=2), encoding="utf-8")
    (OUT_DIR / "README.md").write_text(build_readme(readme_entries), encoding="utf-8")

    # Update main mapbox README
    main_readme = ROOT / "docs" / "mapbox" / "README.md"
    if main_readme.exists():
        text = main_readme.read_text(encoding="utf-8")
        link = "- [Android View 示例（中文）](./examples/android-view/README.md)"
        if link not in text:
            insert_after = "## 阅读顺序建议"
            if insert_after in text:
                text = text.replace(
                    insert_after,
                    insert_after + "\n\n| | |\n|:---:|:---|\n| 📱 | [**Android View 示例（中文）**](./examples/android-view/README.md) | 107 个官方 Demo 对照开发 |\n",
                )
                main_readme.write_text(text, encoding="utf-8")

    print(f"Done: {total} examples -> {OUT_DIR}")


if __name__ == "__main__":
    main()
