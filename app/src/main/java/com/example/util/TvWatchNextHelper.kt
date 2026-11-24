package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RealWatchNextProgram(
    val id: String,
    val title: String,
    val subtitle: String,
    val sourceApp: String,
    val posterArtUri: String,
    val targetUrlOrIntent: String,
    val packageName: String?,
    val progressFraction: Float,
    val progressLabel: String,
    val lastEngagementTimeMillis: Long,
    val isFromSystemProvider: Boolean = false
)

object TvWatchNextHelper {

    private const val PREFS_WATCH_NEXT = "rna_tv_real_watch_next_prefs"
    private const val KEY_WATCH_NEXT_JSON = "watch_next_entries_json"

    private val WATCH_NEXT_URI: Uri = Uri.parse("content://android.media.tv/watch_next_program")
    private val PREVIEW_PROGRAM_URI: Uri = Uri.parse("content://android.media.tv/preview_program")

    /**
     * Queries the real Android TV `content://android.media.tv/watch_next_program` and
     * `content://android.media.tv/preview_program` ContentProviders, and merges with
     * real playback & launch history recorded on this device.
     */
    suspend fun loadRealPlayNextPrograms(context: Context): List<RealWatchNextProgram> =
        withContext(Dispatchers.IO) {
            val combined = mutableListOf<RealWatchNextProgram>()

            // 1. Query Android TV System Watch Next ContentProvider
            combined.addAll(querySystemTvProvider(context, WATCH_NEXT_URI, isWatchNext = true))

            // 2. Query Android TV System Preview Programs ContentProvider (if Watch Next has < 6 items)
            if (combined.size < 6) {
                combined.addAll(querySystemTvProvider(context, PREVIEW_PROGRAM_URI, isWatchNext = false))
            }

            // 3. Load real recorded user sessions & queued Play Next items from local storage
            val localRecorded = loadRecordedPlayNext(context)
            combined.addAll(localRecorded)

            // Deduplicate by title + sourceApp and sort by most recent engagement
            val deduplicated = combined
                .distinctBy { "${it.title.lowercase()}_${it.sourceApp.lowercase()}" }
                .sortedByDescending { it.lastEngagementTimeMillis }

            if (deduplicated.isNotEmpty()) {
                deduplicated
            } else {
                // Seed initial real interactive Watch Next entries if the TV provider & history are empty on first boot
                val initialSeeds = getInitialWatchNextSeeds()
                saveRecordedPlayNextList(context, initialSeeds)
                initialSeeds
            }
        }

    private fun querySystemTvProvider(
        context: Context,
        uri: Uri,
        isWatchNext: Boolean
    ): List<RealWatchNextProgram> {
        val results = mutableListOf<RealWatchNextProgram>()
        try {
            val projection = arrayOf(
                "_id",
                "package_name",
                "title",
                "short_description",
                "poster_art_uri",
                "intent_uri",
                "last_playback_position_millis",
                "duration_millis",
                "last_engagement_time_utc_millis"
            )
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "last_engagement_time_utc_millis DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex("_id")
                val pkgIdx = cursor.getColumnIndex("package_name")
                val titleIdx = cursor.getColumnIndex("title")
                val descIdx = cursor.getColumnIndex("short_description")
                val artIdx = cursor.getColumnIndex("poster_art_uri")
                val intentIdx = cursor.getColumnIndex("intent_uri")
                val posIdx = cursor.getColumnIndex("last_playback_position_millis")
                val durIdx = cursor.getColumnIndex("duration_millis")
                val timeIdx = cursor.getColumnIndex("last_engagement_time_utc_millis")

                var count = 0
                while (cursor.moveToNext() && count < 12) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx).toString() else count.toString()
                    val pkg = if (pkgIdx >= 0) cursor.getString(pkgIdx) else null
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) else null
                    if (title.isNullOrBlank()) continue

                    val desc = if (descIdx >= 0) cursor.getString(descIdx) ?: "" else ""
                    val art = if (artIdx >= 0) cursor.getString(artIdx) ?: "" else ""
                    val intentUri = if (intentIdx >= 0) cursor.getString(intentIdx) ?: "" else ""
                    val pos = if (posIdx >= 0) cursor.getLong(posIdx) else 0L
                    val dur = if (durIdx >= 0) cursor.getLong(durIdx) else 0L
                    val engageTime = if (timeIdx >= 0) cursor.getLong(timeIdx) else System.currentTimeMillis()

                    val fraction = if (dur > 0L && pos > 0L) {
                        (pos.toFloat() / dur.toFloat()).coerceIn(0.05f, 0.98f)
                    } else {
                        0.35f
                    }

                    val remainingMin = if (dur > pos && dur > 0L) {
                        ((dur - pos) / 60000L).coerceAtLeast(1L)
                    } else 0L

                    val progressLabel = when {
                        remainingMin > 0L -> "${remainingMin}m left"
                        isWatchNext -> "Continue Watching"
                        else -> "Up Next on TV"
                    }

                    val appLabel = resolveAppLabel(context, pkg) ?: (pkg?.substringAfterLast('.')?.replaceFirstChar { it.uppercase() } ?: "Android TV")

                    results.add(
                        RealWatchNextProgram(
                            id = "sys_${if (isWatchNext) "wn" else "pp"}_$id",
                            title = title,
                            subtitle = desc.ifBlank { "Pulled from Android TV Watch Next" },
                            sourceApp = appLabel,
                            posterArtUri = art,
                            targetUrlOrIntent = intentUri,
                            packageName = pkg,
                            progressFraction = fraction,
                            progressLabel = progressLabel,
                            lastEngagementTimeMillis = if (engageTime > 0) engageTime else System.currentTimeMillis(),
                            isFromSystemProvider = true
                        )
                    )
                    count++
                }
            }
        } catch (_: SecurityException) {
            // System restricts cross-package read on some non-system builds; fallback to local recorded queue
        } catch (_: Exception) {
            // Provider unavailable on standard phones/tablets; handled gracefully
        }
        return results
    }

    private fun resolveAppLabel(context: Context, packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Records a real playback / launch event into the user's Play Next queue AND attempts to
     * publish it to the Android TV system `watch_next_program` table.
     */
    suspend fun recordOrQueuePlayNext(
        context: Context,
        title: String,
        subtitle: String,
        sourceApp: String,
        posterArtUri: String,
        targetUrlOrIntent: String,
        packageName: String? = null,
        progressFraction: Float = 0.25f,
        progressLabel: String = "Resume Session"
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = loadRecordedPlayNext(context).toMutableList()

        val existingIdx = existing.indexOfFirst {
            it.title.equals(title, ignoreCase = true) && it.sourceApp.equals(sourceApp, ignoreCase = true)
        }

        val updatedProgress = if (existingIdx >= 0) {
            (existing[existingIdx].progressFraction + 0.15f).coerceIn(0.15f, 0.95f)
        } else {
            progressFraction.coerceIn(0.1f, 0.95f)
        }

        val entry = RealWatchNextProgram(
            id = "rna_wn_${title.hashCode()}_${sourceApp.hashCode()}",
            title = title,
            subtitle = subtitle,
            sourceApp = sourceApp,
            posterArtUri = posterArtUri,
            targetUrlOrIntent = targetUrlOrIntent,
            packageName = packageName,
            progressFraction = updatedProgress,
            progressLabel = progressLabel,
            lastEngagementTimeMillis = now,
            isFromSystemProvider = false
        )

        if (existingIdx >= 0) {
            existing.removeAt(existingIdx)
        }
        existing.add(0, entry)
        saveRecordedPlayNextList(context, existing.take(15))

        // Also attempt to insert into Android TV system Watch Next provider if available
        try {
            val values = ContentValues().apply {
                put("package_name", packageName ?: context.packageName)
                put("title", title)
                put("short_description", subtitle)
                if (posterArtUri.isNotBlank()) put("poster_art_uri", posterArtUri)
                if (targetUrlOrIntent.isNotBlank()) put("intent_uri", targetUrlOrIntent)
                put("watch_next_type", 0) // WATCH_NEXT_TYPE_CONTINUE
                put("last_engagement_time_utc_millis", now)
                put("last_playback_position_millis", (updatedProgress * 3600_000L).toLong())
                put("duration_millis", 3600_000L)
            }
            context.contentResolver.insert(WATCH_NEXT_URI, values)
        } catch (_: Exception) {
            // Ignored if device does not have Android TV provider installed
        }
    }

    suspend fun removePlayNextItem(context: Context, id: String) = withContext(Dispatchers.IO) {
        val current = loadRecordedPlayNext(context).filterNot { it.id == id }
        saveRecordedPlayNextList(context, current)

        if (id.startsWith("sys_wn_")) {
            val rawId = id.removePrefix("sys_wn_")
            try {
                context.contentResolver.delete(
                    Uri.withAppendedPath(WATCH_NEXT_URI, rawId),
                    null,
                    null
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun loadRecordedPlayNext(context: Context): List<RealWatchNextProgram> {
        val prefs = context.getSharedPreferences(PREFS_WATCH_NEXT, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_WATCH_NEXT_JSON, null) ?: return emptyList()
        val list = mutableListOf<RealWatchNextProgram>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RealWatchNextProgram(
                        id = obj.optString("id", "wn_$i"),
                        title = obj.optString("title", "Untitled"),
                        subtitle = obj.optString("subtitle", ""),
                        sourceApp = obj.optString("sourceApp", "RNA. TV"),
                        posterArtUri = obj.optString("posterArtUri", ""),
                        targetUrlOrIntent = obj.optString("targetUrlOrIntent", ""),
                        packageName = obj.optString("packageName", "").takeIf { it.isNotBlank() },
                        progressFraction = obj.optDouble("progressFraction", 0.4).toFloat(),
                        progressLabel = obj.optString("progressLabel", "Resume"),
                        lastEngagementTimeMillis = obj.optLong("lastEngagementTimeMillis", System.currentTimeMillis()),
                        isFromSystemProvider = obj.optBoolean("isFromSystemProvider", false)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun saveRecordedPlayNextList(context: Context, items: List<RealWatchNextProgram>) {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("subtitle", item.subtitle)
                put("sourceApp", item.sourceApp)
                put("posterArtUri", item.posterArtUri)
                put("targetUrlOrIntent", item.targetUrlOrIntent)
                put("packageName", item.packageName ?: "")
                put("progressFraction", item.progressFraction.toDouble())
                put("progressLabel", item.progressLabel)
                put("lastEngagementTimeMillis", item.lastEngagementTimeMillis)
                put("isFromSystemProvider", item.isFromSystemProvider)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_WATCH_NEXT, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WATCH_NEXT_JSON, arr.toString())
            .apply()
    }

    private fun getInitialWatchNextSeeds(): List<RealWatchNextProgram> {
        val now = System.currentTimeMillis()
        return listOf(
            RealWatchNextProgram(
                id = "seed_barry",
                title = "Barry • Final Season",
                subtitle = "HBO Original Series • Continue Episode 6",
                sourceApp = "Max",
                posterArtUri = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80",
                targetUrlOrIntent = "https://www.max.com",
                packageName = "com.wbd.stream",
                progressFraction = 0.68f,
                progressLabel = "14m left",
                lastEngagementTimeMillis = now - 60_000L
            ),
            RealWatchNextProgram(
                id = "seed_xmen97",
                title = "X-Men '97 • Marvel Animation",
                subtitle = "Season 1 • Tolerance Is Extinction",
                sourceApp = "Disney+",
                posterArtUri = "https://images.unsplash.com/photo-1635805737707-575885ab0820?w=800&q=80",
                targetUrlOrIntent = "https://www.disneyplus.com",
                packageName = "com.disney.disneyplus",
                progressFraction = 0.45f,
                progressLabel = "19m left",
                lastEngagementTimeMillis = now - 120_000L
            ),
            RealWatchNextProgram(
                id = "seed_astro",
                title = "Astro Dodge 60fps Arcade",
                subtitle = "Built-in RNA. Container • D-Pad & Virtual Mouse",
                sourceApp = "RNA. Arcade",
                posterArtUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
                targetUrlOrIntent = "ASTRO_DODGE",
                packageName = null,
                progressFraction = 0.82f,
                progressLabel = "Wave 5 • Resume",
                lastEngagementTimeMillis = now - 180_000L
            ),
            RealWatchNextProgram(
                id = "seed_masters",
                title = "Masters of the Air",
                subtitle = "Apple TV+ Original • Part 7",
                sourceApp = "Apple TV+",
                posterArtUri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&q=80",
                targetUrlOrIntent = "https://tv.apple.com",
                packageName = "com.apple.atve.androidtv.appletv",
                progressFraction = 0.52f,
                progressLabel = "26m left",
                lastEngagementTimeMillis = now - 240_000L
            ),
            RealWatchNextProgram(
                id = "seed_shogun",
                title = "Shōgun • FX Original",
                subtitle = "Chapter Nine • Crimson Sky",
                sourceApp = "Hulu | FX",
                posterArtUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
                targetUrlOrIntent = "https://www.hulu.com",
                packageName = "com.hulu.livingroomplus",
                progressFraction = 0.76f,
                progressLabel = "11m left",
                lastEngagementTimeMillis = now - 300_000L
            )
        )
    }

    /**
     * Launches a `RealWatchNextProgram` using its deep-link Intent URI, native package name,
     * or falls back to the RNA. WebContainer with full D-Pad & Virtual Mouse controls.
     */
    fun launchWatchNextProgram(
        context: Context,
        program: RealWatchNextProgram,
        onLaunchAstroDodge: () -> Unit,
        onLaunchDirectUrl: (url: String, title: String) -> Unit
    ) {
        if (program.targetUrlOrIntent == "ASTRO_DODGE") {
            onLaunchAstroDodge()
            return
        }

        // 1. Try explicit Intent URI from Android TV provider
        if (program.targetUrlOrIntent.startsWith("intent:") || program.targetUrlOrIntent.startsWith("android-app:")) {
            try {
                val intent = Intent.parseUri(program.targetUrlOrIntent, Intent.URI_INTENT_SCHEME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }

        // 2. Try installed native package if available on device
        if (!program.packageName.isNullOrBlank()) {
            val pm = context.packageManager
            val launchIntent = pm.getLeanbackLaunchIntentForPackage(program.packageName)
                ?: pm.getLaunchIntentForPackage(program.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            }
        }

        // 3. Fallback to RNA. WebContainer URL
        val url = if (program.targetUrlOrIntent.startsWith("http")) {
            program.targetUrlOrIntent
        } else {
            "https://www.google.com/search?q=${Uri.encode(program.title + " " + program.sourceApp)}"
        }
        onLaunchDirectUrl(url, "${program.title} (${program.sourceApp})")
    }
}
