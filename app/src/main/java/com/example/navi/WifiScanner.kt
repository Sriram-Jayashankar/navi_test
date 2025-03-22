package com.example.navi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper

class WifiScanner(
    private val context: Context,
    private val targetSSIDs: List<String>,
    // Explicitly annotate the lambda parameter type.
    private val onScanResults: (results: kotlin.collections.Map<String, Int>) -> Unit
) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val scanHandler = Handler(Looper.getMainLooper())
    private val scanInterval = 100L  // Scan every 2 seconds

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val scanResults: List<ScanResult> = wifiManager.scanResults
            val resultMap = mutableMapOf<String, Int>()
            targetSSIDs.forEach { ssid ->
                val result = scanResults.find { it.SSID == ssid }
                result?.let {
                    resultMap[ssid] = it.level
                }
            }
            onScanResults(resultMap)
        }
    }

    fun start() {
        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        scanHandler.post(scanRunnable)
    }

    fun stop() {
        context.unregisterReceiver(receiver)
        scanHandler.removeCallbacks(scanRunnable)
    }

    private val scanRunnable = object : Runnable {
        override fun run() {
            wifiManager.startScan()
            scanHandler.postDelayed(this, scanInterval)
        }
    }
}
