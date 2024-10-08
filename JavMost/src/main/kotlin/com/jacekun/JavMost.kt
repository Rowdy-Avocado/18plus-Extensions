package com.jacekun

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.app

class JavMost : MainAPI() {
    private val DEV = "DevDebug"
    private val globaltvType = TvType.NSFW

    override var name = "JavMost"
    override var mainUrl = "https://www5.javmost.com"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasDownloadSupport = false
    override val hasMainPage = true
    override val hasQuickSearch = false

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(mainUrl).document
        val all = ArrayList<HomePageList>()

        val mainbody = document.getElementsByTag("body")
            ?.select("div#page-container > div#content > div#content-update > div")
            ?.select("div.col-md-4.col-sm-6")
        val title = "Homepage"
        // Fetch list of items and map
        val elements: List<SearchResponse> = mainbody!!.map {

            val inner = it.select("div.card")
            val linkA = inner.select("div.card-block > a")
            val link = linkA?.firstOrNull()?.attr("href") ?: ""
            val name = listOfNotNull(linkA?.firstOrNull()?.text(), linkA?.getOrNull(1)?.text()).joinToString(" ")
            //Log.i(DEV, "Result => (name and link) ${name} / ${link}")
            val image = inner.select("center > a > img").attr("data-src").orEmpty().ifBlank {
                inner.select("center > a > img").attr("src")
            }.run {
                if (this.equals("http")) {
                    inner.select("center > a > img").attr("src")
                } else {
                    this
                }
            }
            //Log.i(DEV, "Result => (image) ${image}")
            val year = inner.select("div.card-block > p").text()
                ?.substring(0, 20)?.replace("Release", "")?.trim()
                ?.substring(0, 4)?.toIntOrNull()

            MovieSearchResponse(
                name,
                link,
                this.name,
                globaltvType,
                image,
                year,
                null,
            )
        }

        all.add(
            HomePageList(
                title, elements
            )
        )

        return HomePageResponse(all)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val document = app.get("$mainUrl/search/${query}/").document
        val mainbody = document.getElementsByTag("body")
            ?.select("div#page-container > div#content > div#content-update > div")
            ?.select("div.col-md-4.col-sm-6")
        //Log.i(DEV, "Result => $document")
        if (mainbody != null) {
            return mainbody.map {
                val content = it.select("div.card").firstOrNull()
                val linkImg = content?.select("a")?.firstOrNull()

                val href = fixUrl(linkImg?.attr("href") ?: "")
                var image = linkImg?.select("img")?.attr("data-src")?.trim('\'')
                if (image != null) { image = fixUrl(image) }
                //Log.i(DEV, "Result => (link) ${href}, (img) ${image}")
                val titleContent = content?.select("div.card-block > a")
                //Log.i(DEV, "Result => (titleContent) ${titleContent}")
                val title = when (titleContent?.size) {
                    2 -> listOfNotNull(
                        titleContent[0]?.text(),
                        titleContent[1]?.text()
                    ).joinToString(" ")
                    1 -> titleContent[0]?.text()
                    else -> "<No Title found>"
                } ?: "<No Title found>"
                //Log.i(DEV, "Result => (title) ${title}")
                var year: Int? = null
                val yearP = content?.select("div.card-block")?.firstOrNull()?.select("p")
                //Log.i(DEV, "Result => (yearP) ${yearP}")
                val yearElem = when(yearP != null) {
                    true -> yearP?.filter { yearit -> yearit.text()?.contains("Release") == true }
                    false -> null
                }
                val yearString = when (yearElem?.size!! > 0) {
                    true -> yearElem?.get(0)?.text()?.substring(0, 22)?.trim()
                        ?.replace("Release", "")?.trim()
                    false -> null
                }
                //Log.i(DEV, "Result => (yearString) ${yearString}")
                if (yearString != null)  {
                    val maxSize = if (yearString.length > 4) { 4 } else { yearString.length }
                    year = yearString?.substring(0, maxSize)?.toIntOrNull()
                }
                //Log.i(DEV, "Result => (year) ${year}")

                MovieSearchResponse(
                    title,
                    href,
                    this.name,
                    globaltvType,
                    image,
                    year
                )
            }
        }
        return null
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        //Log.i(DEV, "Url => ${url}")
        val body = document.getElementsByTag("head")

        //Log.i(DEV, "Result => ${body}")
        var poster = body?.select("meta[property=og:image]")?.firstOrNull()?.attr("content")
        if (poster != null) { poster = fixUrl(poster) }
        //Log.i(DEV, "Result (image) => ${poster}")
        val title = body?.select("meta[property=og:title]")?.firstOrNull()?.attr("content") ?: "<No Title>"
        val descript = body?.select("meta[property=og:description]")?.firstOrNull()?.attr("content") ?: "<No Synopsis found>"
        //Log.i(DEV, "Result => ${descript}")
        val streamUrl = ""
        val year = null
        return MovieLoadResponse(
            name = title,
            url = url,
            apiName = this.name,
            type = globaltvType,
            dataUrl = streamUrl,
            posterUrl = poster,
            year = year,
            plot = descript,
            comingSoon = true
        )
    }
}