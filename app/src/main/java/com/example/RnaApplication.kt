package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.data.RnaDatabase
import com.example.data.RnaRepository
import com.example.ui.tv.RnaAutonomousAgentController
import com.example.ui.tv.RnaControllerNetworkBridge
import com.example.ui.tv.TvProfilePersistenceStore
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RnaApplication : Application() {

    lateinit var database: RnaDatabase
        private set

    lateinit var repository: RnaRepository
        private set

    override fun onCreate() {
        super.onCreate()
        TvProfilePersistenceStore.ensureProfilesSyncedOnBootOrUpdate(this)
        database = RnaDatabase.getDatabase(this)
        repository = RnaRepository(this, database.rnaAppDao())

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                RnaAutonomousAgentController.activeActivityRef = WeakReference(activity)
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                RnaAutonomousAgentController.activeActivityRef = WeakReference(activity)
                RnaControllerNetworkBridge.activeForegroundActivityRef = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (RnaControllerNetworkBridge.activeForegroundActivityRef?.get() === activity) {
                    RnaControllerNetworkBridge.activeForegroundActivityRef = null
                }
            }

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (RnaControllerNetworkBridge.activeForegroundActivityRef?.get() === activity) {
                    RnaControllerNetworkBridge.activeForegroundActivityRef = null
                }
            }
        })

        // Seed initial apps asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            repository.initializeIfNeeded()
        }
    }
}
