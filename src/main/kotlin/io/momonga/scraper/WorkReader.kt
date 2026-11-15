package io.momonga.scraper

import org.jsoup.nodes.Document

private val pageNumberRegex = Regex("\\d+")
private val numericTextRegex = Regex("[^0-9]")

/** Parses an individual work page and its related metadata. */
internal class WorkReader(private val http: SiteHttp) {
    fun work(url: String): WorkDetails {
        require(http.isSiteUrl(url)) { "Only momon-ga.com URLs are supported" }
        val doc = http.fetch(url)
        val summary = summaryFromDetail(doc, url)
        val tagGroups = doc.select("#post-tag .post-tag-table")
        val tagsByGroup = tagGroups.associate { group ->
            group.selectFirst(".post-tag-title")?.text()?.clean().orEmpty() to
                group.select(".post-tags a").map { it.text().clean() }.filter { it.isNotBlank() }.distinct()
        }
        fun group(label: String): List<String> = tagsByGroup[label].orEmpty()

        val contents = group("内容")
        val circle = group("サークル").firstOrNull()
        val parodies = group("パロディ")
        val characters = group("キャラ")
        val category = firstText(doc, "#post-category")
        val pageCount = firstText(doc, "#post-number")?.let { pageNumberRegex.find(it)?.value?.toIntOrNull() }
        val creator = group("作者").firstOrNull()
            ?: doc.select("#post-tag a[href*='/cartoonist/']").firstOrNull()?.text()?.clean()
        val synopsis = firstText(doc, "[itemprop=description], .post-content, .entry-content, .work-description, .description")
        // Keep this lookup bounded: scanning doc.text() would also traverse every gallery image's alt text.
        val adminComment = firstText(doc, "#admin-comment, .admin-comment, .post-admin-comment, .manager-comment")
        val galleryPages = doc.select("#post-hentai img[src]").map { it.absUrl("src") }.filter { it.isNotBlank() }
        val comments = doc.select(".comment[id^=comment-]").mapNotNull { element ->
            val text = element.selectFirst(".comment-content, .comment-text, .comment-body, p")?.text()?.clean()
            if (text.isNullOrBlank()) null else WorkComment(
                author = element.selectFirst(".comment_author, .fn, .comment-author, .author")?.text()?.clean(),
                text = text,
                date = element.selectFirst(".comment_date, .comment-meta, time")?.text()?.clean(),
            )
        }
        val relatedWorks = doc.select(".yarpp-related a[href]").mapNotNull { a ->
            if (a.selectFirst(".na_pr") != null) return@mapNotNull null
            val relatedUrl = a.absUrl("href").ifBlank { a.attr("href") }
            if (!http.isSiteUrl(relatedUrl)) return@mapNotNull null
            val title = a.selectFirst("span")?.text()?.clean()
                ?: a.selectFirst("img[alt]")?.attr("alt")?.clean()
                ?: a.text().clean()
            if (title.isBlank()) return@mapNotNull null
            val image = a.selectFirst("img")?.absUrl("src")?.takeIf { it.isNotBlank() }
            val likes = a.selectFirst(".post-list-wpulike")?.text()?.replace(numericTextRegex, "")?.toIntOrNull()
            WorkSummary(title, relatedUrl, image, likes = likes)
        }.distinctBy { it.url }

        return WorkDetails(summary, creator, contents, synopsis, adminComment, category, pageCount, galleryPages, comments, relatedWorks, circle, parodies, characters)
    }

    private fun summaryFromDetail(doc: Document, url: String): WorkSummary {
        val title = firstText(doc, "h1") ?: firstText(doc, "[property=og:title], title")
            ?.substringBefore(" - momon")?.substringBefore(" | momon") ?: url
        val image = doc.selectFirst(".history[data-thumb]")?.attr("data-thumb")
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
        val likes = doc.selectFirst("#post-wpulike")?.text()?.replace(numericTextRegex, "")?.toIntOrNull()
        val cover = image ?: doc.selectFirst("#post-hentai img[src]")?.absUrl("src")
        return WorkSummary(title, url, cover, firstText(doc, "#post-time"), likes)
    }

    private fun firstText(doc: Document, selector: String): String? =
        doc.selectFirst(selector)?.text()?.clean()?.takeIf { it.isNotBlank() }
}
