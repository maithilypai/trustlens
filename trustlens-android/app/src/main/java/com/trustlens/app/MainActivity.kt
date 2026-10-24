package com.trustlens.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.trustlens.app.data.model.ScreenDestination
import com.trustlens.app.ui.screens.DemoScenariosScreen
import com.trustlens.app.ui.screens.HomeScreen
import com.trustlens.app.ui.screens.MessageAnalysisScreen
import com.trustlens.app.ui.screens.PrivacyEducationScreen
import com.trustlens.app.ui.screens.QrAnalysisScreen
import com.trustlens.app.ui.screens.ResultScreen
import com.trustlens.app.ui.screens.SafeBrowserScreen
import com.trustlens.app.ui.screens.WarningDialog
import com.trustlens.app.ui.theme.NavyBackground
import com.trustlens.app.ui.theme.TRUSTLENSTheme
import com.trustlens.app.ui.viewmodel.TrustlensViewModel
import com.trustlens.app.ui.viewmodel.TrustlensViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: TrustlensViewModel by viewModels {
        TrustlensViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming URL or message text via Android Share Sheet (ACTION_SEND) or browser link (ACTION_VIEW)
        handleIncomingIntent(intent)

        setContent {
            LaunchedEffect(Unit) {
                try {
                    reportFullyDrawn()
                } catch (_: Exception) {
                }
            }

            TRUSTLENSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NavyBackground
                ) {
                    TrustlensApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.handleSharedIntentUrl(sharedText)
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val dataUri = intent.dataString
                if (!dataUri.isNullOrBlank()) {
                    viewModel.handleSharedIntentUrl(dataUri)
                }
            }
        }
    }
}

@Composable
fun TrustlensApp(viewModel: TrustlensViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Android hardware / gesture Back button interceptor
    BackHandler(enabled = uiState.currentScreen != ScreenDestination.HOME) {
        if (uiState.currentScreen == ScreenDestination.RESULT) {
            viewModel.clearResult()
        } else {
            viewModel.navigateTo(ScreenDestination.HOME)
        }
    }

    // Dynamic screen switching based on active navigation state
    when (uiState.currentScreen) {
        ScreenDestination.HOME -> {
            HomeScreen(
                viewModel = viewModel,
                uiState = uiState
            )
        }

        ScreenDestination.SAFE_BROWSER -> {
            SafeBrowserScreen(
                viewModel = viewModel,
                uiState = uiState
            )
        }

        ScreenDestination.MESSAGE_ANALYZER -> {
            MessageAnalysisScreen(
                viewModel = viewModel,
                uiState = uiState
            )
        }

        ScreenDestination.QR_INSPECTOR -> {
            QrAnalysisScreen(
                viewModel = viewModel,
                uiState = uiState
            )
        }

        ScreenDestination.DEMOS -> {
            DemoScenariosScreen(
                viewModel = viewModel,
                uiState = uiState
            )
        }

        ScreenDestination.PRIVACY_INFO -> {
            PrivacyEducationScreen(
                viewModel = viewModel
            )
        }

        ScreenDestination.RESULT -> {
            if (uiState.result != null) {
                ResultScreen(
                    response = uiState.result!!,
                    onBackToHome = { viewModel.clearResult() }
                )
            } else {
                HomeScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }

    // High-impact modal warning when a threat is confirmed on quick scan
    if (uiState.isWarningDialogVisible && uiState.result != null) {
        WarningDialog(
            hostname = uiState.result!!.urlValidation.hostname,
            onGoBackToSafety = { viewModel.clearResult() },
            onViewDetails = { viewModel.dismissWarningDialog() }
        )
    }
}
