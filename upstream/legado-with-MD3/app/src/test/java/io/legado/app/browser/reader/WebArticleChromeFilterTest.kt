package io.legado.app.browser.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebArticleChromeFilterTest {
    @Test
    fun filtersNavChromeLines() {
        val raw = """
            首页 | 目录 | 书架
            这是正文第一段，内容足够长以便保留。
            上一章
            下一章
            第二段正文继续讲述故事。
            加入书架
        """.trimIndent()
        val out = WebArticleChromeFilter.filter(raw)
        assertFalse(out.contains("上一章"))
        assertFalse(out.contains("下一章"))
        assertFalse(out.contains("加入书架"))
        assertFalse(out.lines().any { it.trim() == "首页 | 目录 | 书架" })
        assertTrue(out.contains("正文第一段"))
        assertTrue(out.contains("第二段正文"))
    }

    @Test
    fun keepsNormalProse() {
        val line = "他回到了阔别已久的家乡，心中感慨万千。"
        assertFalse(WebArticleChromeFilter.isChromeLine(line))
    }

    @Test
    fun filterHtmlKeepsImagesAndDropsChromeParagraphs() {
        val html = """
            <p>首页 | 目录 | 书架</p>
            <p>正文段落在这里，足够长应该保留。</p>
            <img src="https://example.com/a.jpg">
            <p>上一章</p>
        """.trimIndent()
        val out = WebArticleChromeFilter.filterHtml(html)
        assertTrue(out.contains("https://example.com/a.jpg"))
        assertTrue(out.contains("正文段落"))
        assertFalse(out.contains("上一章"))
        assertFalse(out.contains("首页 | 目录"))
    }

    @Test
    fun filtersDownloadAppChrome() {
        assertTrue(WebArticleChromeFilter.isChromeLine("请下载客户端阅读器"))
    }

    @Test
    fun filtersBaiduMobileJunkLines() {
        // SPEC-202: 净读实测百度移动页开头三条杂质。
        assertTrue(WebArticleChromeFilter.isChromeLine("百度一下，你就知道"))
        assertTrue(WebArticleChromeFilter.isChromeLine("APP内打开"))
        assertTrue(WebArticleChromeFilter.isChromeLine("app内查看"))
        assertTrue(WebArticleChromeFilter.isChromeLine("应用内阅读"))
        assertTrue(WebArticleChromeFilter.isChromeLine("打开APP"))
        assertTrue(WebArticleChromeFilter.isChromeLine("听全文约2分钟"))
        assertTrue(WebArticleChromeFilter.isChromeLine("全文约30秒"))
        // 防误杀边界：多余前缀/正文叙述不得命中。
        assertFalse(WebArticleChromeFilter.isChromeLine("听了全文约2分钟，他很感动。"))
        assertFalse(WebArticleChromeFilter.isChromeLine("他打开APP查看最新消息。"))
        assertFalse(WebArticleChromeFilter.isChromeLine("新华社北京10月28日电（记者王立彬）今年以来，我国海洋经济实现平稳增长。"))
        assertFalse(WebArticleChromeFilter.isChromeLine("自然资源部10月28日发布数据显示，前三季度全国海洋资源配置扎实推进。"))
    }

    @Test
    fun filtersControlNumbersButKeepsProse() {
        assertTrue(WebArticleChromeFilter.isChromeLine("36"))
        assertTrue(WebArticleChromeFilter.isChromeLine("1 2"))
        assertTrue(WebArticleChromeFilter.isChromeLine("点赞 12"))
        assertTrue(WebArticleChromeFilter.isChromeLine("阅读 1.2万"))
        assertTrue(WebArticleChromeFilter.isChromeLine("展开"))
        assertFalse(WebArticleChromeFilter.isChromeLine("他回到了阔别已久的家乡，心中感慨万千。"))
        assertFalse(WebArticleChromeFilter.isChromeLine("第12章 他终于离开了小镇，走向未知的远方。"))
    }

    @Test
    fun dropsSmallImagesAndKeepsArticleImages() {
        val html = """
            <p>正文段落在这里，足够长应该保留。</p>
            <img src="https://example.com/a.jpg" width="800" height="600">
            <img src="https://example.com/thumb.jpg" width="40" height="40">
            <img class="thumbnail" src="https://example.com/small.jpg">
            <img data-src="https://example.com/real.jpg" width="1" height="1">
            <p>36</p>
        """.trimIndent()
        val out = WebArticleChromeFilter.filterHtml(html)
        assertTrue(out.contains("https://example.com/a.jpg"))
        assertTrue(out.contains("https://example.com/real.jpg"))
        assertTrue(out.contains("正文段落"))
        assertFalse(out.contains("thumb.jpg"))
        assertFalse(out.contains("small.jpg"))
        assertFalse(out.contains("36"))
    }

    @Test
    fun filtersPortalShareAndScanChromeLines() {
        // SPEC-211: 腾讯新闻桌面 UA 净读实测杂质。
        assertTrue(WebArticleChromeFilter.isChromeLine("复制链接"))
        assertTrue(WebArticleChromeFilter.isChromeLine("微信好友"))
        assertTrue(WebArticleChromeFilter.isChromeLine("QQ好友"))
        assertTrue(WebArticleChromeFilter.isChromeLine("手机看"))
        assertTrue(WebArticleChromeFilter.isChromeLine("手机看新闻"))
        assertTrue(WebArticleChromeFilter.isChromeLine("微信高于4.0.2版本可点击分享"))
        assertTrue(WebArticleChromeFilter.isChromeLine("QQ高于5.1版本可点击分享"))
        assertTrue(WebArticleChromeFilter.isChromeLine("微信扫一扫，随时随地看"))
        assertTrue(WebArticleChromeFilter.isChromeLine("元宝 · 新闻妹"))
        assertTrue(WebArticleChromeFilter.isChromeLine("问AI · 俄方系统性引入外国文学背后有何文化考量？"))
        assertTrue(WebArticleChromeFilter.isChromeLine("2026-10-23 19:23发布于北京 参考消息官方账号"))
        assertFalse(WebArticleChromeFilter.isChromeLine("他复制链接后发给了远方的朋友，心里有些忐忑。"))
        assertFalse(WebArticleChromeFilter.isChromeLine("新华社北京10月28日电（记者王立彬）今年以来，我国海洋经济实现平稳增长。"))
    }

    @Test
    fun dropsNearSquareControlAndQrImages() {
        val html = """
            <p>正文段落在这里，足够长应该保留。</p>
            <img src="https://example.com/story.jpg" width="800" height="450">
            <img src="https://example.com/avatar.png" width="240" height="240">
            <img src="https://cdn.example.com/qrcode/promo.png" width="600" height="400">
        """.trimIndent()
        val out = WebArticleChromeFilter.filterHtml(html)
        assertTrue(out.contains("story.jpg"))
        assertTrue(out.contains("正文段落"))
        assertFalse(out.contains("avatar.png"))
        assertFalse(out.contains("qrcode/promo"))
    }

    @Test
    fun filtersVideoPlayerDebugLines() {
        // SPEC-212: 腾讯新闻净读吸入的播放器调试字。
        assertTrue(WebArticleChromeFilter.isChromeLine("视频信息"))
        assertTrue(WebArticleChromeFilter.isChromeLine("播放信息 上传日志"))
        assertTrue(WebArticleChromeFilter.isChromeLine("视频ID"))
        assertTrue(WebArticleChromeFilter.isChromeLine("VID"))
        assertTrue(WebArticleChromeFilter.isChromeLine("Flowid"))
        assertTrue(WebArticleChromeFilter.isChromeLine("播放内核"))
        assertTrue(WebArticleChromeFilter.isChromeLine("缓冲健康度"))
        assertTrue(WebArticleChromeFilter.isChromeLine("mp4/origin (1.33.5)"))
        assertTrue(WebArticleChromeFilter.isChromeLine("0 KB/s"))
        assertTrue(WebArticleChromeFilter.isChromeLine("32ef90afe282fa28b92da5fbb8b34c25"))
        assertTrue(WebArticleChromeFilter.isChromeLine("br:0.000-8.598 t:0.22 pg:1"))
        assertTrue(WebArticleChromeFilter.isChromeLine("https://cdn.example.com/a.mp4"))
        // SPEC-212 补强：播放器控件与 Kernel 行。
        assertTrue(WebArticleChromeFilter.isChromeLine("播放"))
        assertTrue(WebArticleChromeFilter.isChromeLine("下一个"))
        assertTrue(WebArticleChromeFilter.isChromeLine("打开循环播放"))
        assertTrue(WebArticleChromeFilter.isChromeLine("画质"))
        assertTrue(WebArticleChromeFilter.isChromeLine("离线SD时"))
        assertTrue(WebArticleChromeFilter.isChromeLine("原高清"))
        assertTrue(WebArticleChromeFilter.isChromeLine("标清"))
        assertTrue(WebArticleChromeFilter.isChromeLine("语言"))
        assertTrue(WebArticleChromeFilter.isChromeLine("播放速度"))
        assertTrue(WebArticleChromeFilter.isChromeLine("[Basic]"))
        assertTrue(WebArticleChromeFilter.isChromeLine("Kernel m3u8/hls.js (1.39.5-p2p-1.6.9)"))
        assertTrue(WebArticleChromeFilter.isChromeLine("2481.19 KB/s 0.00/0.00/0.00/0/0/0/0/0"))
        assertTrue(WebArticleChromeFilter.isChromeLine("p3371y1cd3a"))
        // SPEC-213
        assertTrue(WebArticleChromeFilter.isChromeLine("听全文"))
        assertTrue(WebArticleChromeFilter.isChromeLine("生成AI播客"))
        assertTrue(WebArticleChromeFilter.isChromeLine("生成 AI 播客"))
        assertTrue(WebArticleChromeFilter.isChromeLine("获取全网一手热点"))
        assertTrue(WebArticleChromeFilter.isChromeLine("腾讯新闻 · 获取全网一手热点"))
        assertTrue(WebArticleChromeFilter.isChromeLine("问AI · 首型舰载隐身无人机上舰意味海军战力有何新跨越？"))
        assertTrue(WebArticleChromeFilter.isChromeLine("高清SDR"))
        assertTrue(WebArticleChromeFilter.isChromeLine("准高清"))
        assertTrue(WebArticleChromeFilter.isChromeLine("你可以 刷新 试试"))
        assertTrue(WebArticleChromeFilter.isChromeLine("你可以播放试试"))
        assertTrue(WebArticleChromeFilter.isChromeLine("Kernel"))
        assertTrue(WebArticleChromeFilter.isChromeLine("(Basic)"))
        // SPEC-215
        assertTrue(WebArticleChromeFilter.isChromeLine("多音轨"))
        assertTrue(WebArticleChromeFilter.isChromeLine("AirPlay"))
        assertTrue(WebArticleChromeFilter.isChromeLine("画中画"))
        assertTrue(WebArticleChromeFilter.isChromeLine("网页全屏"))
        assertTrue(WebArticleChromeFilter.isChromeLine("全屏"))
        assertTrue(WebArticleChromeFilter.isChromeLine("拆条中"))
        assertTrue(WebArticleChromeFilter.isChromeLine("暂停"))
        assertTrue(WebArticleChromeFilter.isChromeLine("静音播放中"))
        assertTrue(WebArticleChromeFilter.isChromeLine("静音播放中，点击 恢复音量"))
        assertTrue(WebArticleChromeFilter.isChromeLine("点击 恢复音量"))
        assertFalse(WebArticleChromeFilter.isChromeLine("参考消息网10月23日报道，据俄新社10月22日报道"))
        assertFalse(WebArticleChromeFilter.isChromeLine("他按下播放键后，整间屋子忽然安静下来。"))
        assertFalse(WebArticleChromeFilter.isChromeLine("本周我国在军事、能源、海洋科考等领域捷报频传。"))
        assertFalse(WebArticleChromeFilter.isChromeLine("试验过程中一度暂停，随后继续航行。"))
        val html = """
            <p>视频信息</p>
            <p>VID</p>
            <p>播放</p>
            <p>画质</p>
            <p>Kernel m3u8/hls.js (1.39.5-p2p-1.6.9)</p>
            <img src="https://cdn.example.com/p.jpg#browsesth-jingshi">
            <p>参考消息网10月23日报道，据俄新社10月22日报道，俄罗斯联邦教育。</p>
        """.trimIndent()
        val out = WebArticleChromeFilter.filterHtml(html)
        assertFalse(out.contains("视频信息"))
        assertFalse(out.contains("VID"))
        assertFalse(out.contains("画质"))
        assertFalse(out.contains("Kernel"))
        assertTrue(out.contains("#browsesth-jingshi"))
        assertTrue(out.contains("参考消息网"))
    }

    @Test
    fun filtersOpenAppAndAskAiChromeLines() {
        // SPEC-217: 正文外诱导 / 控件短行。
        assertTrue(WebArticleChromeFilter.isChromeLine("打开腾讯新闻查看精彩图片"))
        assertTrue(WebArticleChromeFilter.isChromeLine("打开XX查看精彩图片"))
        assertTrue(WebArticleChromeFilter.isChromeLine("打开"))
        assertTrue(WebArticleChromeFilter.isChromeLine("打开应用"))
        assertTrue(WebArticleChromeFilter.isChromeLine("打开客户端"))
        assertTrue(WebArticleChromeFilter.isChromeLine("问AI"))
        assertTrue(WebArticleChromeFilter.isChromeLine("问 AI"))
        assertTrue(WebArticleChromeFilter.isChromeLine("听全文"))
        assertTrue(WebArticleChromeFilter.isChromeLine("去App查看"))
        assertTrue(WebArticleChromeFilter.isChromeLine("请打开APP领取福利"))
        // 防误杀：正文叙述保留。
        assertFalse(WebArticleChromeFilter.isChromeLine("他打开APP查看最新消息。"))
        assertFalse(WebArticleChromeFilter.isChromeLine("记者打开文件后发现了新的线索。"))
        assertFalse(WebArticleChromeFilter.isChromeLine("新华社北京11月2日电（记者王立彬）今年以来，我国海洋经济实现平稳增长。"))
    }
}
