package com.example.noreel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class WebViewViewport(
    private val context: Context,
    private val onUrlChanged: (String?) -> Unit = {}
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString()
        if (request != null && !request.isForMainFrame) {
            return !WebViewSecurityPolicy.canNavigateInWebView(url)
        }

        return shouldBlockNavigation(url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return shouldBlockNavigation(url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onUrlChanged(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        //injectJS(view)
        super.onPageFinished(view, url)
        onUrlChanged(url)
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        onUrlChanged(url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        // Not working on Pixel3a API34 extension level 7 on slow internet
        if ((request == null || request.isForMainFrame) && error?.errorCode == ERROR_HOST_LOOKUP) {
            Log.e("WebInternal", error.description.toString() + error.errorCode)

            view?.loadUrl(WebViewSecurityPolicy.ERROR_PAGE_URL)


        } else {
            super.onReceivedError(view, request, error)
        }
    }

    private fun shouldBlockNavigation(url: String?): Boolean {
        if (WebViewSecurityPolicy.canNavigateInWebView(url)) {
            return false
        }

        if (WebViewSecurityPolicy.canOpenExternalBrowserUrl(url)) {
            openExternalBrowser(url!!)
        } else {
            Log.w("WebInternal", "Blocked WebView navigation: $url")
        }

        return true
    }

    private fun openExternalBrowser(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
            context.startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w("WebInternal", "No browser available for $url", e)
        }
    }
}
