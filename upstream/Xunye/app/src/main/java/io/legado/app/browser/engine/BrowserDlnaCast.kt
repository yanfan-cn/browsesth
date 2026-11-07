package io.legado.app.browser.engine

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * SPEC-136: lightweight DLNA MediaRenderer discovery + AVTransport SetAVTransportURI/Play.
 * Best-effort MVP — encrypted/auth streams may fail on the TV.
 */
object BrowserDlnaCast {

    data class Device(
        val usn: String,
        val friendlyName: String,
        val controlUrl: String,
        val baseUrl: String,
    )

    suspend fun discover(context: Context, timeoutMs: Long = 3500L): List<Device> =
        withContext(Dispatchers.IO) {
        val found = ConcurrentHashMap<String, Device>()
        val multicast = acquireMulticastLock(context)
        try {
            val socket = DatagramSocket().apply {
                soTimeout = 800
                broadcast = true
            }
            val msg = buildString {
                append("M-SEARCH * HTTP/1.1\r\n")
                append("HOST: 239.255.255.250:1900\r\n")
                append("MAN: \"ssdp:discover\"\r\n")
                append("MX: 2\r\n")
                append("ST: urn:schemas-upnp-org:service:AVTransport:1\r\n")
                append("\r\n")
            }.toByteArray(StandardCharsets.UTF_8)
            val group = InetAddress.getByName("239.255.255.250")
            socket.send(DatagramPacket(msg, msg.size, group, 1900))
            val deadline = System.currentTimeMillis() + timeoutMs
            val buf = ByteArray(4096)
            while (System.currentTimeMillis() < deadline) {
                try {
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val text = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                    val location = headerValue(text, "LOCATION") ?: continue
                    val usn = headerValue(text, "USN") ?: location
                    if (found.containsKey(usn)) continue
                    parseDevice(location)?.let { found[usn] = it.copy(usn = usn) }
                } catch (_: Throwable) {
                    // timeout / parse — keep scanning until deadline
                }
            }
            socket.close()
        } catch (_: Throwable) {
            // no wifi / blocked
        } finally {
            runCatching { multicast?.release() }
        }
        found.values.sortedBy { it.friendlyName.lowercase() }
    }

    /** Acquire multicast lock when Context is available (call from UI). */
    fun acquireMulticastLock(context: Context): WifiManager.MulticastLock? {
        return runCatching {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifi.createMulticastLock("browsesth-dlna").also {
                it.setReferenceCounted(true)
                it.acquire()
            }
        }.getOrNull()
    }

    suspend fun castUrl(device: Device, mediaUrl: String, title: String = ""): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val meta = didlLite(mediaUrl, title.ifBlank { "Video" })
                soap(
                    device.controlUrl,
                    "SetAVTransportURI",
                    """
                    <InstanceID>0</InstanceID>
                    <CurrentURI>${xmlEscape(mediaUrl)}</CurrentURI>
                    <CurrentURIMetaData>${xmlEscape(meta)}</CurrentURIMetaData>
                    """.trimIndent(),
                )
                soap(
                    device.controlUrl,
                    "Play",
                    """
                    <InstanceID>0</InstanceID>
                    <Speed>1</Speed>
                    """.trimIndent(),
                )
            }
        }

    private fun headerValue(response: String, name: String): String? {
        val re = Regex("""(?im)^$name:\s*(.+)$""")
        return re.find(response)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun parseDevice(location: String): Device? {
        val conn = (URL(location).openConnection() as HttpURLConnection).apply {
            connectTimeout = 2500
            readTimeout = 2500
            requestMethod = "GET"
        }
        val xml = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val base = location.substringBeforeLast('/', missingDelimiterValue = location)
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())
        var friendly = "DLNA 设备"
        var controlUrl: String? = null
        var inAvTransport = false
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "friendlyName" -> friendly = parser.nextText().trim().ifBlank { friendly }
                    "serviceType" -> {
                        val t = parser.nextText()
                        inAvTransport = t.contains("AVTransport", ignoreCase = true)
                    }
                    "controlURL" -> if (inAvTransport) {
                        val raw = parser.nextText().trim()
                        controlUrl = resolveUrl(base, location, raw)
                    }
                }
            }
            eventType = parser.next()
        }
        val ctrl = controlUrl ?: return null
        return Device(
            usn = UUID.nameUUIDFromBytes(ctrl.toByteArray()).toString(),
            friendlyName = friendly,
            controlUrl = ctrl,
            baseUrl = base,
        )
    }

    private fun resolveUrl(base: String, location: String, raw: String): String {
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        val root = try {
            val u = URL(location)
            "${u.protocol}://${u.host}${if (u.port > 0) ":${u.port}" else ""}"
        } catch (_: Throwable) {
            base
        }
        return if (raw.startsWith("/")) root + raw else "$base/$raw"
    }

    private fun soap(controlUrl: String, action: String, bodyInner: String) {
        val envelope = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
             s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:$action xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  $bodyInner
                </u:$action>
              </s:Body>
            </s:Envelope>
        """.trimIndent()
        val conn = (URL(controlUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            setRequestProperty(
                "SOAPACTION",
                "\"urn:schemas-upnp-org:service:AVTransport:1#$action\"",
            )
        }
        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(envelope) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        stream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } }
        conn.disconnect()
        if (code !in 200..299) {
            error("DLNA $action 失败 ($code)")
        }
    }

    private fun didlLite(url: String, title: String): String {
        return """
            <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"
             xmlns:dc="http://purl.org/dc/elements/1.1/"
             xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
              <item id="0" parentID="-1" restricted="1">
                <dc:title>${xmlEscape(title)}</dc:title>
                <upnp:class>object.item.videoItem</upnp:class>
                <res protocolInfo="http-get:*:video/mp4:*">${xmlEscape(url)}</res>
              </item>
            </DIDL-Lite>
        """.trimIndent()
    }

    private fun xmlEscape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
