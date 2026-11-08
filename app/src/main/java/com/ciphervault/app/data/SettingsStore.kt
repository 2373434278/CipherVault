package com.ciphervault.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ciphervault.app.core.port.SettingsGateway
import com.ciphervault.app.domain.VaultMeta
import com.ciphervault.app.domain.VaultMetaCodec
import com.ciphervault.app.domain.VaultSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ciphervault_settings")

/**
 * DataStore 设置实现。
 *
 * 同时保存保险库元数据（安全关键）与自动锁/剪贴板/截屏/排序等偏好；
 * 默认值统一来自 [SettingsDefaults]，避免 UI 首帧与存储值不一致。
 */
class SettingsStore(context: Context) : SettingsGateway {
    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val vaultMeta = stringPreferencesKey("vault_meta")
        val autoLockMinutes = intPreferencesKey("auto_lock_minutes")
        val clipboardClearSeconds = intPreferencesKey("clipboard_clear_seconds")
        val screenshotProtection = booleanPreferencesKey("screenshot_protection")
        val vaultSort = stringPreferencesKey("vault_sort")
    }

    override val metaFlow: Flow<VaultMeta?> = store.data.map { prefs ->
        prefs[Keys.vaultMeta]?.let(VaultMetaCodec::decode)
    }

    override val autoLockMinutes: Flow<Int> =
        store.data.map { it[Keys.autoLockMinutes] ?: SettingsDefaults.AUTO_LOCK_MINUTES }
    override val clipboardClearSeconds: Flow<Int> =
        store.data.map { it[Keys.clipboardClearSeconds] ?: SettingsDefaults.CLIPBOARD_CLEAR_SECONDS }
    override val screenshotProtection: Flow<Boolean> =
        store.data.map { it[Keys.screenshotProtection] ?: SettingsDefaults.SCREENSHOT_PROTECTION }
    override val vaultSortMode: Flow<VaultSort> = store.data.map { VaultSort.from(it[Keys.vaultSort]) }

    override suspend fun readMeta(): VaultMeta? = metaFlow.first()

    override suspend fun saveMeta(meta: VaultMeta) {
        store.edit { it[Keys.vaultMeta] = VaultMetaCodec.encode(meta) }
    }

    override suspend fun setAutoLockMinutes(minutes: Int) {
        store.edit { it[Keys.autoLockMinutes] = minutes }
    }

    override suspend fun setClipboardClearSeconds(seconds: Int) {
        store.edit { it[Keys.clipboardClearSeconds] = seconds }
    }

    override suspend fun setScreenshotProtection(enabled: Boolean) {
        store.edit { it[Keys.screenshotProtection] = enabled }
    }

    override suspend fun setVaultSort(mode: VaultSort) {
        store.edit { it[Keys.vaultSort] = mode.storage }
    }
}
