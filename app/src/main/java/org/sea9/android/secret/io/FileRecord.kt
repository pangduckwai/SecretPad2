package org.sea9.android.secret.io

import java.util.*

data class FileRecord(
		  var path: String
		, var name: String
		, var modified: Date
		, var size: Long
		, var isDirectory: Boolean
) {
	override fun equals(other: Any?): Boolean {
		val value = other as FileRecord
		return (path == value.path)
	}

	override fun hashCode(): Int {
		return path.hashCode()
	}
}