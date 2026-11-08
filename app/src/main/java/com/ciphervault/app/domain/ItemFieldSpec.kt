package com.ciphervault.app.domain

import androidx.annotation.StringRes
import com.ciphervault.app.R

/** 银行卡/身份信息的 extra 字段键；属于持久化契约，改动会破坏导入与备份兼容。 */
object ItemFieldKeys {
    const val CARDHOLDER = "cardholder"
    const val CARD_NUMBER = "number"
    const val EXPIRY = "expiry"
    const val CVV = "cvv"
    const val FULL_NAME = "fullName"
    const val EMAIL = "email"
    const val PHONE = "phone"
    const val ID_NUMBER = "idNumber"
    const val ADDRESS = "address"
}

/** 编辑表单所需的键盘类型；由 UI 层映射到 Compose KeyboardType。 */
enum class FieldKeyboard { TEXT, NUMBER, EMAIL, PHONE, NUMBER_PASSWORD }

/** 单个额外字段的展示/编辑规格。 */
data class ExtraFieldSpec(
    val key: String,
    @StringRes val labelRes: Int,
    val keyboard: FieldKeyboard = FieldKeyboard.TEXT,
    val mono: Boolean = false,
)

/**
 * 条目类型的额外字段注册表。
 *
 * 编辑页、详情页与导入富化共用同一份字段顺序与键名，避免五处重复。
 */
val VaultItemType.extraFields: List<ExtraFieldSpec>
    get() = when (this) {
        VaultItemType.CARD -> listOf(
            ExtraFieldSpec(ItemFieldKeys.CARDHOLDER, R.string.cardholder, mono = true),
            ExtraFieldSpec(ItemFieldKeys.CARD_NUMBER, R.string.card_number, FieldKeyboard.NUMBER, mono = true),
            ExtraFieldSpec(ItemFieldKeys.EXPIRY, R.string.expiry, mono = true),
            ExtraFieldSpec(ItemFieldKeys.CVV, R.string.cvv, FieldKeyboard.NUMBER_PASSWORD, mono = true),
        )
        VaultItemType.IDENTITY -> listOf(
            ExtraFieldSpec(ItemFieldKeys.FULL_NAME, R.string.full_name),
            ExtraFieldSpec(ItemFieldKeys.EMAIL, R.string.email, FieldKeyboard.EMAIL),
            ExtraFieldSpec(ItemFieldKeys.PHONE, R.string.phone, FieldKeyboard.PHONE),
            ExtraFieldSpec(ItemFieldKeys.ID_NUMBER, R.string.id_number),
            ExtraFieldSpec(ItemFieldKeys.ADDRESS, R.string.address),
        )
        else -> emptyList()
    }
