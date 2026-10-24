package com.antimatter.applocker.ui.pin

/** Formats a number of seconds as m:ss, e.g. 27 -> "0:27", 905 -> "15:05". */
fun formatCountdown(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
