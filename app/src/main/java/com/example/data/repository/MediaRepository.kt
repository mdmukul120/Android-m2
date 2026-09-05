package com.example.data.repository

import android.util.Log
import com.example.data.api.ApiService
import com.example.data.api.ExtractorInfoResult
import com.example.data.model.ContentSection
import com.example.data.model.EpisodeItem
import com.example.data.model.LiveChannel
import com.example.data.model.MediaItem
import com.example.data.model.StreamServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MediaRepository {
    private val TAG = "MediaRepository"

    suspend fun getHomeContent(): Triple<MediaItem?, List<ContentSection>, List<LiveChannel>> = withContext(Dispatchers.IO) {
        // Ping services in background
        ApiService.pingServices()

        var heroItem: MediaItem? = null
        val sections = mutableListOf<ContentSection>()

        // 1. Fetch real Bioscope sections
        val bioscopeJson = ApiService.fetchBioscopePage()
        val bioscopeMovies = mutableListOf<MediaItem>()
        val bioscopeSeries = mutableListOf<MediaItem>()

        if (!bioscopeJson.isNullOrEmpty()) {
            try {
                val root = JSONObject(bioscopeJson)
                val result = root.optJSONObject("result")
                val jsonSections = result?.optJSONArray("sections")

                if (jsonSections != null) {
                    for (i in 0 until jsonSections.length()) {
                        val secObj = jsonSections.getJSONObject(i)
                        val title = secObj.optString("title", "Featured")
                        val size = secObj.optString("size")
                        val itemsArray = secObj.optJSONArray("items") ?: continue

                        val mediaList = mutableListOf<MediaItem>()
                        for (j in 0 until itemsArray.length()) {
                            val itemObj = itemsArray.getJSONObject(j)
                            val contentObj = itemObj.optJSONObject("content") ?: itemObj
                            val id = contentObj.optString("id", itemObj.optString("id", "item_$j"))
                            val itemTitle = contentObj.optString("title", itemObj.optString("title", "Untitled"))
                            val desc = contentObj.optString("description")
                            val type = contentObj.optString("type", "movies")
                            val poster = contentObj.optString("poster").ifEmpty {
                                contentObj.optString("tv_cover").ifEmpty {
                                    contentObj.optString("thumbnail")
                                }
                            }
                            val backdrop = contentObj.optString("poster_background").ifEmpty {
                                contentObj.optString("thumbnail_background").ifEmpty { poster }
                            }
                            val streamUrl = contentObj.optString("url")
                            val label = contentObj.optString("label", "Free")
                            val durationSec = contentObj.optInt("duration", 0)
                            val formattedDuration = if (durationSec > 0) {
                                val hrs = durationSec / 3600
                                val mins = (durationSec % 3600) / 60
                                if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                            } else "2h 10m"

                            val castList = mutableListOf<String>()
                            val metas = contentObj.optJSONObject("metas")
                            val castsArray = metas?.optJSONArray("casts")
                            if (castsArray != null) {
                                for (k in 0 until castsArray.length()) {
                                    val c = castsArray.getJSONObject(k).optString("title")
                                    if (c.isNotEmpty()) castList.add(c)
                                }
                            }

                            val mediaItem = MediaItem(
                                id = id,
                                title = itemTitle,
                                description = desc,
                                category = if (type.contains("series", ignoreCase = true)) "series" else "movies",
                                posterUrl = poster,
                                backdropUrl = backdrop,
                                streamUrl = streamUrl,
                                rating = "4.8",
                                year = "2024",
                                duration = formattedDuration,
                                genre = if (type.contains("series")) "Bangla Series" else "Bangla Cinema",
                                label = label.ifEmpty { "HD" },
                                cast = castList
                            )

                            if (size == "hero_slider" && heroItem == null && poster.isNotEmpty()) {
                                heroItem = mediaItem
                            }
                            mediaList.add(mediaItem)

                            if (mediaItem.category == "series") {
                                bioscopeSeries.add(mediaItem)
                            } else {
                                bioscopeMovies.add(mediaItem)
                            }
                        }

                        if (mediaList.isNotEmpty() && size != "hero_slider") {
                            sections.add(ContentSection(title = title, items = mediaList))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Bioscope JSON: ${e.message}")
            }
        }

        // 2. Fetch real Movie & Series from SorryBro Extractor Providers (moviesmod, hdhub4u, topmovies, moviesdrive, uhd)
        coroutineScope {
            val hdhubDeferred = async { ApiService.fetchExtractorPosts("hdhub4u", 1) }
            val moviesmodDeferred = async { ApiService.fetchExtractorPosts("moviesmod", 1) }
            val topmoviesDeferred = async { ApiService.fetchExtractorPosts("topmovies", 1) }
            val moviesdriveDeferred = async { ApiService.fetchExtractorPosts("moviesdrive", 1) }
            val uhdDeferred = async { ApiService.fetchExtractorPosts("uhd", 1) }

            val hdhubItems = try { hdhubDeferred.await() } catch (e: Exception) { emptyList() }
            val moviesmodItems = try { moviesmodDeferred.await() } catch (e: Exception) { emptyList() }
            val topmoviesItems = try { topmoviesDeferred.await() } catch (e: Exception) { emptyList() }
            val moviesdriveItems = try { moviesdriveDeferred.await() } catch (e: Exception) { emptyList() }
            val uhdItems = try { uhdDeferred.await() } catch (e: Exception) { emptyList() }

            if (hdhubItems.isNotEmpty()) {
                sections.add(0, ContentSection(title = "Latest Blockbusters (2026)", items = hdhubItems))
                if (heroItem == null) {
                    heroItem = hdhubItems[0]
                }
            }

            if (moviesmodItems.isNotEmpty()) {
                val modSeries = moviesmodItems.filter { it.category == "series" }
                val modMovies = moviesmodItems.filter { it.category == "movies" }
                if (modMovies.isNotEmpty()) {
                    sections.add(ContentSection(title = "Bollywood & Multi-Audio Hits", items = modMovies))
                }
                if (modSeries.isNotEmpty()) {
                    sections.add(ContentSection(title = "Top Web Series & Dramas", items = modSeries))
                }
            }

            if (topmoviesItems.isNotEmpty()) {
                sections.add(ContentSection(title = "Top Movies & Dubbed Cinema", items = topmoviesItems))
            }

            if (moviesdriveItems.isNotEmpty()) {
                sections.add(ContentSection(title = "MoviesDrive 4K Collection", items = moviesdriveItems))
            }

            if (uhdItems.isNotEmpty()) {
                sections.add(ContentSection(title = "Ultra HD Movies", items = uhdItems))
            }
        }

        // 3. Fetch Live IPTV Channels (Bangla channels prioritized first)
        val liveChannels = ApiService.fetchIptvPlaylist()

        Triple(heroItem, sections, liveChannels)
    }

    suspend fun getLiveChannels(): List<LiveChannel> = withContext(Dispatchers.IO) {
        ApiService.fetchIptvPlaylist()
    }

    // Resolve details, qualities, and playable streaming link for extractor movie/show
    suspend fun resolveMediaDetails(media: MediaItem): MediaItem = withContext(Dispatchers.IO) {
        if (media.provider.isEmpty() || media.link.isEmpty()) {
            return@withContext media
        }

        try {
            val infoResult = ApiService.fetchExtractorInfo(media.provider, media.link)
            if (infoResult != null) {
                var updatedEpisodes = media.episodes
                if (infoResult.episodesLink.isNotEmpty()) {
                    val epList = ApiService.fetchExtractorEpisodes(media.provider, infoResult.episodesLink)
                    if (epList.isNotEmpty()) {
                        updatedEpisodes = epList
                    }
                }

                // If movie has direct links in qualities, extract first available stream
                var resolvedStreamUrl = media.streamUrl
                var resolvedDownloadUrl = media.downloadUrl
                var updatedQualities = infoResult.qualities

                val firstDirectLink = infoResult.qualities.firstOrNull { it.directLink.isNotEmpty() }?.directLink
                if (resolvedStreamUrl.isEmpty() && !firstDirectLink.isNullOrEmpty()) {
                    val servers = ApiService.extractStreamServers(media.provider, firstDirectLink)
                    if (servers.isNotEmpty()) {
                        // Pick best streaming server (CF Worker, Pixeldrain, CF Storage)
                        val streamServer = servers.firstOrNull { it.server.contains("CF Worker", true) }
                            ?: servers.firstOrNull { it.server.contains("Pixeldrain", true) }
                            ?: servers.firstOrNull { it.server.contains("CF Storage", true) }
                            ?: servers[0]
                        resolvedStreamUrl = streamServer.link

                        // Pick download link (GDrive or first server)
                        val downloadServer = servers.firstOrNull { it.server.contains("GDrive", true) }
                            ?: servers.firstOrNull { it.server.contains("Pixeldrain", true) }
                            ?: servers[0]
                        resolvedDownloadUrl = downloadServer.link
                    }
                }

                return@withContext media.copy(
                    title = if (infoResult.title.isNotEmpty()) infoResult.title else media.title,
                    description = if (infoResult.synopsis.isNotEmpty()) infoResult.synopsis else media.description,
                    posterUrl = if (infoResult.image.isNotEmpty()) infoResult.image else media.posterUrl,
                    backdropUrl = if (infoResult.image.isNotEmpty()) infoResult.image else media.backdropUrl,
                    qualities = updatedQualities,
                    episodes = updatedEpisodes,
                    streamUrl = resolvedStreamUrl,
                    downloadUrl = resolvedDownloadUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving media details: ${e.message}")
        }
        media
    }

    suspend fun resolveStreamUrl(provider: String, directUrl: String): List<StreamServer> = withContext(Dispatchers.IO) {
        ApiService.extractStreamServers(provider, directUrl)
    }
}
