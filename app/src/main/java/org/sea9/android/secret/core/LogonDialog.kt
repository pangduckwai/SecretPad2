package org.sea9.android.secret.core

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import org.sea9.android.secret.R
import org.sea9.android.secret.crypto.CryptoUtils

class LogonDialog : DialogFragment() {
	companion object {
		const val TAG = "secret.dialog_logon"

		fun getInstance(): LogonDialog {
			val instance = LogonDialog()
			instance.isCancelable = false
			return instance
		}
	}

	private lateinit var txtPasswd: EditText

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.dialog_login, container, false)

		dialog.setOnKeyListener { _, keyCode, event ->
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (event.action == KeyEvent.ACTION_UP) cancel()
				true
			} else {
				false
			}
		}

		txtPasswd = view.findViewById(R.id.password)
		txtPasswd.setOnEditorActionListener { v, actionId, _ ->
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				logon(v)
			}
			false
		}

		view.findViewById<Button>(R.id.logon).setOnClickListener {
			logon(txtPasswd)
		}

		val win = dialog.window
		win?.requestFeature(Window.FEATURE_NO_TITLE)
		return view
	}

	private fun logon(view: View) : Boolean {
		val txt = txtPasswd.text
		val len = txt.length
		return if ((txt == null) || (len <= 0)) {
			Snackbar.make(view, getString(R.string.msg_passwd_needed), Snackbar.LENGTH_LONG).show()
			false
		} else {
			val ret = CharArray(len)
			txtPasswd.text.getChars(0, len, ret, 0)
			txtPasswd.text.clear()
			callback?.onLogon(CryptoUtils.convert(CryptoUtils.encode(CryptoUtils.hash(CryptoUtils.convert(ret)))))
			dismiss()
			true
		}
	}

	private fun cancel() {
		callback?.onLogon(null)
		dismiss()
	}

	interface Listener {
		fun onLogon(value: CharArray?)
	}
	private var callback : Listener? = null

	override fun onAttach(context: Context?) {
		super.onAttach(context)
		try {
			callback = context as Listener
		} catch (e: ClassCastException) {
			throw ClassCastException(context.toString() + " missing implementation of DetailFragment.Listener")
		}
	}

	override fun onDetach() {
		super.onDetach()
		callback = null
	}
}