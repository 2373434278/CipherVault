package com.ciphervault.app.domain

import java.security.SecureRandom
import java.util.Random

/** 随机密码生成选项；字符集本身已排除 I/l/O/0/1 等易混淆字符。 */
data class PasswordOptions(
    val length: Int = 16,
    val uppercase: Boolean = true,
    val lowercase: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
)

/**
 * 随机密码与易记口令生成器。
 *
 * 生成逻辑通过 [Random] 参数注入随机源，测试可传入固定种子；生产环境默认使用 [SecureRandom]。
 */
object PasswordGenerator {
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?"
    private val defaultRandom: Random = SecureRandom()

    private val words = listOf(
        "amber", "anchor", "apple", "apricot", "atlas", "aurora", "bamboo", "beacon",
        "birch", "bison", "bloom", "breeze", "cactus", "canyon", "cedar", "cipher",
        "cobalt", "comet", "coral", "cosmos", "cricket", "crystal", "dawn", "delta",
        "dune", "eagle", "ember", "emerald", "falcon", "feather", "fjord", "forest",
        "fossil", "fox", "galaxy", "garden", "glacier", "granite", "harbor", "hazel",
        "heron", "honey", "island", "ivory", "jade", "jasmine", "jungle", "kite",
        "lagoon", "lantern", "lark", "lemon", "lily", "lotus", "lunar", "maple",
        "marble", "meadow", "meteor", "mist", "moon", "moss", "nebula", "nectar",
        "oak", "ocean", "olive", "onyx", "orchid", "otter", "pebble", "pepper",
        "pine", "planet", "plum", "pollen", "quartz", "quill", "raven", "reef",
        "river", "robin", "saffron", "sage", "sail", "shadow", "silver", "sky",
        "snow", "solar", "sparrow", "spruce", "star", "stone", "storm", "sunset",
        "tiger", "topaz", "trail", "tulip", "umber", "valley", "velvet", "violet",
        "willow", "wind", "wolf", "zephyr",
    )

    fun generate(options: PasswordOptions, random: Random = defaultRandom): String {
        val sets = buildList {
            if (options.uppercase) add(UPPER)
            if (options.lowercase) add(LOWER)
            if (options.digits) add(DIGITS)
            if (options.symbols) add(SYMBOLS)
        }.ifEmpty { listOf(LOWER) }
        val length = options.length.coerceIn(8, 64)
        val required = sets.map { it[random.nextInt(it.length)] }.toMutableList()
        val all = sets.joinToString("")
        while (required.size < length) required.add(all[random.nextInt(all.length)])
        java.util.Collections.shuffle(required, random)
        return required.joinToString("")
    }

    fun generatePassphrase(wordCount: Int = 5, separator: String = "-", random: Random = defaultRandom): String {
        val count = wordCount.coerceIn(3, 10)
        return List(count) { words[random.nextInt(words.size)] }
            .joinToString(separator) { word -> word.replaceFirstChar { it.uppercase() } }
    }

    /** 0..4 的强度分：长度与字符类别共同决定；空口令为 0。 */
    fun strength(password: String): Int {
        if (password.isBlank()) return 0
        var score = when {
            password.length >= 20 -> 2
            password.length >= 14 -> 1
            password.length >= 10 -> 0
            else -> -1
        }
        val classes = listOf(
            password.any { it.isUpperCase() },
            password.any { it.isLowerCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() },
        ).count { it }
        score += when {
            classes >= 4 -> 2
            classes == 3 -> 1
            else -> 0
        }
        return score.coerceIn(0, 4)
    }
}
