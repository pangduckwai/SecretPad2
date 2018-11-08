package org.sea9.android.secret.core

import android.os.AsyncTask
import android.util.Log
import org.sea9.android.secret.R
import org.sea9.android.secret.data.DbContract
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.RuntimeException
import javax.crypto.BadPaddingException

class AsyncImportTask(private val caller: ContextFragment): AsyncTask<File, Void, Int>() {
	companion object {
		const val TAG = "secret.task_import"
	}

	private var filePath: String? = null
	private var errorMessage: String? = null
	private fun writeError(msg: String?) {
		if (errorMessage == null)
			errorMessage = msg
		else if (msg != null) {
			errorMessage += (ContextFragment.NEWLINE + msg)
		}
	}

	override fun onPreExecute() {
		caller.callback.setBusyState(true)
	}

	override fun doInBackground(vararg files: File?): Int {
		if (files.isNotEmpty()) {
			var count = 0
			var success = 0
			var reader: BufferedReader? = null
			try {
				filePath = files[0]?.path
				reader = files[0]?.bufferedReader()
				reader?.useLines { lines ->
					lines.forEach {
						if (isCancelled) return -1 //Return value not clash, since this gos to onCancelled()

						val row = it.split(DbContract.Notes.TAB).toTypedArray()
						if (count == 0) { // First row in the import data file
							if (row.size == DbContract.Notes.OLD_FORMAT_COLUMN_COUNT) {
								// Old Secret Pad format: ID, salt, category, title*, content*, modified
								// Need to exit the task, ask for old password, and run import again...
								Log.d(TAG, "Old file format")
								cancel(true)
								caller.tempFile = files[0]
								return DbContract.Notes.OLD_FORMAT_COLUMN_COUNT
							} else if (row.size != DbContract.Notes.EXPORT_FORMAT_1STROW_COL) { // First row has 3 columns: App name, version and export time
								writeError("Invalid file format")
								cancel(true)
								return -2 //Return value not clash, since this gos to onCancelled()
							}
						} else if (row.size >= DbContract.Notes.EXPORT_FORMAT_MIN_COLUMN) {
							val ret = DbContract.Notes.doImport(caller.dbHelper, caller, row)
							if (ret >= 0)
								success ++
							else if (ret < -1)
								return -4
						} else {
							writeError("Invalid file format at row $count")
						}
						count ++
					}
				}
				return success
			} catch (e: FileNotFoundException) {
				Log.w(AsyncImportOldTask.TAG, e)
				writeError(e.message)
				return -3
			} catch (e: IOException) {
				Log.w(TAG, e)
				writeError(e.message)
				return -2
			} catch (e: RuntimeException) {
				if ((e.cause != null) && (e.cause is BadPaddingException)) {
					writeError(e.message)
				} else {
					writeError(e.message)
				}
				Log.w(TAG, e)
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
		return -5
	}

	override fun onPostExecute(result: Int?) {
		if (result != null) {
			if (result >= 0) {
				if (errorMessage.isNullOrEmpty()) {
					caller.callback.doNotify(
							String.format(caller.getString(R.string.msg_import_okay)
									, result
									, filePath
									, if (result > 1) ContextFragment.PLURAL else ContextFragment.EMPTY)
							, false)
				} else {
					caller.callback.doNotify(
							String.format(caller.getString(R.string.msg_import_error)
									, filePath
									, ContextFragment.NEWLINE + errorMessage)
							, true)
				}
				caller.tagsAdaptor.populateCache()
				caller.adaptor.populateCache()
				caller.adaptor.notifyDataSetChanged()
			} else if (result != -4) { //-4 already handled
				caller.callback.doNotify(
						String.format(caller.getString(R.string.msg_import_fail)
								, filePath, result, ContextFragment.NEWLINE + errorMessage)
						, true)
			}
		}
		caller.callback.setBusyState(false)
	}

	override fun onCancelled(result: Int?) {
		if ((result != null) && (result == DbContract.Notes.OLD_FORMAT_COLUMN_COUNT)) {
			caller.callback.doCompatLogon()
		} else {
			caller.callback.doNotify(errorMessage, true)
		}
		caller.callback.setBusyState(false)
	}
}