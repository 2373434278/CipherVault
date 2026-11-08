package com.ciphervault.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.R
import com.ciphervault.app.domain.PasswordGenerator
import com.ciphervault.app.session.SessionManager
import com.ciphervault.app.ui.components.BiometricHelper
import com.ciphervault.app.ui.components.ErrorText
import com.ciphervault.app.ui.components.GlassCard
import com.ciphervault.app.ui.components.LogoMark
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTextField
import com.ciphervault.app.ui.components.PasswordField
import com.ciphervault.app.ui.components.SecurityFooter
import com.ciphervault.app.ui.components.PrimaryButton
import com.ciphervault.app.ui.components.StrengthBar
import com.ciphervault.app.ui.components.SwitchRow
import kotlinx.coroutines.launch

/** 首次建库：主密码、确认密码、强度提示与可选生物识别。 */
@Composable
fun SetupScreen(session: SessionManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var biometric by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val canBiometric = remember { BiometricHelper.canAuthenticate(context) }
    val strength = PasswordGenerator.strength(password)

    fun strengthLabel(score: Int): String = context.getString(
        when (score) {
            0 -> R.string.strength_0
            1 -> R.string.strength_1
            2 -> R.string.strength_2
            3 -> R.string.strength_3
            else -> R.string.strength_4
        },
    )

    fun submit() {
        when {
            password.length < 8 -> {
                error = context.getString(R.string.password_too_short)
                return
            }
            password != confirm -> {
                error = context.getString(R.string.password_mismatch)
                return
            }
        }
        loading = true
        error = null
        scope.launch {
            val result = session.createVault(password.toCharArray(), biometric && canBiometric)
            if (result.isFailure) {
                error = result.exceptionOrNull()?.message ?: context.getString(R.string.error_generic)
            }
            loading = false
        }
    }

    ObsidianScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(26.dp))
            LogoMark(size = 76)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            PasswordField(
                value = password,
                onValueChange = { password = it; error = null },
                label = stringResource(R.string.master_password),
                placeholder = stringResource(R.string.password_hint),
                reveal = reveal,
                onToggleReveal = { reveal = !reveal },
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.password_strength),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = strengthLabel(strength),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            StrengthBar(score = strength)
            Spacer(Modifier.height(16.dp))
            PasswordField(
                value = confirm,
                onValueChange = { confirm = it; error = null },
                label = stringResource(R.string.confirm_password),
                placeholder = stringResource(R.string.password_hint),
                reveal = reveal,
                onToggleReveal = { reveal = !reveal },
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { submit() }),
                showRevealToggle = false,
            )
            if (canBiometric) {
                Spacer(Modifier.height(14.dp))
                SwitchRow(
                    title = stringResource(R.string.enable_biometric_setup),
                    subtitle = stringResource(R.string.enable_biometric_desc),
                    checked = biometric,
                    onCheckedChange = { biometric = it },
                )
            }
            Spacer(Modifier.height(16.dp))
            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.setup_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            PrimaryButton(
                text = if (loading) stringResource(R.string.creating_vault) else stringResource(R.string.create_vault),
                onClick = ::submit,
                loading = loading,
                icon = Icons.Outlined.Lock,
            )
            Spacer(Modifier.height(12.dp))
            ErrorText(error)
            Spacer(Modifier.height(22.dp))
            SecurityFooter()
        }
    }
}
