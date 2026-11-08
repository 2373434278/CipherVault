package com.ciphervault.app.domain

import org.json.JSONObject

/**
 * `extra` 字段的 JSON 编解码器。
 *
 * `extra` 是银行卡、身份信息等类型条目的无模式字段包，会同时写入数据库
 * `payloadJson`、CSV `extra_json` 与加密备份 payload，因此三处必须共用本编解码器。
 */
object ExtraCodec {
    /** 解码数据库 `payloadJson`；空值或损坏 JSON 返回空 Map。 */
    fun decode(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { fromJson(JSONObject(raw)) }.getOrDefault(emptyMap())
    }

    /** 编码数据库 `payloadJson`；过滤空值，全部为空时返回 null。 */
    fun encode(extra: Map<String, String>): String? {
        val filtered = extra.filterValues { it.isNotBlank() }
        if (filtered.isEmpty()) return null
        return intoJson(filtered).toString()
    }

    /** 从 JSON 对象读取全部键；CSV 导入与备份恢复使用。 */
    fun fromJson(json: JSONObject): Map<String, String> = buildMap {
        json.keys().forEach { key -> put(key, json.optString(key)) }
    }

    /** 写入 JSON 对象；保留所有键值，不过滤空值（备份 payload 的语义）。 */
    fun intoJson(extra: Map<String, String>): JSONObject = JSONObject().apply {
        extra.forEach { (key, value) -> put(key, value) }
    }

    /** 解码 CSV `extra_json` 列；空串或损坏 JSON 返回空 Map。 */
    fun fromCsv(raw: String): Map<String, String> =
        if (raw.isBlank()) emptyMap() else runCatching { fromJson(JSONObject(raw)) }.getOrDefault(emptyMap())

    /** 编码 CSV `extra_json` 列；空 Map 输出空串。 */
    fun toCsv(extra: Map<String, String>): String = if (extra.isEmpty()) "" else intoJson(extra).toString()
}
