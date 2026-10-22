package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hrp_secure_prefs")

open class TokenManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "hrp_auth_cache"
        private const val SP_KEY_TOKEN = "cached_token"
        private const val SP_KEY_ROLE = "cached_role"
        private const val SP_KEY_PHONE = "cached_phone"
        private const val SP_KEY_NAME = "cached_name"
        private const val SP_KEY_PHONE_VERIFIED = "cached_phone_verified"
        private const val SP_KEY_VERIFICATION_STATUS = "cached_verification_status"
        private const val SP_KEY_HAS_ASSIGNMENT = "cached_has_assignment"
        private const val SP_KEY_LAST_CHECK_IN_MS = "cached_last_check_in_ms"
        private const val SP_KEY_LAST_CHECK_OUT_MS = "cached_last_check_out_ms"
        private const val SP_KEY_REMEMBERED_PHONE = "remembered_account_phone"
        private const val SP_KEY_REMEMBERED_PASSWORD = "remembered_account_password"
        private const val SP_KEY_REMEMBERED_NAME = "remembered_account_name"
        private const val SP_KEY_REMEMBERED_TOKEN = "remembered_account_token"

        val PREF_TOKEN = stringPreferencesKey("jwt_token")
        val PREF_ROLE = stringPreferencesKey("user_role")
        val PREF_PHONE = stringPreferencesKey("user_phone")
        val PREF_NAME_KEY = stringPreferencesKey("user_full_name")
        val PREF_PHONE_VERIFIED = booleanPreferencesKey("user_phone_verified")
        val PREF_VERIFICATION_STATUS = stringPreferencesKey("user_verification_status")
        val PREF_HAS_ASSIGNMENT = booleanPreferencesKey("user_has_assignment")
        val PREF_FCM_TOKEN = stringPreferencesKey("fcm_token")

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Fast synchronous in-memory cache for OkHttp AuthInterceptor & UI state initialization
    private val cachedToken = AtomicReference<String?>(sharedPrefs.getString(SP_KEY_TOKEN, null))
    private val cachedRole = AtomicReference<String?>(sharedPrefs.getString(SP_KEY_ROLE, null))

    private val _tokenFlow = MutableStateFlow(cachedToken.get())
    val tokenFlow: Flow<String?> = _tokenFlow.asStateFlow()

    init {
        // Sync DataStore in background
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data.map { preferences ->
                preferences[PREF_TOKEN]
            }.collect { token ->
                cachedToken.set(token)
                _tokenFlow.value = token
            }
        }
    }

    /**
     * Synchronous token retrieval for AuthInterceptor (zero-blocking)
     */
    open fun getToken(): String? {
        return cachedToken.get() ?: sharedPrefs.getString(SP_KEY_TOKEN, null)
    }

    open fun getUserRole(): String? {
        return cachedRole.get() ?: sharedPrefs.getString(SP_KEY_ROLE, null)
    }

    open fun getPhone(): String? {
        return sharedPrefs.getString(SP_KEY_PHONE, null)
    }

    open fun getFullName(): String? {
        return sharedPrefs.getString(SP_KEY_NAME, null)
    }

    open fun isPhoneVerified(): Boolean {
        return sharedPrefs.getBoolean(SP_KEY_PHONE_VERIFIED, false)
    }

    open fun getVerificationStatus(): String {
        return sharedPrefs.getString(SP_KEY_VERIFICATION_STATUS, "NONE") ?: "NONE"
    }

    open fun hasActiveAssignment(): Boolean {
        return sharedPrefs.getBoolean(SP_KEY_HAS_ASSIGNMENT, false)
    }

    open fun getLastCheckInEpochMs(): Long {
        return sharedPrefs.getLong(SP_KEY_LAST_CHECK_IN_MS, 0L)
    }

    open fun getLastCheckOutEpochMs(): Long {
        return sharedPrefs.getLong(SP_KEY_LAST_CHECK_OUT_MS, 0L)
    }

    open fun getRememberedPhone(): String {
        return sharedPrefs.getString(SP_KEY_REMEMBERED_PHONE, "") ?: ""
    }

    open fun getRememberedPassword(): String {
        return sharedPrefs.getString(SP_KEY_REMEMBERED_PASSWORD, "") ?: ""
    }

    open fun getRememberedFullName(): String {
        return sharedPrefs.getString(SP_KEY_REMEMBERED_NAME, "") ?: ""
    }

    open fun getRememberedToken(): String {
        return sharedPrefs.getString(SP_KEY_REMEMBERED_TOKEN, "") ?: ""
    }

    open fun saveRememberedCredentials(
        phone: String,
        password: String? = null,
        fullName: String? = null,
        verifiedToken: String? = null
    ) {
        val editor = sharedPrefs.edit()
        if (phone.isNotBlank()) {
            editor.putString(SP_KEY_REMEMBERED_PHONE, phone.trim())
        }
        if (!password.isNullOrBlank()) {
            editor.putString(SP_KEY_REMEMBERED_PASSWORD, password)
        }
        if (!fullName.isNullOrBlank() && fullName != "Người Lao Động") {
            editor.putString(SP_KEY_REMEMBERED_NAME, fullName.trim())
        }
        if (!verifiedToken.isNullOrBlank()) {
            editor.putString(SP_KEY_REMEMBERED_TOKEN, verifiedToken)
        }
        editor.apply()
    }

    open fun saveCheckInOutTimestamps(checkInEpochMs: Long? = null, checkOutEpochMs: Long? = null) {
        val editor = sharedPrefs.edit()
        if (checkInEpochMs != null) {
            editor.putLong(SP_KEY_LAST_CHECK_IN_MS, checkInEpochMs)
        }
        if (checkOutEpochMs != null) {
            editor.putLong(SP_KEY_LAST_CHECK_OUT_MS, checkOutEpochMs)
        }
        editor.apply()
    }

    open fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank() && isPhoneVerified()
    }

    open fun saveTokenSync(token: String) {
        cachedToken.set(token)
        _tokenFlow.value = token
        sharedPrefs.edit().putString(SP_KEY_TOKEN, token).apply()
    }

    suspend fun saveToken(token: String) {
        cachedToken.set(token)
        _tokenFlow.value = token
        sharedPrefs.edit().putString(SP_KEY_TOKEN, token).apply()

        context.dataStore.edit { preferences ->
            preferences[PREF_TOKEN] = token
        }
    }

    suspend fun saveUserSession(
        token: String,
        phone: String,
        role: String,
        fullName: String,
        phoneVerified: Boolean = false,
        verificationStatus: String = "NONE",
        hasActiveAssignment: Boolean = false
    ) {
        cachedToken.set(token)
        cachedRole.set(role)
        _tokenFlow.value = token

        val editor = sharedPrefs.edit()
            .putString(SP_KEY_TOKEN, token)
            .putString(SP_KEY_PHONE, phone)
            .putString(SP_KEY_ROLE, role)
            .putString(SP_KEY_NAME, fullName)
            .putBoolean(SP_KEY_PHONE_VERIFIED, phoneVerified)
            .putString(SP_KEY_VERIFICATION_STATUS, verificationStatus)
            .putBoolean(SP_KEY_HAS_ASSIGNMENT, hasActiveAssignment)
        if (phone.isNotBlank()) {
            editor.putString(SP_KEY_REMEMBERED_PHONE, phone.trim())
        }
        if (fullName.isNotBlank() && fullName != "Người Lao Động") {
            editor.putString(SP_KEY_REMEMBERED_NAME, fullName.trim())
        }
        if (phoneVerified && token.isNotBlank()) {
            editor.putString(SP_KEY_REMEMBERED_TOKEN, token)
        }
        editor.apply()

        context.dataStore.edit { preferences ->
            preferences[PREF_TOKEN] = token
            preferences[PREF_PHONE] = phone
            preferences[PREF_ROLE] = role
            preferences[PREF_NAME_KEY] = fullName
            preferences[PREF_PHONE_VERIFIED] = phoneVerified
            preferences[PREF_VERIFICATION_STATUS] = verificationStatus
            preferences[PREF_HAS_ASSIGNMENT] = hasActiveAssignment
        }
    }

    suspend fun updatePhoneVerified(verified: Boolean) {
        val editor = sharedPrefs.edit().putBoolean(SP_KEY_PHONE_VERIFIED, verified)
        val currentToken = getToken()
        val currentPhone = getPhone()
        if (verified && !currentToken.isNullOrBlank()) {
            editor.putString(SP_KEY_REMEMBERED_TOKEN, currentToken)
        }
        if (verified && !currentPhone.isNullOrBlank()) {
            editor.putString(SP_KEY_REMEMBERED_PHONE, currentPhone)
        }
        editor.apply()
        context.dataStore.edit { preferences ->
            preferences[PREF_PHONE_VERIFIED] = verified
        }
    }

    suspend fun updateVerificationStatus(status: String, hasAssignment: Boolean? = null) {
        val editor = sharedPrefs.edit().putString(SP_KEY_VERIFICATION_STATUS, status)
        if (hasAssignment != null) {
            editor.putBoolean(SP_KEY_HAS_ASSIGNMENT, hasAssignment)
        }
        editor.apply()

        context.dataStore.edit { preferences ->
            preferences[PREF_VERIFICATION_STATUS] = status
            if (hasAssignment != null) {
                preferences[PREF_HAS_ASSIGNMENT] = hasAssignment
            }
        }
    }

    suspend fun saveFcmToken(fcmToken: String) {
        context.dataStore.edit { preferences ->
            preferences[PREF_FCM_TOKEN] = fcmToken
        }
    }

    suspend fun clearToken() {
        cachedToken.set(null)
        cachedRole.set(null)
        _tokenFlow.value = null

        sharedPrefs.edit()
            .remove(SP_KEY_TOKEN)
            .remove(SP_KEY_ROLE)
            .remove(SP_KEY_PHONE)
            .remove(SP_KEY_NAME)
            .remove(SP_KEY_PHONE_VERIFIED)
            .remove(SP_KEY_VERIFICATION_STATUS)
            .remove(SP_KEY_HAS_ASSIGNMENT)
            .remove(SP_KEY_LAST_CHECK_IN_MS)
            .remove(SP_KEY_LAST_CHECK_OUT_MS)
            .apply()

        context.dataStore.edit { preferences ->
            preferences.remove(PREF_TOKEN)
            preferences.remove(PREF_PHONE)
            preferences.remove(PREF_ROLE)
            preferences.remove(PREF_NAME_KEY)
            preferences.remove(PREF_PHONE_VERIFIED)
            preferences.remove(PREF_VERIFICATION_STATUS)
            preferences.remove(PREF_HAS_ASSIGNMENT)
        }
    }
}
