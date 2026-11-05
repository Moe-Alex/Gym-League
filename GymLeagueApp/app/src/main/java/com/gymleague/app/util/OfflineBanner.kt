package com.gymleague.app.util

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.gymleague.app.GymLeagueApplication
import com.gymleague.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * A small bar above the bottom navigation that tells the user when the phone is offline
 * or when workouts are waiting to sync. It is added to every screen from one place, so no
 * layout or screen needs to be changed.
 */
object OfflineBanner {

    private const val BANNER_TAG = "offline_banner"

    private var currentActivity: WeakReference<Activity>? = null
    private var state = BannerState.HIDDEN
    private var pendingCount = 0

    fun install(app: GymLeagueApplication, scope: CoroutineScope) {

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                currentActivity = WeakReference(activity)
                render(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (currentActivity?.get() === activity) currentActivity = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // Work out the banner from the connection and the number of unsynced workouts
        scope.launch {
            combine(
                NetworkMonitor.isOnline,
                app.database.workoutDao().observePendingCount(OfflineRules.MAX_SYNC_ATTEMPTS)
            ) { online, count -> Pair(OfflineRules.bannerState(online, count), count) }
                .collect { (newState, count) ->
                    state = newState
                    pendingCount = count

                    launch(Dispatchers.Main) {
                        currentActivity?.get()?.let { render(it) }
                    }
                }
        }
    }

    private fun render(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        var banner = content.findViewWithTag<TextView>(BANNER_TAG)

        if (state == BannerState.HIDDEN) {
            banner?.visibility = View.GONE
            return
        }

        if (banner == null) {
            banner = TextView(activity).apply {
                tag = BANNER_TAG
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 13f
                setPadding(24, 16, 24, 16)
                elevation = 24f
            }
            content.addView(
                banner,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
            )
        }

        val resources = activity.resources

        when (state) {
            BannerState.OFFLINE -> {
                banner.text = resources.getString(R.string.offline_banner_offline)
                banner.setBackgroundColor(Color.parseColor("#B26A00"))
            }

            BannerState.OFFLINE_PENDING -> {
                banner.text = resources.getQuantityString(
                    R.plurals.offline_banner_pending, pendingCount, pendingCount
                )
                banner.setBackgroundColor(Color.parseColor("#B26A00"))
            }

            else -> {
                banner.text = resources.getQuantityString(
                    R.plurals.offline_banner_syncing, pendingCount, pendingCount
                )
                banner.setBackgroundColor(Color.parseColor("#1565C0"))
            }
        }

        banner.visibility = View.VISIBLE

        // Sit just above the bottom navigation bar when the screen has one
        val nav = activity.findViewById<View>(R.id.bottomNavigation)
        val navHeight = if (nav != null && nav.visibility == View.VISIBLE) nav.height else 0

        (banner.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.bottomMargin = navHeight
            banner.layoutParams = it
        }

        // The bottom bar may not have been measured yet on first show, so check once more
        if (nav != null && nav.visibility == View.VISIBLE && nav.height == 0 && !nav.isLaidOut) {
            nav.post { render(activity) }
        }
    }
}