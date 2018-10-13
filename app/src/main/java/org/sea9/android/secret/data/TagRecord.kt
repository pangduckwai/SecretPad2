package org.sea9.android.secret.data

data class TagRecord(
		  var pid: Long
		, var tag: String
		, var modified: Long
) {
	override fun equals(other: Any?): Boolean {
		val value = other as TagRecord
		return (tag == value.tag)
	}

	override fun hashCode(): Int {
		return tag.hashCode()
	}
}