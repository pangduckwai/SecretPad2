package org.sea9.android.secret.io

import android.content.Context
import android.os.Environment
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.sea9.android.secret.R
import org.sea9.android.secret.core.ContextFragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileChooserAdaptor(ctx: Caller): RecyclerView.Adapter<FileChooserAdaptor.ViewHolder>() {
	companion object {
		const val TAG = "secret.files_adaptor"
		const val FILE_EXT = ".txt"
		const val FILE_SELF = "."
		const val FILE_PARENT = ".."
	}

	private val caller: Caller = ctx
	private var cache: List<FileRecord> = mutableListOf()
	private val ready: Boolean
	private var currentPath: String? = null
	private lateinit var recyclerView: RecyclerView

	var hasPermission = false

	private var selectedPos = -1
	private fun isSelected(position: Int): Boolean {
		return (selectedPos == position)
	}

	init {
		val state = Environment.getExternalStorageState()
		ready = (Environment.MEDIA_MOUNTED == state) || (Environment.MEDIA_MOUNTED_READ_ONLY == state)

		if (ready) {
			val context = caller.getContext()
			val root: File? = context?.getExternalFilesDir(null)
			currentPath = root?.path
		}
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
		val item = LayoutInflater.from(parent.context).inflate(R.layout.filechooser_item, parent, false)
		item.setOnClickListener {
			if (!caller.isBusy()) {
				val position = recyclerView.getChildLayoutPosition(it)
				if (position == selectedPos) {
					selectedPos = -1 //Un-select
				} else if (cache.size > position) {
					caller.setBusy(true)
					selectedPos = position
					val selected = cache[position]
					recyclerView.postDelayed({
						if (selected.isDirectory) {
							if (selected.path == FILE_PARENT) {
								val cp = File(currentPath)
								populateCache(cp.parent)
								caller.directorySelected(cp)
								Log.d(TAG, "Parent directory $currentPath selected")
							} else if (selected.path != FILE_SELF) {
								populateCache(selected.path)
								caller.directorySelected(File(selected.path))
								Log.d(TAG, "Directory ${selected.path} selected")
							}
						} else {
							caller.fileSelected(File(selected.path))
							Log.d(TAG, "File ${selected.path} selected")
						}
						selectedPos = -1
						caller.setBusy(false)
						notifyDataSetChanged()
					}, 200)
				}
				notifyDataSetChanged()
			}
		}
		return ViewHolder(item)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.itemView.isSelected = isSelected(position)
		val formatter = SimpleDateFormat(ContextFragment.PATTERN_DATE, Locale.getDefault())
		val selected = cache[position]
		if (selected.isDirectory) {
			holder.iconDir.visibility = View.VISIBLE
			holder.iconFile.visibility = View.GONE
		} else {
			holder.iconDir.visibility = View.GONE
			holder.iconFile.visibility = View.VISIBLE
		}
		holder.name.text = selected.name
		holder.time.text = formatter.format(selected.modified)
		holder.size.text = selected.size.toString()
	}

	override fun getItemCount(): Int {
		return cache.size
	}

	fun populateCache(current: String?) {
		if (ready) {
			val curr = if (current == null) File(currentPath) else File(current)
			if (current == null) caller.directorySelected(curr)
			if (curr.exists()) {
				if (curr.isDirectory) {
					val files = curr.listFiles { p ->
						if (p == null)
							false
						else
							p.exists() && (p.isDirectory || p.name.toLowerCase().endsWith(FILE_EXT))
					}
					if (files != null) {
						if (current != null) currentPath = current
						val list = files.asSequence()
								.map { FileRecord(it.path, it.name, Date(it.lastModified()), it.length(), it.isDirectory) }
								.toMutableList()

						if (hasPermission)
							list.add(FileRecord(FILE_PARENT, FILE_PARENT, Date(), 0, true))
						list.add(FileRecord(FILE_SELF, FILE_SELF, Date(), 0, true))

						cache = list.asSequence()
								.sortedWith(Comparator { x, y ->
									if (x.name == FILE_PARENT) {
										-1
									} else if (y.name == FILE_PARENT) {
										1
									} else if (x.name == FILE_SELF && y.name != FILE_PARENT) {
										-1
									} else if (y.name == FILE_SELF && x.name != FILE_PARENT) {
										1
									} else if (x.isDirectory && !y.isDirectory) {
										-1
									} else if (y.isDirectory && !x.isDirectory) {
										1
									} else {
										x.name.compareTo(y.name)
									}
								}).toMutableList()
					} //else do nothing
				} else {
					cache = mutableListOf() //Should not reach here
				}
			}
		}
	}

	/*=============
	 * View holder
	 */
	class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
		val iconDir: ImageView = view.findViewById(R.id.icon_dir)
		val iconFile: ImageView = view.findViewById(R.id.icon_file)
		val name: TextView = view.findViewById(R.id.file_name)
		val time: TextView = view.findViewById(R.id.modify_time)
		val size: TextView = view.findViewById(R.id.file_size)
	}

	/*=========================================
	 * Access interface to the ContextFragment
	 */
	interface Caller {
		fun getContext(): Context?
		fun directorySelected(selected: File)
		fun fileSelected(selected: File)
		fun isBusy(): Boolean
		fun setBusy(flag: Boolean)
	}
}