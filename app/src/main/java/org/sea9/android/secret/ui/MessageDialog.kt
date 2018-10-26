package org.sea9.android.secret.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import org.sea9.android.secret.R

class MessageDialog : DialogFragment() {
	companion object {
		const val TAG = "secret.message_dialog"
		const val REF = "secret.reference"
		const val FLG = "secret.flags"
		const val MSG = "secret.message"
		const val NEU = "secret.neutral"
		const val POS = "secret.positive"
		const val NEG = "secret.negative"

		fun getInstance(reference: Int, buttons: Int, title: String?, message: String, neutral: String?, positive: String?, negative: String?) : MessageDialog {
			val instance = MessageDialog()
			instance.isCancelable = false

			val args = Bundle()
			var flag = 0
			args.putInt(REF, reference)
			args.putString(MSG, message)
			title?.let {
				args.putString(TAG, it)
			}
			neutral?.let {
				args.putString(NEU, it)
				flag += 1
			}
			positive?.let {
				args.putString(POS, it)
				flag += 2
			}
			negative?.let {
				args.putString(NEG, it)
				flag += 4
			}

			args.putInt(FLG, if (buttons > 0) buttons else if (flag == 0) 1 else flag)
			instance.arguments = args
			return instance
		}
		fun getInstance(reference: Int, message: String) : MessageDialog {
			return getInstance(reference, 1, null, message, null, null, null)
		}
		fun getOkayCancelDialog(reference: Int, message: String) : MessageDialog {
			return getInstance(reference, 6, null, message, null, null, null)
		}
	}

	@SuppressLint("InflateParams")
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(activity)

		val args = arguments
		val reference = args?.getInt(REF)
		builder.setMessage(args?.getString(MSG))
		args?.getString(TAG)?.let {
			builder.setTitle(it)
		}
		val neutral = args?.getString(NEU)?.let {
			it
		} ?: context?.getString(R.string.btn_okay)
		val positive = args?.getString(POS)?.let {
			it
		} ?: context?.getString(R.string.btn_okay)
		val negative = args?.getString(NEG)?.let {
			it
		} ?: context?.getString(R.string.btn_cancel)

		val flag = args?.getInt(FLG)
		flag?.let {
			if ((it and 1) > 0) {
				builder.setNeutralButton(neutral) { dialog, id -> callback?.neutral(reference!!, dialog, id) }
			}
			if ((it and 2) > 0) {
				builder.setPositiveButton(positive) { dialog, id -> callback?.positive(reference!!, dialog, id) }
			}
			if ((it and 4) > 0) {
				builder.setNegativeButton(negative) { dialog, id -> callback?.negative(reference!!, dialog, id) }
			}
		}

		return builder.create()
	}

	/*========================================
	 * Callback interface to the MainActivity
	 */
	interface Callback {
		fun neutral(reference: Int, dialog: DialogInterface, id: Int)
		fun positive(reference: Int, dialog: DialogInterface, id: Int)
		fun negative(reference: Int, dialog: DialogInterface, id: Int)
	}
	private var callback: Callback? = null

	override fun onAttach(context: Context?) {
		super.onAttach(context)
		try {
			callback = context as Callback
		} catch (e: ClassCastException) {
			throw ClassCastException("$context missing implementation of MessageDialog.Callback")
		}
	}

	override fun onDetach() {
		super.onDetach()
		callback = null
	}
}