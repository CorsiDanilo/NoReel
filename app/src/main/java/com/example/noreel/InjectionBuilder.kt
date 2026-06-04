package com.example.noreel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class InjectionBuilder(
    private val application: Application,
    private val preferences: SharedPreferences
) {
    private fun fetchRemote(callback: (String?) -> Unit) {
        Thread {
            var connection: HttpURLConnection? = null
            var remoteCode: String? = null

            try {
                if (!WebViewSecurityPolicy.isAllowedRemoteInjectorUrl(WebViewSecurityPolicy.REMOTE_INJECTOR_URL)) {
                    Log.e("InjectionBuilder", "Remote injector URL is not allowed")
                    return@Thread
                }

                val url = URL(WebViewSecurityPolicy.REMOTE_INJECTOR_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 1000
                connection.readTimeout = 1000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val scriptBytes = BufferedInputStream(connection.inputStream).use { it.readBytes() }

                    if (WebViewSecurityPolicy.hasExpectedRemoteInjectorHash(scriptBytes)) {
                        remoteCode = reader(ByteArrayInputStream(scriptBytes))
                    } else {
                        Log.e("InjectionBuilder", "Remote injector hash mismatch")
                    }
                } else {
                    Log.e("InjectionBuilder", "Error response code: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("InjectionBuilder", "Error fetching JavaScript file: ${e.message}")
            } finally {
                connection?.disconnect()
                callback(remoteCode)
            }
        }.start()
    }

    fun fetchLocal(callback: (String) -> Unit) {
        this.application.assets.open("Injector.js").use {
            callback(reader(it))
        }
    }

    fun getCode(callback: (String) -> Unit) {
        if (isRemoteFetchingEnabled()) {
            fetchRemote {
                if (it != null) {
                    callback(it)
                } else {
                    Log.w("InjectionBuilder", "Falling back to local script")
                    fetchLocal(callback)
                }
            }
            Log.d("InjectionBuilder", "Use remote script")
        } else { // local
            fetchLocal { callback(it) }
            Log.d("InjectionBuilder", "Use local script")
        }
    }

    private fun isRemoteFetchingEnabled(): Boolean {
        return try {
            preferences.getBoolean("remote_fetching", false)
        } catch (e: ClassCastException) {
            Log.w("InjectionBuilder", "Invalid 'remote_fetching' preference type")
            false
        }
    }

    private fun reader(input_stream: InputStream): String {
        val reader = input_stream.bufferedReader()
        reader.useLines {
            reader.use {
                var add_future_lines = false
                var injector_string = ""
                for (raw_line in it.lines().toArray()) {
                    val line: String = raw_line.toString()

                    if (line.startsWith("/**")) {
                        //Using the space to split the element and to find out its identifier e.g REEL_FEED
                        val identifier = line.split(" ").toTypedArray()[1]

                        // No lines till the next identifier
                        if (identifier == "END") {
                            add_future_lines = false
                        }

                        // No checking for preference rules at ALWAYS_EXECUTE
                        else if (identifier == "ALWAYS_EXECUTE") {
                            add_future_lines = true
                        }

                        // Settings Identifier was found
                        else {
                            try {
                                val state = preferences.all?.getValue(identifier) as Boolean
                                // On true execute
                                if (identifier == "use_followed_feed" || identifier == "audio_on") {
                                    if (state) {
                                        add_future_lines = true
                                    }
                                }
                                // On false execute
                                else {
                                    if (!state) {
                                        add_future_lines = true
                                    }
                                }
                            } catch (e: NoSuchElementException) {
                                Log.w("InjectionBuilder", "Preference '${identifier}' not found")
                            }
                        }
                    }
                    // Normal content to be added
                    else {
                        // Checking if this content line (no identifier) should be added
                        if (add_future_lines && line != "") {
                            injector_string += line.trim()
                        }
                    }
                }
                Log.d("InjectionBuilder", injector_string)
                return injector_string
            }
        }
    }
}
