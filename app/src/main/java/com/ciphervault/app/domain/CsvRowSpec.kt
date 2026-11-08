package com.ciphervault.app.domain

/** CSV 列的解析结果；缺列为 -1。 */
data class CsvColumns(
    val name: Int,
    val url: Int,
    val username: Int,
    val password: Int,
    val notes: Int,
    val totp: Int,
    val folder: Int,
    val type: Int,
    val favorite: Int,
    val pinned: Int,
    val extra: Int,
)

/** CSV 布局：列映射 + 去掉表头后的数据行。 */
data class CsvLayout(
    val columns: CsvColumns,
    val dataRows: List<List<String>>,
)

/**
 * CSV 列布局规则。
 *
 * 同时支持两种契约：
 * 1. 有表头：按别名（如 `login_username`）定位；
 * 2. 无表头：按固定位置 `name,url,username,password,notes,totp` 读取。
 *
 * 导出列顺序见 [HEADER]，改动会破坏历史导出文件的回读。
 */
object CsvRowSpec {
    val HEADER = listOf(
        "name", "type", "folder", "url", "username", "password",
        "notes", "totp", "extra_json", "favorite", "pinned",
    )

    private val NAME_ALIASES = arrayOf("name", "title")
    private val URL_ALIASES = arrayOf("url", "login_uri", "website")
    private val USERNAME_ALIASES = arrayOf("username", "login_username")
    private val PASSWORD_ALIASES = arrayOf("password", "login_password")
    private val NOTES_ALIASES = arrayOf("notes")
    private val TOTP_ALIASES = arrayOf("totp", "login_totp", "otp")
    private val FOLDER_ALIASES = arrayOf("folder")
    private val TYPE_ALIASES = arrayOf("type")
    private val FAVORITE_ALIASES = arrayOf("favorite")
    private val PINNED_ALIASES = arrayOf("pinned")
    private val EXTRA_ALIASES = arrayOf("extra_json")

    /** 首行是否表头：只要出现 `name` 或 `title` 即视为表头。 */
    fun isHeader(row: List<String>): Boolean {
        val normalized = row.map { it.trim().lowercase() }
        return normalized.any { it == "name" || it == "title" }
    }

    fun layout(rows: List<List<String>>): CsvLayout {
        val header = rows.firstOrNull()?.map { it.trim().lowercase() }.orEmpty()
        val hasHeader = isHeader(header)
        val dataRows = if (hasHeader) rows.drop(1) else rows
        val columns = if (hasHeader) {
            fun idx(vararg names: String): Int =
                names.firstNotNullOfOrNull { name -> header.indexOf(name).takeIf { it >= 0 } } ?: -1

            CsvColumns(
                name = idx(*NAME_ALIASES),
                url = idx(*URL_ALIASES),
                username = idx(*USERNAME_ALIASES),
                password = idx(*PASSWORD_ALIASES),
                notes = idx(*NOTES_ALIASES),
                totp = idx(*TOTP_ALIASES),
                folder = idx(*FOLDER_ALIASES),
                type = idx(*TYPE_ALIASES),
                favorite = idx(*FAVORITE_ALIASES),
                pinned = idx(*PINNED_ALIASES),
                extra = idx(*EXTRA_ALIASES),
            )
        } else {
            CsvColumns(
                name = 0,
                url = 1,
                username = 2,
                password = 3,
                notes = 4,
                totp = 5,
                folder = -1,
                type = -1,
                favorite = -1,
                pinned = -1,
                extra = -1,
            )
        }
        return CsvLayout(columns, dataRows)
    }
}
