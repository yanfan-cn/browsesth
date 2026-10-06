package io.legado.app.ui.widget.components.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

interface GlassTopAppBarScrollBehavior {
    val nestedScrollConnection: NestedScrollConnection
    val collapsedFraction: Float
}

@OptIn(ExperimentalMaterial3Api::class)
class M3GlassScrollBehavior(
    val m3Behavior: TopAppBarScrollBehavior
) : GlassTopAppBarScrollBehavior {
    override val nestedScrollConnection: NestedScrollConnection
        get() = m3Behavior.nestedScrollConnection

    override val collapsedFraction: Float
        get() = m3Behavior.state.collapsedFraction
}
