package com.muhasibtech.app.util

import android.content.Context
import android.net.Uri
import com.muhasibtech.app.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Exports a consistent copy of the local Room database to a user-selected document.
 * The Storage Access Framework owns the destination URI; no storage permission is needed.
 */
object DatabaseBackup {
    suspend fun export(context: Context, database: AppDatabase, destination: Uri) {
        withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)").close()

            val source = context.getDatabasePath("muhasib_tech.db")
            check(source.isFile && source.length() > 0L) {
                "ملف قاعدة البيانات غير موجود أو فارغ"
            }

            val output = context.contentResolver.openOutputStream(destination, "w")
                ?: error("تعذر فتح ملف النسخة الاحتياطية")

            output.use { target ->
                source.inputStream().use { input -> input.copyTo(target) }
            }
        }
    }
}
