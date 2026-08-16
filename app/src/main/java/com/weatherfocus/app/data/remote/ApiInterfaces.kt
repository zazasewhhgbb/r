package com.weatherfocus.app.data.remote

import com.weatherfocus.app.data.model.OpenMeteoForecastResponse
import com.weatherfocus.app.data.model.OpenMeteoGeocodingResponse
import com.weatherfocus.app.data.model.OpenWeatherCurrentResponse
import com.weatherfocus.app.data.model.WttrResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Free, no API key required. Primary source: current + hourly (48h, for custom alerts) + daily (16 days). */
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,surface_pressure,weather_code,is_day",
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weather_code,wind_speed_10m,uv_index",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max,uv_index_max,sunrise,sunset",
        @Query("forecast_days") forecastDays: Int = 16,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoForecastResponse

    /** Historical daily conditions for a past date range at this location - averaged to build the "long-range outlook" tail of the month view. */
    @GET("v1/archive")
    suspend fun getArchive(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoForecastResponse
}

/** Free, no API key required. City search-as-you-type autocomplete. */
interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponse
}

/** Free, no API key required. Used as a second independent source for cross-checking current conditions. */
interface WttrApi {
    @GET("{location}")
    suspend fun getWeather(
        @Path("location") location: String,
        @Query("format") format: String = "j1"
    ): WttrResponse
}

/** Optional third source - only used if the user supplies their own free OpenWeather API key in Settings. */
interface OpenWeatherApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") cityAndCountry: String,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String
    ): OpenWeatherCurrentResponse
}
