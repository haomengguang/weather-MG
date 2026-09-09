package com.skypulse.weather.data

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject

data class CityEntry(
    val name: String,
    val province: String,
    val lat: Double,
    val lon: Double
)

/**
 * Open-Meteo 城市搜索（免费无 key，请求只携带关键词本身）。
 * 文档: https://open-meteo.com/en/docs/geocoding-api
 */
interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponse
}

@JsonClass(generateAdapter = true)
data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoPlace>? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoPlace(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val admin1: String? = null,
    val country: String? = null
)

/**
 * 城市搜索服务。
 *
 * 原实现依赖小米内部天气接口（需不可公开获取的 XIAOMI_APP_KEY/SIGN，实机必然搜不到），
 * 已替换为 Open-Meteo Geocoding：支持中文/拼音/英文关键词，返回全球城市及行政区。
 */
class GeocodingService @Inject constructor(
    private val api: OpenMeteoGeocodingApi
) {

    suspend fun search(query: String): List<CityEntry> {
        if (query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                val response = api.search(query.trim())
                Log.d("GeocodingService", "Open-Meteo geocoding returned ${response.results?.size ?: 0} results for '$query'")

                response.results.orEmpty().mapNotNull { place ->
                    val name = place.name ?: return@mapNotNull null
                    val lat = place.latitude ?: return@mapNotNull null
                    val lon = place.longitude ?: return@mapNotNull null
                    // 省级信息优先 admin1（如"浙江"），海外城市附国家名
                    val province = place.admin1
                        ?: place.country
                        ?: ""
                    CityEntry(name = name, province = province, lat = lat, lon = lon)
                }
            } catch (e: Exception) {
                Log.e("GeocodingService", "search failed", e)
                emptyList()
            }
        }
    }
}
