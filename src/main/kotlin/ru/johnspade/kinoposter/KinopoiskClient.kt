package ru.johnspade.kinoposter

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.support.jdk7.use
import java.awt.image.BufferedImage
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

class KinopoiskClient {

	fun getRating(id: Long): Rating {
		val client = OkHttpClient()
		val url = HttpUrl.Builder().scheme("https").host("rating.kinopoisk.ru").addPathSegment("$id.xml").build()
		val request = Request.Builder().url(url).get().build()
		val mapper = XmlMapper().registerModule(KotlinModule())
		val response = client.newCall(request).execute()
		response.body().use {
			return mapper.readValue(it.string(), Rating::class.java)
		}
	}

	fun getStill(url: URL): Optional<BufferedImage> {
		(0..2).forEach {
			try {
				val image = ImageIO.read(url)
				return Optional.ofNullable(image)
			}
			catch (e: Exception) {
				Thread.sleep(3000)
			}
		}
		return Optional.empty()
	}

}
