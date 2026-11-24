package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.WindowManager
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tv.FileExplorerPickerMode
import com.example.ui.tv.RnaFileExplorerScreen
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder

class RnaFileExplorerActivity : ComponentActivity() {
    private var resolvedInitialPath by mutableStateOf<String?>(null)
    private var resolvedInitialCategory by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                RnaFileExplorerScreen(
                    initialPath = resolvedInitialPath,
                    initialCategory = resolvedInitialCategory,
                    pickerMode = FileExplorerPickerMode.NONE,
                    onFilePicked = null,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        val explicitPath = incomingIntent?.getStringExtra(EXTRA_INITIAL_PATH)
        val explicitCategory = incomingIntent?.getStringExtra(EXTRA_INITIAL_CATEGORY)
        if (!explicitPath.isNullOrBlank()) {
            resolvedInitialPath = explicitPath
            resolvedInitialCategory = explicitCategory
            return
        }
        val uri: Uri? = incomingIntent?.data ?: incomingIntent?.let {
            @Suppress("DEPRECATION")
            it.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }
        if (uri != null) {
            val localFile = resolveUriToReadableFile(uri, incomingIntent?.type)
            if (localFile != null && localFile.exists()) {
                resolvedInitialPath = localFile.absolutePath
                resolvedInitialCategory = explicitCategory
                return
            }
        }
        resolvedInitialPath = explicitPath
        resolvedInitialCategory = explicitCategory
    }

    private fun resolveUriToReadableFile(uri: Uri, mimeTypeHint: String?): File? {
        try {
            if (uri.scheme.equals("file", ignoreCase = true)) {
                val path = uri.path ?: return null
                val direct = File(path)
                if (direct.exists()) return direct
                val decoded = File(URLDecoder.decode(path, "UTF-8"))
                if (decoded.exists()) return decoded
            }

            // Try matching direct external storage path if encoded in content:// URI
            val uriPath = uri.path.orEmpty()
            val extRoot = Environment.getExternalStorageDirectory()
            if (extRoot != null) {
                val primaryIdx = uriPath.indexOf("primary:")
                if (primaryIdx >= 0) {
                    val rel = uriPath.substring(primaryIdx + "primary:".length)
                    val candidate = File(extRoot, rel)
                    if (candidate.exists() && candidate.canRead()) return candidate
                }
                val rootIdx = uriPath.indexOf("/storage/")
                if (rootIdx >= 0) {
                    val sub = uriPath.substring(rootIdx)
                    val candidate = File(sub)
                    if (candidate.exists() && candidate.canRead()) return candidate
                }
            }

            // Determine display name and extension from ContentResolver
            var displayName: String? = null
            try {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {
            }

            if (displayName.isNullOrBlank()) {
                displayName = uri.lastPathSegment?.substringAfterLast('/')
            }

            val resolvedMime = mimeTypeHint ?: try { contentResolver.getType(uri) } catch (_: Exception) { null }
            val extFromMime = resolvedMime?.let {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
            }

            val safeBaseName = (displayName ?: "opened_file")
                .replace(Regex("[^a-zA-Z0-9._\\- ()]"), "_")
                .ifBlank { "opened_file" }

            val finalFileName = if (! safeBaseName.contains('.') && !extFromMime.isNullOrBlank()) {
                "$safeBaseName.$extFromMime"
            } else {
                safeBaseName
            }

            val targetDir = File(filesDir, "external_opened_files").apply { mkdirs() }
            val targetFile = File(targetDir, finalFileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0L) {
                return targetFile
            }
        } catch (_: Exception) {
        }
        return null
    }

    companion object {
        const val EXTRA_INITIAL_PATH = "extra_initial_path"
        const val EXTRA_INITIAL_CATEGORY = "extra_initial_category"
    }
}
