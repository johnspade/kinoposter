package ru.johnspade.kinoposter

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.queries.wall.WallGetFilter
import mu.KLogging
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

data class Rating(val kp_rating: String, val imdb_rating: String?)
data class Post(val message: String, val attachments: List<String>, val dateTime: LocalDateTime)

class KinoPoster(userId: Int, accessToken: String) {

	companion object: KLogging()

	private val kinopoiskClient = KinopoiskClient()
	private val vk = VkApiClient(HttpTransportClient())
	private val actor = UserActor(userId, accessToken)
	private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")

	fun post(movies: List<Movie>, groupId: Int, trailerAlbumId: Int) {
		val posts = mutableListOf<Post>()
		var lastDateTime = LocalDateTime.now()
		val wallGetQuery = vk.wall().get(actor).ownerId(-groupId)
		var getResponse = wallGetQuery.filter(WallGetFilter.POSTPONED).count(100).execute()
		getResponse.items.lastOrNull()?.let { lastDateTime = getMaxDate(it.date, lastDateTime) }
		getResponse = wallGetQuery.filter(WallGetFilter.OWNER).count(1).execute()
		getResponse.items.firstOrNull()?.let { lastDateTime = getMaxDate(it.date, lastDateTime) }
		var dateTime: LocalDateTime
		movies.mapTo(posts) { movie ->
			dateTime = getNextDateTime(lastDateTime)
			lastDateTime = dateTime
			val rating = kinopoiskClient.getRating(movie.id)
			val message = """
				|${movie.nameRu} ${movie.nameEn?.let { "($it)" }?: ""}
				|
				|Жанр: ${movie.genre.map { "#$it" }.joinToString(", ")}
				|Год: ${movie.year}
				|Страна: ${movie.country}
				|Режиссер: ${movie.director}
				|В главных ролях: ${movie.actors.take(4).joinToString(", ")}
				|Оценка на Кинопоиске: ${rating.kp_rating}${rating.imdb_rating?.let { "\nОценка на IMDb: $it" }?: ""}
				|
				|${movie.description}
			""".trimMargin()
			val attachments = mutableListOf("https://www.kinopoisk.ru/film/${movie.id}")
			val links = listOf("https://st.kp.yandex.net/images/film_big/${movie.id}.jpg") + movie.stills.take(4)
			val serverResponse = vk.photos().getWallUploadServer(actor).groupId(groupId).execute()
			links.forEach {
				val url = URL(it)
				val image = kinopoiskClient.getStill(url)
				if (image.isPresent) {
					val file = createTempFile(suffix = ".jpg")
					try {
						ImageIO.write(image.get(), "jpg", file)
						val uploadResponse = vk.upload().photoWall(serverResponse.uploadUrl, file).execute()
						val photoList = vk.photos().saveWallPhoto(actor, uploadResponse.photo)
								.server(uploadResponse.server)
								.groupId(groupId)
								.hash(uploadResponse.hash)
								.execute()
						attachments.addAll(photoList.map { "photo${it.ownerId}_${it.id}" })
					}
					finally {
						file.delete()
					}
				}
				else
					throw RuntimeException("Не удалось загрузить изображение $it")
			}
			movie.trailer?.let {
				val saveResponse = vk.videos().save(actor).groupId(groupId).albumId(trailerAlbumId).link(it).execute()
				try {
					vk.upload().video(saveResponse.uploadUrl, null).execute()
					attachments.add("video${saveResponse.ownerId}_${saveResponse.videoId}")
				}
				catch (e: Exception) {
					logger.error { "Не удалось прикрепить видео для фильма ${movie.nameRu} (${movie.id})" }
				}
			}
			Post(message, attachments, dateTime)
		}
		logger.info {
			"Посты для публикации:\n" + posts.joinToString("\n") {
				"Дата публикации: ${it.dateTime.format(dateTimeFormatter)}, текст:\n${it.message}"
			}
		}
		val queries = posts.map {
			vk.wall().post(actor)
					.ownerId(-groupId)
					.message(it.message.replace("\n", "\\n").replace("\r", "\\r"))
					.attachments(it.attachments)
					.publishDate(it.dateTime.atZone(ZoneId.systemDefault()).toEpochSecond().toInt())
					.fromGroup(true)
		}
		vk.execute().batch(actor, queries).execute()
	}

	private fun getMaxDate(unixTime: Int, dateTime: LocalDateTime): LocalDateTime {
		val lastPostDateTime = Instant.ofEpochSecond(unixTime.toLong()).atZone(ZoneId.systemDefault()).toLocalDateTime()
		return if (lastPostDateTime > dateTime) lastPostDateTime else dateTime
	}

	private fun getNextDateTime(lastDateTime: LocalDateTime): LocalDateTime {
		val now = LocalDateTime.now()
		val dateTime = if (lastDateTime > now) lastDateTime else now
		val lastHour = dateTime.hour
		val hoursToAdd = when (lastHour) {
			in 0..8 -> 14L
			in 9..14 -> 20L
			else -> 32L
		}
		return dateTime.plusHours(hoursToAdd - lastHour).withMinute(0)
	}

}