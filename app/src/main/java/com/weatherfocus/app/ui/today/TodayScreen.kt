package com.weatherfocus.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherfocus.app.data.model.CurrentWeather
import com.weatherfocus.app.data.model.DayForecast
import com.weatherfocus.app.ui.WeatherFormat
import com.weatherfocus.app.ui.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: WeatherViewModel,
    onOpenMonth: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.settings.cityName) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoading && state.bundle == null) {
                Spacer(Modifier.height(80.dp))
                CircularProgressIndicator()
            } else {
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                if (state.activeAlerts.isNotEmpty()) {
                    AlertBanner(count = state.activeAlerts.size)
                    Spacer(Modifier.height(12.dp))
                }
                state.bundle?.current?.let { current ->
                    TodaySection(current = current, useFahrenheit = state.settings.useFahrenheit)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Next 3 Days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                state.bundle?.next3Days?.let { days ->
                    ThreeDayStrip(days = days, useFahrenheit = state.settings.useFahrenheit, onClick = onOpenMonth)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AlertBanner(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            "$count custom weather alert${if (count > 1) "s" else ""} active",
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TodaySection(current: CurrentWeather, useFahrenheit: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(WeatherFormat.emojiFor(current.conditionGroup), fontSize = 48.sp)
            Text(
                WeatherFormat.temp(current.temp, useFahrenheit),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                current.description ?: "",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "Feels like ${WeatherFormat.temp(current.feelsLike, useFahrenheit)} \u00B7 H:${WeatherFormat.tempNoUnit(current.tempMax, useFahrenheit)} L:${WeatherFormat.tempNoUnit(current.tempMin, useFahrenheit)}",
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("Humidity", current.humidity?.let { "$it%" } ?: "--")
                StatColumn("Wind", current.windSpeedKmh?.let { "${it.toInt()} km/h" } ?: "--")
                StatColumn("Pressure", current.pressure?.let { "$it hPa" } ?: "--")
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatColumn("Sunrise", current.sunrise ?: "--")
                StatColumn("Sunset", current.sunset ?: "--")
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (current.sourcesTotal > 0)
                    "Live \u00B7 ${current.sourcesAgreeing}/${current.sourcesTotal} sources agree (${current.sourcesUsed.joinToString(", ")})"
                else "Live",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
        Text(label, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
private fun ThreeDayStrip(days: List<DayForecast>, useFahrenheit: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            days.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(day.dayLabel, fontWeight = FontWeight.SemiBold)
                    Text(day.dateLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(WeatherFormat.emojiFor(day.conditionGroup), fontSize = 26.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("${WeatherFormat.tempNoUnit(day.maxTemp, useFahrenheit)} / ${WeatherFormat.tempNoUnit(day.minTemp, useFahrenheit)}")
                    day.rainChancePercent?.let {
                        Text("$it% rain", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Text(
            "Tap to see the next month \u2192",
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
