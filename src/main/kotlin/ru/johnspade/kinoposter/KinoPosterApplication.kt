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

object App : PropertyGroup() {
	val database by stringType
	val moviesCount by intType
}

object Vk : PropertyGroup() {
	val groupId by intType
	val accessToken by stringType
	val userId by intType
}

class KinoPosterApplication {

	companion object: KLogging()

	fun run() {
		val config = ConfigurationProperties.fromFile(File("config.properties"))
		logger.info { "Заданное количество фильмов: ${config[App.moviesCount]}" }
		val databaseConnector = DatabaseConnector(config[App.database])
		val movies = databaseConnector.getMovies(config[App.moviesCount])
		logger.info { "Выбранные фильмы: ${movies.joinToString(", ") { "${it.nameRu} (${it.id})" }}" }
		KinoPoster(config[Vk.userId], config[Vk.accessToken]).post(movies, config[Vk.groupId])
		databaseConnector.markPostedMovies(movies)
		logger.info("Фильмы отмечены как опубликованные")
	}

}
