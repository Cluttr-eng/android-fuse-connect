package com.letsfuse.connect

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.plaid.link.Plaid
import com.plaid.link.PlaidHandler
import com.plaid.link.linkTokenConfiguration
import com.plaid.link.result.LinkResultHandler
import java.nio.charset.StandardCharsets
import java.util.*

data class Exit(
    val err: ConnectError?,
    val metadata: Map<String, Any>?
)

data class ConnectError(
    val errorCode: String?,
    val errorType: String?,
    val displayMessage: String?,
    val errorMessage: String?
)

class FuseConnectActivity : Activity() {
    companion object {
        var WEB_VIEW_BASE_URL = "https://connect.letsfuse.com"
        const val SECRET_CONFIGURATION_EXTRA = "secret_configuration_extra"
        const val CLASS_LOG_TAG_NAME = "FuseConnectActivity"
        const val EVENT_EXTRA = "EventExtra"
        var clientSecret: String? = null
        var lastConnectError: ConnectError? = null

        var onInstitutionSelected: ((String, callback: (String) -> Unit) -> Unit)? = null
        var onSuccess: ((String) -> Unit)? = null
        var onExit: ((Exit) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("Testing logs 123");
        clientSecret = intent.getStringExtra("clientSecret")

        val secretConfiguration =
            @Suppress("DEPRECATION") intent.getParcelableExtra<SecretConfiguration>(
                SECRET_CONFIGURATION_EXTRA
            )



        if (secretConfiguration != null) {
            Log.i(CLASS_LOG_TAG_NAME, secretConfiguration.clientSecret)
        }

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

        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true


        Log.i(CLASS_LOG_TAG_NAME, "finished loading")
        webView.loadUrl("$WEB_VIEW_BASE_URL/intro?client_secret=$clientSecret&webview=true")
        Log.i(CLASS_LOG_TAG_NAME, "started loading")
    }

    private fun handleUri(uri: Uri): Boolean {

        if (uri.scheme == "fuse") {
            Log.i(CLASS_LOG_TAG_NAME, uri.toString())
            val eventName = uri.getQueryParameter("event_name")!!
            Log.i(CLASS_LOG_TAG_NAME, "Event name $eventName")

            processWebViewEvent(eventName = eventName, uri = uri)

            // Handle URL scheme here
            return true
        }
        return false
    }

    private fun processWebViewEvent(eventName: String, uri: Uri) {
        when (eventName) {
            "ON_SUCCESS" -> {
                onSuccess!!(uri.getQueryParameter("public_token")!!)
                finish()
            }
            "ON_INSTITUTION_SELECTED" -> {
                Log.i(CLASS_LOG_TAG_NAME, "Sending ON_INSTITUTION_SELECTED")
                onInstitutionSelected!!(uri.getQueryParameter("institution_id")!!) { linkToken ->

                    val webView = findViewById<WebView>(R.id.webview)
                    webView.loadUrl("$WEB_VIEW_BASE_URL/bank-link?link_token=$linkToken")
                }
            }
            "OPEN_PLAID" -> {
                openPlaid(uri.getQueryParameter("plaid_link_token")!!)
            }
            "OPEN_SNAPTRADE" -> {
                Log.d(CLASS_LOG_TAG_NAME, "OPEN_SNAPTRADE ${uri.getQueryParameter("redirect_uri")}")
                openSnaptrade(uri.getQueryParameter("redirect_uri")!!)
            }
            "ON_EXIT" -> {
                if (lastConnectError != null) {
                    onExit?.invoke(Exit(lastConnectError, null))
                } else {
                    val error = uri.getQueryParameter("error")
                    if (error == null) {
                        onExit?.invoke(Exit(null, null))
                    } else {
                        val errorType = uri.getQueryParameter("error_type")
                        val errorMessage = uri.getQueryParameter("error_message")
                        val connectError = ConnectError(error, errorType, null, errorMessage)
                        val exit = Exit(connectError, null)
                        onExit?.invoke(exit)
                    }
                }
                finish()
            }
        }
    }


    private val linkResultHandler = LinkResultHandler(
        onSuccess = { linkSuccess ->
            val jsonString = """
                {
                  "session_client_secret": "${clientSecret!!}",
                  "data": {
                    "type": "plaid",
                    "public_token": "${linkSuccess.publicToken}"
                  }
                }
            """.trimIndent()
            Log.d(CLASS_LOG_TAG_NAME, "jsonString $jsonString")

            val jsonStringByte = jsonString.toByteArray(StandardCharsets.UTF_8)
            Log.d(CLASS_LOG_TAG_NAME, "jsonStringBye $jsonStringByte")

            val encodedJson = Base64.encodeToString(jsonStringByte, Base64.DEFAULT)
            Log.d(CLASS_LOG_TAG_NAME, "Encoded json $encodedJson")

            onSuccess!!(encodedJson)

            finish()
        },
        onExit = { linkExit ->
            val error = linkExit.error
            error?.let { err ->
                val errorCode = err.errorCode
                val errorMessage = err.errorMessage
                val displayMessage = err.displayMessage
                lastConnectError = ConnectError(errorCode.json, null, displayMessage, errorMessage)
            }
        }
    )

    private fun openPlaid(linkToken: String) {
        val linkTokenConfiguration = linkTokenConfiguration {
            token = linkToken
        }

        val plaidHandler: PlaidHandler = Plaid.create(application, linkTokenConfiguration)

        plaidHandler.open(this)
    }

    private fun openSnaptrade(redirectUri: String) {
        val intent = Intent(this, SnaptradeConnectActivity::class.java)
        intent.putExtra("redirectUri", redirectUri)
        this.startActivityForResult(intent, 928)

        SnaptradeConnectActivity.onSuccess = { authorizationId ->
            val jsonString = """
                {
                  "session_client_secret": "${clientSecret!!}",
                  "data": {
                    "type": "snaptrade",
                    "brokerage_authorization_id": "$authorizationId"
                  }
                }
            """.trimIndent()
            Log.d(CLASS_LOG_TAG_NAME, "jsonString $jsonString")

            val jsonStringByte = jsonString.toByteArray(StandardCharsets.UTF_8)
            Log.d(CLASS_LOG_TAG_NAME, "jsonStringBye $jsonStringByte")

            val encodedJson = Base64.encodeToString(jsonStringByte, Base64.DEFAULT)
            Log.d(CLASS_LOG_TAG_NAME, "Encoded json $encodedJson")

            onSuccess!!(encodedJson)

            finish()
        }

        SnaptradeConnectActivity.onExit = {

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!linkResultHandler.onActivityResult(requestCode, resultCode, data)) {
            // Not handled by the LinkResultHandler
        }
    }
}


