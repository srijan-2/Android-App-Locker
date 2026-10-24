package com.antimatter.applocker.lock

/**
 * Decides when a protected app must be locked. Pure logic: no Android classes, so it is
 * unit-tested. Call it from the main thread only.
 *
 * Rules:
 * - A protected app that is not unlocked is locked when it comes to the front.
 * - After an unlock it stays unlocked while in the foreground, and for [relockDelayMillis]
 *   after the user leaves it. "Immediately" is a delay of 0.
 * - Switching between apps within the delay never asks again.
 * - Screen off counts as leaving the app; coming back is checked on screen on / unlock.
 */
class LockEngine(private val clock: () -> Long = System::currentTimeMillis) {

    var enabled: Boolean = false
    var protectedPackages: Set<String> = emptySet()
    var relockDelayMillis: Long = 0L

    private val unlocked = HashSet<String>()
    private val leftAt = HashMap<String, Long>()
    private var foreground: String? = null
    private var foregroundAtScreenOff: String? = null
    private var lockRequestedFor: String? = null
    private var lockRequestedAt = 0L

    /** The foreground app changed. Returns the package to lock now, or null. */
    fun onForegroundChanged(pkg: String): String? {
        val now = clock()
        val previous = foreground
        if (previous != null && previous != pkg && previous in unlocked) {
            leftAt[previous] = now
        }
        foreground = pkg
        return evaluate(pkg, now)
    }

    /** The user passed the PIN or fingerprint check for [pkg]. */
    fun onUnlocked(pkg: String) {
        unlocked.add(pkg)
        leftAt.remove(pkg)
        if (lockRequestedFor == pkg) lockRequestedFor = null
    }

    /** The lock screen for [pkg] went away without an unlock (home, back, task switch). */
    fun onLockClosed(pkg: String) {
        if (lockRequestedFor == pkg) lockRequestedFor = null
    }

    fun onScreenOff() {
        val now = clock()
        foregroundAtScreenOff = foreground
        for (pkg in unlocked) {
            if (pkg !in leftAt) leftAt[pkg] = now
        }
    }

    /** Screen turned on (no keyguard) or the user unlocked the phone. */
    fun onScreenOn(): String? {
        val pkg = foregroundAtScreenOff ?: return null
        return evaluate(pkg, clock())
    }

    private fun evaluate(pkg: String, now: Long): String? {
        if (!enabled || pkg !in protectedPackages) return null

        if (pkg in unlocked) {
            val left = leftAt[pkg]
            if (left == null || now - left < relockDelayMillis) {
                leftAt.remove(pkg)
                return null
            }
            unlocked.remove(pkg)
            leftAt.remove(pkg)
        }

        // The system can report the same window change several times in a row.
        if (lockRequestedFor == pkg && now - lockRequestedAt < DEDUPE_MILLIS) return null
        lockRequestedFor = pkg
        lockRequestedAt = now
        return pkg
    }

    private companion object {
        const val DEDUPE_MILLIS = 1_500L
    }
}
