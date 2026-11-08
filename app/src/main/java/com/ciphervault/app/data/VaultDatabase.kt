package com.ciphervault.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** SQLCipher 加密的 Room 数据库；口令由当前会话的 vaultKey 提供。 */
@Database(
    entities = [VaultItemEntity::class, FolderEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
}

/** 数据库工厂；每次解锁都用当前 vaultKey 新建连接。 */
object VaultDatabaseFactory {
    // v2 增加置顶字段；迁移必须保持可重复。
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE vault_items ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun open(context: Context, vaultKey: ByteArray): VaultDatabase {
        val factory = SupportOpenHelperFactory(vaultKey.copyOf(), null, false)
        return Room.databaseBuilder(context.applicationContext, VaultDatabase::class.java, "ciphervault.db")
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
