package com.makemission.makeflow.core.logging

import android.content.Context
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.AppLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val MAX_DB_ENTRIES = 1000
    private const val CRASH_FILE_NAME = "crash_log.txt"

    private var appLogDao: com.makemission.makeflow.data.local.dao.AppLogDao? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(database: AppDatabase) {
        appLogDao = database.appLogDao()
        pruneStale()
    }

    fun info(tag: String, message: String) {
        writeAsync("INFO", tag, message)
    }

    fun warn(tag: String, message: String) {
        writeAsync("WARN", tag, message)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val stackTrace = throwable?.let { formatStackTrace(it) }
        writeAsync("ERROR", tag, message, stackTrace)
    }

    fun writeSync(level: String, tag: String, message: String, stackTrace: String? = null) {
        val log = AppLogEntity(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            stackTrace = stackTrace
        )
        try {
            appLogDao?.insertSync(log)
        } catch (_: Exception) {
        }
    }

    fun writeSyncCrash(context: Context, throwable: Throwable) {
        val timestamp = System.currentTimeMillis()
        val stackTrace = formatStackTrace(throwable)
        val formatted = buildString {
            appendLine("=== CRASH LOG ===")
            appendLine("Timestamp: ${formatTimestamp(timestamp)}")
            appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message}")
            appendLine("Stack Trace:")
            appendLine(stackTrace)
            appendLine("=== END CRASH LOG ===")
            appendLine()
        }

        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            file.appendText(formatted)
        } catch (_: Exception) {
        }

        writeSync("ERROR", "Crash", "${throwable.javaClass.name}: ${throwable.message}", stackTrace)
    }

    suspend fun getCrashLogContent(context: Context): String? {
        return try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    fun clearCrashFile(context: Context) {
        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
    }

    private fun writeAsync(level: String, tag: String, message: String, stackTrace: String? = null) {
        scope.launch {
            val log = AppLogEntity(
                timestamp = System.currentTimeMillis(),
                level = level,
                tag = tag,
                message = message,
                stackTrace = stackTrace
            )
            try {
                appLogDao?.insert(log)
                pruneIfNeeded()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun pruneIfNeeded() {
        try {
            val dao = appLogDao ?: return
            val count = dao.count()
            if (count > MAX_DB_ENTRIES) {
                dao.pruneToMostRecent(MAX_DB_ENTRIES)
            }
        } catch (_: Exception) {
        }
    }

    private fun pruneStale() {
        scope.launch {
            try {
                val dao = appLogDao ?: return@launch
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                dao.deleteOlderThan(thirtyDaysAgo)
                pruneIfNeeded()
            } catch (_: Exception) {
            }
        }
    }

    private fun formatStackTrace(throwable: Throwable): String {
        return throwable.stackTraceToString()
    }

    fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }
}
