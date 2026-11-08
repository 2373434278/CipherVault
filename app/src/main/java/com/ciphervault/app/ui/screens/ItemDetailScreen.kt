package com.ciphervault.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ciphervault.app.R
import com.ciphervault.app.domain.extraFields
import com.ciphervault.app.ui.item.ItemTypeVisuals
import com.ciphervault.app.ui.state.ItemDetailStateHolder
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.TotpGenerator
import com.ciphervault.app.domain.VaultItem
import com.ciphervault.app.domain.VaultItemType
import com.ciphervault.app.ui.components.ConfirmDialog
import com.ciphervault.app.ui.components.GlassCard
import com.ciphervault.app.ui.components.ObsidianIconButton
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.components.PrimaryButton
import com.ciphervault.app.ui.components.SecondaryButton
import com.ciphervault.app.ui.components.SectionLabel
import com.ciphervault.app.ui.components.TotpDigits
import com.ciphervault.app.ui.components.TotpRing
import com.ciphervault.app.core.port.ClipboardGateway
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 条目详情：按类型展示字段、复制、TOTP 倒计时与删除入口。 */
@Composable
fun ItemDetailScreen(
    repository: VaultRepository,
    clipboard: ClipboardGateway,
    itemId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember(repository, itemId) { ItemDetailStateHolder(repository, itemId, scope) }
    val itemState by state.item.collectAsStateWithLifecycle()
    val item = itemState
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 只有存在 TOTP 时才需要每秒刷新倒计时，避免无意义重组。
    LaunchedEffect(item?.hasTotp) {
        if (item?.hasTotp != true) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    if (item == null) {
        ObsidianScaffold(topBar = { ObsidianTopBar(title = stringResource(R.string.item_detail), onBack = onBack) }) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_search), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    ObsidianScaffold(
        topBar = {
            ObsidianTopBar(
                title = stringResource(R.string.item_detail),
                onBack = onBack,
                actions = {
                    ObsidianIconButton(
                        icon = if (item.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(R.string.favorite),
                        onClick = {
                            scope.launch { repository.setFavorite(item.id, !item.favorite) }
                        },
                    )
                    ObsidianIconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.edit),
                        onClick = onEdit,
                    )
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        ItemTypeVisuals.icon(item.type),
                        null,
                        tint = ItemTypeVisuals.accent(item.type),
                        modifier = Modifier.size(27.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        ItemTypeVisuals.label(item.type),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            when (item.type) {
                VaultItemType.LOGIN -> {
                    DetailField(
                        label = stringResource(R.string.username),
                        value = item.username,
                        onCopy = { clipboard.copy("username", item.username) },
                    )
                    if (item.password.isNotBlank()) {
                        DetailField(
                            label = stringResource(R.string.password),
                            value = if (state.revealed) item.password else "••••••••••••",
                            mono = true,
                            onCopy = { clipboard.copy("password", item.password) },
                            trailing = {
                                ObsidianIconButton(
                                    icon = if (state.revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    onClick = state::toggleReveal,
                                )
                            },
                        )
                    }
                    if (item.url.isNotBlank()) {
                        DetailField(
                            label = stringResource(R.string.website),
                            value = item.url,
                            valueColor = MaterialTheme.colorScheme.secondary,
                            onCopy = { clipboard.copy("url", item.url) },
                        )
                    }
                }
                VaultItemType.CARD -> {
                    ExtraFields(item) { value -> clipboard.copy("field", value) }
                }
                VaultItemType.IDENTITY -> {
                    ExtraFields(item) { value -> clipboard.copy("field", value) }
                }
                VaultItemType.NOTE -> Unit
            }

            if (item.hasTotp) {
                Spacer(Modifier.height(12.dp))
                val totp = runCatching {
                    TotpGenerator.generate(
                        secret = item.totpSecret,
                        algorithm = item.totpAlgorithm,
                        digits = item.totpDigits,
                        period = item.totpPeriod,
                        timestampMillis = now,
                    )
                }.getOrNull()
                if (totp != null) {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "TOTP · ${item.totpAlgorithm}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                TotpDigits(totp.code)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.totp_remaining, totp.remainingSeconds),
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TotpRing(totp, size = 52)
                        }
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton(
                            text = stringResource(R.string.copy),
                            onClick = { clipboard.copy("totp", totp.code) },
                        )
                    }
                }
            }

            if (item.notes.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                SectionLabel(stringResource(R.string.notes_label))
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
            if (item.type == VaultItemType.LOGIN && item.url.isNotBlank()) {
                SecondaryButton(
                    text = stringResource(R.string.open_browser),
                    icon = Icons.Outlined.Public,
                    onClick = {
                        val url = if (item.url.startsWith("http", ignoreCase = true)) item.url else "https://${item.url}"
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                )
                Spacer(Modifier.height(10.dp))
            }
            SecondaryButton(
                text = stringResource(R.string.delete_item),
                icon = Icons.Outlined.Delete,
                onClick = state::requestDelete,
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (state.showDelete) {
        AlertDialog(
            onDismissRequest = state::cancelDelete,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.delete_confirm)) },
            text = { Text(stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    state.cancelDelete()
                    scope.launch {
                        repository.softDelete(item.id)
                        onBack()
                    }
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = state::cancelDelete) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    mono: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onCopy: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.5.sp, letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                    letterSpacing = if (mono) 1.5.sp else 0.sp,
                ),
                color = valueColor,
            )
        }
        trailing?.invoke()
        ObsidianIconButton(
            icon = Icons.Outlined.ContentCopy,
            contentDescription = stringResource(R.string.copy),
            onClick = onCopy,
        )
    }
}

@Composable
private fun ExtraFields(item: VaultItem, onCopy: (String) -> Unit) {
    item.type.extraFields.forEach { spec ->
        val value = item.extra[spec.key].orEmpty()
        if (value.isNotBlank()) {
            DetailField(
                label = stringResource(spec.labelRes),
                value = value,
                mono = spec.mono,
                onCopy = { onCopy(value) },
            )
        }
    }
}
