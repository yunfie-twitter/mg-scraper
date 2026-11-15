package io.momonga.scraper

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

internal class SiteHttp(client: OkHttpClient, private val userAgent: String) {
    // Also checks redirected requests so a site redirect cannot load a third-party tracker.
    private val client = client.newBuilder().addNetworkInterceptor { chain ->
        val host = chain.request().url.host.lowercase()
        if (host != "momon-ga.com" && !host.endsWith(".momon-ga.com")) {
            throw IOException("Blocked request to non-site host: $host")
        }
        chain.proceed(chain.request())
    }.build()

    fun isSiteUrl(raw: String): Boolean = runCatching {
        val host = java.net.URI(raw).host?.lowercase() ?: return false
        host == "momon-ga.com" || host.endsWith(".momon-ga.com")
    }.getOrDefault(false)

    fun fetch(url: String): Document {
        require(isSiteUrl(url)) { "Only momon-ga.com URLs are supported" }
        val request = Request.Builder().url(url).header("User-Agent", userAgent).get().build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code} for $url" }
            val body = response.body?.string() ?: error("Empty response for $url")
            val document = Jsoup.parse(body, response.request.url.toString())
            document.select(".na_pr").forEach { marker ->
                marker.closest("a")?.remove() ?: marker.remove()
            }
            document.select("script, iframe, .pop--excl").remove()
            return document
        }
    }
}
