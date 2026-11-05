package com.gymleague.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Watches the phone's internet connection and tells the rest of the app when it changes.
 * [isOnline] is true only when the phone has a connection that really reaches the internet.
 */
object NetworkMonitor {

    private const val TAG = "GymLeagueOffline"

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline

    private var started = false

    // Called once from GymLeagueApplication
    fun start(context: Context) {
        if (started) return
        started = true

        val manager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (manager == null) {
            Log.w(TAG, "No ConnectivityManager, assuming online")
            return
        }

        _isOnline.value = hasInternet(manager, manager.activeNetwork)

        try {
            manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    setOnline(
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    )
                }

                override fun onLost(network: Network) {
                    setOnline(false)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Could not watch the network: ${e.javaClass.simpleName}")
        }
    }

    private fun setOnline(online: Boolean) {
        if (_isOnline.value != online) {
            Log.d(TAG, if (online) "Network is back" else "Network lost, working offline")
        }
        _isOnline.value = online
    }

    private fun hasInternet(manager: ConnectivityManager, network: Network?): Boolean {
        if (network == null) return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}