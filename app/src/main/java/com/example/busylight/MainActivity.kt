package com.example.busylight

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : Activity() {
    private lateinit var status: TextView
    private val ui = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() { status.text = describe(); ui.postDelayed(this, 1000) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 48, 48, 48) }
        status = TextView(this).apply { textSize = 18f }
        root.addView(status)

        fun btn(label: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                text = label; setOnClickListener { action() }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
        }
        btn("1. Grant Usage Access") {
            try { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            catch (_: ActivityNotFoundException) { status.text = "No settings screen on this device. Use adb (see README)." }
        }
        btn("2. Start light service") { startForegroundService(Intent(this, BusyLightService::class.java)) }
        btn("Stop service") { stopService(Intent(this, BusyLightService::class.java)) }
        btn("Force RED") { BusyState.mode = "red" }
        btn("Force GREEN") { BusyState.mode = "green" }
        btn("Automatic") { BusyState.mode = "auto" }

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() { super.onResume(); ui.post(refresh) }
    override fun onPause() { super.onPause(); ui.removeCallbacks(refresh) }

    private fun describe(): String = buildString {
        appendLine("Usage access: ${if (BusyLightService.hasUsageAccess(this@MainActivity)) "granted" else "NOT granted"}")
        appendLine("USB light: ${if (BusyState.usbPresent) "connected" else "not found"}")
        appendLine("Light color: ${BusyState.color}")
        appendLine("Mode: ${BusyState.mode}")
        appendLine("Foreground app: ${BusyState.foregroundApp ?: "-"}")
        appendLine("In a call (mic in use): ${BusyState.inCall}")
        appendLine("Audio playing: ${BusyState.audioPlaying}")
        appendLine("USB debug: ${UsbLight.lastStatus}")
        append("API: http://${localIp()}:${Config.API_PORT}/status")
    }

    private fun localIp(): String = try {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }?.hostAddress ?: "device-ip"
    } catch (_: Exception) { "device-ip" }
}
