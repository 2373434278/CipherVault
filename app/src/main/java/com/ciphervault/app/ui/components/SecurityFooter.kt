package com.ciphervault.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.ciphervault.app.R

/** 加密栈页脚；三处页面共用同一文案与默认字号。 */
@Composable
fun SecurityFooter(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp),
    textAlign: TextAlign? = null,
) {
    Text(
        text = stringResource(R.string.crypto_stack),
        modifier = modifier.fillMaxWidth(),
        style = style,
        textAlign = textAlign,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
