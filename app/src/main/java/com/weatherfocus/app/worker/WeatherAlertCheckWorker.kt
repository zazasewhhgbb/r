package com.weatherfocus.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.weatherfocus.app.data.alert.CustomWeatherAlertEngine
import com.weatherfocus.app.data.prefs.SettingsRepository
import com.weatherfocus.app.data.repository.WeatherRepository
import com.weatherfocus.app.notification.NotificationHelper
import kotlinx.coroutines.flow.first

/** Periodically re-checks the user's custom weather alert rules and fires a notification for anything inside the lead-time window. */
class WeatherAlertCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val settingsRepo = SettingsRepository(applicationContext)
            val settings = settingsRepo.settingsFlow.first()
            if (!settings.customAlertRules.enabled) return Result.success()

            val bundle = WeatherRepository().loadWeather(settings)
            val matches = CustomWeatherAlertEngine.evaluate(bundle.hourlyForAlerts, settings.customAlertRules)
            matches.filter { it.leadWarning }.forEach { NotificationHelper.showAlert(applicationContext, it) }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
