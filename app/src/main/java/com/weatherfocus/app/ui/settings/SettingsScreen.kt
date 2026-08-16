package com.weatherfocus.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherfocus.app.data.model.CustomAlertRules
import com.weatherfocus.app.data.prefs.CountryCatalog
import com.weatherfocus.app.ui.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: WeatherViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var cityQuery by remember(state.settings.cityName) { mutableStateOf(state.settings.cityName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SectionTitle("Location")

            ExposedDropdownMenuBox(expanded = countryMenuExpanded, onExpandedChange = { countryMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.settings.countryLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Country") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = countryMenuExpanded,
                    onDismissRequest = { countryMenuExpanded = false }
                ) {
                    CountryCatalog.ALL.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(entry.label) },
                            onClick = {
                                viewModel.onCountrySelected(entry.code, entry.label)
                                countryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer4()

            Column {
                OutlinedTextField(
                    value = cityQuery,
                    onValueChange = {
                        cityQuery = it
                        viewModel.onCityQueryChanged(it)
                    },
                    label = { Text("City") },
                    placeholder = { Text("Start typing a city name...") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.citySuggestions.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Column {
                            state.citySuggestions.take(6).forEach { place ->
                                DropdownMenuItem(
                                    text = { Text(place.displayLabel) },
                                    onClick = {
                                        cityQuery = place.name
                                        viewModel.onCitySelected(place)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer4()
            HorizontalDivider()
            Spacer4()

            SectionTitle("Units")
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Use Fahrenheit", modifier = Modifier.weight(1f))
                Switch(checked = state.settings.useFahrenheit, onCheckedChange = { viewModel.onFahrenheitToggled(it) })
            }

            Spacer4()
            HorizontalDivider()
            Spacer4()

            SectionTitle("Optional: OpenWeather API key")
            Text(
                "Adds a 3rd independent weather source for cross-checking (free key from openweathermap.org). Not required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer4()
            OutlinedTextField(
                value = state.settings.openWeatherApiKey,
                onValueChange = { viewModel.onOpenWeatherKeyChanged(it) },
                label = { Text("API key (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer4()
            HorizontalDivider()
            Spacer4()

            SectionTitle("Custom Weather Alerts")
            AlertRulesEditor(rules = state.settings.customAlertRules, onChange = { viewModel.updateAlertRules(it) })
        }
    }
}

@Composable
private fun AlertRulesEditor(rules: CustomAlertRules, onChange: (CustomAlertRules) -> Unit) {
    RowSwitch("Enable custom alerts", rules.enabled) { onChange(rules.copy(enabled = it)) }
    if (!rules.enabled) return

    Spacer4()
    Text("Check the next ${rules.horizonHours}h, warn ${rules.leadTimeHours}h before", style = MaterialTheme.typography.bodySmall)
    Slider(value = rules.horizonHours.toFloat(), onValueChange = { onChange(rules.copy(horizonHours = it.toInt())) }, valueRange = 1f..48f)
    Slider(value = rules.leadTimeHours.toFloat(), onValueChange = { onChange(rules.copy(leadTimeHours = it.toInt())) }, valueRange = 1f..24f)

    Spacer4()
    RowSwitch("Temperature above ${rules.tempAboveValue.toInt()}\u00B0C", rules.tempAboveEnabled) { onChange(rules.copy(tempAboveEnabled = it)) }
    if (rules.tempAboveEnabled) Slider(value = rules.tempAboveValue.toFloat(), onValueChange = { onChange(rules.copy(tempAboveValue = it.toDouble())) }, valueRange = -10f..45f)

    RowSwitch("Temperature below ${rules.tempBelowValue.toInt()}\u00B0C", rules.tempBelowEnabled) { onChange(rules.copy(tempBelowEnabled = it)) }
    if (rules.tempBelowEnabled) Slider(value = rules.tempBelowValue.toFloat(), onValueChange = { onChange(rules.copy(tempBelowValue = it.toDouble())) }, valueRange = -30f..20f)

    RowSwitch("UV index above ${rules.uvIndexValue.toInt()}", rules.uvIndexEnabled) { onChange(rules.copy(uvIndexEnabled = it)) }
    if (rules.uvIndexEnabled) Slider(value = rules.uvIndexValue.toFloat(), onValueChange = { onChange(rules.copy(uvIndexValue = it.toDouble())) }, valueRange = 1f..12f)

    RowSwitch("Wind speed above ${rules.windSpeedValue.toInt()} km/h", rules.windSpeedEnabled) { onChange(rules.copy(windSpeedEnabled = it)) }
    if (rules.windSpeedEnabled) Slider(value = rules.windSpeedValue.toFloat(), onValueChange = { onChange(rules.copy(windSpeedValue = it.toDouble())) }, valueRange = 5f..100f)

    RowSwitch("Rain probability above ${rules.rainProbValue}%", rules.rainProbEnabled) { onChange(rules.copy(rainProbEnabled = it)) }
    if (rules.rainProbEnabled) Slider(value = rules.rainProbValue.toFloat(), onValueChange = { onChange(rules.copy(rainProbValue = it.toInt())) }, valueRange = 10f..100f)

    RowSwitch("Thunderstorm expected", rules.thunderstormEnabled) { onChange(rules.copy(thunderstormEnabled = it)) }
    RowSwitch("Snow expected", rules.snowEnabled) { onChange(rules.copy(snowEnabled = it)) }
}

@Composable
private fun RowSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Spacer4() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
}
