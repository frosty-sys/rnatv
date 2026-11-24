package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tv_app_customizations")
data class TvAppCustomEntity(
    @PrimaryKey
    val packageName: String,
    val customTitle: String? = null,
    val customLogoUri: String? = null,
    val customColorHex: String? = null,
    val isFavorite: Boolean = false,
    val favoriteOrder: Int = 100,
    val allAppsOrder: Int = 100
)
