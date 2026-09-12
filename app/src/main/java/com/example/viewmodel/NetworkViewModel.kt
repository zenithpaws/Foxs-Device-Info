package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.Inet6Address

data class NetworkState(
    val isConnected: Boolean = false,
    val networkType: String = "None",
    val ipAddressV4: String = "Unknown",
    val ipAddressV6: String = "Unknown",
    val dnsServers: List<String> = emptyList(),
    val macAddress: String = "02:00:00:00:00:00 (Restricted)",
    val rxSpeedBps: Long = 0L,
    val txSpeedBps: Long = 0L,
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L
)

class NetworkViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(NetworkState())
    val uiState: StateFlow<NetworkState> = _uiState.asStateFlow()

    private var lastRxBytes: Long = TrafficStats.getTotalRxBytes()
    private var lastTxBytes: Long = TrafficStats.getTotalTxBytes()
    private var lastTime: Long = System.currentTimeMillis()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            while (true) {
                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()
                
                val timeDiff = (currentTime - lastTime).coerceAtLeast(1)
                val rxSpeed = ((currentRx - lastRxBytes) * 1000) / timeDiff
                val txSpeed = ((currentTx - lastTxBytes) * 1000) / timeDiff
                
                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastTime = currentTime

                val activeNetwork: Network? = cm.activeNetwork
                val caps: NetworkCapabilities? = cm.getNetworkCapabilities(activeNetwork)
                val linkProps: LinkProperties? = cm.getLinkProperties(activeNetwork)

                val isConnected = activeNetwork != null && caps != null
                var type = "Unknown"
                if (caps != null) {
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) type = "WiFi"
                    else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) type = "Cellular"
                    else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) type = "Ethernet"
                }

                var ipV4 = "Unknown"
                var ipV6 = "Unknown"
                val dns = mutableListOf<String>()

                if (linkProps != null) {
                    linkProps.linkAddresses.forEach { linkAddress ->
                        val address = linkAddress.address
                        if (!address.isLoopbackAddress) {
                            if (address is Inet4Address) ipV4 = address.hostAddress ?: "Unknown"
                            else if (address is Inet6Address) {
                                // Prefer the first non-link-local IPv6 if possible, or just take the first
                                if (ipV6 == "Unknown" || !address.isLinkLocalAddress) {
                                    val fullIp = address.hostAddress ?: "Unknown"
                                    // Strip scope id from IPv6
                                    ipV6 = fullIp.split("%")[0]
                                }
                            }
                        }
                    }
                    linkProps.dnsServers.forEach { dnsAddress ->
                        dnsAddress.hostAddress?.let { dns.add(it) }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isConnected = isConnected,
                    networkType = type,
                    ipAddressV4 = ipV4,
                    ipAddressV6 = ipV6,
                    dnsServers = dns,
                    rxSpeedBps = rxSpeed,
                    txSpeedBps = txSpeed,
                    totalRxBytes = currentRx,
                    totalTxBytes = currentTx
                )

                delay(1000)
            }
        }
    }
}
