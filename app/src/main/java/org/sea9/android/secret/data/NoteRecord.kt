package org.sea9.android.secret.data

data class NoteRecord(
		  var pid: Long
		, var key: String
		, var content: String?
		, var tags: MutableList<Long>? = ArrayList(3)
		, var tagNames: String?
		, var modified: Long
) {
	override fun equals(other: Any?): Boolean {
		val value = other as NoteRecord
		return (key == value.key)
	}

	override fun hashCode(): Int {
		return key.hashCode()
	}
}