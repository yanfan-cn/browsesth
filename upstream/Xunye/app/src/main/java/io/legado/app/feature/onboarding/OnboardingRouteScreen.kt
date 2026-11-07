package io.legado.app.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.help.config.ThemeConfigStore
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingRouteScreen(
    onFinish: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                OnboardingEffect.NavigateHome -> onNavigateHome()
                OnboardingEffect.Finish -> onFinish()
                OnboardingEffect.ApplyDayNight -> ThemeConfigStore.applyDayNightLive()
                is OnboardingEffect.ShowToast -> context.toastOnUi(effect.resId)
            }
        }
    }

    OnboardingScreen(
        state = state,
        onIntent = viewModel::onIntent,
    )
}
