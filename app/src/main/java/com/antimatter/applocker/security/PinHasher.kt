package com.antimatter.applocker.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    // The iteration count is measured on the device so a PIN check takes about
    // TARGET_MILLIS, then stored with the PIN so every check uses the same count.
    const val MIN_ITERATIONS = 10_000
    const val MAX_ITERATIONS = 100_000
    const val TARGET_MILLIS = 25L
    /** Used only if a stored PIN has no iteration count recorded. */
    const val FALLBACK_ITERATIONS = 50_000
    private const val PROBE_ITERATIONS = 5_000

    const val SALT_BYTES = 16
    const val KEY_BITS = 256
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8

    fun isValidFormat(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

    fun newSalt(): ByteArray =
        ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun derive(pin: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Picks an iteration count that takes roughly [targetMillis] on this device,
     * clamped to [MIN_ITERATIONS]..[MAX_ITERATIONS].
     */
    fun calibrateIterations(targetMillis: Long = TARGET_MILLIS): Int {
        val probePin = charArrayOf('0', '0', '0', '0')
        val probeSalt = ByteArray(SALT_BYTES)
        derive(probePin, probeSalt, PROBE_ITERATIONS) // warm-up, result ignored
        var best = Long.MAX_VALUE
        repeat(3) {
            val start = System.nanoTime()
            derive(probePin, probeSalt, PROBE_ITERATIONS)
            best = minOf(best, System.nanoTime() - start)
        }
        val nanosPerIteration = best.toDouble().coerceAtLeast(1.0) / PROBE_ITERATIONS
        val wanted = (targetMillis * 1_000_000.0 / nanosPerIteration).toInt()
        return wanted.coerceIn(MIN_ITERATIONS, MAX_ITERATIONS)
    }

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean =
        MessageDigest.isEqual(a, b)
}
