package ru.johnspade.kinoposter

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.intType
import com.natpryce.konfig.stringType
import mu.KLogging
import java.io.File

fun main(args: Array<String>) {
	try {
		KinoPosterApplication().run()
	}
	catch(e: Exception) {
		KinoPosterApplication.logger.error(e.message, e)
	}
}

object app : PropertyGroup() {
	val database by stringType
	val moviesCount by intType
}

object vk : PropertyGroup() {
	val groupId by intType
	val accessToken by stringType
	val trailerAlbumId by intType
	val userId by intType
}

class KinoPosterApplication {

	companion object: KLogging()

	fun run() {
		val config = ConfigurationProperties.fromFile(File("config.properties"))
		logger.info { "Заданное количество фильмов: ${config[app.moviesCount]}" }
		val databaseConnector = DatabaseConnector(config[app.database])
		val movies = databaseConnector.getMovies(config[app.moviesCount])
		logger.info { "Выбранные фильмы: ${movies.joinToString(", ") { "${it.nameRu} (${it.id})" }}" }
		KinoPoster(config[vk.userId], config[vk.accessToken]).post(movies, config[vk.groupId], config[vk.trailerAlbumId])
		databaseConnector.markPostedMovies(movies)
		logger.info("Фильмы отмечены как опубликованные")
	}

}
