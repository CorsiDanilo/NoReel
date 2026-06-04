package com.example.noreel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Callable
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Button

class AndroidJSInterface(
    private val preference_button: Button,
    private val mContext: Context,
    private val updateViewport: Runnable,
    private val currentUrlProvider: () -> String?
) {
    private val alreadyUsedUrls = mutableSetOf<String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun log(msg: String) {
        if (!isBridgeAllowed("log")) {
            return
        }

        Log.d("WebInternal", msg)
    }

    @JavascriptInterface
    fun setSettingsMenuButton() {
        if (!isBridgeAllowed("setSettingsMenuButton")) {
            return
        }

        mainHandler.post(Runnable { preference_button.visibility = View.VISIBLE })
    }

    @JavascriptInterface
    fun deleteSettingsMenuButton() {
        if (!isBridgeAllowed("deleteSettingsMenuButton")) {
            return
        }

        mainHandler.post(Runnable { preference_button.visibility = View.GONE })
    }

    @JavascriptInterface
    fun openInStdBrowser(url: String){
        if (!isBridgeAllowed("openInStdBrowser")) {
            return
        }

        if (!WebViewSecurityPolicy.canOpenExternalBrowserUrl(url)) {
            Log.w("StdBrowserRequest", "Blocked URL: $url")
            return
        }

        //If element was not redirected before
        if(alreadyUsedUrls.add(url)){
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
            mContext.startActivity(browserIntent)

            Log.d("StdBrowserRequest", url)
        }
    }

    @JavascriptInterface
    fun reloadPage(){
        if (!isBridgeAllowed("reloadPage")) {
            return
        }

        mainHandler.post(updateViewport)
        Log.d("WebInternal", "Reloading triggered")
    }

    private fun isBridgeAllowed(method: String): Boolean {
        val currentUrl = currentUrlProvider()
        if (WebViewSecurityPolicy.canUseJavascriptBridge(currentUrl)) {
            return true
        }

        Log.w("WebInternal", "Blocked bridge method $method from $currentUrl")
        return false
    }
}
