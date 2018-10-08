package org.sea9.android.secret.data

data class TagRecord(
		  var pid: Long
		, var tag: String
		, var modified: Long
) {
	override fun equals(other: Any?): Boolean {
		val othr = other as TagRecord
		return (tag == othr.tag)
	}
}