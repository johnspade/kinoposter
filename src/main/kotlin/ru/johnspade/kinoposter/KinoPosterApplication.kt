package ru.johnspade.kinoposter

import mu.KLogging
import us.jimschubert.kopper.Parser

fun main(args: Array<String>) {
	try {
		KinoPosterApplication().run(args)
	}
	catch(e: Exception) {
		KinoPosterApplication.logger.error(e.message, e)
	}
}

class KinoPosterApplication {

	companion object: KLogging()

	private val databaseArg = "d"
	private val tokenArg = "t"
	private val moviesCountArg = "m"
	private val groupIdArg = "g"
	private val trailerAlbumIdArg = "a"
	private val userIdArg = "u"

	fun run(args: Array<String>) {
		val arguments = Parser().option(databaseArg).option(tokenArg).option(moviesCountArg).option(groupIdArg)
				.option(trailerAlbumIdArg).option(userIdArg).parse(args)
		val database = arguments.option(databaseArg)!!
		val moviesCount = arguments.option(moviesCountArg)!!.toInt()
		val accessToken = arguments.option(tokenArg)!!
		val groupId = arguments.option(groupIdArg)!!.toInt()
		val trailerAlbumId = arguments.option(trailerAlbumIdArg)!!.toInt()
		val userId = arguments.option(userIdArg)!!.toInt()
		logger.info { "Заданное количество фильмов: $moviesCount" }
		val databaseConnector = DatabaseConnector(database)
		val movies = databaseConnector.getMovies(moviesCount)
		logger.info { "Выбранные фильмы: ${movies.joinToString(", ") { "${it.nameRu} (${it.id})" } }" }
		KinoPoster(userId, accessToken).post(movies, groupId, trailerAlbumId)
		databaseConnector.markPostedMovies(movies)
		logger.info("Фильмы отмечены как опубликованные")
	}

}
