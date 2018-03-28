package com.example.alastair.webviewurlhandlerdemo

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.HttpURLConnection
import java.net.URL

class SplitHeaders(headers: HeaderCollection) {

    // The URLConnection appears to allow multiple header values, whereas the WebResourceResponse
    // expects a single string. So we'll flatten them with commas.
    val remainingHeaders = headers.mapValues { list -> list.value.joinToString(",") }.toMutableMap()

    // For whatever reason, WebResourceResponse specifies Content-Type and Content-Encoding
    // manually, then whatever other headers you have. So if we don't remove these headers
    // it ends up sending them twice.
    val type = remainingHeaders.remove("Content-Type") ?: ""
    val encoding = remainingHeaders.remove("Content-Encoding") ?: ""

}

/* The WebViewClient that will route requests through our JS environment */
class WebViewCacheClient(private val cache: Caches) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {

        // This doesn't do anything like handling POST requests, though that is possible

        if (view == null || request == null || request.url == null) {
            // Not clear how this could happen, but they're all optional, so...
            return null
        }

        if (request.url.scheme != "nyt") {
            // If we're loading a non-NYT protocol URL then skip this process entirely. Android
            // can still intercept these requests, but iOS cannot, so for platform consistency
            // we won't do it here either.
            return null
        }

        // Take our nyt:// URL and transform it into an https:// URL. In theory you'd switch between
        // HTTP and HTTPS here somehow, but why not forget HTTPS entirely at this point?
        val modifiedURL = URL(request.url.buildUpon().scheme("https").build().toString())

        Log.d("Cache", "Received a request for $modifiedURL")

        val cacheMatch = this.cache.match(modifiedURL.toString())

        if (cacheMatch != null) {

            Log.d("Cache", "Found a cache match for $modifiedURL")

            val headers = SplitHeaders(cacheMatch.headers)

            return WebResourceResponse(
                    headers.type,
                    headers.encoding,
                    cacheMatch.status,
                    cacheMatch.statusMessage,
                    headers.remainingHeaders,
                    cacheMatch.body.inputStream()
            )

        }

        Log.d("Cache", "No cache match for ${modifiedURL.toString()}, going over the wire")

        val connection = modifiedURL.openConnection() as HttpURLConnection

        // Copy the HTTP headers over from the browser request to our native one. In a real version
        // we'd want to do some filtering here to make sure Origin headers and the like have the right
        // protocol.

        for (key in request.requestHeaders.keys) {
            connection.setRequestProperty(key, request.requestHeaders[key])
        }

        val headers = SplitHeaders(connection.headerFields)

        return WebResourceResponse(
                headers.type,
                headers.encoding,
                connection.responseCode,
                connection.responseMessage,
                headers.remainingHeaders,
                connection.inputStream
        )

    }

}