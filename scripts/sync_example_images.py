#!/usr/bin/env python3
"""Embed example screenshots from local images/ directory only."""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS_DIR = ROOT / "docs" / "mapbox" / "examples" / "android-view"
IMG_DIR = DOCS_DIR / "images"

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

EFFECT_SECTION = re.compile(
    r"## 示例效果\n\n.*?\n\n(?=## 功能说明)",
    re.DOTALL,
)


def build_effect_block(slug: str, title_zh: str) -> str | None:
    mp4 = IMG_DIR / f"{slug}.mp4"
    png = IMG_DIR / f"{slug}.png"

    if mp4.exists() and mp4.stat().st_size > 10_000:
        return (
            f"## 示例效果\n\n"
            f'<video controls width="100%" src="./images/{slug}.mp4"></video>\n\n'
            f"> 本示例为视频效果（本地文件 `./images/{slug}.mp4`）。\n\n"
        )
    if png.exists() and png.stat().st_size > 5_000:
        return f"## 示例效果\n\n![{title_zh}](./images/{slug}.png)\n\n"
    return None


def main() -> None:
    updated = 0
    missing = []

    for md in sorted(DOCS_DIR.glob("[0-9]*-*.md")):
        m = re.match(r"\d+-(.+)\.md$", md.name)
        if not m:
            continue
        slug = m.group(1)
        title_zh = TITLE_ZH.get(slug, slug)
        effect = build_effect_block(slug, title_zh)
        if effect is None:
            missing.append(slug)
            continue

        text = md.read_text(encoding="utf-8")
        new_text, count = EFFECT_SECTION.subn(effect, text, count=1)
        if count and new_text != text:
            md.write_text(new_text, encoding="utf-8")
            updated += 1

    print(f"Updated {updated} docs with local image paths")
    if missing:
        print(f"Missing local media ({len(missing)}): {missing}")


if __name__ == "__main__":
    main()
