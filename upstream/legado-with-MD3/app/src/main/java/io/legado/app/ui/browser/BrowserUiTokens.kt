package io.legado.app.ui.browser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared browser chrome tokens from docs/UI设计文档.md §1.3.
 * Keep visuals here so NTP / session / lists stay consistent.
 */
object BrowserUiTokens {
    val Primary = Color(0xFF243B5A)
    val PrimaryOnDark = Color(0xFF8FA8C8)

    val PageBgLight = Color(0xFFFCFBF8)
    val PageBgDark = Color(0xFF121212)
    val FieldBgLight = Color(0xFFEFEDE8)
    val FieldBgDark = Color(0xFF1E1E1E)
    val CardBgLight = Color(0xFFFFFFFF)
    val CardBgDark = Color(0xFF1A1A1A)

    val TextPrimaryLight = Color(0xFF1C1B19)
    val TextPrimaryDark = Color(0xFFE8E6E1)
    val TextSecondaryLight = Color(0xFF6B6560)
    val TextSecondaryDark = Color(0xFFA8A29A)

    val IncognitoPageBg = Color(0xFF1A1A1A)
    val IncognitoFieldBg = Color(0xFF2A2A2A)

    val PagePaddingH = 16.dp
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 24.dp

    /** SPEC-088/127/140-R5: NTP content downshift — spacing between the chrome row
     *  bottom (status 32 + bar 54 = 86dp) and the search capsule top: 32 + 187 + 2
     *  card inset = 221dp = 86 + 135dp gap (operator directive). Applied INSIDE the
     *  scroll (scrolls away with content — no fixed blank band). Independent of
     *  [NtpTopBarHeight] — no coupling. */
    val NtpContentShiftDown = 187.dp
    /** SPEC-129: phone NTP four-icon floating row only. FIXED at 54dp by operator
     *  directive (SPEC-140-R2); must never be coupled to [NtpContentShiftDown]. */
    val NtpTopBarHeight = 54.dp
    val NtpOverlayBlur = 28.dp
    /** SPEC-088: higher opacity for customize / tab menus (more readable). */
    val NtpOverlaySurfaceLight = 0.96f
    val NtpOverlaySurfaceDark = 0.97f
    /** SPEC-072/088: liquid-glass NTP tint — light 0.4 / dark 0.35. */
    val NtpGlassOverlayLight = 0.4f
    val NtpGlassOverlayDark = 0.35f
    val NtpEngineMenuWidth = 140.dp
    val NtpEngineIconSize = 32.dp
    val NtpShortcutIconSize = 52.dp
    val NtpShortcutScreenInset = 20.dp
    val NtpCustomizeSwitchScale = 0.78f

    val CardCorner = 24.dp
    val ButtonCorner = 12.dp
    val TopBarHeight = 54.dp
    val InputHeightLarge = 44.dp
    val InputHeightSmall = 32.dp
    val ControlLarge = 44.dp
    val ControlMedium = 36.dp
    val ControlSmall = 28.dp
    val ReaderFabSize = 56.dp
    val BottomBarHeight = 56.dp

    /**
     * SPEC-024: gap above floating function bar + reader FAB.
     */
    val FloatingChromeTopGap = 8.dp

    /**
     * SPEC-025: phone floating chrome gap above system gesture / home inset.
     */
    val FloatingChromeBottomGap = 8.dp
}
