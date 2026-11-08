package com.ciphervault.app.domain.importexport

import com.ciphervault.app.domain.ExtraCodec
import com.ciphervault.app.domain.Folder
import com.ciphervault.app.domain.TotpSettings
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import com.ciphervault.app.domain.VaultPayload
import org.json.JSONArray
import org.json.JSONObject

/**
 * 自有 payload JSON 编解码器（加密备份的明文部分）。
 *
 * 字段集合与顺序是备份兼容契约；可选字段用 `isNull` 判断，
 * 避免 Android 与 JVM 的 `org.json` 对 `JSONObject.NULL` 处理差异。
 */
object PayloadJsonCodec {
    const val FORMAT = "ciphervault-payload"
    const val VERSION = 1

    internal fun encode(payload: VaultPayload): String {
        val folders = JSONArray()
        payload.folders.forEach { folder ->
            folders.put(JSONObject().apply {
                put("id", folder.id)
                put("name", folder.name)
                put("sortOrder", folder.sortOrder)
            })
        }
        val items = JSONArray()
        payload.items.forEach { item ->
            items.put(JSONObject().apply {
                put("id", item.id)
                put("type", item.type.storage)
                put("name", item.name)
                put("username", item.username)
                put("password", item.password)
                put("url", item.url)
                put("notes", item.notes)
                put("totpSecret", item.totpSecret)
                put("totpAlgorithm", item.totpAlgorithm)
                put("totpDigits", item.totpDigits)
                put("totpPeriod", item.totpPeriod)
                put("extra", ExtraCodec.intoJson(item.extra))
                put("folderId", item.folderId ?: JSONObject.NULL)
                put("favorite", item.favorite)
                put("pinned", item.pinned)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
            })
        }
        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("folders", folders)
            put("items", items)
        }.toString()
    }

    internal fun decode(text: String): VaultPayload {
        val root = JSONObject(text)
        val folders = mutableListOf<Folder>()
        root.optJSONArray("folders")?.let { array ->
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                folders.add(Folder(json.getString("id"), json.getString("name"), json.optInt("sortOrder")))
            }
        }
        val items = mutableListOf<VaultItem>()
        root.optJSONArray("items")?.let { array ->
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                val totp = TotpSettings(
                    secret = json.optString("totpSecret"),
                    algorithm = json.optString("totpAlgorithm", TotpSettings.DEFAULT_ALGORITHM),
                    digits = json.optInt("totpDigits", TotpSettings.DEFAULT_DIGITS),
                    period = json.optInt("totpPeriod", TotpSettings.DEFAULT_PERIOD),
                )
                items.add(
                    VaultItem(
                        id = json.getString("id"),
                        type = VaultItemType.from(json.optString("type")),
                        name = json.getString("name"),
                        username = json.optString("username"),
                        password = json.optString("password"),
                        url = json.optString("url"),
                        notes = json.optString("notes"),
                        totpSecret = totp.secret,
                        totpAlgorithm = totp.algorithm,
                        totpDigits = totp.digits,
                        totpPeriod = totp.period,
                        extra = json.optJSONObject("extra")?.let(ExtraCodec::fromJson) ?: emptyMap(),
                        folderId = if (json.isNull("folderId")) {
                            null
                        } else {
                            json.optString("folderId").takeIf { it.isNotBlank() }
                        },
                        favorite = json.optBoolean("favorite"),
                        pinned = json.optBoolean("pinned", false),
                        createdAt = json.optLong("createdAt"),
                        updatedAt = json.optLong("updatedAt"),
                    ),
                )
            }
        }
        return VaultPayload(items, folders)
    }
}
