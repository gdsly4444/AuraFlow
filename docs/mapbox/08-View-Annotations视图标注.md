# View Annotations 视图标注

> 原文：[View annotations](https://docs.mapbox.com/android/maps/guides/add-your-data/view-annotations/)

在 `MapView` 上叠加 Android **View** 或 Compose **Composable**，可绑定到 `Geometry` 或地图上的 **Layer 要素**。

## 适用场景

- 点击 POI 显示信息窗
- 导航路线上的 ETA 条
- 任意复杂 Android UI

## 可绑定的图层类型

`LineLayer`、`FillLayer`、`FillExtrusionLayer`、`CircleLayer`、`SymbolLayer`

## 优点与限制

| 优点 | 限制 |
|------|------|
| 与现有 Android UI 集成 | >250 且 allowOverlap 时性能差 |
| 合理数量（通常 <100）性能尚可 | 高级逻辑需自写 |
| 支持重叠、选中、绑 feature | 无内置聚合 |

## Android View

1. 写布局 XML（如 `annotation_view.xml`）
2. 获取 `viewAnnotationManager = mapView.viewAnnotationManager`
3. `addViewAnnotation(resId, viewAnnotationOptions { geometry(point) })`

```kotlin
viewAnnotationManager.addViewAnnotation(
    resId = R.layout.annotation_view,
    options = viewAnnotationOptions {
        geometry(Point.fromLngLat(18.06, 59.31))
    }
)
```

也可用已 inflate 的 `View`，或异步 inflate（需 `async inflater` 依赖）。

## Jetpack Compose

```kotlin
MapboxMap(Modifier.fillMaxSize()) {
    ViewAnnotation(
        options = viewAnnotationOptions {
            geometry(Point.fromLngLat(18.06, 59.31))
        }
    ) {
        Text("Hello world", /* ... */)
    }
}
```

`annotatedFeature` 可为 `Geometry` 或 `LayerFeature`（绑到具体 feature，随要素显隐）。

## 外观控制

### 层级（z-index）

后添加的在上；用 `priority(10)` 可强制置顶。

### 锚点

```kotlin
annotationAnchor {
    anchor(ViewAnnotationAnchor.BOTTOM)
    offsetX(-10.0)
    offsetY(20.0)
}
```

可设多个 anchor，SDK 选用第一个能放进视口的。

### 可见性

`visible(false)` 或控制 `view.visibility` / Compose 条件渲染。

## 常见模式

1. **点击地图显示**：在 `OnMapClickListener` 或 query 结果处 `addViewAnnotation`
2. **绑几何体**：线/面上的动态标注会随几何在视口内移动
3. **绑 SymbolLayer 要素**：`annotatedLayerFeature(layerId) { featureId(...) }`，与要素碰撞规则一致

Symbol 弹窗开关逻辑需自行实现（见官方 advanced example）。
