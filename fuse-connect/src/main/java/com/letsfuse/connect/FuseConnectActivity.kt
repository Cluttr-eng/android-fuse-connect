package com.letsfuse.connect
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class FuseConnectActivity : AppCompatActivity() {
    companion object {
        const val WEB_VIEW_BASE_URL = "https://shoreditch-indol.vercel.app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fuse_connect)

        val webView = findViewById<WebView>(R.id.webview)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return if (request != null) {
                    handleUri(request.url)
                } else {
                    true
                }
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUri(Uri.parse(url))
            }
        }


        webView.loadUrl("$WEB_VIEW_BASE_URL/")
    }

    private fun handleUri( uri: Uri): Boolean {
        if (uri.scheme == "fuse:") {
            // Handle URL scheme here
            return true
        }
        return false
    }
}
