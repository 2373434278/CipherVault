package com.ciphervault.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ciphervault.app.R
import com.ciphervault.app.core.port.SettingsGateway
import com.ciphervault.app.session.SessionManager
import com.ciphervault.app.ui.components.BiometricHelper
import com.ciphervault.app.ui.components.ErrorText
import com.ciphervault.app.ui.components.LogoMark
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTextField
import com.ciphervault.app.ui.components.PasswordField
import com.ciphervault.app.ui.components.PrimaryButton
import com.ciphervault.app.ui.components.SecondaryButton
import com.ciphervault.app.ui.components.findFragmentActivity
import kotlinx.coroutines.launch
import javax.crypto.AEADBadTagException

/** 解锁页：主密码或生物识别解锁；也复用于 Autofill 的认证流程。 */
@Composable
fun LockScreen(
    session: SessionManager,
    settings: SettingsGateway,
    onCancel: (() -> Unit)? = null,
    titleOverride: String? = null,
    subtitleOverride: String? = null,
    onUnlocked: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    val scope = rememberCoroutineScope()
    val meta by settings.metaFlow.collectAsStateWithLifecycle(initialValue = null)
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val biometricEnabled = meta?.biometricEnabled == true && activity != null
    val canBiometric = remember { BiometricHelper.canAuthenticate(context) }

    fun submit() {
        if (loading || password.isBlank()) return
        loading = true
        error = null
        scope.launch {
            val result = session.unlockWithPassword(password.toCharArray())
            if (result.isSuccess) {
                onUnlocked?.invoke()
            } else {
                val cause = result.exceptionOrNull()
                error = if (cause is AEADBadTagException) {
                    context.getString(R.string.wrong_password)
                } else {
                    context.getString(R.string.unlock_failed)
                }
            }
            loading = false
        }
    }

    fun biometricUnlock() {
        if (!biometricEnabled || activity == null || loading) return
        scope.launch {
            error = null
            val cipher = session.prepareBiometricCipher()
            if (cipher == null) {
                error = context.getString(R.string.biometric_failed)
                return@launch
            }
            BiometricHelper.authenticate(
                activity = activity,
                title = context.getString(R.string.biometric_prompt_title),
                subtitle = context.getString(R.string.biometric_prompt_subtitle),
                negativeText = context.getString(R.string.cancel),
                cipher = cipher,
                onSuccess = { result ->
                    val authenticatedCipher = result.cryptoObject?.cipher
                    if (authenticatedCipher != null) {
                        scope.launch {
                            val unlock = session.completeBiometricUnlock(authenticatedCipher)
                            if (unlock.isSuccess) {
                                onUnlocked?.invoke()
                            } else {
                                error = context.getString(R.string.biometric_failed)
                            }
                        }
                    }
                },
                onError = { code, message ->
                    if (code != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        code != androidx.biometric.BiometricPrompt.ERROR_CANCELED
                    ) {
                        error = message.toString()
                    }
                },
            )
        }
    }

    ObsidianScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (onCancel != null) {
                Box(Modifier.fillMaxWidth()) {
                    TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            LogoMark(size = 72)
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 3.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(44.dp))
            Text(
                text = titleOverride ?: stringResource(R.string.unlock_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = subtitleOverride ?: stringResource(R.string.unlock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            PasswordField(
                value = password,
                onValueChange = { password = it; error = null },
                label = stringResource(R.string.master_password),
                placeholder = stringResource(R.string.password_hint),
                reveal = reveal,
                onToggleReveal = { reveal = !reveal },
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = if (loading) stringResource(R.string.unlocking) else stringResource(R.string.unlock),
                onClick = ::submit,
                loading = loading,
                icon = Icons.Outlined.Lock,
            )
            if (biometricEnabled && canBiometric) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    text = stringResource(R.string.use_biometric),
                    onClick = ::biometricUnlock,
                    icon = Icons.Outlined.Fingerprint,
                )
            }
            Spacer(Modifier.height(12.dp))
            ErrorText(error)
            Spacer(Modifier.height(32.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Argon2id · AES-256-GCM · SQLCipher",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
