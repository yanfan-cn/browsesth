package io.legado.app.ui.browser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * SPEC-189 R3: the reading-list row keeps horizontal leftovers and flings.
 *
 * Post-scroll and post-fling run from the inner scroller outward, after [LazyRow]
 * has already taken what it can. Eating the horizontal remainder here means the
 * outer main pager never receives it. Vertical remainder is left untouched so the
 * NTP page still scrolls.
 */
internal object NtpShelfScroll : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = Offset(available.x, 0f)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        Velocity(available.x, 0f)
}
