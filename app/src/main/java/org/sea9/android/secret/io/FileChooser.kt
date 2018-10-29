package org.sea9.android.secret.io

import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.sea9.android.secret.R

class FileChooser : DialogFragment() {
	companion object {
		const val TAG = "secret.file_chooser"

		fun getInstance(): FileChooser {
			val instance = FileChooser()
			instance.isCancelable = false
			instance.setStyle(DialogFragment.STYLE_NORMAL, R.style.FullDialog)
			return instance
		}
	}

	private lateinit var fileList: RecyclerView

	private lateinit var currentPath: TextView
	fun setCurrentPath(path: String?) {
		currentPath.text = path
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.filechooser_dialog, container, false)
		currentPath = view.findViewById(R.id.current_dir)
		fileList = view.findViewById(R.id.file_list)

		fileList.setHasFixedSize(true)
		fileList.layoutManager = LinearLayoutManager(context)

		dialog.setOnKeyListener { _, keyCode, event ->
			if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.action == KeyEvent.ACTION_UP)) {
				dismiss()
				true
			} else {
				false
			}
		}

		dialog.setTitle(R.string.action_import)

		return view
	}

	override fun onResume() {
		super.onResume()
		val adaptor = callback?.getFileAdaptor()
		fileList.adapter = adaptor

//		val root = context?.getExternalFilesDir(null)?.path
		adaptor?.select(null)
//		setCurrentPath(root)
	}

	/*========================================
	 * Callback interface to the MainActivity
	 */
	interface Callback {
		fun getFileAdaptor(): FileChooserAdaptor
	}
	private var callback: Callback? = null

	override fun onAttach(context: Context?) {
		super.onAttach(context)
		try {
			callback = context as Callback
		} catch (e: ClassCastException) {
			throw ClassCastException("$context missing implementation of FileChooser.Callback")
		}
	}

	override fun onDetach() {
		super.onDetach()
		callback = null
	}
}