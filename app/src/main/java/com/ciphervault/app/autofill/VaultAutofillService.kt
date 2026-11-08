package com.ciphervault.app.autofill

import android.app.PendingIntent
import android.content.Intent
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillValue
import com.ciphervault.app.R
import com.ciphervault.app.di.appServices
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * 自动填充服务适配器。
 *
 * 只保留系统回调入口与同步应答；字段识别、会话门禁、数据集构建、保存策略分别委托给
 * [AutofillRepositoryGate]、[AutofillDatasetFactory] 与 [AutofillDomainResolver]。
 */
class VaultAutofillService : AutofillService() {
    private val container get() = appServices()
    private val datasetFactory by lazy { AutofillDatasetFactory(packageName) }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: android.os.CancellationSignal,
        callback: FillCallback,
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }
        val fields = AutofillParser.parse(structure)
        if (fields.usernameId == null && fields.passwordId == null && fields.otpId == null) {
            callback.onSuccess(null)
            return
        }

        when (val gate = AutofillRepositoryGate.snapshot(container.sessionManager.state.value)) {
            is AutofillRepositoryGate.Result.Locked -> {
                callback.onSuccess(buildAuthPendingResponse(fields))
            }
            is AutofillRepositoryGate.Result.Unlocked -> {
                // AutofillService 的 FillCallback 必须在回调线程同步应答，不能用 suspend 挂起；
                // 本地 Room 查询结果集很小，阻塞窗口为毫秒级。
                val domain = AutofillDomainResolver.resolve(fields)
                val items = runCatching {
                    runBlocking(Dispatchers.IO) {
                        gate.repository.findForDomain(domain, MAX_FILL_CANDIDATES)
                    }
                }.getOrDefault(emptyList())
                val datasets = items.mapNotNull { item -> datasetFactory.build(item, fields) }
                if (datasets.isEmpty()) {
                    callback.onSuccess(null)
                    return
                }
                val builder = FillResponse.Builder()
                datasets.forEach(builder::addDataset)
                val ids = listOfNotNull(fields.usernameId, fields.passwordId).toTypedArray()
                if (ids.isNotEmpty()) {
                    builder.setSaveInfo(
                        SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_PASSWORD, ids).build(),
                    )
                }
                callback.onSuccess(builder.build())
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess()
            return
        }
        val fields = AutofillParser.parse(structure)
        if (fields.usernameValue.isBlank() && fields.passwordValue.isBlank()) {
            callback.onSuccess()
            return
        }
        val gate = AutofillRepositoryGate.snapshot(container.sessionManager.state.value)
        if (gate !is AutofillRepositoryGate.Result.Unlocked) {
            callback.onFailure(getString(R.string.unlock))
            return
        }
        val domain = AutofillDomainResolver.resolve(fields)
        runCatching {
            runBlocking(Dispatchers.IO) {
                val existing = gate.repository.findForDomain(domain, MAX_SAVE_CANDIDATES)
                if (existing.none { it.username == fields.usernameValue }) {
                    gate.repository.saveItem(
                        VaultItem(
                            id = UUID.randomUUID().toString(),
                            type = VaultItemType.LOGIN,
                            name = AutofillDomainResolver.displayName(domain),
                            username = fields.usernameValue,
                            password = fields.passwordValue,
                            url = AutofillDomainResolver.urlFor(fields),
                            notes = "Saved by Autofill",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }.onSuccess { callback.onSuccess() }
            .onFailure { callback.onFailure(getString(R.string.error_generic)) }
    }

    private fun buildAuthPendingResponse(fields: AutofillFields): FillResponse {
        val builder = Dataset.Builder(
            datasetFactory.presentation(
                title = getString(R.string.autofill_auth_title),
                subtitle = getString(R.string.autofill_auth_subtitle),
            ),
        ).setAuthentication(authPendingIntent().intentSender)
        val placeholderId = fields.usernameId ?: fields.passwordId ?: fields.otpId
        if (placeholderId != null) {
            val placeholderValue = when (placeholderId) {
                fields.usernameId -> fields.usernameValue
                fields.passwordId -> fields.passwordValue
                else -> fields.otpValue
            }
            builder.setValue(placeholderId, AutofillValue.forText(placeholderValue))
        }
        return FillResponse.Builder().addDataset(builder.build()).build()
    }

    private fun authPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        AUTH_REQUEST_CODE,
        Intent(this, AutofillAuthActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val MAX_FILL_CANDIDATES = 5
        const val MAX_SAVE_CANDIDATES = 20
        const val AUTH_REQUEST_CODE = 1001
    }
}
