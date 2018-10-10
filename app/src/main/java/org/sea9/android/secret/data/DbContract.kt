package org.sea9.android.secret.data

import android.content.ContentValues
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import java.lang.IllegalStateException
import java.util.*

object DbContract {
	const val DATABASE = "Secret.db"
	const val PKEY = BaseColumns._ID
	const val COMMON_MODF = "modified"
	const val COMMON_PKEY = "$PKEY = ?"

	class Tags : BaseColumns {
		companion object {
			const val TABLE = "Tags"
			const val COL_TAG_NAME = "tagName"

			private val COLUMNS = arrayOf(PKEY, COL_TAG_NAME, COMMON_MODF)

			const val SQL_CREATE =
					"create table $TABLE (" +
							"$PKEY integer primary key autoincrement," +
							"$COL_TAG_NAME text not null," +
							"$COMMON_MODF integer)"
			const val SQL_DROP = "drop table if exists $TABLE"

			private const val QUERY_DELETE =
					"delete from $TABLE where $PKEY not in " +
						"(select nt.${NoteTags.COL_TID} from ${NoteTags.TABLE} as nt inner join $TABLE as t on t.$PKEY = nt.${NoteTags.COL_TID})"

			/**
			 * Select all tags.
			 */
			fun select(helper: SQLiteOpenHelper): List<TagRecord> {
				val cursor = helper.readableDatabase
						.query(TABLE, COLUMNS, null, null, null, null, COL_TAG_NAME)

				val result = mutableListOf<TagRecord>()
				with(cursor) {
					while (moveToNext()) {
						val pid = getLong(getColumnIndexOrThrow(PKEY))
						val name = getString(getColumnIndexOrThrow(COL_TAG_NAME))
						val modified = getLong(getColumnIndexOrThrow(COMMON_MODF))
						val item = TagRecord(pid, name, modified)
						result.add(item)
					}
				}

				cursor.close()
				return result
			}

			/**
			 * Insert a tag.
			 */
			fun insert(helper: SQLiteOpenHelper, tagName: String): TagRecord? {
				val timestamp = Date().time
				val newRow = ContentValues().apply {
					put(COL_TAG_NAME, tagName)
					put(COMMON_MODF, timestamp)
				}
				val pid = helper.writableDatabase.insert(TABLE, null, newRow)
				return if (pid >= 0) {
					TagRecord(pid, tagName, timestamp)
				} else {
					null
				}
			}

			/**
			 * Delete any unused tags.
			 */
			fun delete(helper: SQLiteOpenHelper): Int {
				return helper.writableDatabase.compileStatement(QUERY_DELETE).executeUpdateDelete()
			}
		}
	}

	class Notes : BaseColumns {
		companion object {
			const val TABLE = "Notes"
			private const val COL_KEY = "noteKey"
			private const val COL_KEY_SALT = "keySalt"
			private const val COL_CONTENT = "noteContent"
			private const val COL_CONTENT_SALT = "contentSalt"

			private val KEYS = arrayOf(PKEY, COL_KEY, COL_KEY_SALT, COMMON_MODF)
			private val COLUMNS = arrayOf(PKEY, COL_CONTENT, COL_CONTENT_SALT, COMMON_MODF)

			const val SQL_CREATE =
					"create table $TABLE (" +
							"$PKEY integer primary key autoincrement," +
							"$COL_KEY_SALT text not null," +
							"$COL_KEY text not null," +
							"$COL_CONTENT_SALT text not null," +
							"$COL_CONTENT text not null," +
							"$COMMON_MODF integer)"
			const val SQL_DROP = "drop table if exists $TABLE"

			/**
			 * Select all notes.
			 */
			fun select(helper: SQLiteOpenHelper): List<NoteRecord> {
				// Not using order-by in query because keys are encrypted as well
				val cursor = helper.readableDatabase
						.query(TABLE, KEYS, null, null, null, null, null)

				val result = mutableSetOf<NoteRecord>()
				with(cursor) {
					while (moveToNext()) {
						val pid = getLong((getColumnIndexOrThrow(PKEY)))
						val key = getString(getColumnIndexOrThrow(COL_KEY)) //TODO remember to decrypt...
						val modified = getLong(getColumnIndexOrThrow(COMMON_MODF))
						val item = NoteRecord(pid, key, null, modified)
						result.add(item)
					}
				}

				cursor.close()
				return result.sortedWith(compareBy { it.key }) // Sort here after decrypt
			}

			/**
			 * Select one note by its ID, returns only the content.
			 */
			fun select(helper: SQLiteOpenHelper, nid: Long): String? {
				val args = arrayOf(nid.toString())
				val cursor = helper.readableDatabase
						.query(TABLE, COLUMNS, COMMON_PKEY, args, null, null, null)

				var rowCount = 0
				var ctn: String? = null
				with(cursor) {
					while (moveToNext()) {
						rowCount ++
						ctn = getString(getColumnIndexOrThrow(COL_CONTENT)) //TODO remember to decrypt...
					}
				}

				cursor.close()
				if (rowCount != 1) {
					throw IllegalStateException("Corrupted database table")
				} else {
					return ctn
				}
			}

			/**
			 * Insert one note.
			 */
			fun insert(helper: SQLiteOpenHelper, k: String, c: String): NoteRecord? {
				val timestamp = Date().time
				val newRow = ContentValues().apply {
					put(COL_KEY_SALT, "") // TODO Generate new salt
					put(COL_KEY, k) // TODO Remember to encrypt it
					put(COL_CONTENT_SALT, "") // TODO Generate new salt
					put(COL_CONTENT, c) // TODO Remember to encrypt it
					put(COMMON_MODF, timestamp)
				}
				val pid = helper.writableDatabase.insert(TABLE, null, newRow)
				return if (pid >= 0) {
					NoteRecord(pid, k,null, timestamp)
				} else {
					null
				}
			}

			/**
			 * Update the content of a note by the note ID.
			 */
			fun update(helper: SQLiteOpenHelper, nid: Long, content: String): Int {
				val args = arrayOf(nid.toString())
				val newRow = ContentValues().apply {
					put(COL_CONTENT_SALT, "") // TODO Generate new salt
					put(COL_CONTENT, content) // TODO Remember to encrypt it
					put(COMMON_MODF, Date().time)
				}
				return helper.writableDatabase.update(TABLE, newRow, COMMON_PKEY, args)
			}

			/**
			 * Delete one note by its ID.
			 */
			fun delete(helper: SQLiteOpenHelper, nid: Long): Int {
				val args = arrayOf(nid.toString())
				return helper.writableDatabase.delete(TABLE, COMMON_PKEY, args)
			}
		}
	}

	class NoteTags : BaseColumns {
		companion object {
			const val TABLE = "NoteTags"
			const val COL_TID = "tagId"
			private const val COL_NID = "noteId"

			const val SQL_CREATE =
					"create table $TABLE (" +
							"$PKEY integer primary key autoincrement," +
							"$COL_NID integer not null," +
							"$COL_TID integer not null," +
							"$COMMON_MODF integer)"
			const val SQL_DROP = "drop table if exists $TABLE"

			private const val QUERY_JOIN =
					"select nt.$COL_NID, nt.$COL_TID, t.${Tags.COL_TAG_NAME}, nt.$COMMON_MODF" +
					"  from $TABLE as nt" +
					" inner join ${Tags.TABLE} as t on nt.$COL_TID = t.$PKEY" +
					" where nt.$COL_NID = ?" +
					" order by t.${Tags.COL_TAG_NAME}"

			/**
			 * Select one note by its ID and return all tags associate with it.
			 */
			fun select(helper: SQLiteOpenHelper, nid: Long): List<TagRecord> {
				val args = arrayOf(nid.toString())
				val cursor = helper.readableDatabase.rawQuery(QUERY_JOIN, args)

				val result = mutableListOf<TagRecord>()
				with(cursor) {
					while (moveToNext()) {
						val tid = getLong(getColumnIndexOrThrow(COL_TID))
						val tag = getString(getColumnIndexOrThrow(Tags.COL_TAG_NAME))
						val mod = getLong(getColumnIndexOrThrow(COMMON_MODF))
						result.add(TagRecord(tid, tag, mod))
					}
				}

				cursor.close()
				return result
			}

			/**
			 * Add a note/tag relationship.
			 */
			fun insert(helper: SQLiteOpenHelper, nid: Long, tid: Long): Long {
				val newRow = ContentValues().apply {
					put(COL_NID, nid)
					put(COL_TID, tid)
					put(COMMON_MODF, Date().time)
				}
				return helper.writableDatabase.insert(TABLE, null, newRow)
			}

			/**
			 * Delete one note/tag relationship by the node ID and tag ID.
			 */
			fun delete(helper: SQLiteOpenHelper, nid: Long, tid: Long): Int {
				val where = "$COL_NID = ? and $COL_TID = ?"
				val args = arrayOf(nid.toString(), tid.toString())
				return helper.writableDatabase.delete(TABLE, where, args)
			}
		}
	}
}