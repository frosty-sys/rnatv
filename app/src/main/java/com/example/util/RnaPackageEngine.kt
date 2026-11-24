package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.RnaApp
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object RnaPackageEngine {

    /**
     * Exports an RNA app package directly to the device's public Downloads directory.
     * Uses MediaStore on Android 10+ (API 29+) or direct Downloads file on Android 9 and below / fallback,
     * so users can see, share, and export the real .rna file from their device storage.
     */
    fun exportRnaPackageToDownloads(context: Context, app: RnaApp): File? {
        val rawBaseName = app.title.ifBlank { "rna_app" }.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        val fileName = "${rawBaseName}_v${app.version}.rna"

        // Strategy A: Direct file in Environment.DIRECTORY_DOWNLOADS
        try {
            val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!publicDownloadsDir.exists()) {
                publicDownloadsDir.mkdirs()
            }
            if (publicDownloadsDir.exists() && (publicDownloadsDir.canWrite() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)) {
                val targetFile = File(publicDownloadsDir, fileName)
                if (exportRnaPackage(context, app, targetFile)) {
                    // Trigger media scanner so it shows immediately in file managers and Downloads
                    try {
                        val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                            data = Uri.fromFile(targetFile)
                        }
                        context.sendBroadcast(scanIntent)
                    } catch (_: Exception) {}
                    return targetFile
                }
            }
        } catch (_: Exception) {}

        // Strategy B: MediaStore.Downloads for scoped storage on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // First write to a temporary file
                val tempFile = File(context.cacheDir, fileName)
                if (exportRnaPackage(context, app, tempFile)) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            tempFile.inputStream().use { input -> input.copyTo(out) }
                        }
                        tempFile.delete()
                        // Also provide direct file reference if accessible
                        val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val publicFile = File(publicDownloadsDir, fileName)
                        return if (publicFile.exists()) publicFile else tempFile
                    }
                }
            } catch (_: Exception) {}
        }

        // Strategy C: External files directory under Downloads
        try {
            val externalDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.getExternalFilesDir(null)
                ?: File(context.filesDir, "Downloads").apply { mkdirs() }
            val fallbackFile = File(externalDownloadsDir, fileName)
            if (exportRnaPackage(context, app, fallbackFile)) {
                return fallbackFile
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Creates an Intent to share or send the exported .rna file via device apps (Drive, Bluetooth, Messaging, etc.)
     */
    fun createShareRnaIntent(context: Context, exportedFile: File, appTitle: String): Intent {
        val fileUri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportedFile)
        } catch (_: Exception) {
            Uri.fromFile(exportedFile)
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "RNA Package: $appTitle")
            putExtra(Intent.EXTRA_TEXT, "Exported RNA App Package: $appTitle (${exportedFile.name})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun exportRnaPackage(context: Context, app: RnaApp, destinationFile: File): Boolean {
        return try {
            val fos = FileOutputStream(destinationFile)
            val zos = ZipOutputStream(BufferedOutputStream(fos))

            // 1. Write manifest.json
            val manifestJson = JSONObject().apply {
                put("id", app.id)
                put("packageName", app.packageName)
                put("title", app.title)
                put("version", app.version)
                put("sourceType", app.sourceType)
                put("sourceUriOrContent", app.sourceUriOrContent)
                put("iconBase64", app.iconBase64 ?: "")
                put("bannerBase64", app.bannerBase64 ?: "")
                put("isTvEnabled", app.isTvEnabled)
                put("defaultInputMode", app.defaultInputMode)
                put("mouseSpeed", app.mouseSpeed.toDouble())
                put("forceDarkMode", app.forceDarkMode)
                put("showLoadingBar", app.showLoadingBar)
                put("loadingBarColor", app.loadingBarColor)
                put("assignedSlot", app.assignedSlot ?: -1)
                put("isPinnedShortcut", app.isPinnedShortcut)
                val permsArray = JSONArray()
                app.permissions.forEach { permsArray.put(it) }
                put("permissions", permsArray)
            }

            val manifestEntry = ZipEntry("manifest.json")
            zos.putNextEntry(manifestEntry)
            zos.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. If app has local bundle files in app storage, package them into web/
            if (app.sourceType == "BUNDLE" && app.sourceUriOrContent.startsWith("file://")) {
                val bundleFile = File(app.sourceUriOrContent.removePrefix("file://"))
                val bundleDir = if (bundleFile.isDirectory) bundleFile else bundleFile.parentFile
                if (bundleDir != null && bundleDir.exists() && bundleDir.isDirectory) {
                    bundleDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relPath = "web/" + file.relativeTo(bundleDir).path
                            val entry = ZipEntry(relPath)
                            zos.putNextEntry(entry)
                            file.inputStream().use { input -> input.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }

            zos.finish()
            zos.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun parseRnaPackage(context: Context, uri: Uri): RnaApp? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val zis = ZipInputStream(BufferedInputStream(inputStream))

            var manifestContent: String? = null
            var hasWebFiles = false
            var entry: ZipEntry? = zis.nextEntry

            val targetDir = File(context.filesDir, "imported_${System.currentTimeMillis()}")
            targetDir.mkdirs()

            while (entry != null) {
                if (entry.name == "manifest.json") {
                    manifestContent = zis.bufferedReader(Charsets.UTF_8).readText()
                } else if (entry.name.startsWith("web/") || entry.name.endsWith(".html") || entry.name.endsWith(".js") || entry.name.endsWith(".css") || entry.name.endsWith(".ts")) {
                    hasWebFiles = true
                    val relPath = entry.name.removePrefix("web/")
                    val destFile = File(targetDir, relPath)
                    destFile.parentFile?.mkdirs()
                    if (!entry.isDirectory) {
                        FileOutputStream(destFile).use { out ->
                            zis.copyTo(out)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()

            if (manifestContent != null) {
                val json = JSONObject(manifestContent)
                val permsArray = json.optJSONArray("permissions") ?: JSONArray()
                val perms = mutableListOf<String>()
                for (i in 0 until permsArray.length()) {
                    perms.add(permsArray.getString(i))
                }

                val sourceType = json.optString("sourceType", "URL")
                var sourceContent = json.optString("sourceUriOrContent", "")
                if (sourceType == "BUNDLE") {
                    val indexFile = File(targetDir, "index.html")
                    sourceContent = "file://${indexFile.absolutePath}"
                }

                val assignedSlot = json.optInt("assignedSlot", -1).let { if (it > 0) it else null }

                RnaApp(
                    id = UUID.randomUUID().toString(),
                    packageName = json.optString("packageName", "com.rna.custom"),
                    title = json.optString("title", "Imported Web App"),
                    version = json.optString("version", "1.0.0"),
                    sourceType = sourceType,
                    sourceUriOrContent = sourceContent,
                    iconBase64 = json.optString("iconBase64", "").ifBlank { null },
                    bannerBase64 = json.optString("bannerBase64", "").ifBlank { null },
                    isTvEnabled = json.optBoolean("isTvEnabled", true),
                    defaultInputMode = json.optString("defaultInputMode", "DPAD"),
                    mouseSpeed = json.optDouble("mouseSpeed", 1.5).toFloat(),
                    forceDarkMode = json.optBoolean("forceDarkMode", false),
                    showLoadingBar = json.optBoolean("showLoadingBar", true),
                    loadingBarColor = json.optString("loadingBarColor", "#00E5FF"),
                    isInstalled = true,
                    permissions = if (perms.isEmpty()) listOf("INTERNET") else perms,
                    assignedSlot = assignedSlot,
                    isPinnedShortcut = json.optBoolean("isPinnedShortcut", false)
                )
            } else if (hasWebFiles) {
                // Imported a raw ZIP bundle of HTML/TS/Vite files
                val indexHtml = targetDir.walkTopDown().firstOrNull { it.name.equals("index.html", ignoreCase = true) }
                    ?: targetDir.walkTopDown().firstOrNull { it.name.endsWith(".html", ignoreCase = true) }
                if (indexHtml != null) {
                    RnaApp(
                        id = UUID.randomUUID().toString(),
                        packageName = "com.rna.bundle.${System.currentTimeMillis() % 10000}",
                        title = "Web Bundle App",
                        version = "1.0.0",
                        sourceType = "BUNDLE",
                        sourceUriOrContent = "file://${indexHtml.absolutePath}",
                        isTvEnabled = true,
                        defaultInputMode = "DPAD",
                        mouseSpeed = 1.5f,
                        showLoadingBar = true,
                        loadingBarColor = "#00E5FF"
                    )
                } else null
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
