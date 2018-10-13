package org.sea9.android.secret.main

import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import org.sea9.android.secret.R

class AboutDialog : DialogFragment() {
	companion object {
		const val TAG = "secret.dialog_about"
		fun getInstance() : AboutDialog {
			return AboutDialog()
		}
	}

	private var version: String? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.dialog_about, container, false)

		val txtVer = view.findViewById<TextView>(R.id.version)
		version?.let {
			txtVer.text = String.format(getString(R.string.app_version), it)
		}

		val win = dialog.window
		win?.let {
			it.requestFeature(Window.FEATURE_NO_TITLE)
		}

		return view
	}

	override fun onAttach(context: Context?) {
		super.onAttach(context)
		context?.let {
			version = it.packageManager.getPackageInfo(it.packageName, 0).versionName
		}
	}
}