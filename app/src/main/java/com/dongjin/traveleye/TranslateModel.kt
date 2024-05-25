package com.dongjin.traveleye
import retrofit2.http.POST
import retrofit2.http.Query

data class ApiResponse(
    val data: Data
)
data class Data(
    val translations: List<Translation>
)

data class Translation(
    val translatedText: String
)

interface TranslateApi{
    @POST("v2?")
    suspend fun translate(@Query("key")key:String,@Query("q") q:String, @Query("source") source:String,@Query("target") target:String,@Query("format") format:String) : ApiResponse
}