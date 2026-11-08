package com.ciphervault.app.autofill

import com.ciphervault.app.data.VaultRepository
import com.ciphervault.app.session.SessionState

/**
 * 自动填充的会话门禁。
 *
 * 只读取一次 `SessionState`，由 `Unlocked` 自带 repository，避免“先查状态再查 repository”
 * 之间被锁定插入的 TOCTOU 窗口。
 */
object AutofillRepositoryGate {
    sealed interface Result {
        data class Unlocked(val repository: VaultRepository) : Result
        data object Locked : Result
    }

    fun snapshot(state: SessionState): Result = when (state) {
        is SessionState.Unlocked -> Result.Unlocked(state.repository)
        else -> Result.Locked
    }
}
