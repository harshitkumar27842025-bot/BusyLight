package com.example.busylight

object Config {
    const val API_PORT = 8765
    const val POLL_SECONDS = 2L
    const val TO_RED_AFTER = 2          // polls in a row saying "busy" before going red (~4 s)
    const val TO_GREEN_AFTER = 4        // polls in a row saying "free" before going green (~8 s)
    const val RESEND_MS = 10_000L       // keep-alive re-send of the same color

    /** Light goes RED whenever one of these is in the foreground. */
    val OFFICE_PACKAGES = setOf(
        "com.microsoft.office.word",
        "com.microsoft.office.excel",
        "com.microsoft.office.powerpoint",
        "com.microsoft.office.officehubrow"      // Microsoft 365 app
    )

    /** Light goes RED only if one of these is in the foreground AND the microphone is in use (a call). */
    val MEETING_PACKAGES = setOf(
        "com.microsoft.teams",
        "us.zoom.videomeetings",
        "com.google.android.apps.tachyon",      // Google Meet
        "com.cisco.webex.meetings",
        "com.skype.raider",
        "com.android.chrome"                    // Meet in the browser
    )
}

/** Shared state between service, API and UI. */
object BusyState {
    @Volatile var mode = "auto"          // auto | red | green
    @Volatile var color = "none"         // what the light was last set to
    @Volatile var foregroundApp: String? = null
    @Volatile var inCall = false
    @Volatile var usbPresent = false
    @Volatile var usageAccess = false

    fun json(): String =
        """{"color":"$color","mode":"$mode","foregroundApp":${foregroundApp?.let { "\"$it\"" } ?: "null"},""" +
        """"inCall":$inCall,"usbLightConnected":$usbPresent,"usageAccessGranted":$usageAccess}"""
}
