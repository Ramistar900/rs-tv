package com.sinjartv.rstv

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal enum class AdultContentType {
    AGE_RATED,
    EXPLICIT
}

internal object AdultContentPolicy {
    private val explicitGroupPattern = Regex(
        """(?i)(?:^|[,;|/])\s*(?:xxx|porn|pornographic|erotic|adult|adults|adult\s+(?:content|channels|movies|entertainment)|sex\s*(?:tv|channel))(?:$|[,;|/])|للكبار فقط|محتوى صريح|إباحي|اباحي|إباحية|اباحية|قنوات جنسية"""
    )
    private val explicitNamePattern = Regex(
        """(?i)(?:^|[^a-z0-9])(?:xxx|porn|pornographic|erotic|adult\s*(?:tv|channel|entertainment)|sex\s*(?:tv|channel))(?:$|[^a-z0-9])|للكبار فقط|محتوى صريح|إباحي|اباحي|إباحية|اباحية|قنوات جنسية"""
    )
    private val ageRatingPattern = Regex(
        """(?i)^(?:18\+?|r18\+?|tv-?ma|nc-?17|ao|mature|18\s*years?\s*old|للكبار|للبالغين|مناسب للبالغين)$"""
    )

    fun classify(channel: RSTVChannel): AdultContentType? {
        if (explicitGroupPattern.containsMatchIn(channel.group.orEmpty()) ||
            explicitNamePattern.containsMatchIn(channel.name)
        ) return AdultContentType.EXPLICIT

        val rating = channel.contentRating?.trim().orEmpty()
        if (rating.isNotBlank() && ageRatingPattern.matches(rating)) {
            return AdultContentType.AGE_RATED
        }
        if (ageRatingPattern.matches(channel.group?.trim().orEmpty())) {
            return AdultContentType.AGE_RATED
        }
        return null
    }

    fun isAdult(channel: RSTVChannel): Boolean = classify(channel) != null
}

internal object AdultAccessStore {
    private const val PREFS = "rs_tv_parental_access"
    private const val SALT_KEY = "adult_pin_salt"
    private const val HASH_KEY = "adult_pin_hash"
    private const val HIDDEN_KEY = "adult_section_hidden"
    private const val FAILED_KEY = "adult_pin_failed_attempts"
    private const val LOCKED_UNTIL_KEY = "adult_pin_locked_until"
    private const val ITERATIONS = 150_000
    private const val KEY_BITS = 256
    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCKOUT_MS = 30_000L

    fun hasPin(context: Context): Boolean {
        val prefs = prefs(context)
        return !prefs.getString(SALT_KEY, null).isNullOrBlank() &&
            !prefs.getString(HASH_KEY, null).isNullOrBlank()
    }

    fun setPin(context: Context, pin: String) {
        require(pin.matches(Regex("\\d{4,8}"))) { "رمز PIN يجب أن يتكون من 4 إلى 8 أرقام" }
        val secureRandom = SecureRandom()
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val hash = derive(pin, salt)
        prefs(context).edit()
            .putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(HASH_KEY, Base64.encodeToString(hash, Base64.NO_WRAP))
            .remove(FAILED_KEY)
            .remove(LOCKED_UNTIL_KEY)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val preferences = prefs(context)
        val now = System.currentTimeMillis()
        if (preferences.getLong(LOCKED_UNTIL_KEY, 0L) > now) return false

        val salt = decode(preferences.getString(SALT_KEY, null)) ?: return false
        val expected = decode(preferences.getString(HASH_KEY, null)) ?: return false
        if (pin.length !in 4..8 || !pin.all { it.isDigit() }) return false

        val matches = MessageDigest.isEqual(expected, derive(pin, salt))
        if (matches) {
            preferences.edit().remove(FAILED_KEY).remove(LOCKED_UNTIL_KEY).apply()
            return true
        }

        val failures = preferences.getInt(FAILED_KEY, 0) + 1
        val editor = preferences.edit().putInt(FAILED_KEY, failures)
        if (failures >= MAX_FAILED_ATTEMPTS) {
            editor.putLong(LOCKED_UNTIL_KEY, now + LOCKOUT_MS).putInt(FAILED_KEY, 0)
        }
        editor.apply()
        return false
    }

    fun isSectionHidden(context: Context): Boolean =
        prefs(context).getBoolean(HIDDEN_KEY, false)

    fun setSectionHidden(context: Context, hidden: Boolean) {
        prefs(context).edit().putBoolean(HIDDEN_KEY, hidden).apply()
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun decode(value: String?): ByteArray? =
        value?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
