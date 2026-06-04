package com.catclaw.aura.data.ambient.capture

import org.json.JSONObject

/**
 * Distinguishes street/POI labels from city/district-only, and composes admin fallbacks (e.g. 北京市朝阳区).
 */
internal object PlaceLabelHeuristics {

    private val STREET_MARKERS = listOf(
        "路", "街", "号", "道", "巷", "弄", "里", "村", "镇", "（",
        "大厦", "广场", "中心", "商场", "酒店", "公园", "站", "楼", "苑", "园",
    )

    private val COARSE_FEATURE_TYPES = setOf(
        "district",
        "place",
        "locality",
        "neighborhood",
        "region",
        "country",
        "city",
        "admin",
    )

    private val ADMIN_CONTEXT_ORDER = listOf(
        "region",
        "place",
        "district",
        "locality",
        "neighborhood",
    )

    private val COUNTRY_NAMES = setOf("中国", "china", "中华人民共和国")

    fun hasStreetLevelDetail(label: String): Boolean {
        val t = label.trim()
        if (t.isBlank()) return false
        return STREET_MARKERS.any { t.contains(it) }
    }

    fun isCoarseAdminLabel(label: String, featureType: String?): Boolean {
        val t = label.trim()
        if (t.isBlank()) return true
        if (hasStreetLevelDetail(t)) return false
        if (featureType == "poi") return false
        if (t.endsWith("区") || t.endsWith("市") || t.endsWith("省") || t.endsWith("县")) return true
        return featureType in COARSE_FEATURE_TYPES
    }

    /** Joins admin parts without commas: 北京市 + 朝阳区 → 北京市朝阳区. */
    fun composeAdminLabel(parts: List<String>): String? {
        val cleaned = parts
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.lowercase() in COUNTRY_NAMES }
            .distinct()
        if (cleaned.isEmpty()) return null
        return cleaned.joinToString("")
    }

    fun composeFromCommaSeparated(text: String): String? {
        val t = text.trim()
        if (t.isBlank()) return null
        val parts = t.split(',', '，')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.lowercase() in COUNTRY_NAMES }
        if (parts.isEmpty()) return null
        return composeAdminLabel(parts.asReversed())
    }

    fun composeFromSearchBoxContext(props: JSONObject): String? {
        val context = props.optJSONObject("context")
        val parts = mutableListOf<String>()
        if (context != null) {
            for (key in ADMIN_CONTEXT_ORDER) {
                val name = context.optJSONObject(key)?.optString("name")?.trim().orEmpty()
                if (name.isNotBlank() && name !in parts) parts.add(name)
            }
        }
        val featureType = props.optString("feature_type")
        val name = props.optString("name_preferred")
            .ifBlank { props.optString("name") }
            .trim()
        if (name.isNotBlank() && featureType in ADMIN_CONTEXT_ORDER && name !in parts) {
            val index = ADMIN_CONTEXT_ORDER.indexOf(featureType)
            if (index >= 0 && index <= parts.size) {
                parts.add(index, name)
            } else {
                parts.add(name)
            }
        }
        composeAdminLabel(parts)?.let { return it }
        return composeFromCommaSeparated(props.optString("place_formatted"))
    }

    fun composeFromGeocodingFeature(feature: JSONObject): String? {
        val byType = mutableMapOf<String, String>()
        val context = feature.optJSONArray("context")
        if (context != null) {
            for (i in 0 until context.length()) {
                val ctx = context.getJSONObject(i)
                val type = ctx.optString("id").substringBefore('.')
                val text = ctx.optString("text").trim()
                if (text.isNotBlank() && type in ADMIN_CONTEXT_ORDER) {
                    byType.putIfAbsent(type, text)
                }
            }
        }
        val text = feature.optString("text").trim()
        val primaryType = feature.optJSONArray("place_type")?.optString(0).orEmpty()
        if (text.isNotBlank() && primaryType in ADMIN_CONTEXT_ORDER) {
            byType.putIfAbsent(primaryType, text)
        }
        val ordered = ADMIN_CONTEXT_ORDER.mapNotNull { byType[it] }
        composeAdminLabel(ordered)?.let { return it }
        return composeFromCommaSeparated(feature.optString("place_name"))
    }
}
