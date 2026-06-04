package com.example.noreel

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient

class ChromeViewport : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        Log.d(
            "WebInternal",
            "${consoleMessage.message()} -- Line: ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}"
        )
        return true
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        val allowedResources = WebViewSecurityPolicy.allowedPermissionResources(
            request?.origin?.toString(),
            request?.resources
        )

        if (allowedResources.isEmpty()) {
            Log.w("WebInternal", "Denied permission request from ${request?.origin}")
            request?.deny()
            return
        }

        Log.d("WebInternal", "Granted WebView resources: ${allowedResources.contentToString()}")
        request?.grant(allowedResources)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        Log.w("WebInternal", "Denied geolocation request from $origin")
        callback?.invoke(origin, false, false)
    }
}
