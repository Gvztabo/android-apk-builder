package com.cuentasclaras.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cuentasclaras.app.ui.navigation.CuentasClarasNavGraph
import com.cuentasclaras.app.ui.theme.CuentasClarasTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as CuentasClarasApp).repository

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    if (repository.settings.value.autoRefreshBcv) {
                        repository.refreshBcvRateIfStaleForToday()
                    }
                    delay(30 * 60 * 1000L)
                }
            }
        }

        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle()
            CuentasClarasTheme(
                darkTheme = settings.isDarkTheme,
                largeText = settings.largeText
            ) {
                CuentasClarasNavGraph(repository = repository)
            }
        }
    }
}
