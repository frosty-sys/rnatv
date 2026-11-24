package com.example.data

import android.content.Context
import android.os.Environment
import com.example.data.model.RnaApp
import com.example.util.RnaPackageEngine
import com.example.util.ShortcutHelper
import com.example.util.StarterApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class RnaRepository(
    private val context: Context,
    private val rnaAppDao: RnaAppDao
) {
    val allApps: Flow<List<RnaApp>> = rnaAppDao.getAllApps().map { entities ->
        entities.map { it.toDomain() }
    }

    val allTvCustomizations: Flow<List<TvAppCustomEntity>> = rnaAppDao.getAllTvCustomizations()

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        // Ensure the 3 legacy test RNA apps are removed if they exist from an earlier install
        StarterApps.LEGACY_TEST_APP_IDS.forEach { legacyId ->
            val existing = rnaAppDao.getAppById(legacyId)
            if (existing != null) {
                existing.assignedSlot?.let { slot ->
                    ShortcutHelper.updateActivityAlias(context, slot, false)
                }
                rnaAppDao.deleteAppById(legacyId)
            }
        }
    }

    suspend fun getAppById(id: String): RnaApp? = withContext(Dispatchers.IO) {
        rnaAppDao.getAppById(id)?.toDomain()
    }

    suspend fun getAppBySlot(slot: Int): RnaApp? = withContext(Dispatchers.IO) {
        rnaAppDao.getAppBySlot(slot)?.toDomain()
    }

    suspend fun getNextAvailableSlot(): Int? = withContext(Dispatchers.IO) {
        val usedSlots = rnaAppDao.getUsedSlots().toSet()
        (1..20).firstOrNull { it !in usedSlots }
    }

    /**
     * Saves an app to the Manage list without activating an activity alias slot yet,
     * so the user can click "Install" in the Manage tab to run the Package Installer flow.
     */
    suspend fun saveAppOnly(app: RnaApp): RnaApp = withContext(Dispatchers.IO) {
        val saved = app.copy(isInstalled = false, assignedSlot = null)
        rnaAppDao.insertOrUpdateApp(RnaAppEntity.fromDomain(saved))
        saved
    }

    /**
     * Installs the app via the Mock Package Installer: marks isInstalled = true,
     * assigns an activity alias slot (1..20) if requested, and pins a shortcut if requested.
     */
    suspend fun installApp(app: RnaApp, useSlotAlias: Boolean, pinShortcut: Boolean): RnaApp = withContext(Dispatchers.IO) {
        var assignedSlot: Int? = app.assignedSlot
        if (useSlotAlias && assignedSlot == null) {
            assignedSlot = getNextAvailableSlot()
        }

        val updatedApp = app.copy(
            isInstalled = true,
            assignedSlot = assignedSlot,
            isPinnedShortcut = pinShortcut,
            lastLaunchedAt = System.currentTimeMillis()
        )

        // Always persist to Room first so the converted app is guaranteed installed & visible in Launcher
        rnaAppDao.insertOrUpdateApp(RnaAppEntity.fromDomain(updatedApp))

        if (assignedSlot != null) {
            runCatching {
                ShortcutHelper.updateActivityAlias(context, assignedSlot, true)
            }
        }

        if (pinShortcut) {
            withContext(Dispatchers.Main) {
                runCatching {
                    ShortcutHelper.pinAppShortcut(context, updatedApp)
                }
            }
        }

        updatedApp
    }

    suspend fun updateApp(app: RnaApp, oldSlot: Int?, newSlotEnabled: Boolean): RnaApp = withContext(Dispatchers.IO) {
        var finalSlot = app.assignedSlot
        if (oldSlot != null && !newSlotEnabled) {
            ShortcutHelper.updateActivityAlias(context, oldSlot, false)
            finalSlot = null
        } else if (oldSlot == null && newSlotEnabled) {
            finalSlot = getNextAvailableSlot()
            if (finalSlot != null) {
                ShortcutHelper.updateActivityAlias(context, finalSlot, true)
            }
        }

        val updated = app.copy(assignedSlot = finalSlot)
        rnaAppDao.insertOrUpdateApp(RnaAppEntity.fromDomain(updated))
        updated
    }

    suspend fun updateAppDirectly(app: RnaApp) = withContext(Dispatchers.IO) {
        rnaAppDao.insertOrUpdateApp(RnaAppEntity.fromDomain(app))
    }

    suspend fun upsertTvCustomization(entity: TvAppCustomEntity) = withContext(Dispatchers.IO) {
        rnaAppDao.upsertTvCustomization(entity)
    }

    suspend fun deleteApp(app: RnaApp) = withContext(Dispatchers.IO) {
        app.assignedSlot?.let { slot ->
            ShortcutHelper.updateActivityAlias(context, slot, false)
        }
        if (app.sourceType == "BUNDLE" && app.sourceUriOrContent.startsWith("file://")) {
            val dir = File(app.sourceUriOrContent.removePrefix("file://")).parentFile
            dir?.deleteRecursively()
        }
        rnaAppDao.deleteAppById(app.id)
    }

    suspend fun markAppLaunched(id: String) = withContext(Dispatchers.IO) {
        rnaAppDao.updateLastLaunched(id, System.currentTimeMillis())
    }

    suspend fun exportApp(app: RnaApp, destinationDir: File? = null): File? = withContext(Dispatchers.IO) {
        val exported = RnaPackageEngine.exportRnaPackageToDownloads(context, app)
        if (exported != null) return@withContext exported

        val targetDir = destinationDir ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val fileName = "${app.title.replace("\\s+".toRegex(), "_")}_v${app.version}.rna"
        val destFile = File(targetDir, fileName)
        val success = RnaPackageEngine.exportRnaPackage(context, app, destFile)
        if (success) destFile else null
    }
}
