package org.sea9.android.secret

data class DataRecord(
		  val key: String
		, var content: String
		, val tags: MutableList<Int> = ArrayList(3))