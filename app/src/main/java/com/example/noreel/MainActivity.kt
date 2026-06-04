package com.example.noreel

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import src.UpdateChecker


open class MainActivity : ComponentActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var settingsChanged: Boolean = false

    @Volatile
    private var currentUrl: String? = WebViewSecurityPolicy.HOME_URL

    private lateinit var webView: WebView

    var injector_content = ""

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    fun updateBrowser(webView: WebView) {
        currentUrl = WebViewSecurityPolicy.HOME_URL
        webView.loadUrl(WebViewSecurityPolicy.HOME_URL)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, p1: String?) {
        Log.d("Settings", "Changed: ${preferences?.all?.entries?.toTypedArray().contentToString()}")
        if (preferences == null) {
            return
        }

        InjectionBuilder(application, preferences).getCode() {
            injector_content = it
        }
    }

    override fun onResume() {
        super.onResume()
        if (settingsChanged) {
            settingsChanged = false
            updateBrowser(webView)
            Log.d("Settings", "Reload browser")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.instagram_webview)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)

        val preferences_button = findViewById<Button>(R.id.preferencesButton)

        preferences_button.setOnClickListener {
            settingsChanged = true
            val settings_intent = Intent(this, SettingsActivity::class.java)
            startActivity(settings_intent)
        }
        onSharedPreferenceChanged(preferences, "")

        requestNotificationPermissionIfNeeded()
        createNotificationChannel()

        val updateChecker = UpdateChecker(this)
        updateChecker.fetchRemote()

        webView = findViewById(R.id.webview)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setAllowContentAccess(false)
            setAllowFileAccess(false)
            setAllowFileAccessFromFileURLs(false)
            setAllowUniversalAccessFromFileURLs(false)
            setGeolocationEnabled(false)
        }
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.isVerticalScrollBarEnabled = false

        val JSInterface = AndroidJSInterface(
            preferences_button,
            this,
            Runnable { updateBrowser(webView) },
            { currentUrl }
        )
        webView.addJavascriptInterface(JSInterface, "Android")

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webView.webChromeClient = ChromeViewport()

        fun injectJS(webview: WebView?) {
            val target = webview ?: return
            if (WebViewSecurityPolicy.canInjectJavaScript(target.url) && injector_content.isNotBlank()) {
                target.evaluateJavascript("(function f(){${injector_content}})()", null)
            }
        }

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(
            object : Runnable {
                override fun run() {
                    injectJS(webView)
                    mainHandler.postDelayed(this, 200)
                }
            })

        webView.webViewClient = WebViewViewport(this) { url ->
            currentUrl = url
        }

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    }
                }
            })

        updateBrowser(webView)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val name = "Updates"
        val descriptionText = "Information if there is an update available"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("Update", name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
