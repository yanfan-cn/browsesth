package io.legado.app.ui.main

import io.legado.app.domain.model.PlaybackCapsuleSource
import io.legado.app.domain.model.PlaybackCapsuleState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPlayerAnchorsTest {
    private val audio = PlaybackCapsuleState(
        source = PlaybackCapsuleSource.AudioBook,
        bookUrl = "audio-a",
    )

    @Test
    fun currentAudiobookUsesItsCapsule() {
        assertTrue(capsuleMatchesPlayer(audio, PlaybackCapsuleSource.AudioBook, "audio-a"))
    }

    @Test
    fun differentAudiobookDoesNotFlyThePreviousCover() {
        assertFalse(capsuleMatchesPlayer(audio, PlaybackCapsuleSource.AudioBook, "audio-b"))
    }

    @Test
    fun notificationWithoutBookUrlUsesCurrentAudioSession() {
        assertTrue(capsuleMatchesPlayer(audio, PlaybackCapsuleSource.AudioBook, ""))
    }

    @Test
    fun readAloudCapsuleCannotAnimateIntoAudioPlayer() {
        val readAloud = audio.copy(source = PlaybackCapsuleSource.ReadAloud)
        assertFalse(capsuleMatchesPlayer(readAloud, PlaybackCapsuleSource.AudioBook, "audio-a"))
        assertFalse(capsuleMatchesPlayer(audio, PlaybackCapsuleSource.ReadAloud, ""))
    }

    @Test
    fun stoppedSessionUsesFallbackRatherThanStaleAnchors() {
        assertFalse(
            capsuleMatchesPlayer(
                audio.copy(source = null),
                PlaybackCapsuleSource.AudioBook,
                "audio-a"
            )
        )
    }

    @Test
    fun hiddenBottomBarAlwaysUsesGlobalCapsuleEvenIfFloatingBarIsEnabled() {
        assertFalse(shouldUseHomePlaybackCapsule(true, false, true, false))
        assertTrue(shouldUseHomePlaybackCapsule(true, true, true, false))
    }

    @Test
    fun otherRoutesRailAndStandardBottomBarUseGlobalCapsule() {
        assertFalse(shouldUseHomePlaybackCapsule(false, true, true, false))
        assertFalse(shouldUseHomePlaybackCapsule(true, true, true, true))
        assertFalse(shouldUseHomePlaybackCapsule(true, true, false, false))
    }

    /** SPEC-235: 平板不渲染悬浮底栏，home 胶囊不可用，必须回落全局胶囊。 */
    @Test
    fun missingHomeBarAlwaysUsesGlobalCapsule() {
        assertFalse(shouldUseHomePlaybackCapsule(true, true, true, false, homeBarPresent = false))
        assertFalse(shouldUseHomePlaybackCapsule(true, true, true, true, homeBarPresent = false))
        assertFalse(shouldUseHomePlaybackCapsule(false, false, false, false, homeBarPresent = false))
        assertTrue(shouldUseHomePlaybackCapsule(true, true, true, false, homeBarPresent = true))
    }

    @Test
    fun backNeverPopsTheCoveredRouteDuringPlayerOpeningOrClosing() {
        assertFalse(shouldHandleActivityBack(false, playerPresent = true))
        assertFalse(shouldHandleActivityBack(true, playerPresent = true))
        assertTrue(shouldHandleActivityBack(false, playerPresent = false))
        assertFalse(shouldHandleActivityBack(true, playerPresent = false))
    }
}
