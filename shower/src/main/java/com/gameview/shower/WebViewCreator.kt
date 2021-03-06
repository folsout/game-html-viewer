package com.gameview.shower

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebBackForwardList
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.LinkedList
import java.util.Objects

class WebViewCreator(private val chooserHelper: ChooserHelper) {

    val backPage = LinkedList<String>()
    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var progressBar: ProgressBar
    private val OFFER_ID = "offer_id"
    private lateinit var script: String
    private var doubleSaveUrl = false
    private var isBack = false

    fun createWebView(context: Context, progressBar: ProgressBar): WebView {
        webView = WebView(context)
        this.progressBar = progressBar
        setSettingWebView(context)
        createWebViewClient()
        createWebViewChromeClient()
        return webView
    }

    fun setSharedPreferences(sharedPreferences: SharedPreferences) {
        this.sharedPreferences = sharedPreferences
        sharedPreferencesEditor = sharedPreferences.edit()
    }

    fun setScript(script: String) {
        this.script = script
    }

    private fun setSettingWebView(context: Context) {
        webView.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        webView.fitsSystemWindows = true
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setAppCacheEnabled(true)
        webView.settings.allowContentAccess = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.settings.setGeolocationEnabled(true)
        webView.settings.setAppCachePath(context.cacheDir.absolutePath)
        webView.settings.useWideViewPort = true
        webView.settings.blockNetworkImage = false
        webView.settings.allowFileAccess = true
        webView.settings.pluginState = WebSettings.PluginState.ON
        webView.settings.loadsImagesAutomatically = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webView.settings.mediaPlaybackRequiresUserGesture = false
        }
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }

    }

    private fun createWebViewChromeClient() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.isVisible = newProgress < 70
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (chooserHelper.uploadMessage != null) {
                    chooserHelper.uploadMessage?.onReceiveValue(null)
                    chooserHelper.uploadMessage = null
                }
                chooserHelper.uploadMessage = filePathCallback
                val intent = fileChooserParams.createIntent()
                try {
                    chooserHelper.startActivityForResult(intent, ChooserHelper.REQUEST_SELECT)
                    (chooserHelper as Activity).overridePendingTransition(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                } catch (e: ActivityNotFoundException) {
                    chooserHelper.uploadMessage = null
                    return false
                }
                return true
            }
        }
    }

    private fun createWebViewClient() {

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {

                Uri.parse(url).getQueryParameter("cust_offer_id")
                    ?.let {
                        sharedPreferencesEditor.putString(OFFER_ID, it).apply()
                    }
                url?.let {
                    view?.loadUrl(it)
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().flush()
                }
                val offId = sharedPreferences.getString(OFFER_ID, "")
                webView.evaluateJavascript(script) {
                    webView.evaluateJavascript("mainContextFunc('" + offId + "');") {}
                }
            }
        }
    }

    fun webViewCreatorRestoreState(context: Context): Boolean {
        val stackPath = File(context.externalCacheDir, "webstack")
        if (stackPath.exists()) {
            val parcel = Parcel.obtain()
            val data = stackPath.readBytes()
            parcel.unmarshall(data, 0, data.size)
            parcel.setDataPosition(0)
            val bundle = Bundle()
            bundle.readFromParcel(parcel)
            parcel.recycle()
            webView.restoreState(bundle)
            return true
        } else {
            return false
        }
    }

    fun webViewCreatorSaveState(context: Context) {
        val stackPath = File(context.externalCacheDir, "webstack")
        val bundle = Bundle()
        val stacks = webView.saveState(bundle)
        if (stacks != null && stacks.size > 0) {
            val parcel = Parcel.obtain()
            parcel.setDataPosition(0)
            bundle.writeToParcel(parcel, 0)
            val byte = parcel.marshall()
            parcel.recycle()
            try {
                val stream = FileOutputStream(stackPath)
                stream.write(byte)
            } catch (e: Exception) {
            }
        } else {
            stackPath.delete()
        }
    }

    fun webViewGoBack(): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

}