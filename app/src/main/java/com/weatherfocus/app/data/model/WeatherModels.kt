package com.weatherfocus.app.data.model

import com.google.gson.annotations.SerializedName

/* ================= Open-Meteo DTOs (free, no API key) ================= */

data class OpenMeteoForecastResponse(
    val current: OpenMeteoCurrent?,
    val hourly: OpenMeteoHourly?,
    val daily: OpenMeteoDaily?
)

data class OpenMeteoCurrent(
    val temperature_2m: Double?,
    val apparent_temperature: Double?,
    val relative_humidity_2m: Int?,
    val wind_speed_10m: Double?,
    val surface_pressure: Double?,
    val weather_code: Int?,
    val is_day: Int?
)

data class OpenMeteoHourly(
    val time: List<String>?,
    val temperature_2m: List<Double?>?,
    val precipitation_probability: List<Int?>?,
    val weather_code: List<Int?>?,
    val wind_speed_10m: List<Double?>?,
    val uv_index: List<Double?>?
)

data class OpenMeteoDaily(
    val time: List<String>?,
    val weather_code: List<Int?>?,
    val temperature_2m_max: List<Double?>?,
    val temperature_2m_min: List<Double?>?,
    val precipitation_probability_max: List<Int?>?,
    val wind_speed_10m_max: List<Double?>?,
    val uv_index_max: List<Double?>?,
    val sunrise: List<String>?,
    val sunset: List<String>?
)

/** Open-Meteo geocoding (city search-as-you-type), also free / no key. */
data class OpenMeteoGeocodingResponse(val results: List<OpenMeteoGeoResult>?)

data class OpenMeteoGeoResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val country_code: String?,
    val admin1: String?
)

/* ================= wttr.in DTO (free, no API key, used as 2nd source) ================= */

data class WttrResponse(
    val current_condition: List<WttrCurrent>?,
    val weather: List<WttrDay>?
)

data class WttrCurrent(
    val temp_C: String?,
    val FeelsLikeC: String?,
    val humidity: String?,
    val windspeedKmph: String?,
    val pressure: String?,
    val weatherDesc: List<WttrDesc>?
)

data class WttrDesc(val value: String?)

data class WttrDay(
    val date: String?,
    val maxtempC: String?,
    val mintempC: String?,
    val hourly: List<WttrHourly>?
)

data class WttrHourly(val chanceofrain: String?, val weatherDesc: List<WttrDesc>?)

/* ================= OpenWeather DTO (optional 3rd source, needs user API key) ================= */

data class OpenWeatherCurrentResponse(
    val main: OwMain?,
    val wind: OwWind?,
    val weather: List<OwDesc>?
)

data class OwMain(val temp: Double?, val feels_like: Double?, val humidity: Int?, val pressure: Int?)
data class OwWind(val speed: Double?)
data class OwDesc(val description: String?, val main: String?)

/* ================= Clean, UI-ready domain models (mirrors the original app's shape) ================= */

/** One source's reading of current conditions, used to build [CurrentWeather.consensus]. */
data class SourceReading(
    val sourceName: String,
    val tempC: Double?,
    val conditionGroup: ConditionGroup
)

enum class ConditionGroup { CLEAR, CLOUDY, RAIN, SNOW, THUNDER, FOG, UNKNOWN }

data class CurrentWeather(
    val temp: Double? = null,
    val feelsLike: Double? = null,
    val humidity: Int? = null,
    val windSpeedKmh: Double? = null,
    val pressure: Int? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
    val description: String? = null,
    val conditionGroup: ConditionGroup = ConditionGroup.UNKNOWN,
    val tempMin: Double? = null,
    val tempMax: Double? = null,
    val available: Boolean = false,
    /** How many/which sources were combined, and how well they agreed. */
    val sourcesUsed: List<String> = emptyList(),
    val sourcesAgreeing: Int = 0,
    val sourcesTotal: Int = 0
)

/** One day in the "Next 3 Days" strip. */
data class DayForecast(
    val dayLabel: String,
    val dateLabel: String,
    val minTemp: Double? = null,
    val maxTemp: Double? = null,
    val description: String = "",
    val conditionGroup: ConditionGroup = ConditionGroup.UNKNOWN,
    val rainChancePercent: Int? = null,
    val windSpeedKmh: Double? = null,
    /** True for the ~14 tail days beyond Open-Meteo's 16-day real forecast, built from historical averages instead. */
    val isOutlookEstimate: Boolean = false
)

data class GeoPlace(
    val name: String,
    val admin1: String?,
    val country: String?,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double
) {
    val displayLabel: String
        get() = listOfNotNull(name, admin1, country).joinToString(", ")
}

/* ================= Custom alert rules & matches (same shape/behaviour as the original app) ================= */

enum class AlertRuleType {
    TEMP_ABOVE, TEMP_BELOW, UV_INDEX, WIND_SPEED, RAIN_PROBABILITY, THUNDERSTORM, SNOW
}

data class CustomAlertRules(
    val enabled: Boolean = false,
    val horizonHours: Int = 24,
    val leadTimeHours: Int = 3,
    val tempAboveEnabled: Boolean = false,
    val tempAboveValue: Double = 30.0,
    val tempBelowEnabled: Boolean = false,
    val tempBelowValue: Double = 0.0,
    val uvIndexEnabled: Boolean = false,
    val uvIndexValue: Double = 6.0,
    val windSpeedEnabled: Boolean = false,
    val windSpeedValue: Double = 40.0,
    val rainProbEnabled: Boolean = false,
    val rainProbValue: Int = 70,
    val thunderstormEnabled: Boolean = false,
    val snowEnabled: Boolean = false
)

data class CustomAlertMatch(
    val type: AlertRuleType = AlertRuleType.TEMP_ABOVE,
    val label: String = "",
    val detail: String = "",
    val dayLabel: String = "",
    val triggerAtMillis: Long = 0L,
    val leadWarning: Boolean = false
)

data class AppSettings(
    val cityName: String = "Vikersund",
    val countryCode: String = "NO",
    val countryLabel: String = "Norway",
    val latitude: Double = 60.35,
    val longitude: Double = 10.03,
    val useFahrenheit: Boolean = false,
    val openWeatherApiKey: String = "",
    val customAlertRules: CustomAlertRules = CustomAlertRules()
)
