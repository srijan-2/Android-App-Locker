package com.antimatter.applocker.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.antimatter.applocker.security.KeystoreHmac
import com.antimatter.applocker.security.LockoutPolicy
import com.antimatter.applocker.security.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.pinDataStore: DataStore<Preferences> by preferencesDataStore(name = "pin_store")

data class PinState(val isSet: Boolean, val length: Int)

sealed interface VerifyResult {
    data object Success : VerifyResult
    data object NotSet : VerifyResult
    data class LockedOut(val untilMillis: Long) : VerifyResult
    /** [lockedUntilMillis] is 0 when this failure did not trigger a lockout. */
    data class WrongPin(val failedAttempts: Int, val lockedUntilMillis: Long) : VerifyResult
}

class PinRepository(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private object Keys {
        val SALT = stringPreferencesKey("pin_salt")
        val HASH = stringPreferencesKey("pin_hash")
        val ITERATIONS = intPreferencesKey("pin_iterations")
        val LENGTH = intPreferencesKey("pin_length")
        val FAILS = intPreferencesKey("pin_failed_attempts")
        val LOCK_UNTIL = longPreferencesKey("pin_lock_until")
    }

    private val store: DataStore<Preferences> get() = context.pinDataStore

    val state: Flow<PinState> = store.data.map { prefs ->
        PinState(isSet = prefs[Keys.HASH] != null, length = prefs[Keys.LENGTH] ?: 0)
    }

    suspend fun setPin(pin: String) {
        require(PinHasher.isValidFormat(pin)) { "PIN must be 4 to 8 digits" }
        val salt = PinHasher.newSalt()
        val hash = computeHash(pin, salt, PinHasher.ITERATIONS)
        store.edit { prefs ->
            prefs[Keys.SALT] = encode(salt)
            prefs[Keys.HASH] = encode(hash)
            prefs[Keys.ITERATIONS] = PinHasher.ITERATIONS
            prefs[Keys.LENGTH] = pin.length
            prefs[Keys.FAILS] = 0
            prefs[Keys.LOCK_UNTIL] = 0L
        }
    }

    suspend fun verify(pin: String): VerifyResult {
        val prefs = store.data.first()
        val storedHash = prefs[Keys.HASH]?.let(::decode) ?: return VerifyResult.NotSet
        val salt = prefs[Keys.SALT]?.let(::decode) ?: return VerifyResult.NotSet
        val iterations = prefs[Keys.ITERATIONS] ?: PinHasher.ITERATIONS

        val lockUntil = prefs[Keys.LOCK_UNTIL] ?: 0L
        if (clock() < lockUntil) return VerifyResult.LockedOut(lockUntil)

        val candidate = computeHash(pin, salt, iterations)
        if (PinHasher.constantTimeEquals(candidate, storedHash)) {
            store.edit { p ->
                p[Keys.FAILS] = 0
                p[Keys.LOCK_UNTIL] = 0L
            }
            return VerifyResult.Success
        }

        val fails = (prefs[Keys.FAILS] ?: 0) + 1
        val duration = LockoutPolicy.lockDurationMillis(fails)
        val until = if (duration > 0) clock() + duration else 0L
        store.edit { p ->
            p[Keys.FAILS] = fails
            p[Keys.LOCK_UNTIL] = until
        }
        return VerifyResult.WrongPin(fails, until)
    }

    /** Verifies [oldPin]; only if it is correct is the PIN replaced with [newPin]. */
    suspend fun changePin(oldPin: String, newPin: String): VerifyResult {
        require(PinHasher.isValidFormat(newPin)) { "PIN must be 4 to 8 digits" }
        val result = verify(oldPin)
        if (result is VerifyResult.Success) setPin(newPin)
        return result
    }

    private suspend fun computeHash(pin: String, salt: ByteArray, iterations: Int): ByteArray =
        withContext(Dispatchers.Default) {
            val chars = pin.toCharArray()
            try {
                KeystoreHmac.sign(PinHasher.derive(chars, salt, iterations))
            } finally {
                chars.fill('\u0000')
            }
        }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)
}
