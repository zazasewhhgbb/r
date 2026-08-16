package com.weatherfocus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.installSplashScreen
import com.weatherfocus.app.ui.WeatherViewModel
import com.weatherfocus.app.ui.nav.WeatherNavGraph
import com.weatherfocus.app.ui.theme.WeatherOnlyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            WeatherOnlyTheme {
                WeatherNavGraph(viewModel = viewModel)
            }
        }
    }
}
