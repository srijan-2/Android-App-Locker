package com.antimatter.applocker.ui.biometric

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.antimatter.applocker.security.BiometricKey
import java.security.SecureRandom

enum class BiometricStatus { Available, NoHardware, NoneEnrolled, Unavailable }

fun biometricStatus(context: Context): BiometricStatus =
    when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NoneEnrolled
        else -> BiometricStatus.Unavailable
    }

class BiometricAuthenticator(private val activity: FragmentActivity) {

    /**
     * Shows the system fingerprint prompt, bound to the Keystore biometric key.
     *
     * onSuccess: a strong biometric was verified AND the key was used successfully.
     * onFallback: fall back to the PIN; the message (if any) is safe to show to the user.
     * onInvalidated: the key is gone or was invalidated (fingerprints changed).
     */
    fun authenticate(
        title: String,
        negativeText: String,
        onSuccess: () -> Unit,
        onFallback: (String?) -> Unit,
        onInvalidated: () -> Unit
    ) {
        val mac = try {
            BiometricKey.newMac()
        } catch (e: KeyPermanentlyInvalidatedException) {
            BiometricKey.delete()
            onInvalidated()
            return
        } catch (e: IllegalStateException) {
            onInvalidated()
            return
        } catch (e: Exception) {
            onFallback("Fingerprint isn’t available right now. Use your PIN.")
            return
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val verified = try {
                    val unlockedMac = result.cryptoObject?.mac
                    unlockedMac != null && unlockedMac.doFinal(newChallenge()).isNotEmpty()
                } catch (e: Exception) {
                    false
                }
                if (verified) {
                    onSuccess()
                } else {
                    onFallback("Fingerprint check failed. Use your PIN.")
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFallback(
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> null
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                            "Fingerprint is locked for now. Use your PIN."
                        else -> "Fingerprint isn’t available right now. Use your PIN."
                    }
                )
            }
        }

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()

        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            .authenticate(info, BiometricPrompt.CryptoObject(mac))
    }

    private fun newChallenge(): ByteArray =
        ByteArray(16).also { SecureRandom().nextBytes(it) }
}
