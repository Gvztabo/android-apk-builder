package com.cuentasclaras.app

import android.app.Application
import com.cuentasclaras.app.data.local.AppDatabase
import com.cuentasclaras.app.data.local.AppSettingsService
import com.cuentasclaras.app.data.remote.BcvRateService
import com.cuentasclaras.app.data.repository.FinanceRepository

class CuentasClarasApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val bcvRateService: BcvRateService by lazy { BcvRateService(this) }
    val settingsService: AppSettingsService by lazy { AppSettingsService(this) }
    val repository: FinanceRepository by lazy {
        FinanceRepository(
            database = database,
            bcvRateService = bcvRateService,
            settingsService = settingsService
        )
    }
}
