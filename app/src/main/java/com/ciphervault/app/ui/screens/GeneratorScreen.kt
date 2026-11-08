package com.ciphervault.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciphervault.app.R
import com.ciphervault.app.domain.PasswordGenerator
import com.ciphervault.app.domain.PasswordOptions
import com.ciphervault.app.ui.components.FilterPill
import com.ciphervault.app.ui.components.ObsidianBottomBar
import com.ciphervault.app.ui.navigation.TopLevelDestination
import com.ciphervault.app.ui.state.GeneratorStateHolder
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.components.PrimaryButton
import com.ciphervault.app.ui.components.SectionLabel
import com.ciphervault.app.ui.components.StrengthBar
import com.ciphervault.app.ui.components.SwitchRow
import com.ciphervault.app.core.port.ClipboardGateway
import kotlin.math.roundToInt

/** 密码/口令生成器：字符集选项、长度、强度提示与复制。 */
@Composable
fun GeneratorScreen(
    clipboard: ClipboardGateway,
    onTab: (TopLevelDestination) -> Unit,
) {
    val state = remember { GeneratorStateHolder() }
    val score = state.strength
    val scoreLabel = stringResource(
        when (score) {
            0 -> R.string.strength_0
            1 -> R.string.strength_1
            2 -> R.string.strength_2
            3 -> R.string.strength_3
            else -> R.string.strength_4
        },
    )

    ObsidianScaffold(
        topBar = {
            ObsidianTopBar(
                title = stringResource(R.string.password_generator),
                subtitle = stringResource(R.string.generator_subtitle),
                actions = {
                    IconButton(onClick = state::regenerate) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.generate), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
            )
        },
        bottomBar = { ObsidianBottomBar(selected = TopLevelDestination.GENERATOR, onSelect = onTab) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill(stringResource(R.string.random_password), !state.passphraseMode) { state.setMode(false) }
                FilterPill(stringResource(R.string.passphrase), state.passphraseMode) { state.setMode(true) }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    .padding(18.dp),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.generated_password).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.generated,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 22.sp,
                            letterSpacing = 0.4.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = stringResource(R.string.copy),
                            onClick = { clipboard.copy("password", state.generated) },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.ContentCopy,
                        )
                        IconButton(
                            onClick = state::regenerate,
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            StrengthBar(score)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.strength),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(scoreLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))
            if (state.passphraseMode) {
                SectionLabel(stringResource(R.string.word_count))
                Text(
                    text = state.wordCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = state.wordCount.toFloat(),
                    onValueChange = { state.changeWordCount(it.roundToInt()) },
                    valueRange = 3f..8f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.length),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = state.options.length.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = state.options.length.toFloat(),
                    onValueChange = { state.setLength(it.roundToInt()) },
                    valueRange = 8f..32f,
                    steps = 23,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = stringResource(R.string.uppercase),
                    checked = state.options.uppercase,
                    onCheckedChange = state::setUppercase,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = stringResource(R.string.lowercase),
                    checked = state.options.lowercase,
                    onCheckedChange = state::setLowercase,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = stringResource(R.string.numbers),
                    checked = state.options.digits,
                    onCheckedChange = state::setDigits,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    title = stringResource(R.string.symbols),
                    checked = state.options.symbols,
                    onCheckedChange = state::setSymbols,
                )
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = stringResource(R.string.generate),
                onClick = state::regenerate,
                icon = Icons.Outlined.AutoAwesome,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
