package com.dongjin.traveleye

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class StoreLanguageSetting(private val context: Context) {
    companion object{
        private val Context.dataStore : DataStore<Preferences> by preferencesDataStore(name = "TravelEyeSetting")//TravelEyeSetting라는 언어설정 datastore 제작
        val USER_LANGUAGE_SETTING = stringPreferencesKey("LanguageSetting")
    }

    val getLanguageSetting : Flow<String> = context.dataStore.data.map {//dataStore에 있는 언어설정을 불러옴, 이전에 설정한 언어가 없으면 한국어로 세팅
            preferences -> preferences[USER_LANGUAGE_SETTING] ?: "korean"
    }

    suspend fun updateLanguageSetting(language : String = "korean") {//dataStore에 있는 언어 설정 변경
        context.dataStore.edit { settings -> settings[USER_LANGUAGE_SETTING] = language }
    }
}