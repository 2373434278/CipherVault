package com.ciphervault.app.core.port

/** 剪贴板端口；实现负责敏感标记与定时清理。 */
interface ClipboardGateway {
    fun copy(label: String, value: String)
}
