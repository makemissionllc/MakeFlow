package com.makemission.makeflow.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userPrefDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesDataStore(private val dataStore: DataStore<Preferences>) {

    val selectedCurrency: Flow<String> = dataStore.data.map { prefs ->
        prefs[SELECTED_CURRENCY_KEY] ?: "KES"
    }

    suspend fun saveCurrency(currency: String) {
        dataStore.edit { prefs ->
            prefs[SELECTED_CURRENCY_KEY] = currency
        }
    }

    companion object {
        private val SELECTED_CURRENCY_KEY = stringPreferencesKey("selected_currency")
    }
}
