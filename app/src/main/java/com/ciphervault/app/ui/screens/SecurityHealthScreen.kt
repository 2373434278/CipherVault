package com.ciphervault.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ciphervault.app.R
import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.domain.ReusedGroup
import com.ciphervault.app.domain.SecurityAudit
import com.ciphervault.app.domain.SecurityReport
import com.ciphervault.app.ui.components.EmptyState
import com.ciphervault.app.ui.components.GlassCard
import com.ciphervault.app.ui.components.ObsidianScaffold
import com.ciphervault.app.ui.components.ObsidianTopBar
import com.ciphervault.app.ui.components.ScoreRing
import com.ciphervault.app.ui.components.SectionLabel
import com.ciphervault.app.ui.components.SettingRow
import com.ciphervault.app.ui.components.StrengthBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/** 密码安全体检：弱口令、重复使用与缺少 TOTP 的分组展示。 */
@Composable
fun SecurityHealthScreen(
    repository: VaultRepository,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
) {
    val report by produceState<SecurityReport?>(initialValue = null, repository) {
        repository.items.collect { items ->
            value = withContext(Dispatchers.Default) { SecurityAudit.audit(items) }
        }
    }

    ObsidianScaffold(
        topBar = {
            ObsidianTopBar(
                title = stringResource(R.string.security_health),
                subtitle = stringResource(R.string.security_health_subtitle),
                onBack = onBack,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (val current = report) {
                null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                else -> HealthContent(current, onOpenItem)
            }
        }
    }
}

@Composable
private fun HealthContent(report: SecurityReport, onOpenItem: (String) -> Unit) {
    when {
        report.totalLogins == 0 || report.checkedPasswords == 0 -> EmptyState(
            icon = Icons.Outlined.Shield,
            title = stringResource(R.string.security_health_empty),
            hint = stringResource(R.string.security_health_empty_hint),
        )

        report.isHealthy -> {
            EmptyState(
                icon = Icons.Outlined.VerifiedUser,
                title = stringResource(R.string.security_health_ok_title),
                hint = stringResource(R.string.security_health_ok_hint),
            )
            PrivacyNote()
        }

        else -> {
            ScoreCard(report)
            if (report.weak.isNotEmpty()) {
                IssueSection(
                    title = stringResource(R.string.security_health_weak),
                    count = report.weak.size,
                ) {
                    report.weak.forEach { item ->
                        SettingRow(
                            title = item.name,
                            subtitle = item.username.ifBlank { stringResource(R.string.security_health_weak_desc) },
                            onClick = { onOpenItem(item.id) },
                            trailing = { StrengthBar(item.strength, Modifier.width(72.dp)) },
                        )
                    }
                }
            }
            if (report.reusedGroups.isNotEmpty()) {
                IssueSection(
                    title = stringResource(R.string.security_health_reused),
                    count = report.reusedCount,
                ) {
                    report.reusedGroups.forEachIndexed { index, group ->
                        if (index > 0) Spacer(Modifier.height(4.dp))
                        GroupLabel(group)
                        group.items.forEach { item ->
                            SettingRow(
                                title = item.name,
                                subtitle = item.username,
                                onClick = { onOpenItem(item.id) },
                            )
                        }
                    }
                }
            }
            if (report.missingTotp.isNotEmpty()) {
                IssueSection(
                    title = stringResource(R.string.security_health_no_totp),
                    count = report.missingTotp.size,
                ) {
                    report.missingTotp.forEach { item ->
                        SettingRow(
                            title = item.name,
                            subtitle = item.username.ifBlank { stringResource(R.string.security_health_no_totp_desc) },
                            onClick = { onOpenItem(item.id) },
                        )
                    }
                }
            }
            PrivacyNote()
        }
    }
}

@Composable
private fun ScoreCard(report: SecurityReport) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScoreRing(report.score)
            Column {
                Text(
                    text = stringResource(R.string.security_health_score, report.score),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.security_health_checked, report.checkedPasswords),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.security_health_issues,
                        report.issueCount,
                        report.issueCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IssueSection(
    title: String,
    count: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Spacer(Modifier.height(4.dp))
    SectionLabel("$title · $count")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun GroupLabel(group: ReusedGroup) {
    Text(
        text = stringResource(R.string.security_health_reused_row, group.items.size),
        modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PrivacyNote() {
    Text(
        text = stringResource(R.string.security_health_privacy),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
