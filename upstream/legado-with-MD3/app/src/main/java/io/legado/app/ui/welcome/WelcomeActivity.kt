package io.legado.app.ui.welcome

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import io.legado.app.base.BaseComposeActivity
import io.legado.app.feature.onboarding.OnboardingRouteScreen
import io.legado.app.ui.main.MainActivity

/**
 * 欢迎引导兼容宿主。Manifest 入口与 MainActivity 首启 Intent 依赖本类名，
 * 故保留 Activity；实际状态与行为在 feature/onboarding。
 */
class WelcomeActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // SPEC-165: this screen is light-only.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun Content() {
        OnboardingRouteScreen(
            onFinish = { finish() },
            onNavigateHome = {
                startActivity(MainActivity.createHomeIntent(this@WelcomeActivity))
                finish()
            }
        )
    }
}
