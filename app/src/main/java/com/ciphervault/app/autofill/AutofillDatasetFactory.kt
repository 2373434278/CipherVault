package com.ciphervault.app.autofill

import android.service.autofill.Dataset
import android.widget.RemoteViews
import com.ciphervault.app.R
import com.ciphervault.app.domain.VaultItem

/** 自动填充数据集构建器；负责 RemoteViews 与字段填充策略组合。 */
class AutofillDatasetFactory(private val packageName: String) {
    fun presentation(title: String, subtitle: String): RemoteViews =
        RemoteViews(packageName, R.layout.autofill_dataset).apply {
            setTextViewText(R.id.autofill_title, title)
            setTextViewText(R.id.autofill_subtitle, subtitle)
        }

    fun build(item: VaultItem, fields: AutofillFields): Dataset? {
        val builder = Dataset.Builder(
            presentation(
                title = item.name,
                subtitle = item.username.ifBlank { item.domain.orEmpty() },
            ),
        )
        return if (AutofillFieldFillers.fill(builder, item, fields)) builder.build() else null
    }
}
