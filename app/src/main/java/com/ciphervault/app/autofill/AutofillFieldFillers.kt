package com.ciphervault.app.autofill

import android.service.autofill.Dataset
import android.view.autofill.AutofillValue
import com.ciphervault.app.domain.TotpGenerator
import com.ciphervault.app.domain.VaultItem

/** 单个字段的填充策略。 */
interface AutofillFieldFiller {
    fun fill(builder: Dataset.Builder, item: VaultItem, fields: AutofillFields): Boolean
}

object UsernameFiller : AutofillFieldFiller {
    override fun fill(builder: Dataset.Builder, item: VaultItem, fields: AutofillFields): Boolean {
        val id = fields.usernameId ?: return false
        if (item.username.isBlank()) return false
        builder.setValue(id, AutofillValue.forText(item.username))
        return true
    }
}

object PasswordFiller : AutofillFieldFiller {
    override fun fill(builder: Dataset.Builder, item: VaultItem, fields: AutofillFields): Boolean {
        val id = fields.passwordId ?: return false
        if (item.password.isBlank()) return false
        builder.setValue(id, AutofillValue.forText(item.password))
        return true
    }
}

object OtpFiller : AutofillFieldFiller {
    override fun fill(builder: Dataset.Builder, item: VaultItem, fields: AutofillFields): Boolean {
        val id = fields.otpId ?: return false
        if (!item.hasTotp) return false
        val code = runCatching {
            TotpGenerator.generate(
                secret = item.totpSecret,
                algorithm = item.totpAlgorithm,
                digits = item.totpDigits,
                period = item.totpPeriod,
            ).code
        }.getOrNull() ?: return false
        builder.setValue(id, AutofillValue.forText(code))
        return true
    }
}

/** 填充器注册表；任一字段写入成功即生成数据集。 */
object AutofillFieldFillers {
    private val all: List<AutofillFieldFiller> = listOf(UsernameFiller, PasswordFiller, OtpFiller)

    fun fill(builder: Dataset.Builder, item: VaultItem, fields: AutofillFields): Boolean =
        all.fold(false) { filled, filler -> filler.fill(builder, item, fields) || filled }
}
