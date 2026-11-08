package com.ciphervault.app.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportExportStateHolderTest {
    @Test
    fun dialogsAreMutuallyExclusive() {
        val state = ImportExportStateHolder()

        state.openExportPassword()
        assertEquals(TransferDialog.ExportPassword, state.dialog)

        state.openPlaintextWarning()
        assertEquals(TransferDialog.PlaintextWarning, state.dialog)

        state.openImportPassword("backup-json")
        assertEquals(TransferDialog.ImportPassword, state.dialog)
        assertEquals("backup-json", state.pendingImportText)
        assertEquals("", state.importPassword)

        state.showMessage("done")
        assertEquals(TransferDialog.Message("done"), state.dialog)

        state.dismiss()
        assertNull(state.dialog)
    }

    @Test
    fun loadingAndPasswordResetAreCentralized() {
        val state = ImportExportStateHolder()
        assertFalse(state.loading)

        state.startLoading()
        assertTrue(state.loading)
        state.stopLoading()
        assertFalse(state.loading)

        state.exportPassword = "secret"
        state.exportPasswordConfirm = "secret"
        state.exportPasswordError = "err"
        state.resetExportPasswords()
        assertEquals("", state.exportPassword)
        assertEquals("", state.exportPasswordConfirm)
        assertNull(state.exportPasswordError)

        state.pendingExportContent = "payload"
        state.clearPendingExport()
        assertNull(state.pendingExportContent)
    }
}
