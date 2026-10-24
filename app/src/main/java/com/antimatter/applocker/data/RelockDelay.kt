package com.antimatter.applocker.data

/** How long a protected app stays unlocked after the user leaves it. */
enum class RelockDelay(val seconds: Int, val label: String, val shortLabel: String) {
    Immediately(0, "Immediately", "Immediately"),
    Seconds5(5, "After 5 seconds", "5 sec"),
    Seconds30(30, "After 30 seconds", "30 sec"),
    Minute1(60, "After 1 minute", "1 min");

    companion object {
        fun fromSeconds(seconds: Int): RelockDelay =
            entries.firstOrNull { it.seconds == seconds } ?: Immediately
    }
}
