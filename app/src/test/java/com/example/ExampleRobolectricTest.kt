package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.TvWatchNextHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify Play Next engine`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RNA.", appName)

        TvWatchNextHelper.recordOrQueuePlayNext(
            context = context,
            title = "Despicable Me 4",
            subtitle = "4K UHD Spotlight",
            sourceApp = "YouTube",
            posterArtUri = "https://example.com/poster.jpg",
            targetUrlOrIntent = "https://www.youtube.com"
        )
        val programs = TvWatchNextHelper.loadRealPlayNextPrograms(context)
        assertTrue(programs.isNotEmpty())
        assertEquals("Despicable Me 4", programs.first().title)
    }
}
