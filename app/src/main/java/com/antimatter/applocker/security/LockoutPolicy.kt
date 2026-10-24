package com.antimatter.applocker.security

object LockoutPolicy {
    /** The failed attempt at which the first lockout starts. */
    const val LOCK_STARTS_AT = 5

    /** Lock duration after the given number of consecutive failed attempts. */
    fun lockDurationMillis(failedAttempts: Int): Long = when {
        failedAttempts < LOCK_STARTS_AT -> 0L
        failedAttempts == LOCK_STARTS_AT -> 30_000L
        failedAttempts == LOCK_STARTS_AT + 1 -> 60_000L
        failedAttempts == LOCK_STARTS_AT + 2 -> 5 * 60_000L
        else -> 15 * 60_000L
    }
}
