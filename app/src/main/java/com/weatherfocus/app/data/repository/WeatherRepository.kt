package com.weatherfocus.app.data.repository

import com.weatherfocus.app.data.model.AppSettings
import com.weatherfocus.app.data.model.ConditionGroup
import com.weatherfocus.app.data.model.CurrentWeather
import com.weatherfocus.app.data.model.DayForecast
import com.weatherfocus.app.data.model.GeoPlace
import com.weatherfocus.app.data.model.OpenMeteoHourly
import com.weatherfocus.app.data.model.SourceReading
import com.weatherfocus.app.data.remote.NetworkModule
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Everything fetched/derived for the app's two main screens: today's live consensus reading, the 3-day strip, and the month view. */
data class WeatherBundle(
    val current: CurrentWeather,
    val next3Days: List<DayForecast> = emptyList(),
    val monthDays: List<DayForecast> = emptyList(),
    val hourlyForAlerts: OpenMeteoHourly? = null
)

class WeatherRepository {

    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun searchCities(query: String): List<GeoPlace> {
        if (query.length < 2) return emptyList()
        return runCatching {
            NetworkModule.openMeteoGeocodingApi.search(name = query).results.orEmpty()
                .map {
                    GeoPlace(
                        name = it.name,
                        admin1 = it.admin1,
                        country = it.country,
                        countryCode = it.country_code,
                        latitude = it.latitude,
                        longitude = it.longitude
                    )
                }
        }.getOrElse { emptyList() }
    }

    suspend fun loadWeather(settings: AppSettings): WeatherBundle = coroutineScope {
        val openMeteoDeferred = async {
            runCatching { NetworkModule.openMeteoApi.getForecast(settings.latitude, settings.longitude) }.getOrNull()
        }
        val wttrDeferred = async {
            withTimeoutOrNull(10_000) {
                runCatching { NetworkModule.wttrApi.getWeather("${settings.cityName},${settings.countryCode}") }.getOrNull()
            }
        }
        val openWeatherDeferred = async {
            if (settings.openWeatherApiKey.isBlank()) null else withTimeoutOrNull(10_000) {
                runCatching {
                    NetworkModule.openWeatherApi.getCurrentWeather(
                        "${settings.cityName},${settings.countryCode}",
                        apiKey = settings.openWeatherApiKey
                    )
                }.getOrNull()
            }
        }

        val openMeteo = openMeteoDeferred.await()
        val wttr = wttrDeferred.await()
        val openWeather = openWeatherDeferred.await()

        val current = buildConsensusCurrent(openMeteo, wttr, openWeather)
        val next3 = buildNext3Days(openMeteo)
        val monthReal = buildRealMonthDays(openMeteo)
        val monthOutlook = buildOutlookTailDays(settings, monthReal.size)

        WeatherBundle(
            current = current,
            next3Days = next3,
            monthDays = monthReal + monthOutlook,
            hourlyForAlerts = openMeteo?.hourly
        )
    }

    /* ---------- current conditions: combine up to 3 independent sources ---------- */

    private fun buildConsensusCurrent(
        openMeteo: com.weatherfocus.app.data.model.OpenMeteoForecastResponse?,
        wttr: com.weatherfocus.app.data.model.WttrResponse?,
        openWeather: com.weatherfocus.app.data.model.OpenWeatherCurrentResponse?
    ): CurrentWeather {
        val readings = mutableListOf<SourceReading>()

        val omCurrent = openMeteo?.current
        if (omCurrent?.temperature_2m != null) {
            readings += SourceReading("Open-Meteo", omCurrent.temperature_2m, WeatherCodeMapper.groupOf(omCurrent.weather_code))
        }

        val wttrCurrent = wttr?.current_condition?.firstOrNull()
        val wttrTemp = wttrCurrent?.temp_C?.toDoubleOrNull()
        if (wttrTemp != null) {
            val desc = wttrCurrent.weatherDesc?.firstOrNull()?.value
            readings += SourceReading("wttr.in", wttrTemp, TextConditionMapper.groupOf(desc))
        }

        val owTemp = openWeather?.main?.temp
        if (owTemp != null) {
            val desc = openWeather.weather?.firstOrNull()?.main
            readings += SourceReading("OpenWeather", owTemp, TextConditionMapper.groupOf(desc))
        }

        if (readings.isEmpty()) return CurrentWeather(available = false)

        val avgTemp = readings.mapNotNull { it.tempC }.average()
        val groupCounts = readings.groupingBy { it.conditionGroup }.eachCount()
        val majorityGroup = groupCounts.maxByOrNull { it.value }?.key ?: ConditionGroup.UNKNOWN
        val agreeing = groupCounts[majorityGroup] ?: 0

        val daily = openMeteo?.daily
        val sunriseTime = daily?.sunrise?.firstOrNull()?.let { formatClockTime(it) }
        val sunsetTime = daily?.sunset?.firstOrNull()?.let { formatClockTime(it) }

        return CurrentWeather(
            temp = avgTemp,
            feelsLike = omCurrent?.apparent_temperature ?: avgTemp,
            humidity = omCurrent?.relative_humidity_2m ?: wttrCurrent?.humidity?.toIntOrNull(),
            windSpeedKmh = omCurrent?.wind_speed_10m ?: wttrCurrent?.windspeedKmph?.toDoubleOrNull(),
            pressure = omCurrent?.surface_pressure?.roundToInt() ?: wttrCurrent?.pressure?.toIntOrNull(),
            sunrise = sunriseTime,
            sunset = sunsetTime,
            description = describeGroup(majorityGroup, omCurrent?.weather_code),
            conditionGroup = majorityGroup,
            tempMin = daily?.temperature_2m_min?.firstOrNull(),
            tempMax = daily?.temperature_2m_max?.firstOrNull(),
            available = true,
            sourcesUsed = readings.map { it.sourceName },
            sourcesAgreeing = agreeing,
            sourcesTotal = readings.size
        )
    }

    private fun describeGroup(group: ConditionGroup, omCode: Int?): String =
        if (omCode != null) WeatherCodeMapper.describe(omCode) else TextConditionMapper.label(group)

    /* ---------- next 3 days strip, from Open-Meteo daily block (index 1..3, since index 0 is today) ---------- */

    private fun buildNext3Days(openMeteo: com.weatherfocus.app.data.model.OpenMeteoForecastResponse?): List<DayForecast> {
        val daily = openMeteo?.daily ?: return emptyList()
        val times = daily.time.orEmpty()
        val result = mutableListOf<DayForecast>()
        for (i in 1..3) {
            if (i >= times.size) break
            result += dayForecastFromDaily(daily, i, isOutlook = false)
        }
        return result
    }

    /** All available real daily entries (typically 16 including today) as month-view rows. */
    private fun buildRealMonthDays(openMeteo: com.weatherfocus.app.data.model.OpenMeteoForecastResponse?): List<DayForecast> {
        val daily = openMeteo?.daily ?: return emptyList()
        val times = daily.time.orEmpty()
        return times.indices.map { i -> dayForecastFromDaily(daily, i, isOutlook = false) }
    }

    private fun dayForecastFromDaily(daily: com.weatherfocus.app.data.model.OpenMeteoDaily, i: Int, isOutlook: Boolean): DayForecast {
        val dateStr = daily.time?.getOrNull(i)
        val date = dateStr?.let { runCatching { isoDate.parse(it) }.getOrNull() }
        val code = daily.weather_code?.getOrNull(i)
        return DayForecast(
            dayLabel = date?.let { dayFormat.format(it) } ?: "",
            dateLabel = date?.let { dateFormat.format(it) } ?: (dateStr ?: ""),
            minTemp = daily.temperature_2m_min?.getOrNull(i),
            maxTemp = daily.temperature_2m_max?.getOrNull(i),
            description = WeatherCodeMapper.describe(code),
            conditionGroup = WeatherCodeMapper.groupOf(code),
            rainChancePercent = daily.precipitation_probability_max?.getOrNull(i),
            windSpeedKmh = daily.wind_speed_10m_max?.getOrNull(i),
            isOutlookEstimate = isOutlook
        )
    }

    /**
     * Days beyond Open-Meteo's real 16-day forecast, out to a full month, built from the
     * average of the same calendar dates over the last 3 years (Open-Meteo's historical
     * archive). Clearly flagged via [DayForecast.isOutlookEstimate] - this is a "typical
     * conditions" outlook, not a real forecast, since no provider forecasts that far out.
     */
    private suspend fun buildOutlookTailDays(settings: AppSettings, alreadyHaveDays: Int): List<DayForecast> {
        val remaining = 30 - alreadyHaveDays
        if (remaining <= 0) return emptyList()

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, alreadyHaveDays)
        val windowStart = cal.time
        cal.add(Calendar.DAY_OF_YEAR, remaining - 1)
        val windowEnd = cal.time

        // Fetch the same calendar-date window from each of the last 3 years and average per offset.
        val yearsBack = listOf(1, 2, 3)
        val perYearResults = yearsBack.map { years ->
            val startCal = Calendar.getInstance().apply { time = windowStart; add(Calendar.YEAR, -years) }
            val endCal = Calendar.getInstance().apply { time = windowEnd; add(Calendar.YEAR, -years) }
            runCatching {
                NetworkModule.openMeteoApi.getArchive(
                    settings.latitude, settings.longitude,
                    isoDate.format(startCal.time), isoDate.format(endCal.time)
                ).daily
            }.getOrNull()
        }

        val result = mutableListOf<DayForecast>()
        val labelCal = Calendar.getInstance().apply { time = windowStart }
        for (offset in 0 until remaining) {
            val maxTemps = mutableListOf<Double>()
            val minTemps = mutableListOf<Double>()
            val codes = mutableListOf<Int>()
            perYearResults.forEach { daily ->
                daily?.temperature_2m_max?.getOrNull(offset)?.let { maxTemps += it }
                daily?.temperature_2m_min?.getOrNull(offset)?.let { minTemps += it }
                daily?.weather_code?.getOrNull(offset)?.let { codes += it }
            }
            val avgMax = maxTemps.takeIf { it.isNotEmpty() }?.average()
            val avgMin = minTemps.takeIf { it.isNotEmpty() }?.average()
            val commonCode = codes.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

            val theDate = labelCal.time
            result += DayForecast(
                dayLabel = dayFormat.format(theDate),
                dateLabel = dateFormat.format(theDate),
                minTemp = avgMin,
                maxTemp = avgMax,
                description = if (commonCode != null) WeatherCodeMapper.describe(commonCode) else "No data",
                conditionGroup = WeatherCodeMapper.groupOf(commonCode),
                isOutlookEstimate = true
            )
            labelCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    private fun formatClockTime(isoDateTime: String): String {
        // Open-Meteo returns e.g. "2026-08-16T06:12"
        val timePart = isoDateTime.substringAfter('T', "")
        return timePart.ifBlank { isoDateTime }
    }
}
