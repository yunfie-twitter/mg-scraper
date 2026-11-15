package io.momonga.scraper

import org.jsoup.nodes.Document

private val countRegex = Regex("[\\d,]+")
private val pageCountRegex = Regex("/\\s*([\\d,]+)")
private val pathPageRegex = Regex("/page/(\\d+)/?")
private val whitespaceRegex = Regex("\\s+")

/** Parses listing, ranking, search, and taxonomy index pages. */
internal class PageReader(private val http: SiteHttp) {
    fun list(url: String): WorkPage = parseListing(http.fetch(url), url)

    fun tagIndex(url: String): List<TagSummary> {
        require(http.isSiteUrl(url)) { "Only momon-ga.com URLs are supported" }
        val doc = http.fetch(url)
        return doc.select(".tag-list a[href]").mapNotNull { a ->
            val href = a.absUrl("href").ifBlank { a.attr("href") }
            if (!http.isSiteUrl(href)) return@mapNotNull null
            val count = countRegex.find(a.selectFirst("span")?.text().orEmpty())
                ?.value?.replace(",", "")?.toIntOrNull()
            val name = a.ownText().clean()
            if (name.isBlank()) null else TagSummary(name, href, count)
        }
    }

    private fun parseListing(doc: Document, pageUrl: String): WorkPage {
        val works = linkedMapOf<String, WorkSummary>()
        val cards = doc.select(".post-list > a[href], a[href*='/fanzine/mo'], a[href*='/magazine/mo'], a[href*='/work/']")
        val effectiveUrl = doc.location().ifBlank { pageUrl }
        val mainHeading = doc.selectFirst(".page-h h1, .home-h h1, h1")?.text()?.clean()
        val rankingHeading = doc.selectFirst(".page-h h1")?.text()?.clean()
        val rankingPage = effectiveUrl.contains("/trend/") || effectiveUrl.contains("/popularity/") ||
            effectiveUrl.contains("/rated/") || effectiveUrl.contains("/comments/") ||
            rankingHeading in setOf("急上昇", "人気", "高評価", "コメント指数")

        cards.forEachIndexed { index, a ->
            if (a.selectFirst(".na_pr") != null) return@forEachIndexed
            val href = a.absUrl("href").ifBlank { a.attr("href") }
            if (!http.isSiteUrl(href)) return@forEachIndexed
            val title = a.selectFirst("span")?.text()?.clean()
                ?: a.selectFirst("img[alt]")?.attr("alt")?.clean()
                ?: a.text().clean()
            if (title.isBlank()) return@forEachIndexed
            val image = a.selectFirst("img")
            val likes = a.selectFirst(".post-list-wpulike")?.text()?.replace("+", "")?.trim()?.toIntOrNull()
            val date = a.selectFirst(".post-list-time")?.text()?.clean()
            val rank = if (rankingPage) index + 1 else null
            works.putIfAbsent(href, WorkSummary(title, href, image?.let { it.absUrl("src").ifBlank { it.attr("src") } }, date, likes, rank))
        }

        if (works.isEmpty()) {
            doc.select("article a[href], .post a[href]").forEach { a ->
                if (a.selectFirst(".na_pr") != null) return@forEach
                val href = a.absUrl("href")
                val title = a.text().clean()
                if (http.isSiteUrl(href) && title.isNotBlank()) works.putIfAbsent(href, WorkSummary(title, href))
            }
        }

        val current = doc.selectFirst(".pagination .current, .wp-pagenavi .current")?.text()?.toIntOrNull()
            ?: pathPageRegex.find(pageUrl)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val total = doc.selectFirst(".pagination .pages, .wp-pagenavi .pages")?.text()?.let {
            pageCountRegex.find(it)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
        }
        val next = doc.selectFirst(".wp-pagenavi a.nextpostslink[href], .pagination a.next[href]")?.absUrl("href")
        val previous = doc.selectFirst(".wp-pagenavi a.previouspostslink[href], .pagination a.prev[href]")?.absUrl("href")
        val section = when {
            mainHeading == "商業誌" || effectiveUrl.contains("/magazine/") -> "magazine"
            mainHeading == "同人誌" || effectiveUrl.contains("/fanzine/") -> "fanzine"
            else -> null
        }
        val ranking = when {
            effectiveUrl.contains("/trend/") || rankingHeading == "急上昇" -> "trend"
            effectiveUrl.contains("/popularity/") || rankingHeading == "人気" -> "popularity"
            effectiveUrl.contains("/rated/") || rankingHeading == "高評価" -> "rated"
            effectiveUrl.contains("/comments/") || rankingHeading == "コメント指数" -> "comments"
            else -> null
        }
        return WorkPage(works.values.toList(), pageUrl, current, total, next, previous, section, ranking, mainHeading)
    }
}

internal fun String.clean(): String = replace(whitespaceRegex, " ").trim()
