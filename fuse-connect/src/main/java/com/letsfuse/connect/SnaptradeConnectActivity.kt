package com.letsfuse.connect

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mazenrashed.logdnaandroidclient.LogDna.log

class SnaptradeConnectActivity : Activity() {

    inner class PostMessageClass {
        @JavascriptInterface
        fun postMessage(message: String) {
            Log.d("WebView", "postMessage called: $message")

            message.let {
                when {
                    it.contains("SUCCESS") -> {
                        val components = it.split(":")
                        val authorizationId = components.getOrNull(1)?.trim()?.ifBlank { null }
                        onSuccess!!(authorizationId ?: "")
                        finish()
                    }
                    it.contains("ABANDONED") -> {
                        onExit!!()
                        finish()
                    }
                    it.contains("ERROR") -> {
                        val components = it.split(":")
                        if (components.size > 1) {
                            // handle error message
                        } else {
                            // handle error message with no component
                        }
                    }
                }
            }
        }
    }
    companion object {
        const val CLASS_LOG_TAG_NAME = "SnaptradeConnect"
        var redirectUri: String? = null
        var onSuccess: ((String) -> Unit)? = null
        var onExit: (() -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        redirectUri = intent.getStringExtra("redirectUri")

        setContentView(R.layout.activity_fuse_connect)

        val webView = findViewById<WebView>(R.id.webview)
        webView.addJavascriptInterface(PostMessageClass(), "Android")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.loadUrl(
                    """
            javascript:(function() {
                window.addEventListener('message', function(event) {
                    Android.postMessage(event.data);
                });
            })();
            """
                )
            }
        }

        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true


        Log.i(CLASS_LOG_TAG_NAME, "finished loading")
        webView.loadUrl(redirectUri!!)
        Log.i(CLASS_LOG_TAG_NAME, "started loading")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }
}


