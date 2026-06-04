package com.example.noreel

import java.net.URI
import java.security.MessageDigest
import java.util.Locale

object WebViewSecurityPolicy {
    const val HOME_URL = "https://www.instagram.com/"
    const val ERROR_PAGE_URL = "file:///android_asset/error.html"
    const val REMOTE_INJECTOR_URL =
        "https://raw.githubusercontent.com/Kalbra/NoReel/master/app/src/main/assets/Injector.js"
    const val REMOTE_INJECTOR_SHA256 =
        "0cf7d6c3863940fb1262e04e8215536da34c018282ed50d0f09034096238eeac"

    private val trustedInstagramHosts = setOf("www.instagram.com", "instagram.com")
    private val externalBrowserSchemes = setOf("http", "https")
    private val allowedWebViewPermissionResources = emptySet<String>()

    fun canNavigateInWebView(url: String?): Boolean {
        return isTrustedInstagramUrl(url) || isTrustedAssetUrl(url) || isAboutBlank(url)
    }

    fun canInjectJavaScript(url: String?): Boolean {
        return isTrustedInstagramUrl(url)
    }

    fun canUseJavascriptBridge(url: String?): Boolean {
        return isTrustedInstagramUrl(url) || isTrustedAssetUrl(url)
    }

    fun allowedPermissionResources(origin: String?, resources: Array<String>?): Array<String> {
        if (!isTrustedInstagramUrl(origin) || resources == null) {
            return emptyArray()
        }

        return resources
            .filter { allowedWebViewPermissionResources.contains(it) }
            .toTypedArray()
    }

    fun canOpenExternalBrowserUrl(url: String?): Boolean {
        val uri = parseUri(url) ?: return false
        val scheme = uri.scheme?.lowercase(Locale.US) ?: return false
        return scheme in externalBrowserSchemes && !uri.host.isNullOrBlank()
    }

    fun isAllowedRemoteInjectorUrl(url: String?): Boolean {
        val uri = parseUri(url) ?: return false
        return uri.scheme?.lowercase(Locale.US) == "https" &&
            uri.host?.lowercase(Locale.US) == "raw.githubusercontent.com" &&
            uri.rawPath == "/Kalbra/NoReel/master/app/src/main/assets/Injector.js" &&
            uri.rawQuery == null &&
            uri.rawFragment == null
    }

    fun hasExpectedRemoteInjectorHash(bytes: ByteArray): Boolean {
        return sha256Hex(bytes) == REMOTE_INJECTOR_SHA256
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun isTrustedInstagramUrl(url: String?): Boolean {
        val uri = parseUri(url) ?: return false
        return uri.scheme?.lowercase(Locale.US) == "https" &&
            uri.host?.lowercase(Locale.US) in trustedInstagramHosts
    }

    private fun isTrustedAssetUrl(url: String?): Boolean {
        val uri = parseUri(url) ?: return false
        return uri.scheme?.lowercase(Locale.US) == "file" &&
            uri.host.isNullOrBlank() &&
            uri.path == "/android_asset/error.html"
    }

    private fun isAboutBlank(url: String?): Boolean {
        val uri = parseUri(url) ?: return false
        return uri.scheme?.lowercase(Locale.US) == "about" && uri.schemeSpecificPart == "blank"
    }

    private fun parseUri(url: String?): URI? {
        if (url.isNullOrBlank()) {
            return null
        }

        return try {
            URI(url.trim())
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
