package io.momonga.scraper

/** Public listing item as shown on momon:GA. */
data class WorkSummary(
    val title: String,
    val url: String,
    val coverUrl: String? = null,
    val publishedLabel: String? = null,
    val likes: Int? = null,
    val rankingPosition: Int? = null,
)

data class WorkDetails(
    val summary: WorkSummary,
    val creator: String? = null,
    val tags: List<String> = emptyList(),
    val synopsis: String? = null,
    val administratorComment: String? = null,
    val category: String? = null,
    val pageCount: Int? = null,
    val galleryPageUrls: List<String> = emptyList(),
    val comments: List<WorkComment> = emptyList(),
    val relatedWorks: List<WorkSummary> = emptyList(),
    val circle: String? = null,
    val parodies: List<String> = emptyList(),
    val characters: List<String> = emptyList(),
) {
    val title: String get() = summary.title
    val url: String get() = summary.url
}

data class WorkComment(val author: String?, val text: String, val date: String? = null)

data class WorkPage(
    val works: List<WorkSummary>,
    val pageUrl: String,
    val currentPage: Int = 1,
    val totalPages: Int? = null,
    val nextPageUrl: String? = null,
    val previousPageUrl: String? = null,
    val section: String? = null,
    val ranking: String? = null,
    val title: String? = null,
)

data class TagSummary(val name: String, val url: String, val workCount: Int?)
