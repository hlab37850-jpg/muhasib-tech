package com.muhasibtech.app.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.muhasibtech.app.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Exports a consistent copy of the local Room database and safely restores a
 * compatible backup selected through Android's Storage Access Framework.
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

    /**
     * Validates the selected SQLite file before touching the current database.
     * A copy of the current database is kept in app-private files as a recovery
     * point if replacing the database fails.
     */
    suspend fun restore(context: Context, database: AppDatabase, backupUri: Uri) {
        withContext(Dispatchers.IO) {
            val source = context.getDatabasePath("muhasib_tech.db")
            val staged = File(context.cacheDir, "muhasib_tech.restore.tmp")
            val safetyCopy = File(
                context.filesDir,
                "muhasib-tech-before-restore-${System.currentTimeMillis()}.db"
            )

            try {
                val input = context.contentResolver.openInputStream(backupUri)
                    ?: error("تعذر قراءة ملف النسخة الاحتياطية")
                input.use { stream ->
                    staged.outputStream().use { output -> stream.copyTo(output) }
                }

                check(staged.isFile && staged.length() > 0L) {
                    "ملف النسخة الاحتياطية فارغ"
                }
                validateBackup(staged)

                database.openHelper.writableDatabase
                    .query("PRAGMA wal_checkpoint(TRUNCATE)").close()

                check(source.isFile && source.length() > 0L) {
                    "قاعدة البيانات الحالية غير متاحة لإنشاء نسخة أمان"
                }
                source.copyTo(safetyCopy, overwrite = true)

                AppDatabase.closeInstanceForRestore()
                File(source.parentFile, source.name + "-wal").delete()
                File(source.parentFile, source.name + "-shm").delete()

                try {
                    staged.copyTo(source, overwrite = true)
                    check(source.isFile && source.length() > 0L) {
                        "تعذر تثبيت قاعدة البيانات المستعادة"
                    }
                } catch (failure: Throwable) {
                    safetyCopy.copyTo(source, overwrite = true)
                    throw failure
                }
            } finally {
                staged.delete()
            }
        }
    }

    private fun validateBackup(file: File) {
        val candidate = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        candidate.use { db ->
            val integrity = db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)
            }
            check(integrity) { "ملف النسخة الاحتياطية تالف" }
            check(db.version == 1) {
                "إصدار قاعدة البيانات غير متوافق مع هذا الإصدار من التطبيق"
            }

            val tables = mutableSetOf<String>()
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table'",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) tables.add(cursor.getString(0))
            }
            val requiredTables = setOf(
                "accounts",
                "journal_lines",
                "documents",
                "document_lines",
                "room_master_table"
            )
            check(tables.containsAll(requiredTables)) {
                "الملف المحدد ليس نسخة احتياطية متوافقة من المحاسب تك"
            }
        }
    }
}
