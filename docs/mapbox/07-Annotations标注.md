# Annotations 标注

> 原文：[Annotations](https://docs.mapbox.com/android/maps/guides/add-your-data/annotations/)

## 说明

**Annotations** 是绘制在地图**上方**、钉在经纬度上的图像或几何（圆、线、面）。  
Compose 简单图钉可优先考虑 [Markers](./06-Markers标记.md)；需要自定义图标或 View 体系时用本文 API。

## 优点与限制

| 优点 | 限制 |
|------|------|
| View + Compose | 点标注需自备图片 |
| 点击、拖拽 | >250 个时效率低 |
| 点标注支持聚合 | 无默认图标 |

## Android View：AnnotationPlugin

```kotlin
val pointAnnotationManager = mapView.annotations
    .createPointAnnotationManager(mapView)

val options = PointAnnotationOptions()
    .withPoint(Point.fromLngLat(18.06, 59.31))
    .withIconImage(YOUR_ICON_BITMAP)

pointAnnotationManager.create(options)
```

## Jetpack Compose

```kotlin
val marker = rememberIconImage(key = "red-marker", painter = painterResource(R.drawable.red_marker))
MapboxMap(Modifier.fillMaxSize()) {
    PointAnnotation(point = Point.fromLngLat(18.06, 59.31)) {
        iconImage = marker
    }
}
```

## 其他形状

| 类型 | Manager / Composable |
|------|----------------------|
| 圆 | `CircleAnnotationManager` / `CircleAnnotation` |
| 折线 | `PolylineAnnotationManager` / `PolylineAnnotation` |
| 多边形 | `PolygonAnnotationManager` / `PolygonAnnotation` |

**圆示例（View）：**

```kotlin
CircleAnnotationOptions()
    .withPoint(Point.fromLngLat(18.06, 59.31))
    .withCircleRadius(8.0)
    .withCircleColor("#ee4e8b")
```

**线示例（Compose）：**

```kotlin
PolylineAnnotation(
    points = listOf(
        Point.fromLngLat(17.94, 59.25),
        Point.fromLngLat(18.18, 59.37)
    )
) {
    lineColor = Color(0xffee4e8b)
    lineWidth = 5.0
}
```

## 交互：可拖拽

**View：**

```kotlin
PointAnnotationOptions()
    .withDraggable(true)
```

**Compose：**

```kotlin
PointAnnotation(point = point) {
    interactionsState.onClicked { true }
        .onDragged { /* ... */ }
        .also { it.isDraggable = true }
}
```

## 删除

- **View**：`annotationApi.removeAnnotationManager(manager)`
- **Compose**：从 `MapboxMap { }` 中移除对应 Composable，或用 `if (flag)` 条件渲染
