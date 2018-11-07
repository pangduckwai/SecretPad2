package org.sea9.android.secret.core

import android.os.AsyncTask

class AsyncDbReadTask(private val caller: ContextFragment): AsyncTask<Long, Void, Long>() {
	override fun onPreExecute() {
		caller.callback.setBusyState(true)
	}

	override fun doInBackground(vararg selected: Long?): Long? {
		caller.tagsAdaptor.populateCache()
		caller.adaptor.populateCache()
		return if (selected.isNotEmpty()) {
			selected[0]
		} else {
			-1L
		}
	}

	override fun onPostExecute(pid: Long?) {
		caller.adaptor.notifyDataSetChanged()
		if ((pid != null) && (pid >= 0)) {
			val position = caller.adaptor.findSelectedPosition(pid)
			caller.adaptor.selectRow(position)
			caller.callback.onScrollToPosition(position)
		}
		caller.callback.setBusyState(false)
	}
}