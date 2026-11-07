package io.legado.app.feature.onboarding

import androidx.compose.runtime.Stable
import io.legado.app.domain.model.settings.ThemeSettings

/**
 * SPEC-223: two-page onboarding — privacy + features, then personalize.
 */
@Stable
data class OnboardingUiState(
    val page: Int = 0,
    val pageCount: Int = 2,
    val privacyPolicy: String = "",
    val featuresIntro: String = "",
    val theme: ThemeSettings = ThemeSettings(),
    val themeMode: String = "0",
)

sealed interface OnboardingIntent {
    data object Next : OnboardingIntent
    data object Prev : OnboardingIntent
    data class SelectTheme(val value: String) : OnboardingIntent
    data class SetThemeMode(val value: String) : OnboardingIntent
    data class SetPureBlack(val enabled: Boolean) : OnboardingIntent
    data class SetLiquidGlass(val enabled: Boolean) : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object NavigateHome : OnboardingEffect
    data object Finish : OnboardingEffect
    data object ApplyDayNight : OnboardingEffect
    data class ShowToast(val resId: Int) : OnboardingEffect
}
