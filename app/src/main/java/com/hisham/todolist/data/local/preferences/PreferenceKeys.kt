package com.hisham.todolist.data.local.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    val themeMode = stringPreferencesKey("theme_mode")
    val isAuthenticated = booleanPreferencesKey("is_authenticated")
    val userId = stringPreferencesKey("user_id")
    val displayName = stringPreferencesKey("display_name")
    val email = stringPreferencesKey("email")
    val photoUrl = stringPreferencesKey("photo_url")
}
