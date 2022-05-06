package com.wire.kalium.logger

import android.content.Context
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import kotlin.time.Duration.Companion.minutes

typealias LogElement = Triple<String, Severity, String?>

private const val LOG_FILE_NAME = "insights.log"
private const val LOG_FILE_MAX_SIZE_THRESHOLD = 5 * 1024 * 1024

actual class KaliumFileWriter : LogWriter() {

    private var flush = MutableStateFlow<Long>(0)
    private var flushCompleted = MutableStateFlow<Long>(0)

    private val LOG_FILE_RETENTION = TimeUnit.DAYS.toMillis(14)
    private val LOG_FILE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    val LOG_LINE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val logBuffer = MutableStateFlow(LogElement("", Severity.Warn, ""))
    private lateinit var filePath: String


    actual fun init(context: Context?) {
        context?.filesDir?.absolutePath?.let {
            filePath = try {
                getLogsDirectoryFromPath(context.filesDir?.absolutePath.toString())
            } catch (e: FileNotFoundException) {
                // Fallback to default path
                context.filesDir?.absolutePath.toString()
            }

        }

        var processed = 0
        GlobalScope.launch {
            logBuffer
                .debounce(500).onEach {
                    processed++
                    if (processed % 20 == 0) {
                        flush()
                    }
                }
//            .buffer(merge(flush, tickerFlow(5.minutes)).first())
                .collect {
                    try {
                        // Open file
                        val f = getFile(filePath, LOG_FILE_NAME)

                        // Write to log
                        FileWriter(f, true).use { fw ->
                            // Write log lines to the file
                            fw.append("${it.first}\t${it.second.name}\t${it.third}\n")


                            // Write a line indicating the number of log lines proceed
                            fw.append("${LOG_LINE_TIME_FORMAT.format(Date())}\t${it.second.name /* Verbose */}\tFlushing logs -- total processed: $processed\n")

                            fw.flush()
                        }

                        // Validate file size
                        flushCompleted.emit(f.length())
                    } catch (e: Exception) {

                    }

                }
            flushCompleted
                .filter { filesize -> filesize > LOG_FILE_MAX_SIZE_THRESHOLD }
                .collect {
                    rotateLogs(filePath, LOG_FILE_NAME)
                }

        }

    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        GlobalScope.launch {
            logBuffer.emit(LogElement(LOG_LINE_TIME_FORMAT.format(Date()), severity, message))

        }
    }

    private suspend fun flush(oncomplete: (() -> Unit)? = null) {
        oncomplete?.run {
            withTimeout(5.minutes) {
                flushCompleted
                    .take(1)
//                .subscribeOn(Schedulers.io())
                    .catch { emit(-1L) }
                    .filter { it > 0 }
                    .collect {
                        rotateLogs(filePath, LOG_FILE_NAME)

                        // Delegate back to caller
                        oncomplete()
                    }
            }
        }

        flush.emit(1L)
    }


    private fun getFile(path: String, name: String): File {
        val file = File(path, name)

        if (!file.exists() && !file.createNewFile()) {
            throw IOException("Unable to load log file")
        }

        if (!file.canWrite()) {
            throw IOException("Log file not writable")
        }

        return file
    }

    private fun rotateLogs(path: String, name: String) {
        val file = getFile(path, name)

        if (!compress(file)) {
            // Unable to compress file
            return
        }

        // Truncate the file to zero.
        PrintWriter(file).close()

        // Iterate over the gzipped files in the directory and delete the files outside the
        // retention period.
        val currentTime = System.currentTimeMillis()
        file.parentFile.listFiles()
            ?.filter {
                it.extension.lowercase(Locale.ROOT) == "gz"
                        && it.lastModified() + LOG_FILE_RETENTION < currentTime
            }?.map { it.delete() }
    }

    private fun getLogsDirectoryFromPath(path: String): String {

        val dir = File(path, "logs")

        if (!dir.exists() && !dir.mkdirs()) {
            throw FileNotFoundException("Unable to create logs file")
        }

        return dir.absolutePath
    }

    private fun compress(file: File): Boolean {
        try {
            val compressed =
                File(file.parentFile.absolutePath, "${file.name.substringBeforeLast(".")}_${LOG_FILE_TIME_FORMAT.format(Date())}.gz")

            FileInputStream(file).use { fis ->
                FileOutputStream(compressed).use { fos ->
                    GZIPOutputStream(fos).use { gzos ->

                        val buffer = ByteArray(1024)
                        var length = fis.read(buffer)

                        while (length > 0) {
                            gzos.write(buffer, 0, length)

                            length = fis.read(buffer)
                        }

                        // Finish file compressing and close all streams.
                        gzos.finish()
                    }
                }
            }
        } catch (e: IOException) {

            return false
        }

        return true
    }


}
