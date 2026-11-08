package com.ciphervault.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ciphervault.app.R
import com.ciphervault.app.domain.FieldKeyboard
import com.ciphervault.app.domain.ItemFieldKeys
import com.ciphervault.app.domain.TotpSettings
import com.ciphervault.app.domain.extraFields
import com.ciphervault.app.ui.state.ItemDraft
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.Base32
import com.ciphervault.app.domain.OtpauthParser
import com.ciphervault.app.domain.PasswordGenerator
import com.ciphervault.app.domain.PasswordOptions
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import com.ciphervault.app.ui.components.ConfirmDialog
import com.ciphervault.app.ui.components.ErrorText
import com.ciphervault.app.ui.components.FilterPill
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTextField
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.components.PrimaryButton
import com.ciphervault.app.ui.components.SecondaryButton
import com.ciphervault.app.ui.components.SectionLabel
import com.ciphervault.app.ui.components.SwitchRow
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.util.UUID

/** 条目编辑：类型切换、类型专属字段、TOTP 扫码/解析与保存。 */
@Composable
fun ItemEditScreen(
    repository: VaultRepository,
    itemId: String?,
    initialType: VaultItemType?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val folders by repository.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    var draft by remember { mutableStateOf(ItemDraft(type = initialType ?: VaultItemType.LOGIN)) }
    var reveal by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        if (itemId != null) {
            repository.getItem(itemId)?.let { item -> draft = ItemDraft.from(item) }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@rememberLauncherForActivityResult
        val parsed = OtpauthParser.parse(contents)
        if (parsed == null) {
            error = context.getString(R.string.invalid_totp)
        } else {
            draft = draft.copy(
                totp = TotpSettings.from(parsed),
                name = if (draft.name.isBlank()) parsed.issuer.ifBlank { parsed.account } else draft.name,
            )
            error = null
        }
    }

    fun applySecretInput(value: String) {
        error = null
        val parsed = OtpauthParser.parse(value)
        draft = if (parsed != null) {
            draft.copy(totp = TotpSettings.from(parsed))
        } else {
            draft.copy(totp = draft.totp.copy(secret = value))
        }
    }

    fun save() {
        if (saving) return
        if (draft.name.isBlank()) {
            error = context.getString(R.string.name_hint)
            return
        }
        var totp = draft.totp.copy(secret = draft.totp.secret.trim())
        if (totp.secret.isNotBlank()) {
            val parsed = OtpauthParser.parse(totp.secret)
            if (parsed != null) {
                totp = TotpSettings.from(parsed)
            } else if (runCatching { Base32.decode(totp.secret) }.isFailure) {
                error = context.getString(R.string.invalid_totp)
                return
            }
        }
        saving = true
        error = null
        scope.launch {
            val item = draft.copy(totp = totp).toItem(itemId ?: UUID.randomUUID().toString())
            val result = runCatching { repository.saveItem(item) }
            if (result.isSuccess) {
                onSaved()
            } else {
                error = result.exceptionOrNull()?.message ?: context.getString(R.string.error_generic)
                saving = false
            }
        }
    }

    ObsidianScaffold(
        topBar = {
            ObsidianTopBar(
                title = stringResource(if (itemId == null) R.string.new_item else R.string.edit_item),
                onBack = onBack,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SectionLabel(stringResource(R.string.item_type))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill(stringResource(R.string.logins), draft.type == VaultItemType.LOGIN) { draft = draft.copy(type = VaultItemType.LOGIN) }
                FilterPill(stringResource(R.string.notes), draft.type == VaultItemType.NOTE) { draft = draft.copy(type = VaultItemType.NOTE) }
                FilterPill(stringResource(R.string.cards), draft.type == VaultItemType.CARD) { draft = draft.copy(type = VaultItemType.CARD) }
                FilterPill(stringResource(R.string.identities), draft.type == VaultItemType.IDENTITY) { draft = draft.copy(type = VaultItemType.IDENTITY) }
            }
            Spacer(Modifier.height(14.dp))
            ObsidianTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it); error = null },
                label = stringResource(R.string.name),
                placeholder = stringResource(R.string.name_hint),
            )
            Spacer(Modifier.height(12.dp))

            when (draft.type) {
                VaultItemType.LOGIN -> {
                    ObsidianTextField(
                        value = draft.username,
                        onValueChange = { draft = draft.copy(username = it) },
                        label = stringResource(R.string.username),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(12.dp))
                    ObsidianTextField(
                        value = draft.password,
                        onValueChange = { draft = draft.copy(password = it) },
                        label = stringResource(R.string.password),
                        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    draft = draft.copy(password = PasswordGenerator.generate(PasswordOptions(length = 20)))
                                    reveal = true
                                }) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { reveal = !reveal }) {
                                    Icon(
                                        if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(12.dp))
                    ObsidianTextField(
                        value = draft.url,
                        onValueChange = { draft = draft.copy(url = it) },
                        label = stringResource(R.string.website),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(18.dp))
                    SectionLabel(stringResource(R.string.totp_label))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ObsidianTextField(
                            value = draft.totp.secret,
                            onValueChange = ::applySecretInput,
                            label = stringResource(R.string.totp_secret),
                            placeholder = stringResource(R.string.totp_secret_hint),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                scanLauncher.launch(
                                    ScanOptions()
                                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        .setPrompt(context.getString(R.string.scan_qr))
                                        .setBeepEnabled(false)
                                        .setOrientationLocked(false),
                                )
                            },
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = stringResource(R.string.scan_qr), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (draft.totp.isConfigured) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterPill("SHA1", draft.totp.algorithm == "SHA1") { draft = draft.copy(totp = draft.totp.copy(algorithm = "SHA1")) }
                            FilterPill("SHA256", draft.totp.algorithm == "SHA256") { draft = draft.copy(totp = draft.totp.copy(algorithm = "SHA256")) }
                            FilterPill("SHA512", draft.totp.algorithm == "SHA512") { draft = draft.copy(totp = draft.totp.copy(algorithm = "SHA512")) }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterPill("6", draft.totp.digits == 6) { draft = draft.copy(totp = draft.totp.copy(digits = 6)) }
                            FilterPill("8", draft.totp.digits == 8) { draft = draft.copy(totp = draft.totp.copy(digits = 8)) }
                            FilterPill("30s", draft.totp.period == 30) { draft = draft.copy(totp = draft.totp.copy(period = 30)) }
                            FilterPill("60s", draft.totp.period == 60) { draft = draft.copy(totp = draft.totp.copy(period = 60)) }
                        }
                    }
                }
                VaultItemType.NOTE -> Unit
                VaultItemType.CARD -> {
                    ExtraTextField(
                        stringResource(R.string.cardholder),
                        draft.extra[ItemFieldKeys.CARDHOLDER],
                    ) { draft = draft.copy(extra = draft.extra + (ItemFieldKeys.CARDHOLDER to it)) }
                    Spacer(Modifier.height(12.dp))
                    ExtraTextField(
                        stringResource(R.string.card_number),
                        draft.extra[ItemFieldKeys.CARD_NUMBER],
                        KeyboardType.Number,
                    ) { draft = draft.copy(extra = draft.extra + (ItemFieldKeys.CARD_NUMBER to it)) }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ObsidianTextField(
                            value = draft.extra[ItemFieldKeys.EXPIRY].orEmpty(),
                            onValueChange = { draft = draft.copy(extra = draft.extra + (ItemFieldKeys.EXPIRY to it)) },
                            label = stringResource(R.string.expiry),
                            modifier = Modifier.weight(1f),
                        )
                        ObsidianTextField(
                            value = draft.extra[ItemFieldKeys.CVV].orEmpty(),
                            onValueChange = { draft = draft.copy(extra = draft.extra + (ItemFieldKeys.CVV to it)) },
                            label = stringResource(R.string.cvv),
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        )
                    }
                }
                VaultItemType.IDENTITY -> {
                    VaultItemType.IDENTITY.extraFields.forEachIndexed { index, spec ->
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        ExtraTextField(
                            label = stringResource(spec.labelRes),
                            value = draft.extra[spec.key],
                            keyboardType = spec.keyboard.toKeyboardType(),
                        ) { draft = draft.copy(extra = draft.extra + (spec.key to it)) }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            ObsidianTextField(
                value = draft.notes,
                onValueChange = { draft = draft.copy(notes = it) },
                label = stringResource(R.string.notes_label),
                singleLine = false,
                minLines = 3,
            )

            if (folders.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.folder))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterPill(stringResource(R.string.no_folder), draft.folderId == null) { draft = draft.copy(folderId = null) }
                    }
                    items(folders, key = { it.id }) { folder ->
                        FilterPill(folder.name, draft.folderId == folder.id) { draft = draft.copy(folderId = folder.id) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SwitchRow(
                title = stringResource(R.string.favorite),
                checked = draft.favorite,
                onCheckedChange = { draft = draft.copy(favorite = it) },
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = stringResource(R.string.save_item),
                onClick = ::save,
                loading = saving,
            )
            if (itemId != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    text = stringResource(R.string.delete_item),
                    icon = Icons.Outlined.Delete,
                    onClick = { showDelete = true },
                )
            }
            Spacer(Modifier.height(10.dp))
            ErrorText(error)
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm),
            body = stringResource(R.string.delete_confirm_body),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = {
                showDelete = false
                scope.launch {
                    itemId?.let { repository.softDelete(it) }
                    onBack()
                }
            },
            onDismiss = { showDelete = false },
        )
    }
}

private fun FieldKeyboard.toKeyboardType(): KeyboardType = when (this) {
    FieldKeyboard.TEXT -> KeyboardType.Text
    FieldKeyboard.NUMBER -> KeyboardType.Number
    FieldKeyboard.EMAIL -> KeyboardType.Email
    FieldKeyboard.PHONE -> KeyboardType.Phone
    FieldKeyboard.NUMBER_PASSWORD -> KeyboardType.NumberPassword
}

@Composable
private fun ExtraTextField(
    label: String,
    value: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    ObsidianTextField(
        value = value.orEmpty(),
        onValueChange = onChange,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}
