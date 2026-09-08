package com.skypulse.weather.data.remote

import com.skypulse.weather.model.DailyAstro
import com.skypulse.weather.model.DailyForecast
import com.skypulse.weather.model.DailyPrecipitation
import com.skypulse.weather.model.DailySkycon
import com.skypulse.weather.model.DailyTemperature
import com.skypulse.weather.model.AstroTime
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo 免费开源气象接口（无需 key）。
 *
 * 仅用于补齐彩云免费 token 只有 3 天的逐日预报：
 * 请求只携带经纬度坐标，无任何设备或用户信息。
 * 文档: https://open-meteo.com/en/docs
 */
interface OpenMeteoApi {

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"

        /** WMO 天气代码 → 彩云 skycon，未识别回退多云 */
        fun wmoToSkycon(code: Int?): String = when (code) {
            0 -> "CLEAR_DAY"
            1, 2 -> "PARTLY_CLOUDY_DAY"
            3 -> "CLOUDY"
            45, 48 -> "FOG"
            51, 53, 55, 56, 57 -> "LIGHT_RAIN"
            61, 63, 80, 81 -> "MODERATE_RAIN"
            65, 82 -> "HEAVY_RAIN"
            66, 67 -> "SLEET"
            71, 77, 85 -> "LIGHT_SNOW"
            73, 86 -> "MODERATE_SNOW"
            75 -> "HEAVY_SNOW"
            95, 96, 99 -> "THUNDER_SHOWER"
            else -> "PARTLY_CLOUDY_DAY"
        }
    }

    @GET("v1/forecast")
    suspend fun getDailyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 16
    ): OpenMeteoDailyResponse
}

@JsonClass(generateAdapter = true)
data class OpenMeteoDailyResponse(
    @Json(name = "daily") val daily: OpenMeteoDailyBlock? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDailyBlock(
    @Json(name = "time") val time: List<String>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int?>? = null,
    @Json(name = "temperature_2m_max") val tempMax: List<Double?>? = null,
    @Json(name = "temperature_2m_min") val tempMin: List<Double?>? = null,
    @Json(name = "precipitation_probability_max") val precipProbability: List<Int?>? = null,
    @Json(name = "sunrise") val sunrise: List<String?>? = null,
    @Json(name = "sunset") val sunset: List<String?>? = null
)

/**
 * 将 Open-Meteo 逐日块转换为彩云 DailyForecast（仅填充可映射字段）。
 *
 * @param startIndex 从 OM 数据的第几天下标开始取（用于对齐彩云已覆盖的日期）
 */
fun OpenMeteoDailyBlock.toCaiyunDailyForecast(startIndex: Int): DailyForecast {
    val time = time.orEmpty()
    val safeStart = startIndex.coerceIn(0, time.size)
    if (safeStart >= time.size) {
        return DailyForecast(status = "om_empty")
    }
    val dates = time.drop(safeStart)
    fun <T> List<T>?.takeFrom(): List<T> = this?.drop(safeStart).orEmpty()

    val tempMax = tempMax.takeFrom()
    val tempMin = tempMin.takeFrom()
    val precip = precipProbability.takeFrom()
    val codes = weatherCode.takeFrom()
    val sunriseList = sunrise.orEmpty().drop(safeStart)
    val sunsetList = sunset.orEmpty().drop(safeStart)

    fun caiyunDate(d: String): String = "${d}T00:00+08:00"
    val temperatures = dates.mapIndexed { i, d ->
        DailyTemperature(date = caiyunDate(d), max = tempMax.getOrNull(i), min = tempMin.getOrNull(i))
    }
    val precipitations = dates.mapIndexed { i, d ->
        DailyPrecipitation(date = caiyunDate(d), probability = precip.getOrNull(i)?.toDouble())
    }
    val skycons = dates.mapIndexed { i, d ->
        DailySkycon(date = caiyunDate(d), value = OpenMeteoApi.wmoToSkycon(codes.getOrNull(i)))
    }
    val astros = dates.mapIndexed { i, d ->
        DailyAstro(
            date = caiyunDate(d),
            sunrise = sunriseList.getOrNull(i)?.let { AstroTime(time = it.substringAfter('T')) },
            sunset = sunsetList.getOrNull(i)?.let { AstroTime(time = it.substringAfter('T')) }
        )
    }
    return DailyForecast(
        status = "ok",
        astro = astros,
        precipitation = precipitations,
        temperature = temperatures,
        skycon = skycons
    )
}
