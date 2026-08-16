package com.weatherfocus.app.data.alert

import com.weatherfocus.app.data.model.AlertRuleType
import com.weatherfocus.app.data.model.CustomAlertMatch
import com.weatherfocus.app.data.model.CustomAlertRules
import com.weatherfocus.app.data.model.OpenMeteoHourly
import com.weatherfocus.app.data.repository.WeatherCodeMapper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Evaluates the user's custom weather alert rules (Settings > Custom Weather Alert Rules)
 * against Open-Meteo's free hourly forecast. For each enabled rule this scans forward from
 * "now" out to [CustomAlertRules.horizonHours] and reports the first hour the threshold is
 * met, flagged [CustomAlertMatch.leadWarning] once it's inside the configured lead time.
 */
object CustomWeatherAlertEngine {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val weekdayDateFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())
    private val hourlyTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)

    private data class Point(val atMillis: Long, val temp: Double?, val windSpeed: Double?, val uvi: Double?, val pop: Int?, val code: Int?)

    fun evaluate(hourly: OpenMeteoHourly?, rules: CustomAlertRules, nowMillis: Long = System.currentTimeMillis()): List<CustomAlertMatch> {
        if (!rules.enabled || hourly?.time == null) return emptyList()

        val horizonMillis = TimeUnit.HOURS.toMillis(rules.horizonHours.coerceIn(1, 48).toLong())
        val leadMillis = TimeUnit.HOURS.toMillis(rules.leadTimeHours.coerceIn(1, 24).toLong())
        val windowEnd = nowMillis + horizonMillis

        val points = hourly.time.indices.mapNotNull { i ->
            val millis = runCatching { hourlyTimeFormat.parse(hourly.time[i])?.time }.getOrNull() ?: return@mapNotNull null
            if (millis !in nowMillis..windowEnd) return@mapNotNull null
            Point(
                atMillis = millis,
                temp = hourly.temperature_2m?.getOrNull(i),
                windSpeed = hourly.wind_speed_10m?.getOrNull(i),
                uvi = hourly.uv_index?.getOrNull(i),
                pop = hourly.precipitation_probability?.getOrNull(i),
                code = hourly.weather_code?.getOrNull(i)
            )
        }.sortedBy { it.atMillis }

        val results = mutableListOf<CustomAlertMatch>()
        fun isImminent(atMillis: Long) = atMillis - nowMillis <= leadMillis
        fun whenText(atMillis: Long) = "${dayLabel(atMillis, nowMillis)}, ${timeFormat.format(Date(atMillis))}"

        if (rules.tempAboveEnabled) {
            points.firstOrNull { (it.temp ?: Double.MIN_VALUE) >= rules.tempAboveValue }?.let { p ->
                results += CustomAlertMatch(
                    AlertRuleType.TEMP_ABOVE, "Temperature above ${fmt(rules.tempAboveValue)}\u00B0C",
                    "${whenText(p.atMillis)} \u2014 Forecast ${p.temp?.let { fmt(it) } ?: "\u2014"}\u00B0C",
                    dayLabel(p.atMillis, nowMillis), p.atMillis, isImminent(p.atMillis)
                )
            }
        }
        if (rules.tempBelowEnabled) {
            points.firstOrNull { (it.temp ?: Double.MAX_VALUE) <= rules.tempBelowValue }?.let { p ->
                results += CustomAlertMatch(
                    AlertRuleType.TEMP_BELOW, "Temperature below ${fmt(rules.tempBelowValue)}\u00B0C",
                    "${whenText(p.atMillis)} \u2014 Forecast ${p.temp?.let { fmt(it) } ?: "\u2014"}\u00B0C",
                    dayLabel(p.atMillis, nowMillis), p.atMillis, isImminent(p.atMillis)
                )
            }
        }
        if (rules.uvIndexEnabled) {
            points.firstOrNull { (it.uvi ?: -1.0) >= rules.uvIndexValue }?.let { p ->
                results += CustomAlertMatch(
                    AlertRuleType.UV_INDEX, "UV index above ${fmt(rules.uvIndexValue)}",
                    "${whenText(p.atMillis)} \u2014 Forecast UV ${p.uvi?.let { fmt(it) } ?: "\u2014"}",
                    dayLabel(p.atMillis, nowMillis), p.atMillis, isImminent(p.atMillis)
                )
            }
        }
        if (rules.windSpeedEnabled) {
            points.firstOrNull { (it.windSpeed ?: -1.0) >= rules.windSpeedValue }?.let { p ->
                results += CustomAlertMatch(
                    AlertRuleType.WIND_SPEED, "Wind speed above ${fmt(rules.windSpeedValue)} km/h",
                    "${whenText(p.atMillis)} \u2014 Forecast ${p.windSpeed?.let { fmt(it) } ?: "\u2014"} km/h",
                    dayLabel(p.atMillis, nowMillis), p.atMillis, isImminent(p.atMillis)
                )
            }
        }
        if (rules.rainProbEnabled) {
            points.firstOrNull { (it.pop ?: -1) >= rules.rainProbValue }?.let { p ->
                results += CustomAlertMatch(
                    AlertRuleType.RAIN_PROBABILITY, "Rain probability above ${rules.rainProbValue}%",
                    "${whenText(p.atMillis)} \u2014 Forecast ${p.pop ?: 0}%",
                    dayLabel(p.atMillis, nowMillis), p.atMillis, isImminent(p.atMillis)
                )
            }
        }
        if (rules.thunderstormEnabled) {
            points.firstOrNull { WeatherCodeMapper.isThunder(it.code) }?.let { p ->
                results += CustomAlertMatch(
                    AlertRuleType.THUNDERSTORM, "Thunderstorm expected",
                    "Expected ${whenText(p.atMillis)}", dayLabel(p.atMillis, nowMillis), p.atMillis, isImminent(p.atMillis)
                )
            }
        }
        if (rules.snowEnabled) {
            points.firstOrNull { WeatherCodeMapper.isSnow(it.code) }?.let { p ->
                results += CustomAlertMatch(
                    AlertRuleType.SNOW, "Snow expected",
                    "Expected ${whenText(p.atMillis)}", dayLabel(p.atMillis, nowMillis), p.atMillis, isImminent(p.atMillis)
                )
            }
        }

        return results.sortedBy { it.triggerAtMillis }
    }

    private fun dayLabel(triggerMillis: Long, nowMillis: Long): String {
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val diffDays = ((triggerMillis - startOfToday) / TimeUnit.DAYS.toMillis(1)).toInt()
        return when {
            diffDays <= 0 -> "Today"
            diffDays == 1 -> "Tomorrow"
            diffDays in 2..6 -> "In $diffDays days"
            else -> weekdayDateFormat.format(Date(triggerMillis))
        }
    }

    private fun fmt(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else String.format(Locale.US, "%.1f", value)
}
