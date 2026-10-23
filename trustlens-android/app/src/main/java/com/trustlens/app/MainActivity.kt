package com.trustlens.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.trustlens.app.ui.screens.HomeScreen
import com.trustlens.app.ui.screens.ResultScreen
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

        // Handle incoming URL via Android Share Sheet (ACTION_SEND)
        handleIncomingIntent(intent)

        setContent {
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
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.handleSharedIntentUrl(sharedText)
            }
        }
    }
}

@Composable
fun TrustlensApp(viewModel: TrustlensViewModel) {
    val uiState by viewModel.uiState.collectAsState()

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

    // High-impact modal warning when a threat is confirmed
    if (uiState.isWarningDialogVisible && uiState.result != null) {
        WarningDialog(
            hostname = uiState.result!!.urlValidation.hostname,
            onGoBackToSafety = { viewModel.clearResult() },
            onViewDetails = { viewModel.dismissWarningDialog() }
        )
    }
}
