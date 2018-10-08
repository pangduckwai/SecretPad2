package org.sea9.android.secret.data

data class NoteRecord(
		  var pid: Long
		, var key: String
		, var tags: MutableList<TagRecord>? = ArrayList(3)
		, var modified: Long
)