package com.ciphervault.app.di

import android.content.Context
import com.ciphervault.app.AppServices
import com.ciphervault.app.CipherVaultApp

/**
 * 统一的服务定位入口。
 *
 * 全应用只有这一处 `as CipherVaultApp` 强转，避免每个入口重复依赖 Application 具体类型。
 */
fun Context.appServices(): AppServices = (applicationContext as CipherVaultApp).services
