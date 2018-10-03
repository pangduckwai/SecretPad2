package org.sea9.android.secret.data

data class NoteRecord(
		  var pid: Long
		, var key: String
		, var content: String?
		, var tags: MutableList<Long>? = ArrayList(3)
		, var modified: Long
)