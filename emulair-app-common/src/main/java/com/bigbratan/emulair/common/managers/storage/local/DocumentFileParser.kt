package com.bigbratan.emulair.common.managers.storage.local

import android.content.Context
import com.bigbratan.emulair.common.utils.kotlin.calculateCrc32
import com.bigbratan.emulair.common.utils.kotlin.toStringCRC32
import com.bigbratan.emulair.common.managers.storage.BaseStorageFile
import com.bigbratan.emulair.common.managers.storage.StorageFile
import com.bigbratan.emulair.common.managers.storage.scanner.SerialScanner
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import timber.log.Timber

object DocumentFileParser {

    private const val MAX_CHECKED_ENTRIES = 3
    private const val SINGLE_ARCHIVE_THRESHOLD = 0.9
    private const val MAX_SIZE_CRC32 = 1_000_000_000

    fun parseDocumentFile(context: Context, baseStorageFile: BaseStorageFile): StorageFile {
        return if (baseStorageFile.extension == "zip") {
            Timber.d("Detected zip file. ${baseStorageFile.name}")
            parseZipFile(context, baseStorageFile)
        } else {
            Timber.d("Detected standard file. ${baseStorageFile.name}")
            parseStandardFile(context, baseStorageFile)
        }
    }

    private fun parseZipFile(context: Context, baseStorageFile: BaseStorageFile): StorageFile {
        val inputStream = context.contentResolver.openInputStream(baseStorageFile.uri)
        return ZipInputStream(inputStream).use {
            val gameEntry = findGameEntry(it, baseStorageFile.size)
            if (gameEntry != null) {
                Timber.d("Handing zip file as compressed game: ${baseStorageFile.name}")
                parseCompressedGame(baseStorageFile, gameEntry, it)
            } else {
                Timber.d("Handing zip file as standard: ${baseStorageFile.name}")
                parseStandardFile(context, baseStorageFile)
            }
        }
    }

    private fun parseCompressedGame(
        baseStorageFile: BaseStorageFile,
        entry: ZipEntry,
        zipInputStream: ZipInputStream
    ): StorageFile {
        Timber.d("Processing zipped entry: ${entry.name}")

        val diskInfo = SerialScanner.extractInfo(entry.name, zipInputStream)

        return StorageFile(
            entry.name,
            entry.size,
            entry.crc.toStringCRC32(),
            diskInfo.serial,
            baseStorageFile.uri,
            baseStorageFile.uri.path,
            diskInfo.systemID
        )
    }

    private fun parseStandardFile(context: Context, baseStorageFile: BaseStorageFile): StorageFile {
        val diskInfo = context.contentResolver.openInputStream(baseStorageFile.uri)
            ?.let { inputStream -> SerialScanner.extractInfo(baseStorageFile.name, inputStream) }

        val crc32 = if (baseStorageFile.size < MAX_SIZE_CRC32 && diskInfo?.serial == null) {
            context.contentResolver.openInputStream(baseStorageFile.uri)?.calculateCrc32()
        } else {
            null
        }

        Timber.d("Parsed standard file: $baseStorageFile")

        return StorageFile(
            baseStorageFile.name,
            baseStorageFile.size,
            crc32,
            diskInfo?.serial,
            baseStorageFile.uri,
            baseStorageFile.uri.path,
            diskInfo?.systemID
        )
    }

    /*
    Finds a ZIP entry which we assume is a game. Emulair only supports single archive games,
    so we are looking for an entry which occupies a large percentage of the archive space.
    This is a very fast heuristic way to compute and avoids reading the whole stream in most
    scenarios.
    */
    fun findGameEntry(openedInputStream: ZipInputStream, fileSize: Long = -1): ZipEntry? {
        for (i in 0..MAX_CHECKED_ENTRIES) {
            val entry = openedInputStream.nextEntry ?: break
            if (!isGameEntry(entry, fileSize)) continue
            return entry
        }
        return null
    }

    private fun isGameEntry(entry: ZipEntry, fileSize: Long): Boolean {
        if (fileSize <= 0 || entry.compressedSize <= 0) return false
        return (entry.compressedSize.toFloat() / fileSize.toFloat()) > SINGLE_ARCHIVE_THRESHOLD
    }
}
