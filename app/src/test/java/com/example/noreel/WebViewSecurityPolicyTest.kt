package com.example.noreel

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WebViewSecurityPolicyTest {
    @Test
    fun webViewNavigationIsLimitedToInstagramAssetsAndBlankPage() {
        assertTrue(WebViewSecurityPolicy.canNavigateInWebView("https://www.instagram.com/"))
        assertTrue(WebViewSecurityPolicy.canNavigateInWebView("https://instagram.com/accounts/login/"))
        assertTrue(WebViewSecurityPolicy.canNavigateInWebView(WebViewSecurityPolicy.ERROR_PAGE_URL))
        assertTrue(WebViewSecurityPolicy.canNavigateInWebView("about:blank"))

        assertFalse(WebViewSecurityPolicy.canNavigateInWebView("http://www.instagram.com/"))
        assertFalse(WebViewSecurityPolicy.canNavigateInWebView("https://www.instagram.com.evil.test/"))
        assertFalse(WebViewSecurityPolicy.canNavigateInWebView("file:///sdcard/Injector.js"))
        assertFalse(WebViewSecurityPolicy.canNavigateInWebView("intent://open#Intent;scheme=https;end"))
    }

    @Test
    fun javascriptInjectionOnlyRunsOnInstagramPages() {
        assertTrue(WebViewSecurityPolicy.canInjectJavaScript("https://www.instagram.com/reels/"))
        assertTrue(WebViewSecurityPolicy.canInjectJavaScript("https://instagram.com/"))

        assertFalse(WebViewSecurityPolicy.canInjectJavaScript(WebViewSecurityPolicy.ERROR_PAGE_URL))
        assertFalse(WebViewSecurityPolicy.canInjectJavaScript("https://raw.githubusercontent.com/Kalbra/NoReel/master/app/src/main/assets/Injector.js"))
        assertFalse(WebViewSecurityPolicy.canInjectJavaScript("https://example.com/"))
    }

    @Test
    fun bridgeAndExternalBrowserUrlsRejectUnexpectedSchemes() {
        assertTrue(WebViewSecurityPolicy.canUseJavascriptBridge("https://www.instagram.com/"))
        assertTrue(WebViewSecurityPolicy.canUseJavascriptBridge(WebViewSecurityPolicy.ERROR_PAGE_URL))
        assertFalse(WebViewSecurityPolicy.canUseJavascriptBridge("https://example.com/"))

        assertTrue(WebViewSecurityPolicy.canOpenExternalBrowserUrl("https://example.com/path"))
        assertTrue(WebViewSecurityPolicy.canOpenExternalBrowserUrl("http://example.com/path"))

        assertFalse(WebViewSecurityPolicy.canOpenExternalBrowserUrl("javascript:alert(1)"))
        assertFalse(WebViewSecurityPolicy.canOpenExternalBrowserUrl("file:///sdcard/private.txt"))
        assertFalse(WebViewSecurityPolicy.canOpenExternalBrowserUrl("intent://open#Intent;scheme=https;end"))
    }

    @Test
    fun webViewPermissionRequestsHaveNoAllowedResources() {
        val requested = arrayOf(
            "android.webkit.resource.VIDEO_CAPTURE",
            "android.webkit.resource.AUDIO_CAPTURE"
        )

        assertArrayEquals(
            emptyArray<String>(),
            WebViewSecurityPolicy.allowedPermissionResources("https://www.instagram.com/", requested)
        )
        assertArrayEquals(
            emptyArray<String>(),
            WebViewSecurityPolicy.allowedPermissionResources("https://example.com/", requested)
        )
    }

    @Test
    fun remoteInjectorRequiresExactUrlAndPinnedHash() {
        val bundledInjector = projectFile("src/main/assets/Injector.js")
            .readBytes()
            .normalizedUtf8Bytes()

        assertTrue(WebViewSecurityPolicy.isAllowedRemoteInjectorUrl(WebViewSecurityPolicy.REMOTE_INJECTOR_URL))
        assertFalse(WebViewSecurityPolicy.isAllowedRemoteInjectorUrl("${WebViewSecurityPolicy.REMOTE_INJECTOR_URL}?cache=1"))
        assertFalse(WebViewSecurityPolicy.isAllowedRemoteInjectorUrl("https://raw.githubusercontent.com/Other/NoReel/master/app/src/main/assets/Injector.js"))

        assertEquals(WebViewSecurityPolicy.REMOTE_INJECTOR_SHA256, WebViewSecurityPolicy.sha256Hex(bundledInjector))
        assertTrue(WebViewSecurityPolicy.hasExpectedRemoteInjectorHash(bundledInjector))
        assertFalse(WebViewSecurityPolicy.hasExpectedRemoteInjectorHash("alert(1)".toByteArray()))
    }

    @Test
    fun manifestHardeningDoesNotRegress() {
        val manifest = projectFile("src/main/AndroidManifest.xml").readText()

        assertFalse(manifest.contains("android:debuggable"))
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertFalse(manifest.contains("androidx.core.content.FileProvider"))
        assertFalse(manifest.contains("READ_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("WRITE_EXTERNAL_STORAGE"))
        assertFalse(manifest.contains("ACCESS_FINE_LOCATION"))
        assertFalse(manifest.contains("ACCESS_COARSE_LOCATION"))
        assertFalse(manifest.contains("android.permission.CAMERA"))
        assertFalse(manifest.contains("android.permission.FLASHLIGHT"))
        assertFalse(manifest.contains("android.permission.REQUEST_INSTALL_PACKAGES"))
    }

    @Test
    fun backupRulesAndRemoteDefaultAreDenyByDefault() {
        val preferences = projectFile("src/main/res/xml/preferences.xml").readText()
        val backupRules = projectFile("src/main/res/xml/backup_rules.xml").readText()
        val dataExtractionRules = projectFile("src/main/res/xml/data_extraction_rules.xml").readText()

        assertTrue(preferences.contains("android:key=\"remote_fetching\""))
        assertTrue(preferences.contains("android:defaultValue=\"false\""))
        assertTrue(backupRules.contains("<exclude domain=\"sharedpref\" path=\".\" />"))
        assertTrue(dataExtractionRules.contains("<device-transfer>"))
        assertTrue(dataExtractionRules.contains("<exclude domain=\"sharedpref\" path=\".\" />"))
    }

    private fun projectFile(relativeToApp: String): File {
        var directory = File(System.getProperty("user.dir"))
        repeat(6) {
            val direct = File(directory, relativeToApp)
            if (direct.exists()) {
                return direct
            }

            val fromRoot = File(directory, "app/$relativeToApp")
            if (fromRoot.exists()) {
                return fromRoot
            }

            directory = directory.parentFile ?: return@repeat
        }

        error("Could not locate $relativeToApp from ${System.getProperty("user.dir")}")
    }

    private fun ByteArray.normalizedUtf8Bytes(): ByteArray {
        return toString(Charsets.UTF_8)
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .toByteArray(Charsets.UTF_8)
    }
}
