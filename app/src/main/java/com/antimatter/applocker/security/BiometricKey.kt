package com.antimatter.applocker.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * A Keystore key that can only be used right after a successful strong-biometric check,
 * and that Android invalidates when a new fingerprint is enrolled.
 */
object BiometricKey {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "applocker_biometric_v1"

    /** Throws if no strong biometric is enrolled. */
    fun createKey() {
        delete()
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        generator.generateKey()
    }

    /**
     * Returns a Mac ready to be wrapped in a BiometricPrompt.CryptoObject.
     * Throws KeyPermanentlyInvalidatedException if fingerprints changed,
     * or IllegalStateException if the key does not exist.
     */
    fun newMac(): Mac {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        val key = keyStore.getKey(ALIAS, null) as? SecretKey
            ?: throw IllegalStateException("Biometric key is missing")
        return Mac.getInstance("HmacSHA256").apply { init(key) }
    }

    fun delete() {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(ALIAS)) keyStore.deleteEntry(ALIAS)
    }
}
