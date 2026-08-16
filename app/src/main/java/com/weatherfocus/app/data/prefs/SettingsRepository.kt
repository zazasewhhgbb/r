package com.weatherfocus.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weatherfocus.app.data.model.AppSettings
import com.weatherfocus.app.data.model.CustomAlertRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "weather_only_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val CITY = stringPreferencesKey("city_name")
        val COUNTRY_CODE = stringPreferencesKey("country_code")
        val COUNTRY_LABEL = stringPreferencesKey("country_label")
        val LAT = doublePreferencesKey("latitude")
        val LON = doublePreferencesKey("longitude")
        val FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        val OW_KEY = stringPreferencesKey("openweather_api_key")

        val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        val HORIZON_HOURS = intPreferencesKey("horizon_hours")
        val LEAD_HOURS = intPreferencesKey("lead_hours")
        val TEMP_ABOVE_ON = booleanPreferencesKey("temp_above_on")
        val TEMP_ABOVE_VAL = doublePreferencesKey("temp_above_val")
        val TEMP_BELOW_ON = booleanPreferencesKey("temp_below_on")
        val TEMP_BELOW_VAL = doublePreferencesKey("temp_below_val")
        val UV_ON = booleanPreferencesKey("uv_on")
        val UV_VAL = doublePreferencesKey("uv_val")
        val WIND_ON = booleanPreferencesKey("wind_on")
        val WIND_VAL = doublePreferencesKey("wind_val")
        val RAIN_ON = booleanPreferencesKey("rain_on")
        val RAIN_VAL = intPreferencesKey("rain_val")
        val THUNDER_ON = booleanPreferencesKey("thunder_on")
        val SNOW_ON = booleanPreferencesKey("snow_on")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            cityName = p[Keys.CITY] ?: "Vikersund",
            countryCode = p[Keys.COUNTRY_CODE] ?: "NO",
            countryLabel = p[Keys.COUNTRY_LABEL] ?: "Norway",
            latitude = p[Keys.LAT] ?: 60.35,
            longitude = p[Keys.LON] ?: 10.03,
            useFahrenheit = p[Keys.FAHRENHEIT] ?: false,
            openWeatherApiKey = p[Keys.OW_KEY] ?: "",
            customAlertRules = CustomAlertRules(
                enabled = p[Keys.ALERTS_ENABLED] ?: false,
                horizonHours = p[Keys.HORIZON_HOURS] ?: 24,
                leadTimeHours = p[Keys.LEAD_HOURS] ?: 3,
                tempAboveEnabled = p[Keys.TEMP_ABOVE_ON] ?: false,
                tempAboveValue = p[Keys.TEMP_ABOVE_VAL] ?: 30.0,
                tempBelowEnabled = p[Keys.TEMP_BELOW_ON] ?: false,
                tempBelowValue = p[Keys.TEMP_BELOW_VAL] ?: 0.0,
                uvIndexEnabled = p[Keys.UV_ON] ?: false,
                uvIndexValue = p[Keys.UV_VAL] ?: 6.0,
                windSpeedEnabled = p[Keys.WIND_ON] ?: false,
                windSpeedValue = p[Keys.WIND_VAL] ?: 40.0,
                rainProbEnabled = p[Keys.RAIN_ON] ?: false,
                rainProbValue = p[Keys.RAIN_VAL] ?: 70,
                thunderstormEnabled = p[Keys.THUNDER_ON] ?: false,
                snowEnabled = p[Keys.SNOW_ON] ?: false
            )
        )
    }

    suspend fun setLocation(city: String, countryCode: String, countryLabel: String, lat: Double, lon: Double) {
        context.dataStore.edit {
            it[Keys.CITY] = city
            it[Keys.COUNTRY_CODE] = countryCode
            it[Keys.COUNTRY_LABEL] = countryLabel
            it[Keys.LAT] = lat
            it[Keys.LON] = lon
        }
    }

    suspend fun setFahrenheit(value: Boolean) = context.dataStore.edit { it[Keys.FAHRENHEIT] = value }
    suspend fun setOpenWeatherApiKey(value: String) = context.dataStore.edit { it[Keys.OW_KEY] = value }

    suspend fun updateAlertRules(rules: CustomAlertRules) {
        context.dataStore.edit {
            it[Keys.ALERTS_ENABLED] = rules.enabled
            it[Keys.HORIZON_HOURS] = rules.horizonHours
            it[Keys.LEAD_HOURS] = rules.leadTimeHours
            it[Keys.TEMP_ABOVE_ON] = rules.tempAboveEnabled
            it[Keys.TEMP_ABOVE_VAL] = rules.tempAboveValue
            it[Keys.TEMP_BELOW_ON] = rules.tempBelowEnabled
            it[Keys.TEMP_BELOW_VAL] = rules.tempBelowValue
            it[Keys.UV_ON] = rules.uvIndexEnabled
            it[Keys.UV_VAL] = rules.uvIndexValue
            it[Keys.WIND_ON] = rules.windSpeedEnabled
            it[Keys.WIND_VAL] = rules.windSpeedValue
            it[Keys.RAIN_ON] = rules.rainProbEnabled
            it[Keys.RAIN_VAL] = rules.rainProbValue
            it[Keys.THUNDER_ON] = rules.thunderstormEnabled
            it[Keys.SNOW_ON] = rules.snowEnabled
        }
    }
}

/** Countries offered in the Settings location picker. */
object CountryCatalog {
    data class Entry(val code: String, val label: String)

    val ALL: List<Entry> = listOf(
        Entry("NO", "Norway"), Entry("SE", "Sweden"), Entry("DK", "Denmark"),
        Entry("GB", "United Kingdom"), Entry("US", "United States"), Entry("DE", "Germany"),
        Entry("FR", "France"), Entry("ES", "Spain"), Entry("IT", "Italy"),
        Entry("NL", "Netherlands"), Entry("PL", "Poland"), Entry("FI", "Finland"),
        Entry("PT", "Portugal"), Entry("IE", "Ireland"), Entry("BE", "Belgium"),
        Entry("AT", "Austria"), Entry("CH", "Switzerland"), Entry("CA", "Canada"),
        Entry("AU", "Australia"), Entry("IN", "India"), Entry("BR", "Brazil"),
        Entry("JP", "Japan"), Entry("GR", "Greece"), Entry("TR", "Turkey")
    )
}
