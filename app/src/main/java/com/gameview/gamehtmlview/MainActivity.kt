package com.gameview.gamehtmlview

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.gameview.shower.ChooserHelper
import com.gameview.shower.WebViewCreator

class MainActivity : AppCompatActivity(), ChooserHelper {

	override var uploadMessage: ValueCallback<Array<Uri>>? = null
	private val USER_HISTORY_SHAREDPREFERENCES = "user_history_sharedpreferences"
	private lateinit var webViewCreator: WebViewCreator
	private lateinit var webViewContainer: RelativeLayout
	private lateinit var sharedPreferences: SharedPreferences
	private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
	private lateinit var progressBar: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		sharedPreferencesInit()
		webViewCreator = WebViewCreator(this)
		findViewSource()
		webViewCreator.setSharedPreferences(sharedPreferences)
		findLastPage()
		webViewCreator.setScript("")
		if (webViewCreator.backPage.isNotEmpty()) {
			webViewCreator.loadUrl(webViewCreator.backPage.first)
			webViewCreator.backPage.removeFirst()
		} else {
			webViewCreator.loadUrl("https://google.com")
		}
	}

	private fun findLastPage() {
		val lPage = sharedPreferences.getString(USER_HISTORY_SHAREDPREFERENCES, null)
		lPage?.let {
			for (url in lPage.split(",")) {
				webViewCreator.addToPage(url)
			}
		}
	}

	private fun sharedPreferencesInit() {
		sharedPreferences = getSharedPreferences("THIS_SHARED_PREFERENCES", Context.MODE_PRIVATE)
		sharedPreferencesEditor = sharedPreferences.edit()
	}

	private fun findViewSource() {
		webViewContainer = findViewById(R.id.web_view_container)
		progressBar = ProgressBar(this)
		val progressBarLayoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
		progressBarLayoutParams.setMargins(0, 0, 0, 150)
		progressBarLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
		progressBarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
		webViewContainer.addView(webViewCreator.createWebView(this, progressBar))
		webViewContainer.addView(progressBar, progressBarLayoutParams)
	}

	override fun onBackPressed() {

		if (!webViewCreator.goBackPage()) {
			super.onBackPressed()
		}
	}

	override fun onStop() {
		super.onStop()
		sharedPreferencesEditor.putString(USER_HISTORY_SHAREDPREFERENCES, webViewCreator.backPage.reversed().joinToString(",")).apply()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == ChooserHelper.REQUEST_SELECT) {
			if (uploadMessage == null) return
			uploadMessage?.onReceiveValue(
				WebChromeClient.FileChooserParams.parseResult(
					resultCode,
					data
				)
			)
			uploadMessage = null
		}
	}
}