package com.suek.tech

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        WebView.setWebContentsDebuggingEnabled(true)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isHapticFeedbackEnabled = true
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true

                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val url = request.url.toString()

                    if (url.startsWith("whatsapp://") || url.contains("api.whatsapp.com") || url.contains("wa.me")) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
                            return true
                        }
                    }

                    if (url.startsWith("mailto:")) {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                            startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Aplikasi Mail Client tidak ditemukan", Toast.LENGTH_SHORT).show()
                            return true
                        }
                    }

                    if (url.contains("facebook.com/sharer") || 
                        url.contains("twitter.com/intent") || 
                        url.contains("x.com/intent") || 
                        url.contains("linkedin.com/shareArticle") ||
                        url.contains("linkedin.com/sharing")) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (url.startsWith("intent://")) {
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            if (intent != null) {
                                if (intent.resolveActivity(packageManager) != null) {
                                    startActivity(intent)
                                    return true
                                }
                                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                if (fallbackUrl != null) {
                                    view.loadUrl(fallbackUrl)
                                    return true
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return true
                    }

                    if (url.contains("play.google.com/store/apps/details")) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            // Fallback handler OS
                        }
                    }

                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Aplikasi native tidak merespon", Toast.LENGTH_SHORT).show()
                        }
                        return true
                    }

                    return false
                }
            }

            webViewClient = webViewClient

            webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    result.confirm()
                    return true
                }
            }

            addJavascriptInterface(AndroidWebAppInterface(this@MainActivity), "AndroidBridge")
            loadUrl("https://appassets.androidplatform.net/assets/index.html")
        }

        setContentView(webView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    inner class AndroidWebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun postMessage(message: String) {
            try {
                val json = JSONObject(message)
                val action = json.getString("action")
                
                runOnUiThread {
                    when (action) {
                        "TRIGGER_HAPTIC" -> triggerHaptic()
                        "SHOW_TOAST" -> Toast.makeText(context, json.optString("data"), Toast.LENGTH_SHORT).show()
                        "EXIT_APP" -> finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun triggerHaptic() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(
                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
    }
}