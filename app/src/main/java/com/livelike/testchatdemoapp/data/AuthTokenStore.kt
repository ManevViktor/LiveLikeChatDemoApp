package com.livelike.testchatdemoapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference


private const val AUTH_TOKEN_DATA_STORE_NAME = "auth_token_store"
private val Context.authTokenDataStore: DataStore<Preferences> by preferencesDataStore(
	name = AUTH_TOKEN_DATA_STORE_NAME
)

class AuthTokenStore(context : Context) {
	private val appContext = context.applicationContext
	private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val cachedToken = AtomicReference<String?>(null)

	val tokenFlow : Flow<String?> = appContext.authTokenDataStore.data.map { preferences ->
		preferences[ACCESS_TOKEN_KEY]
	}

	suspend fun saveToken(token : String?) {
		val normalizedToken = token?.takeIf { it.isNotBlank() }
		cachedToken.set(normalizedToken)
		appContext.authTokenDataStore.edit { preferences ->
			if (normalizedToken == null) {
				preferences.remove(ACCESS_TOKEN_KEY)
			} else {
				preferences[ACCESS_TOKEN_KEY] = normalizedToken
			}
		}
	}

	fun saveTokenAsync(token : String?) {
		ioScope.launch {
			saveToken(token)
		}
	}

	fun getTokenBlocking() : String? {
		cachedToken.get()?.let { return it }
		val storedToken = runBlocking(Dispatchers.IO) {
			tokenFlow.first()
		}
		cachedToken.set(storedToken)
		return storedToken
	}

	fun clearTokenAsync() {
		saveTokenAsync(null)
	}

	private companion object {
		val ACCESS_TOKEN_KEY = stringPreferencesKey("livelike_access_token")
	}
}
