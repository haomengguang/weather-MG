package com.skypulse.weather.widget

object WidgetRefreshPolicy {
    const val PERIODIC_REFRESH_MINUTES = 15L
    private const val CACHE_FRESH_MILLIS = 30L * 60 * 1000
    private const val SIGNIFICANT_MOVEMENT_METERS = 1000f

    /**
     * 小组件刷新决策：缓存超过新鲜窗口（30 分钟）必须刷新；
     * 窗口内仅当位移达到显著阈值（1000 米）才刷新，避免附近移动频繁请求。
     */
    fun shouldFetchWeather(
        distanceMeters: Float,
        lastFetchTimeMillis: Long,
        nowMillis: Long
    ): Boolean {
        val ageMillis = (nowMillis - lastFetchTimeMillis).coerceAtLeast(0L)
        if (ageMillis >= CACHE_FRESH_MILLIS) return true
        return distanceMeters >= SIGNIFICANT_MOVEMENT_METERS
    }
}
