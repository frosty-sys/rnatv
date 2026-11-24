package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.RnaApp

@Entity(tableName = "rna_apps")
data class RnaAppEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val title: String,
    val version: String,
    val sourceType: String,
    val sourceUriOrContent: String,
    val iconBase64: String?,
    val bannerBase64: String?,
    val isTvEnabled: Boolean,
    val defaultInputMode: String,
    val mouseSpeed: Float,
    val forceDarkMode: Boolean,
    val showLoadingBar: Boolean = true,
    val loadingBarColor: String = "#00E5FF",
    val isInstalled: Boolean = true,
    val isTvFavorite: Boolean = true,
    val favoriteOrder: Int = 0,
    val allAppsOrder: Int = 0,
    val permissionsJson: String,
    val assignedSlot: Int?,
    val isPinnedShortcut: Boolean,
    val createdAt: Long,
    val lastLaunchedAt: Long
) {
    fun toDomain(): RnaApp {
        val permList = permissionsJson.split(",").filter { it.isNotBlank() }
        return RnaApp(
            id = id,
            packageName = packageName,
            title = title,
            version = version,
            sourceType = sourceType,
            sourceUriOrContent = sourceUriOrContent,
            iconBase64 = iconBase64,
            bannerBase64 = bannerBase64,
            isTvEnabled = isTvEnabled,
            defaultInputMode = defaultInputMode,
            mouseSpeed = mouseSpeed,
            forceDarkMode = forceDarkMode,
            showLoadingBar = showLoadingBar,
            loadingBarColor = loadingBarColor,
            isInstalled = isInstalled,
            isTvFavorite = isTvFavorite,
            favoriteOrder = favoriteOrder,
            allAppsOrder = allAppsOrder,
            permissions = permList,
            assignedSlot = assignedSlot,
            isPinnedShortcut = isPinnedShortcut,
            createdAt = createdAt,
            lastLaunchedAt = lastLaunchedAt
        )
    }

    companion object {
        fun fromDomain(app: RnaApp): RnaAppEntity {
            return RnaAppEntity(
                id = app.id,
                packageName = app.packageName,
                title = app.title,
                version = app.version,
                sourceType = app.sourceType,
                sourceUriOrContent = app.sourceUriOrContent,
                iconBase64 = app.iconBase64,
                bannerBase64 = app.bannerBase64,
                isTvEnabled = app.isTvEnabled,
                defaultInputMode = app.defaultInputMode,
                mouseSpeed = app.mouseSpeed,
                forceDarkMode = app.forceDarkMode,
                showLoadingBar = app.showLoadingBar,
                loadingBarColor = app.loadingBarColor,
                isInstalled = app.isInstalled,
                isTvFavorite = app.isTvFavorite,
                favoriteOrder = app.favoriteOrder,
                allAppsOrder = app.allAppsOrder,
                permissionsJson = app.permissions.joinToString(","),
                assignedSlot = app.assignedSlot,
                isPinnedShortcut = app.isPinnedShortcut,
                createdAt = app.createdAt,
                lastLaunchedAt = app.lastLaunchedAt
            )
        }
    }
}
