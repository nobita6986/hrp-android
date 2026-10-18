package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

        val PREF_TOKEN = stringPreferencesKey("jwt_token")
        val PREF_ROLE = stringPreferencesKey("user_role")
        val PREF_PHONE = stringPreferencesKey("user_phone")
        val PREF_NAME_KEY = stringPreferencesKey("user_full_name")
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

    // Fast synchronous in-memory cache for OkHttp AuthInterceptor
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

    open fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
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

    suspend fun saveUserSession(token: String, phone: String, role: String, fullName: String) {
        cachedToken.set(token)
        cachedRole.set(role)
        _tokenFlow.value = token

        sharedPrefs.edit()
            .putString(SP_KEY_TOKEN, token)
            .putString(SP_KEY_PHONE, phone)
            .putString(SP_KEY_ROLE, role)
            .putString(SP_KEY_NAME, fullName)
            .apply()

        context.dataStore.edit { preferences ->
            preferences[PREF_TOKEN] = token
            preferences[PREF_PHONE] = phone
            preferences[PREF_ROLE] = role
            preferences[PREF_NAME_KEY] = fullName
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

        sharedPrefs.edit().clear().apply()

        context.dataStore.edit { preferences ->
            preferences.remove(PREF_TOKEN)
            preferences.remove(PREF_PHONE)
            preferences.remove(PREF_ROLE)
            preferences.remove(PREF_NAME_KEY)
        }
    }
}
