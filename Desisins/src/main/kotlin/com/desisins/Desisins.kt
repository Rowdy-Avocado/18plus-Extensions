package com.desisins
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class Desisins : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://shorts.desisins.com/"
    override var name = "Desisins"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.NSFW
    )
    
    private suspend fun getData( i: Int): Document {
       
        val response = app.post(
            "$mainUrl/wp-admin/admin-ajax.php",
            data = mapOf(
			    "action" to "grid_ajax_load_more",
			    "cat_id" to "-1",
			    "current_posts" to "$i",
			    "type" to ""
            )
        ).text

        return  Jsoup.parse(response)
    }
    private fun toResult(post: Element): SearchResponse {
        val url = post.select("a")[3].attr("href")
        val title = post.select("a")[3].text()
        var imageUrl = post.select("img").attr("src")
       // Log.d("post",post.toString())
        //val quality = post.select(".video-label").text()
        return newMovieSearchResponse(title, url, TvType.Movie) {
            this.posterUrl = imageUrl
        }
    }
    
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
     //val url =if(page==1) "$mainUrl/${request.data}/" else  "$mainUrl/${request.data}/page/$page/" 
        val document = getData(page*12)
        
        val home = document.select("div.home_post_cont").mapNotNull {
            toResult(it)
        }
        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select("div.home_post_cont").mapNotNull {
            toResult(it)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1").text()
        //val poster = fixUrlNull(document.select("h2 > img").first()?.attr("src"))
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = ""
 
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var src = app.get(data).document.select("iframe").attr("src")
        
        //Log.d("link",src)
        loadExtractor(
                src,
                "$mainUrl/",
                subtitleCallback,
                callback
           
        )
        return true
    }


}
