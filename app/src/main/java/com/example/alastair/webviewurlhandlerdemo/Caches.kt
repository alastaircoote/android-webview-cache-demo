package com.example.alastair.webviewurlhandlerdemo


import android.util.Log
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

typealias HeaderCollection = Map<String, List<String>>

class CacheEntry(val url: String, val headers: HeaderCollection, val body: ByteArray, val status: Int, val statusMessage: String) {
}

class Caches {

    private var cacheEntries = ArrayList<CacheEntry>()

    fun add(url: String) {

        Log.d("Cache", "Caching URL: $url")

        var parsedURL = URL(url)
        val connection = parsedURL.openConnection() as HttpURLConnection
        val stream = connection.inputStream

        val saveStream = ByteArrayOutputStream()

        var byteChunk = ByteArray(4096)

        while (true) {
            val chunkLength = stream.read(byteChunk)
            if (chunkLength <= 0) {
                break
            }
            saveStream.write(byteChunk, 0, chunkLength)
        }

        stream.close()
        saveStream.close()

        this.cacheEntries.add(CacheEntry(url, connection.headerFields, saveStream.toByteArray(), connection.responseCode, connection.responseMessage))

    }

    fun put(url: String, statusCode:Int, statusMessage:String, headers: HeaderCollection, body: ByteArray) {
        this.cacheEntries.add(CacheEntry(url, headers, body,statusCode, statusMessage))
    }

    fun match(url: String): CacheEntry? {
        return this.cacheEntries.find { entry -> entry.url == url }
    }
}