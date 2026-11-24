package com.example.data.model

data class RnaApp(
    val id: String,
    val packageName: String,
    val title: String,
    val version: String = "1.0.0",
    val sourceType: String, // "URL", "CODE", "BUNDLE"
    val sourceUriOrContent: String,
    val iconBase64: String? = null,
    val bannerBase64: String? = null,
    val isTvEnabled: Boolean = true,
    val defaultInputMode: String = "DPAD", // "DPAD", "MOUSE", "SCROLL"
    val mouseSpeed: Float = 1.5f,
    val forceDarkMode: Boolean = false,
    val showLoadingBar: Boolean = true,
    val loadingBarColor: String = "#00E5FF",
    val isInstalled: Boolean = true,
    val isTvFavorite: Boolean = true,
    val favoriteOrder: Int = 0,
    val allAppsOrder: Int = 0,
    val permissions: List<String> = listOf("INTERNET"),
    val assignedSlot: Int? = null, // 1..20
    val isPinnedShortcut: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLaunchedAt: Long = System.currentTimeMillis()
)
