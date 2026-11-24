package com.example

import com.example.data.RnaAppEntity
import com.example.data.model.RnaApp
import com.example.util.StarterApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun starterApps_areValid() {
        val apps = StarterApps.getInitialApps()
        // Test RNA apps are removed on first install as per user requirements
        assertEquals(0, apps.size)
        assertTrue(StarterApps.ASTRO_DODGE_HTML.isNotEmpty())
        assertTrue(StarterApps.NEOCALC_HTML.isNotEmpty())
        assertTrue(StarterApps.PLAYGROUND_HTML.isNotEmpty())
    }

    @Test
    fun rnaAppEntity_mapping_isCorrect() {
        val app = RnaApp(
            id = "test_id",
            packageName = "com.rna.test",
            title = "Test App",
            version = "1.0.0",
            sourceType = "URL",
            sourceUriOrContent = "https://example.com",
            permissions = listOf("INTERNET", "CAMERA"),
            assignedSlot = 1
        )

        val entity = RnaAppEntity.fromDomain(app)
        val domain = entity.toDomain()

        assertEquals(app.id, domain.id)
        assertEquals(app.title, domain.title)
        assertEquals(app.packageName, domain.packageName)
        assertEquals(2, domain.permissions.size)
        assertEquals(1, domain.assignedSlot)
    }
}
