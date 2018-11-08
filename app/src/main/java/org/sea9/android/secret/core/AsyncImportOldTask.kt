package org.sea9.android.secret.core

import android.os.AsyncTask
import android.util.Log
import org.sea9.android.secret.R
import org.sea9.android.secret.compat.SmartConverter
import org.sea9.android.secret.data.DbContract
import org.sea9.android.secret.data.DbHelper
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.RuntimeException

class AsyncImportOldTask( private val caller: ContextFragment
						, private val crypto: DbHelper.Crypto
						, private val converter: SmartConverter?): AsyncTask<Void, Void, Int>() {
	companion object {
		const val TAG = "secret.task_import_old"
		const val TAB = "\t"
		const val NEWLINE = "\n"
		const val PLURAL = "s"
		const val EMPTY = ""
		const val OLD_FORMAT_COLUMN_COUNT = 6
	}

	private var errorMessage: String? = null
	private fun writeError(msg: String?) {
		if (errorMessage == null)
			errorMessage = msg
		else if (msg != null) {
			errorMessage += (NEWLINE + msg)
		}
	}

	override fun onPreExecute() {
		caller.callback.setBusyState(true)
	}

	override fun doInBackground(vararg params: Void?): Int {
		var count = 0
		var success = 0
		var reader: BufferedReader? = null
		try {
			reader = caller.tempFile.bufferedReader()
			reader.useLines { lines ->
				lines.forEach {
					val old: Array<String> = it.split(TAB).toTypedArray()
					if (old.size == OLD_FORMAT_COLUMN_COUNT) {
						// Old format: ID, salt, category, title*, content*, modified
						val ret = DbContract.Notes.doOldImport(caller.dbHelper, crypto, old, converter)
						if (ret >= 0) {
							success++
						} else if (ret < -1) {
							return -4
						}
					} else {
						writeError("Invalid file format at row $count")
					}
					count++
				}
			}
			return success
		} catch (e: FileNotFoundException) {
			Log.w(TAG, e)
			writeError(e.message)
			return -3
		} catch (e: IOException) {
			Log.w(TAG, e)
			writeError(e.message)
			return -2
		} catch (e: RuntimeException) {
			Log.w(TAG, e)
			writeError(e.message)
			return -1
		} finally {
			if (reader != null) {
				try {
					reader.close()
				} catch (e: IOException) {
					Log.d(TAG, e.message)
				}
			}
		}
	}

	override fun onPostExecute(result: Int?) {
		if (result != null) {
			if (result >= 0) {
				if (errorMessage.isNullOrEmpty()) {
					caller.callback.doNotify(
							String.format(caller.getString(R.string.msg_migrate_okay)
									, result
									, caller.tempFile.path
									, if (result > 1) PLURAL else EMPTY)
							, false)
				} else {
					caller.callback.doNotify(
							String.format(caller.getString(R.string.msg_migrate_error)
									, caller.tempFile.path
									, NEWLINE + errorMessage)
							, true)
				}
				caller.tagsAdaptor.populateCache()
				caller.adaptor.populateCache()
				caller.adaptor.notifyDataSetChanged()
			} else {
				caller.callback.doNotify(
						String.format(caller.getString(R.string.msg_migrate_fail), caller.tempFile.path, result)
						, true)
			}
			caller.cleanUp()
		}
		caller.callback.setBusyState(false)
	}
}