package io.legado.app.ui.browser

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.legado.app.help.source.SourceVerificationHelp

/**
 * Route wrapper for deep-link / verification browser sessions (Aloha only).
 */
@Composable
fun WebViewRouteScreen(
    intent: Intent,
    viewModel: WebViewModel,
    onFinish: () -> Unit,
    onImportBookSource: (String) -> Unit,
) {
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(intent) {
        viewModel.initData(intent) { ready = true }
    }

    if (!ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载中…")
        }
        return
    }

    BrowserSessionScreen(
        initialUrl = viewModel.baseUrl.ifBlank { "https://example.com/" },
        showNavigationBack = true,
        onNavigateBack = {
            if (viewModel.sourceVerificationEnable) {
                SourceVerificationHelp.checkResult(viewModel.sourceOrigin)
            }
            onFinish()
        },
        onImportBookSource = onImportBookSource,
        initialHtml = viewModel.html,
    )
}
