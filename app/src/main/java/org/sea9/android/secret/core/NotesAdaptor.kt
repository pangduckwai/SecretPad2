package org.sea9.android.secret.core

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.sea9.android.secret.R
import org.sea9.android.secret.data.DbContract
import org.sea9.android.secret.data.DbHelper
import org.sea9.android.secret.data.NoteRecord
import javax.crypto.BadPaddingException

class NotesAdaptor (ctx: Caller): RecyclerView.Adapter<NotesAdaptor.ViewHolder>() {
	companion object {
		const val TAG = "secret.notes_adaptor"
		const val SPACE = " "
	}

	private val caller: Caller = ctx
	private lateinit var recyclerView: RecyclerView

	private var selectedPos = -1
	fun isSelected(position: Int): Boolean {
		return (selectedPos == position)
	}
	fun getSelectedPosition(): Int {
		return selectedPos
	}
	fun clearSelection() {
		selectedPos = -1
		caller.getCallback()?.onRowSelectionChanged(ContextFragment.EMPTY)
	}
	fun findSelectedPosition(pid: Long): Int {
		return shown.asSequence()
				.indexOfFirst {
					it.pid == pid
				}
	}

	fun selectRow(position: Int) {
		selectedPos = position
		retrieveDetails(position)
	}

	private var shown: MutableList<NoteRecord> = mutableListOf()
	private var cache: MutableList<NoteRecord> = mutableListOf()
	fun getRecord(position: Int): NoteRecord? {
		return if ((position >= 0) && (position < shown.size)) {
			shown[position]
		} else {
			null
		}
	}
	fun filterRecords() {
		val query = caller.getFilterQuery()
		shown = if (query == null)
			cache
		else
			cache.asSequence()
				.filter {
					it.key.contains(query, true) || ((it.tagNames != null) && it.tagNames!!.contains(query, true))
				}.toMutableList()
	}
	fun clearFilter() {
		shown = cache
	}
	fun clearRecords() {
		shown.clear()
		cache.clear()
	}

	/*=====================================================
	 * @see android.support.v7.widget.RecyclerView.Adapter
	 */
	override fun onAttachedToRecyclerView(recycler: RecyclerView) {
		super.onAttachedToRecyclerView(recycler)
		Log.d(TAG, "onAttachedToRecyclerView")
		recyclerView = recycler
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val item = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)

		item.setOnClickListener {
			if (!caller.isBusy()) {
				val position = recyclerView.getChildLayoutPosition(it)
				if (position == selectedPos) {
					clearSelection()
				} else {
					selectRow(position)
				}
				notifyDataSetChanged()
			}
		}

		item.setOnLongClickListener {
			if (!caller.isFiltered() && !caller.isBusy()) {
				val position = recyclerView.getChildLayoutPosition(it)
				selectRow(position)
				caller.getCallback()?.longPressed()
				notifyDataSetChanged()
				true
			} else
				false
		}

		return ViewHolder(item)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.itemView.isSelected = isSelected(position)
		val record = shown[position]
		holder.key.text = record.key
		holder.tag.text = record.tagNames
	}

	override fun getItemCount(): Int {
		return shown.size
	}

	/*======================
	 * Data access methods.
	 */
	fun populateCache() {
		try {
			cache = DbContract.Notes.select(caller.getDbHelper()!!) as MutableList<NoteRecord>

			// Build the concatenated tag string
			val buff = StringBuilder()
			for (record in cache) {
				val tags = record.tags
				if ((tags != null) && (tags.size > 0)) {
					buff.setLength(0)
					buff.append(caller.getTag(tags[0]))
					for (i in 1 until tags.size)
						buff.append(SPACE).append(caller.getTag(tags[i]))
					record.tagNames = buff.toString()
				}
			}

			sortCache()
		} catch (e: RuntimeException) {
			if ((e.cause != null) && (e.cause is BadPaddingException)) {
				val context = caller.getContext()
				var msg = context?.getString(R.string.msg_logon_fail)
				msg = if (msg == null)
					e.message
				else
					String.format(msg, e.message)
				caller.getCallback()?.doNotify(MainActivity.MSG_DIALOG_LOG_FAIL, msg, true)
			} else
				throw e
		}
	}

	fun sortCache() {
		cache = cache.asSequence()
				.sortedWith(Comparator { x, y ->
					val kx = x.key.toUpperCase()
					val ky = y.key.toUpperCase()
					val tx = x.tagNames?.toUpperCase()
					val ty = y.tagNames?.toUpperCase()
					when(caller.getSortBy()) {
						ContextFragment.SETTING_SORTBY_KEY -> kx.compareTo(ky)
						ContextFragment.SETTING_SORTBY_TAG -> {
							if (tx.isNullOrEmpty() && !ty.isNullOrEmpty())
								1
							else if (!tx.isNullOrEmpty() && ty.isNullOrEmpty())
								-1
							else {
								var diff = 0
								if (!tx.isNullOrEmpty() && !ty.isNullOrEmpty()) {
									diff = tx.compareTo(ty)
								}

								if (diff == 0)
									kx.compareTo(ky)
								else
									diff
							}
						}
						else -> x.modified.compareTo(y.modified)
					}
				}).toMutableList()
		filterRecords()
	}

	fun retrieveDetails(position: Int) {
		if (!caller.isBusy()) {
			if ((position >= 0) && (position < shown.size)) {
				val content = DbContract.Notes.select(caller.getDbHelper()!!, shown[position].pid)
				if (content != null) {
					caller.getCallback()?.onRowSelectionChanged(content)
				}
			}
		}
	}

	fun delete(position: Int): Int {
		var ret = -1
		if ((position >= 0) && (position < shown.size)) {
			ret = DbContract.Notes.delete(caller.getDbHelper()!!, shown[position].pid)
			if (ret >= 0) {
				if (position < selectedPos) {
					selectedPos --
				}
			}
		}
		return ret
	}

	/*=============
	 * View holder
	 */
	class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
		val key: TextView = view.findViewById(R.id.item_name)
		val tag: TextView = view.findViewById(R.id.item_tags)
	}
	//=============

	/*=========================================
	 * Access interface to the ContextFragment
	 */
	interface Caller {
		fun getCallback(): ContextFragment.Callback?
		fun getContext(): Context?
		fun getDbHelper(): DbHelper?
		fun getTag(tid: Long): String?
		fun getSortBy(): Int
		fun isFiltered(): Boolean
		fun getFilterQuery(): String?
		fun isBusy(): Boolean
		fun onLogoff()
	}
}