# Markers 标记（Compose）

> 原文：[Markers](https://docs.mapbox.com/android/maps/guides/add-your-data/markers/)

## 说明

**Markers** 是 Jetpack Compose 的便捷 API，在地图上方显示可定制的大头针，**无需**自备图片资源。

- 需 `@OptIn(MapboxExperimental::class)` 与 `import com.mapbox.maps.MapboxExperimental`
- **仅 Compose**，传统 View 项目请用 [Annotations](./07-Annotations标注.md)

## 优点与限制

| 优点 | 限制 |
|------|------|
| 内置默认图钉 | 仅 Compose |
| 可设颜色、描边、内圈色、文字 | 样式不如 Annotation 丰富 |
| API 简单 | 100+ 时性能下降 |

## 基础用法

```kotlin
@OptIn(MapboxExperimental::class)
MapboxMap(Modifier.fillMaxSize()) {
    Marker(point = Point.fromLngLat(-74.0060, 40.7128))
}
```

## 自定义

```kotlin
Marker(
    point = Point.fromLngLat(-74.0060, 40.7128),
    color = Color.Blue,
    stroke = Color.Magenta,
    innerColor = Color.White,
    text = "New York City"
)

// 无描边
Marker(point = point, stroke = null)
```

## 多个标记

```kotlin
locations.forEach { loc ->
    Marker(
        point = loc.coordinate,
        color = Color.Red,
        text = loc.name
    )
}
```

## 显示 / 隐藏

Markers 属于 `MapContent`，从 `MapboxMap` 组合中移除即消失：

```kotlin
var show by remember { mutableStateOf(false) }
MapboxMap(Modifier.fillMaxSize()) {
    if (show) {
        Marker(point = Point.fromLngLat(-74.0060, 40.7128))
    }
}
```

## 性能建议

Markers 底层类似 View Annotation + 文字渲染。超过约 **100** 个时考虑 `PointAnnotation` 或 **Style Layer**。
