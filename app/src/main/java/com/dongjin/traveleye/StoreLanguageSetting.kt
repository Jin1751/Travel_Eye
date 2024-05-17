package com.dongjin.traveleye

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StoreLanguageSetting(private val context: Context) {
    companion object{
        private val Context.dataStore : DataStore<Preferences> by preferencesDataStore(name = "TravelEyeSetting")//TravelEyeSetting라는 언어설정 datastore 제작
        val USER_LANGUAGE_SETTING = stringPreferencesKey("LanguageSetting")
    }

    val getLanguageSetting : Flow<String> = context.dataStore.data.map {//dataStore에 있는 언어설정을 불러옴, 이전에 설정한 언어가 없으면 영어로 세팅
            preferences -> preferences[USER_LANGUAGE_SETTING] ?: "english"//영어로 초기 세팅을 해야 처음 설치했을때 주소를 빠르게 가져옴
    }

    suspend fun updateLanguageSetting(language : String = "korean") {//dataStore에 있는 언어 설정 변경
        context.dataStore.edit { settings -> settings[USER_LANGUAGE_SETTING] = language }
    }
}