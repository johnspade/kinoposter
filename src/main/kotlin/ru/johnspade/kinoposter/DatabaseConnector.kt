package ru.johnspade.kinoposter

import kotlinx.support.jdk7.use
import java.sql.DriverManager

data class Movie(
		val id: Long, val nameRu: String, val nameEn: String?, val year: Int, val country: String,
		val genre: List<String>, val description: String, val director: String, val actors: List<String>,
		val stills: List<String>, val trailer: String?
)

class DatabaseConnector(private val database: String) {

	init { Class.forName("org.postgresql.Driver") }

	fun getMovies(count: Int = 1): List<Movie> {
		val connection = DriverManager.getConnection("jdbc:postgresql:$database")
		val movies = mutableListOf<Movie>()
		connection.use { conn ->
			val sql = """select id, name_ru, name_en, year, country, genre, description, director, actors, stills, trailer
			from good_movies order by random() limit $count;"""
			val statement = conn.createStatement()
			statement.use {
				val resultSet = statement.executeQuery(sql)
				while (resultSet.next()) {
					@Suppress("UNCHECKED_CAST")
					val movie = Movie(
							id = resultSet.getLong("id"),
							nameRu = resultSet.getString("name_ru"),
							nameEn = resultSet.getString("name_en"),
							year = resultSet.getInt("year"),
							country = resultSet.getString("country"),
							genre = resultSet.getString("genre").split(", "),
							description = resultSet.getString("description"),
							director = resultSet.getString("director"),
							actors = resultSet.getString("actors").split(", "),
							stills = (resultSet.getArray("stills").array as Array<String>).toList(),
							trailer = resultSet.getString("trailer")
					)
					movies.add(movie)
				}
			}
		}
		return movies.toList()
	}

	fun markPostedMovies(movies: List<Movie>) {
		val connection = DriverManager.getConnection("jdbc:postgresql:$database")
		connection.use {
			val statement = connection.prepareStatement("update good_movies set posted = true where id = any(?)")
			statement.use {
				statement.setArray(1, connection.createArrayOf("bigint", movies.map { it.id }.toTypedArray()))
				statement.executeUpdate()
			}
		}
	}

}
