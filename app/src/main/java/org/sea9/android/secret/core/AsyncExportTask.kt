package org.sea9.android.secret.core

import android.os.AsyncTask
import android.util.Log
import org.sea9.android.secret.R
import org.sea9.android.secret.data.DbContract
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class AsyncExportTask(private val caller: ContextFragment): AsyncTask<File, Void, Int>() {
	private var exportFileName: String? = null

	companion object {
		const val TAG = "secret.task_export"
		const val PATTERN_TIMESTAMP = "yyyyMMddHHmm"
		const val PLURAL = "s"
		const val EMPTY = ""
	}
	init {
		val formatter = SimpleDateFormat(PATTERN_TIMESTAMP, Locale.getDefault())
		exportFileName = "${caller.context?.getString(R.string.value_export)}${formatter.format(Date())}.txt"
	}

	override fun onPreExecute() {
		caller.callback.setBusyState(true)
	}

	override fun doInBackground(vararg files: File?): Int {
		if ((files.isNotEmpty()) && files[0]!!.isDirectory) {
			val export = File(files[0], exportFileName)
			if (export.exists()) {
				Log.w(TAG, "File ${export.path} already exists")
				return -3
			}

			var writer: PrintWriter? = null
			return try {
				writer = PrintWriter(OutputStreamWriter(FileOutputStream(export)))
				writer.println("${caller.getString(R.string.app_name)}\t${caller.versionCode}\t${Date().time}") //Header
				DbContract.Notes.doExport(caller.dbHelper, writer)
			} catch (e: FileNotFoundException) {
				Log.w(TAG, e)
				-2
			} finally {
				if (writer != null) {
					writer.flush()
					writer.close()
				}
			}
		}
		return -1
	}

	override fun onPostExecute(result: Int?) {
		if (result != null) {
			if (result >= 0) {
				caller.callback.doNotify(
						String.format(caller.getString(R.string.msg_export_okay)
								, result
								, exportFileName
								, if (result > 1) PLURAL else EMPTY)
						, false)
			} else {
				caller.callback.doNotify(String.format(caller.getString(R.string.msg_export_error), result), true)
			}
		}
		caller.callback.setBusyState(false)
	}
}