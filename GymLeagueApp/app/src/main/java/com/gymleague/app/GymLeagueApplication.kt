package com.gymleague.app

import android.app.Application
import com.gymleague.app.database.GymLeagueDatabase
import com.gymleague.app.sync.SyncManager
import com.gymleague.app.util.NetworkMonitor
import com.gymleague.app.util.OfflineBanner
import com.gymleague.app.util.OfflineRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Runs once when the app process starts. It starts the offline features:
 * the network watcher, the offline banner and the background sync.
 */
class GymLeagueApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: GymLeagueDatabase by lazy { GymLeagueDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        NetworkMonitor.start(this)
        OfflineBanner.install(this, appScope)

        // Runs when the app opens and every time the connection comes back.
        // A StateFlow only sends a value when it changes, so no extra filtering is needed.
        appScope.launch {
            NetworkMonitor.isOnline.collect { online ->
                if (online) {
                    val waiting = database.workoutDao().getPending(OfflineRules.MAX_SYNC_ATTEMPTS)
                    if (waiting.isNotEmpty()) {
                        SyncManager.schedule(this@GymLeagueApplication)
                    }
                }
            }
        }
    }

    companion object {
        lateinit var instance: GymLeagueApplication
            private set
    }
}