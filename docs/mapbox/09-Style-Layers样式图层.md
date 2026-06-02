# Style Layers 样式图层入门

> 原文：[Style layers](https://docs.mapbox.com/android/maps/guides/add-your-data/style-layers/)

当数据量大或需要与道路/建筑层级关系时，用 **Source + Layer** 把数据画进地图渲染栈，而不是 Annotation。

## 何时使用

- 数千～数万要素
- 矢量瓦片、服务端 GeoJSON
- 数据驱动样式、热力图等

## 流程概览

1. **添加 Source**（数据从哪来）
2. **添加 Layer**（数据怎么画）
3. （可选）用 Expressions 按属性/zoom 改样式

详细 API 见 [04-源与图层](./04-源与图层.md)。

---

## Vector Source

数据来自矢量瓦片（`mapbox://username.tilesetname`）：

```kotlin
mapView.mapboxMap.getStyle { style ->
    style.addSource(vectorSource("my-vector-source") {
        url("mapbox://{username}.{tilesetname}")
    })
    style.addLayer(circleLayer("my-circle-layer", "my-vector-source") {
        sourceLayer("some-source-layer")  // 瓦片内图层名
        circleColor("#FF0000")
    })
}
```

瓦片可用 Mapbox Data manager、MTS 或自建服务托管。

---

## GeoJSON Source

### 从 URL

```kotlin
geoJsonSource("my-geojson-source") {
    data("https://example.com/data.geojson")
}
```

### 从 assets

```kotlin
val json = assets.open("file.geojson").bufferedReader().use { it.readText() }
geoJsonSource("id") { data(json) }
```

### 内联字符串

```kotlin
geoJsonSource("id") { data("""{ "type": "FeatureCollection", ... }""") }
```

大数据量时性能不如 vector tiles（整包加载）。

---

## 添加 Layer

```kotlin
circleLayer("circle-layer", "my-pointdata-source") {
    circleColor(Color.BLUE)
    circleRadius(6.0)
}
symbolLayer("symbol-layer", "my-pointdata-source") {
    iconImage("my-icon")
    iconSize(1.5)
}
```

### 常用 Layer 类型

| 类型 | 用途 |
|------|------|
| Circle | 点 → 圆 |
| Line | 线、路径 |
| Fill | 面 |
| Symbol | 图标、文字 |

---

## 在 Studio 中预置数据

可在 Mapbox Studio 把 vector source + layer 做进自定义样式，应用只 `loadStyle(url)`，减少运行时代码。Studio 中 **不支持** GeoJSON source，需 vector tileset。

---

## 延伸阅读

- [04-源与图层](./04-源与图层.md)
- [Style Spec](https://docs.mapbox.com/style-spec/)
- Layer / Source [API Reference](https://docs.mapbox.com/android/maps/api/)
