package com.ciphervault.app.domain.importexport

import com.ciphervault.app.domain.Folder
import com.ciphervault.app.domain.ItemFieldKeys
import com.ciphervault.app.domain.TotpSettings
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import com.ciphervault.app.domain.VaultPayload
import org.json.JSONObject
import java.util.UUID

/**
 * Bitwarden JSON 导入策略。
 *
 * 保持两遍结构：先按 `type` 建条目并建立文件夹映射，再按原始类型码选择富化器。
 * 未知类型码不会富化，行为与历史版本一致。
 */
object BitwardenImporter {
    fun import(text: String): VaultPayload {
        val root = JSONObject(text)
        val folderMap = mutableMapOf<String, Folder>()
        val folders = mutableListOf<Folder>()
        root.optJSONArray("folders")?.let { array ->
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val oldId = json.optString("id")
                val name = json.optString("name").ifBlank { "Folder" }
                val folder = Folder(UUID.randomUUID().toString(), name, index)
                if (oldId.isNotBlank()) folderMap[oldId] = folder
                folders.add(folder)
            }
        }
        val items = mutableListOf<VaultItem>()
        root.optJSONArray("items")?.let { array ->
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val typeCode = json.optInt("type", 1)
                val item = VaultItem(
                    id = UUID.randomUUID().toString(),
                    type = VaultItemType.fromBitwarden(typeCode),
                    name = json.optString("name").ifBlank { "Untitled" },
                    notes = json.optString("notes"),
                    favorite = json.optBoolean("favorite", false),
                    folderId = folderMap[json.optString("folderId")]?.id,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
                items.add(BitwardenEnrichers.enrich(typeCode, item, json))
            }
        }
        return VaultPayload(items, folders)
    }
}

/** Bitwarden 单类型富化策略。 */
interface BitwardenEnricher {
    val typeCode: Int
    fun enrich(item: VaultItem, root: JSONObject): VaultItem
}

object LoginEnricher : BitwardenEnricher {
    override val typeCode: Int = 1

    override fun enrich(item: VaultItem, root: JSONObject): VaultItem {
        val login = root.optJSONObject("login") ?: return item
        val uri = login.optJSONArray("uris")?.optJSONObject(0)?.optString("uri").orEmpty()
        val totpData = TotpSettings.parse(login.optString("totp"))
        return item.copy(
            username = login.optString("username"),
            password = login.optString("password"),
            url = uri,
            totpSecret = totpData.secret,
            totpAlgorithm = totpData.algorithm,
            totpDigits = totpData.digits,
            totpPeriod = totpData.period,
        )
    }
}

object CardEnricher : BitwardenEnricher {
    override val typeCode: Int = 3

    override fun enrich(item: VaultItem, root: JSONObject): VaultItem {
        val card = root.optJSONObject("card") ?: return item
        val expiry = listOf(card.optString("expMonth"), card.optString("expYear"))
            .filter { it.isNotBlank() }
            .joinToString("/")
        return item.copy(
            extra = buildMap {
                card.optString("cardholderName").takeIf { it.isNotBlank() }?.let { put(ItemFieldKeys.CARDHOLDER, it) }
                card.optString("number").takeIf { it.isNotBlank() }?.let { put(ItemFieldKeys.CARD_NUMBER, it) }
                expiry.takeIf { it.isNotBlank() }?.let { put(ItemFieldKeys.EXPIRY, it) }
                card.optString("code").takeIf { it.isNotBlank() }?.let { put(ItemFieldKeys.CVV, it) }
            },
        )
    }
}

object IdentityEnricher : BitwardenEnricher {
    override val typeCode: Int = 4

    override fun enrich(item: VaultItem, root: JSONObject): VaultItem {
        val identity = root.optJSONObject("identity") ?: return item
        val fullName = listOf(identity.optString("firstName"), identity.optString("lastName"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val address = listOf(
            identity.optString("address1"),
            identity.optString("city"),
            identity.optString("postalCode"),
            identity.optString("country"),
        ).filter { it.isNotBlank() }.joinToString(", ")
        return item.copy(
            extra = buildMap {
                fullName.takeIf { it.isNotBlank() }?.let { put(ItemFieldKeys.FULL_NAME, it) }
                identity.optString("email").takeIf { it.isNotBlank() }?.let { put(ItemFieldKeys.EMAIL, it) }
                identity.optString("phone").takeIf { it.isNotBlank() }?.let { put(ItemFieldKeys.PHONE, it) }
                identity.optString("ssn").takeIf { it.isNotBlank() }?.let { put(ItemFieldKeys.ID_NUMBER, it) }
                address.takeIf { it.isNotBlank() }?.let { put(ItemFieldKeys.ADDRESS, it) }
            },
        )
    }
}

/** 类型码 → 富化器注册表；未注册的类型保持原条目。 */
object BitwardenEnrichers {
    private val byCode: Map<Int, BitwardenEnricher> =
        listOf(LoginEnricher, CardEnricher, IdentityEnricher).associateBy { it.typeCode }

    fun enrich(typeCode: Int, item: VaultItem, root: JSONObject): VaultItem =
        byCode[typeCode]?.enrich(item, root) ?: item
}
