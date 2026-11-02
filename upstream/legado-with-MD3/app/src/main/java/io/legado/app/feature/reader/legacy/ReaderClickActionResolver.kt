package io.legado.app.feature.reader.legacy

import io.legado.app.constant.PageAnim
import io.legado.app.data.repository.ReadPreferences

/**
 * SPEC-216: 滚动动画用独立点击区；其它翻页动画用原 clickAction*。
 */
object ReaderClickActionResolver {

    data class Grid(
        val tl: Int,
        val tc: Int,
        val tr: Int,
        val ml: Int,
        val mc: Int,
        val mr: Int,
        val bl: Int,
        val bc: Int,
        val br: Int,
    )

    /** 上无操作 / 中菜单 / 下无操作 */
    val SCROLL_DEFAULTS = Grid(
        tl = -1, tc = -1, tr = -1,
        ml = 0, mc = 0, mr = 0,
        bl = -1, bc = -1, br = -1,
    )

    fun resolve(pageAnim: Int, preferences: ReadPreferences): Grid =
        if (pageAnim == PageAnim.scrollPageAnim) {
            Grid(
                tl = preferences.clickActionScrollTL,
                tc = preferences.clickActionScrollTC,
                tr = preferences.clickActionScrollTR,
                ml = preferences.clickActionScrollML,
                mc = preferences.clickActionScrollMC,
                mr = preferences.clickActionScrollMR,
                bl = preferences.clickActionScrollBL,
                bc = preferences.clickActionScrollBC,
                br = preferences.clickActionScrollBR,
            )
        } else {
            Grid(
                tl = preferences.clickActionTL,
                tc = preferences.clickActionTC,
                tr = preferences.clickActionTR,
                ml = preferences.clickActionML,
                mc = preferences.clickActionMC,
                mr = preferences.clickActionMR,
                bl = preferences.clickActionBL,
                bc = preferences.clickActionBC,
                br = preferences.clickActionBR,
            )
        }

    fun isScrollProfile(pageAnim: Int): Boolean =
        pageAnim == PageAnim.scrollPageAnim
}
