package ru.johnspade.kinoposter

import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request

class TelegramClient {

	fun sendNotification(botToken: String, chatId: String, text: String) {
		val httpClient = OkHttpClient()
		val url = HttpUrl.Builder().scheme("https").host("api.telegram.org")
				.addPathSegment(botToken).addPathSegment("sendMessage")
				.addQueryParameter("chat_id", chatId).addEncodedQueryParameter("text", text)
				.addQueryParameter("parse_mode", "Markdown").build()
		val request = Request.Builder().url(url).get().build()
		httpClient.newCall(request).execute()
	}

}
