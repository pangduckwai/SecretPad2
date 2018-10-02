package org.sea9.android.secret.temp

data class DataRecord(
		  val key: String
		, var content: String
		, val tags: MutableList<Int> = ArrayList(3))