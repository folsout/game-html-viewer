package com.gameview.shower

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback

interface ChooserHelper {
	companion object {

		const val REQUEST_SELECT = 100
	}

	var uploadMessage: ValueCallback<Array<Uri>>?

	fun startActivityForResult(intent: Intent, req: Int)
}