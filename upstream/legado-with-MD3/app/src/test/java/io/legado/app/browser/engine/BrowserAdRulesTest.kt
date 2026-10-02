package io.legado.app.browser.engine

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BrowserAdRulesTest {

    @Before
    fun setUp() {
        BrowserAdRules.replaceRulesForTest(
            listOf(
                "doubleclick.net",
                "googlesyndication.com",
                "amazon-adsystem.com",
                "/ad.js",
                "browsesth-ad-probe",
            ),
        )
    }

    @After
    fun tearDown() {
        BrowserAdRules.clearLoadedForTest()
    }

    @Test
    fun blocksKnownAdNetworks() {
        assertTrue(
            BrowserAdRules.shouldBlock(
                "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
                enabled = true,
            ),
        )
        assertTrue(
            BrowserAdRules.shouldBlock(
                "https://ad.doubleclick.net/ddm/track",
                enabled = true,
            ),
        )
    }

    @Test
    fun allowsBaiduFirstPartyEvenIfPathLooksLikeAds() {
        assertFalse(
            BrowserAdRules.shouldBlock(
                "https://www.baidu.com/s?wd=%E8%8B%B9%E6%9E%9C&ads=1",
                enabled = true,
            ),
        )
        assertFalse(
            BrowserAdRules.shouldBlock(
                "https://t7.baidu.com/it/u=123&fm=ads",
                enabled = true,
            ),
        )
        assertFalse(
            BrowserAdRules.shouldBlock(
                "https://dss0.bdstatic.com/5aV1bjqh_Q23odCf/static/superman/img/logo.png",
                enabled = true,
            ),
        )
    }

    @Test
    fun stillBlocksProbeOnAnyHost() {
        assertTrue(
            BrowserAdRules.shouldBlock(
                "https://www.baidu.com/path?browsesth-ad-probe=1",
                enabled = true,
            ),
        )
    }

    @Test
    fun disabledMeansNoBlock() {
        assertFalse(
            BrowserAdRules.shouldBlock(
                "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
                enabled = false,
            ),
        )
    }
}
