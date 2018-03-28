package com.example.alastair.webviewurlhandlerdemo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.webkit.WebView
import kotlinx.coroutines.experimental.async
import kotlin.concurrent.thread

val TEST_IMAGE_1 = "//static01.nyt.com/images/2018/01/20/business/20UP-CENSUS-2/20UP-CENSUS-2-superJumbo.jpg?quality=75&auto=webp"
var TEST_IMAGE_2 = "//static01.nyt.com/images/2018/01/20/business/20UP-CENSUS/merlin_30316030_f827059a-5206-4926-ad06-847aaf59c028-jumbo.jpg?quality=75&auto=webp"

fun putDemoFileInCache(cache: Caches) {

    var headers = mutableMapOf<String, List<String>>()
    headers["Content-Type"] = listOf("text/html")

    cache.put("https://www.example.com/", 200,"OK", headers, """
        <html>
            <head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
            <body>
            <h1>Offline/online mix test</h1>
            <img src="$TEST_IMAGE_1" style="width:100%"/>
            <img src="$TEST_IMAGE_2" style="width:100%"/>
            </body>
        </html>
        """.toByteArray())


}

class TestWebView : AppCompatActivity() {

    var webview: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WebView.setWebContentsDebuggingEnabled(true)
        val createdWebView = WebView(this.baseContext)
        val layoutParams: LayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        val caches = Caches()
        createdWebView.webViewClient = WebViewCacheClient(caches)


        this.addContentView(createdWebView, layoutParams)
        this.webview = createdWebView

        async {

            caches.add("https:" + TEST_IMAGE_1)
            putDemoFileInCache(caches)
            runOnUiThread {
                createdWebView.loadUrl("nyt://www.example.com/")
            }
        }

        super.onCreate(savedInstanceState)
    }
}
