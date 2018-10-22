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

class NewPasswordDialog : DialogFragment() {
	companion object {
		const val TAG = "secret.logon_dialog"

		fun getInstance(): NewPasswordDialog {
			val instance = NewPasswordDialog()
			instance.isCancelable = false
			return instance
		}
	}

	private lateinit var txtPasswd: EditText
	private lateinit var txtConfrm: EditText

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.logon_dialog, container, false)

		dialog.setOnKeyListener { _, keyCode, event ->
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (event.action == KeyEvent.ACTION_UP) cancel()
				true
			} else {
				false
			}
		}

		txtPasswd = view.findViewById(R.id.password)
//		txtPasswd.setOnEditorActionListener { v, actionId, _ ->
//			if (actionId == EditorInfo.IME_ACTION_NEXT) {
//				logon(v)
//			}
//			false
//		}

		txtConfrm = view.findViewById(R.id.confirm)
		txtConfrm.setOnEditorActionListener { v, actionId, _ ->
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
		val txt1 = txtPasswd.text
		val len1 = txt1.length
		val txt2 = txtConfrm.text
		val len2 = txt2.length
		return if ((txt1 == null) || (len1 <= 0) || (txt2 == null) || (len2 <= 0)) {
			Snackbar.make(view, getString(R.string.msg_passwd_needed), Snackbar.LENGTH_LONG).show()
			false
		} else if (txt1 != txt2) {
			Snackbar.make(view, getString(R.string.msg_passwd_mismatch), Snackbar.LENGTH_LONG).show()
			false
		} else {
			val ret = CharArray(len1)
			txtPasswd.text.getChars(0, len1, ret, 0)
			txtPasswd.text.clear()
			txtConfrm.text.clear()
			callback?.onLogon(CryptoUtils.convert(CryptoUtils.encode(CryptoUtils.hash(CryptoUtils.convert(ret)))))
			dismiss()
			true
		}
	}

	private fun cancel() {
		callback?.onLogon(null)
		dismiss()
	}

	interface Callback {
		fun onLogon(value: CharArray?)
	}
	private var callback : Callback? = null

	override fun onAttach(context: Context?) {
		super.onAttach(context)
		try {
			callback = context as Callback
		} catch (e: ClassCastException) {
			throw ClassCastException("${context.toString()} missing implementation of LogonDialog.Callback")
		}
	}

	override fun onDetach() {
		super.onDetach()
		callback = null
	}
}