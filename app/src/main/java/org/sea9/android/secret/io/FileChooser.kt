package org.sea9.android.secret.io

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
import org.sea9.android.secret.core.ContextFragment

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

	private lateinit var ctxFrag: ContextFragment
	private lateinit var currentPath: TextView
	private lateinit var fileList: RecyclerView

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.filechooser_main, container, false)
		currentPath = view.findViewById(R.id.current_dir)
		fileList = view.findViewById(R.id.file_list)

		fileList.setHasFixedSize(true)
		fileList.layoutManager = LinearLayoutManager(context)

		dialog.setOnKeyListener { _, keyCode, event ->
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (event.action == KeyEvent.ACTION_UP) dismiss()
				true
			} else {
				false
			}
		}

		dialog.setTitle(R.string.app_name)

		return view
	}

	override fun onResume() {
		super.onResume()
		ctxFrag = fragmentManager?.findFragmentByTag(ContextFragment.TAG) as ContextFragment
		fileList.adapter = ctxFrag.fileAdaptor

		ctxFrag.fileAdaptor.select(context?.getExternalFilesDir(null)?.path)
	}
}