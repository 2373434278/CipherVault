package com.ciphervault.app.session

import com.ciphervault.app.data.VaultRepository

/** 会话状态机。 */
sealed interface SessionState {
    data object Loading : SessionState
    data object NeedsSetup : SessionState
    data object Locked : SessionState
    data class Unlocked(val repository: VaultRepository) : SessionState
}
