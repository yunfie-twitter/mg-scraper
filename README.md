# momon:GA Scraper for Kotlin

A small Kotlin/JVM client for reading public work listings and metadata from momon:GA. It reads listing cards and individual work pages; it does not download gallery image files.

## Requirements

JDK 17+. Add the source module to your Gradle project or publish the project to your Maven repository.

## Example

```kotlin
import io.momonga.scraper.MomonGaClient

fun main() {
    val client = MomonGaClient()
    val page = client.list("https://momon-ga.com/fanzine/")
    page.works.forEach { println("${it.title} — ${it.url}") }

    val details = client.work(page.works.first().url)
    println(details.title)
    println(details.tags.joinToString())
}
```

Listing pages use the site's WordPress pagination path (`/fanzine/page/2/`):

```kotlin
val secondPage = client.list("https://momon-ga.com/fanzine/page/2/")
println("${secondPage.currentPage} / ${secondPage.totalPages}")
println(secondPage.nextPageUrl)

val magazine = client.list("https://momon-ga.com/magazine/")
val magazinePage2 = client.list("https://momon-ga.com/magazine/page/2/")
println("${magazine.section}: ${magazine.works.size} works")

val trending = client.list("https://momon-ga.com/trend/")
println(trending.ranking) // "trend"
trending.works.forEach { println("${it.rankingPosition}: ${it.title} (${it.likes})") }

val originals = client.list("https://momon-ga.com/parody/%e3%82%aa%e3%83%aa%e3%82%b8%e3%83%8a%e3%83%ab/")
val onePiecePage2 = client.list("https://momon-ga.com/parody/one-piece/page/2/")
println("${originals.title}: ${originals.works.size} works")

val parodyTags = client.parodies()
parodyTags.take(5).forEach { println("${it.name}: ${it.workCount} works — ${it.url}") }

val circles = client.groups()
circles.take(5).forEach { println("${it.name}: ${it.workCount} works — ${it.url}") }

val cartoonists = client.cartoonists()
val contentTags = client.contentTags()
```

`WorkPage` includes the page heading as `title`, detected section (`fanzine` or `magazine`), ranking type (`trend`, `popularity`, `rated`, or `comments`), current page, total pages when exposed by the page, and next/previous URLs. Tag archives such as `/parody/one-piece/` and URL-encoded Japanese slugs use the same card and pagination parser. Ranking cards also receive their displayed `rankingPosition`. `search(query, page)` uses the site's `/page/N/` pattern under the root.

`parodies()`, `groups()`, `cartoonists()`, and `contentTags()` read the `/parody/`, `/group/`, `/cartoonist/`, and `/tag/` indexes into `TagSummary` values with each entry's display name, archive URL, and listed work count. Open an archive URL with `list(url)` to fetch its works.

Search uses the site's public search endpoint and puts the page number before the query string (`/page/2/?s=...`):

```kotlin
val results = client.search("キーワード", page = 1)
val resultsPage2 = client.search("キーワード", page = 2)
println("${resultsPage2.currentPage} / ${resultsPage2.totalPages}")
```

## Data

`WorkSummary`: title, detail URL, cover URL, date label, like count.
`WorkDetails`: summary fields plus creator, circle, parody tags, character tags, content tags, synopsis, and administrator comment when present.
`WorkDetails.relatedWorks` contains the related work cards shown on a work page, including title, URL, cover URL, and like count when available.

The site is WordPress based and its markup can change. Parsers use conservative selectors and expose missing details as null/empty values. The client only requests momon-ga.com pages, blocks redirects to other hosts, and strips scripts, iframes, and ad slots from the parsed document. Jsoup does not execute the page's JavaScript. Use a descriptive User-Agent, keep requests slow, and follow the site's terms and robots policy.

### Work page data

`client.work("https://momon-ga.com/fanzine/mo3426371/")` reads the work page's `h1`, `#post-category`, `#post-number`, `#post-wpulike`, `#post-tag` groups, `#post-hentai` image URLs, and `.comment[id^=comment-]` entries. `tags` contains the page's `内容` tags; `creator` is selected from the `作者` group. Gallery image URLs are returned as metadata only. The site may omit synopsis or administrator comment, in which case those fields are null.

