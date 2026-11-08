package com.ciphervault.app.autofill

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.ciphervault.app.di.appServices
import com.ciphervault.app.R
import com.ciphervault.app.session.SessionState
import com.ciphervault.app.ui.screens.LockScreen
import com.ciphervault.app.ui.theme.CipherVaultTheme
import com.ciphervault.app.util.LocaleHelper

/**
 * Autofill 认证宿主。
 *
 * 未解锁时复用 [LockScreen] 完成主密码/生物识别认证；成功后返回 `RESULT_OK`，
 * 系统会重新发起 FillRequest 并展示条目数据集。
 */
class AutofillAuthActivity : FragmentActivity() {
    private val container by lazy { appServices() }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (container.sessionManager.state.value is SessionState.Unlocked) {
            finishWith(RESULT_OK)
            return
        }
        setContent {
            CipherVaultTheme {
                LockScreen(
                    session = container.sessionManager,
                    settings = container.settingsStore,
                    onCancel = { finishWith(RESULT_CANCELED) },
                    titleOverride = stringResource(R.string.autofill_auth_title),
                    subtitleOverride = stringResource(R.string.autofill_auth_subtitle),
                    onUnlocked = { finishWith(RESULT_OK) },
                )
            }
        }
    }

    private fun finishWith(result: Int) {
        setResult(result)
        finish()
    }
}
