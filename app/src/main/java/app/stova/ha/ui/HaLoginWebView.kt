package app.stova.ha.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.stova.ha.*
import java.security.SecureRandom
import java.util.Base64

class HaLoginClient(private val expectedState: String, private val onCode: (String) -> Unit) : WebViewClient() {
    private var handled = false
    private fun intercept(url: String): Boolean {
        val code = parseCallback(url, expectedState) ?: return false
        if (!handled) { handled = true; onCode(code) }
        return true
    }
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = intercept(request.url.toString())
    @Deprecated("WebView compatibility callback")
    override fun shouldOverrideUrlLoading(view: WebView, url: String) = intercept(url)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HaLoginWebView(address: HaAddress, onCode: (String) -> Unit, onCancel: () -> Unit) {
    val state = remember { Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(16).also { SecureRandom().nextBytes(it) }) }
    val currentCode by rememberUpdatedState(onCode)
    BackHandler(onBack = onCancel)
    AndroidView(
        factory = { context -> WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = HaLoginClient(state) { currentCode(it) }
            loadUrl(authorizeUrl(address, state))
        } },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.stopLoading(); it.destroy(); CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush() },
    )
}
