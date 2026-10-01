package com.browsesth.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.chromium.android_webview.AwContents

/**
 * Browsesth home = Aloha browser (Slice M/B: homepage is browser). No system WebView.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var urlBar: EditText
    private lateinit var awHost: AwContentsHostView
    private lateinit var client: ProbeAwContentsClient
    private lateinit var store: BrowseStore
    private var awContents: AwContents? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var attachError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = BrowseStore(this)
        status = findViewById(R.id.status)
        urlBar = findViewById(R.id.urlBar)
        awHost = findViewById(R.id.awHost)

        urlBar.setText(DEFAULT_URL)
        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                goToBarUrl(); true
            } else false
        }
        findViewById<Button>(R.id.btnGo).setOnClickListener { goToBarUrl() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { awHost.goBack(); refreshStatus() }
        findViewById<Button>(R.id.btnForward).setOnClickListener { awHost.goForward(); refreshStatus() }
        findViewById<Button>(R.id.btnReload).setOnClickListener { awHost.reload() }
        findViewById<Button>(R.id.btnBookmark).setOnClickListener { onBookmarkClick() }
        findViewById<Button>(R.id.btnHistory).setOnClickListener { showEntryList(history = true) }

        client = ProbeAwContentsClient(
            onStatus = { mainHandler.post { refreshStatus(it) } },
            onNavigationOverride = { target ->
                mainHandler.post {
                    awHost.loadUrl(target)
                    refreshStatus("已跳过中页")
                }
            },
            onPageFinishedExtra = { url, title ->
                store.addHistory(url, title)
                mainHandler.post { refreshStatus() }
            },
        )

        if (!BrowsesthApp.initOk) {
            refreshStatus("Aloha 初始化失败")
            return
        }
        awHost.post {
            try {
                awContents = awHost.attach(client)
                refreshStatus("就绪")
                awHost.loadUrl(DEFAULT_URL)
            } catch (t: Throwable) {
                attachError = "${t.javaClass.simpleName}: ${t.message}"
                Log.e(TAG, "AwContents attach failed", t)
                refreshStatus("AwContents 创建失败")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        awHost.onHostResume()
    }

    override fun onPause() {
        awHost.onHostPause()
        super.onPause()
    }

    override fun onDestroy() {
        awHost.destroyContents()
        awContents = null
        super.onDestroy()
    }

    private fun goToBarUrl() {
        val raw = urlBar.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) return
        val url = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        urlBar.setText(url)
        awHost.loadUrl(url)
    }

    private fun onBookmarkClick() {
        val url = client.lastUrl.ifBlank { urlBar.text?.toString().orEmpty() }
        if (url.isBlank()) return
        store.addBookmark(url, client.lastTitle)
        refreshStatus("已加书签")
        AlertDialog.Builder(this)
            .setTitle("书签")
            .setMessage("已保存：\n$url\n\n查看全部书签？")
            .setPositiveButton("查看") { _, _ -> showEntryList(history = false) }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showEntryList(history: Boolean) {
        val items = if (history) store.history() else store.bookmarks()
        val title = if (history) "历史" else "书签"
        if (items.isEmpty()) {
            AlertDialog.Builder(this).setTitle(title).setMessage("暂无记录").setPositiveButton("好", null).show()
            return
        }
        val labels = items.map { e ->
            val t = e.title.ifBlank { e.url }
            if (t.length > 48) t.take(45) + "…" else t
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(labels) { _, which ->
                val e = items[which]
                urlBar.setText(e.url)
                awHost.loadUrl(e.url)
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun refreshStatus(extra: String? = null) {
        status.text = buildString {
            append("Browsesth 浏览器")
            if (BrowsesthApp.initOk) append(" · Aloha OK") else append(" · Aloha 失败")
            append(" · ")
            append(client.lastTitle.ifBlank { client.lastUrl.ifBlank { "—" } })
            if (attachError != null) append(" · 错误:$attachError")
            if (extra != null) append(" · $extra")
            append(" · 史${store.history().size}/签${store.bookmarks().size}")
        }
        val u = client.lastUrl
        if (u.isNotBlank() && !u.startsWith("data:")) {
            urlBar.setText(u)
        }
    }

    companion object {
        private const val TAG = "BrowsesthMain"
        private const val DEFAULT_URL = "https://example.com/"
    }
}
