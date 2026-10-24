package com.antimatter.applocker.security

object LockoutPolicy {
    /** Lock duration after the given number of consecutive failed attempts. */
    fun lockDurationMillis(failedAttempts: Int): Long = when {
        failedAttempts < 5 -> 0L
        failedAttempts == 5 -> 30_000L
        failedAttempts == 6 -> 60_000L
        failedAttempts == 7 -> 5 * 60_000L
        else -> 15 * 60_000L
    }
}
