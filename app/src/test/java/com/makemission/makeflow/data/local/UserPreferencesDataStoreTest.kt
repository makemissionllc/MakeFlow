package com.makemission.makeflow.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserPreferencesDataStoreTest {

    @Test
    fun lastTransactionAccountPreferenceDefaultsToNullAndCanBeCleared() = runTest {
        val preferences = UserPreferencesDataStore(InMemoryDataStore())

        assertNull(preferences.lastTransactionAccountId.first())
        preferences.saveLastTransactionAccountId("cash-account")
        assertEquals("cash-account", preferences.lastTransactionAccountId.first())
        preferences.saveLastTransactionAccountId(null)
        assertNull(preferences.lastTransactionAccountId.first())
    }

    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
