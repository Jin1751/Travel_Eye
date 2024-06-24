package com.dongjin.traveleye
import androidx.annotation.Keep
import retrofit2.http.POST
import retrofit2.http.Query


@Keep
data class ApiResponse(
    val data: Data
)
@Keep
data class Data(
    val translations: List<Translation>
)
@Keep
data class Translation(
    val translatedText: String
)

@Keep
interface TranslateApi{
    @POST("v2?")
    suspend fun translate(@Query("key")key:String,@Query("q") q:String, @Query("source") source:String,@Query("target") target:String,@Query("format") format:String) : ApiResponse
}