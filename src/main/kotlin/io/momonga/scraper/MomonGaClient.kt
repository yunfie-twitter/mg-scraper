package io.momonga.scraper

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import okhttp3.OkHttpClient
import java.time.Duration

/** Entry point for reading public momon-ga.com listings and work pages. */
class MomonGaClient(
    private val baseUrl: String = "https://momon-ga.com/",
    http: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(20))
        .followRedirects(true)
        .build(),
    userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
) {
    private val siteHttp = SiteHttp(http, userAgent)
    private val pages = PageReader(siteHttp)
    private val works = WorkReader(siteHttp)

    fun list(url: String = baseUrl): WorkPage = pages.list(url)

    fun parodies(url: String = "${baseUrl.trimEnd('/')}/parody/"): List<TagSummary> = pages.tagIndex(url)
    fun groups(url: String = "${baseUrl.trimEnd('/')}/group/"): List<TagSummary> = pages.tagIndex(url)
    fun cartoonists(url: String = "${baseUrl.trimEnd('/')}/cartoonist/"): List<TagSummary> = pages.tagIndex(url)
    fun contentTags(url: String = "${baseUrl.trimEnd('/')}/tag/"): List<TagSummary> = pages.tagIndex(url)

    fun search(query: String, page: Int = 1): WorkPage {
        require(query.isNotBlank()) { "query must not be blank" }
        require(page >= 1) { "page must be >= 1" }
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val pagePath = if (page == 1) "" else "/page/$page"
        return list("${baseUrl.trimEnd('/')}$pagePath/?s=$encoded")
    }

    fun work(url: String): WorkDetails = works.work(url)
}
