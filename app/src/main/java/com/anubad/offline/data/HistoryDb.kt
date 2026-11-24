package com.anubad.offline.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class HistoryItem(
    val id: Long = 0,
    val sourceText: String,
    val targetText: String,
    val sourceLang: String,
    val targetLang: String,
    val inputMethod: String, // typed | voice | camera
    val starred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Local translation history. Backed up by Android Auto Backup (allowBackup=true). */
class HistoryDb(context: Context) : SQLiteOpenHelper(context, "history.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE translations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_text TEXT NOT NULL,
                target_text TEXT NOT NULL,
                source_lang TEXT NOT NULL,
                target_lang TEXT NOT NULL,
                input_method TEXT NOT NULL,
                starred INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_translations_created ON translations(created_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insert(item: HistoryItem): Long {
        val values = ContentValues().apply {
            put("source_text", item.sourceText)
            put("target_text", item.targetText)
            put("source_lang", item.sourceLang)
            put("target_lang", item.targetLang)
            put("input_method", item.inputMethod)
            put("starred", if (item.starred) 1 else 0)
            put("created_at", item.createdAt)
        }
        return writableDatabase.insert("translations", null, values)
    }

    fun query(
        search: String = "",
        starredOnly: Boolean = false,
        from: Long? = null,
        to: Long? = null,
    ): List<HistoryItem> {
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (search.isNotBlank()) {
            where += "(source_text LIKE ? OR target_text LIKE ?)"
            val p = "%${search.trim()}%"
            args += p
            args += p
        }
        if (starredOnly) where += "starred = 1"
        if (from != null && to != null) {
            where += "created_at >= ? AND created_at < ?"
            args += from.toString()
            args += to.toString()
        }
        val selection = if (where.isEmpty()) null else where.joinToString(" AND ")
        val selectionArgs = if (args.isEmpty()) null else args.toTypedArray()

        val out = mutableListOf<HistoryItem>()
        readableDatabase.query(
            "translations", null, selection, selectionArgs, null, null, "created_at DESC"
        ).use { c ->
            val iId = c.getColumnIndexOrThrow("id")
            val iSrc = c.getColumnIndexOrThrow("source_text")
            val iTgt = c.getColumnIndexOrThrow("target_text")
            val iSl = c.getColumnIndexOrThrow("source_lang")
            val iTl = c.getColumnIndexOrThrow("target_lang")
            val iIn = c.getColumnIndexOrThrow("input_method")
            val iStar = c.getColumnIndexOrThrow("starred")
            val iAt = c.getColumnIndexOrThrow("created_at")
            while (c.moveToNext()) {
                out += HistoryItem(
                    id = c.getLong(iId),
                    sourceText = c.getString(iSrc),
                    targetText = c.getString(iTgt),
                    sourceLang = c.getString(iSl),
                    targetLang = c.getString(iTl),
                    inputMethod = c.getString(iIn),
                    starred = c.getInt(iStar) == 1,
                    createdAt = c.getLong(iAt),
                )
            }
        }
        return out
    }

    fun allTimestamps(): List<Long> {
        val out = mutableListOf<Long>()
        readableDatabase.rawQuery("SELECT created_at FROM translations", null).use { c ->
            while (c.moveToNext()) out += c.getLong(0)
        }
        return out
    }

    fun setStarred(id: Long, starred: Boolean) {
        val values = ContentValues().apply { put("starred", if (starred) 1 else 0) }
        writableDatabase.update("translations", values, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("translations", "id = ?", arrayOf(id.toString()))
    }

    fun clear() {
        writableDatabase.delete("translations", null, null)
    }
}
