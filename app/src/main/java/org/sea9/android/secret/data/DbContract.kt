package org.sea9.android.secret.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import org.sea9.android.secret.compat.SmartConverter
import org.sea9.android.secret.core.ContextFragment
import org.sea9.android.secret.crypto.CryptoUtils
import java.io.PrintWriter
import java.lang.IllegalStateException
import java.lang.StringBuilder
import java.util.*

object DbContract {
	const val DATABASE = "Secret.db_contract"
	const val PKEY = BaseColumns._ID
	const val COMMON_MODF = "modified"
	const val COMMON_PKEY = "$PKEY = ?"
	const val SQL_CONFIG = "PRAGMA foreign_keys=ON"

	class Tags : BaseColumns {
		companion object {
			const val TABLE = "Tags"
			const val COL_TAG_NAME = "tagName"
			private const val IDX_TAG = "idxTag"

			private val COLUMNS = arrayOf(PKEY, COL_TAG_NAME, COMMON_MODF)

			const val SQL_CREATE =
					"create table $TABLE (" +
					"$PKEY integer primary key autoincrement," +
					"$COL_TAG_NAME text not null COLLATE NOCASE," +
					"$COMMON_MODF integer)"
			const val SQL_CREATE_IDX = "create unique index $IDX_TAG on $TABLE ($COL_TAG_NAME)"
			const val SQL_DROP = "drop table if exists $TABLE"
			const val SQL_DROP_IDX = "drop index if exists $IDX_TAG"

			private const val QUERY_SEARCH =
					"select $PKEY from $TABLE where $COL_TAG_NAME = ?"

			private const val QUERY_DELETE =
					"delete from $TABLE where $PKEY not in " +
					"(select nt.${NoteTags.COL_TID} from ${NoteTags.TABLE} as nt inner join $TABLE as t on t.$PKEY = nt.${NoteTags.COL_TID})"

			/**
			 * Select all tags.
			 */
			fun select(helper: DbHelper): List<TagRecord> {
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
			 * Search by tag name if a record already exists or not.
			 */
			fun search(helper: DbHelper, tagName: String): List<Long> {
				val args = arrayOf(tagName)
				val cursor = helper.readableDatabase.rawQuery(QUERY_SEARCH, args)

				val result = mutableListOf<Long>()
				with(cursor) {
					while (moveToNext()) {
						val pid = getLong(getColumnIndexOrThrow(PKEY))
						result.add(pid)
					}
				}

				cursor.close()
				return result
			}

			/**
			 * Insert a tag.
			 */
			fun insert(helper: DbHelper, tagName: String): TagRecord? {
				val timestamp = Date().time
				val newRow = ContentValues().apply {
					put(COL_TAG_NAME, tagName)
					put(COMMON_MODF, timestamp)
				}
				val pid = helper.writableDatabase.insertOrThrow(TABLE, null, newRow)
				return if (pid >= 0) {
					TagRecord(pid, tagName, timestamp)
				} else {
					null
				}
			}

			/**
			 * Delete any unused tags.
			 */
			fun delete(helper: DbHelper): Int {
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
			private val EXPORTS = arrayOf(PKEY, COL_KEY, COL_KEY_SALT, COL_CONTENT, COL_CONTENT_SALT, COMMON_MODF)

			const val SQL_CREATE =
					"create table $TABLE (" +
					"$PKEY integer primary key autoincrement," +
					"$COL_KEY_SALT text not null," +
					"$COL_KEY text not null," +
					"$COL_CONTENT_SALT text not null," +
					"$COL_CONTENT text not null," +
					"$COMMON_MODF integer)"
			const val SQL_DROP = "drop table if exists $TABLE"
			private const val QUERY_COUNT = "select count($PKEY) from $TABLE"

			const val EXPORT_FORMAT_MIN_COLUMN = 5
			private const val OLD_FORMAT_COLUMN_COUNT = 6
			private const val CONVERTED_MIN_COLUMN = 2

			fun count(helper: DbHelper): Int {
				val cursor = helper.readableDatabase.rawQuery(QUERY_COUNT, null)
				var result = -1
				with(cursor) {
					while (moveToNext()) {
						if (columnCount == 1) {
							result = getInt(0)
							break
						}
					}
				}

				cursor.close()
				return result
			}

			/**
			 * Select all notes.
			 */
			fun select(helper: DbHelper): List<NoteRecord>? {
				// Not using order-by in query because keys are encrypted as well
				val cursor = helper.readableDatabase
						.query(TABLE, KEYS, null, null, null, null, null)

				val result = mutableSetOf<NoteRecord>()
				var error = false
				with(cursor) {
					while (moveToNext()) {
						val pid = getLong((getColumnIndexOrThrow(PKEY)))
						val modified = getLong(getColumnIndexOrThrow(COMMON_MODF))
						val slt = getString(getColumnIndexOrThrow(COL_KEY_SALT))
						val key = getString(getColumnIndexOrThrow(COL_KEY))

						val tags  = NoteTags.selectIds(helper, pid) as MutableList<Long>

						val txt = helper.crypto.decrypt(key.toCharArray(), CryptoUtils.decode(CryptoUtils.convert(slt.toCharArray())))
						if (txt != null) {
							result.add(NoteRecord(pid, String(txt), null, tags, modified))
						} else {
							error = true
							break
						}
					}
				}

				cursor.close()
				return if (!error) {
					result.asSequence()
							.sortedWith(compareBy { it.key }) // Sort here after decrypt
							.toMutableList()
				} else {
					null
				}
			}

			/**
			 * Select one note by its ID, returns only the content.
			 */
			fun select(helper: DbHelper, nid: Long): String? {
				val args = arrayOf(nid.toString())
				val cursor = helper.readableDatabase
						.query(TABLE, COLUMNS, COMMON_PKEY, args, null, null, null)

				var rowCount = 0
				lateinit var ctn: String
				lateinit var slt: String
				with(cursor) {
					while (moveToNext()) {
						rowCount ++
						slt = getString(getColumnIndexOrThrow(COL_CONTENT_SALT))
						ctn = getString(getColumnIndexOrThrow(COL_CONTENT))
					}
				}

				cursor.close()
				if (rowCount != 1) {
					throw IllegalStateException("Corrupted database table")
				} else {
					val ret = helper.crypto.decrypt(ctn.toCharArray(), CryptoUtils.decode(CryptoUtils.convert(slt.toCharArray())))
					return if (ret != null) {
						String(ret)
					} else {
						null
					}
				}
			}

			/**
			 * Insert a new note and all associated tag relations.
			 */
			fun insert(helper: DbHelper, key: String, content: String, tags: List<Long>): Long? {
				val db = helper.writableDatabase

				val ksalt = CryptoUtils.generateSalt()
				val csalt = CryptoUtils.generateSalt()
				val kcphr = helper.crypto.encrypt(key.toCharArray(), ksalt)
				val ccphr = helper.crypto.encrypt(content.toCharArray(), csalt)
				val kcomp = key.trim().toLowerCase()

				val newRow = ContentValues().apply {
					put(COL_KEY_SALT, String(CryptoUtils.convert(CryptoUtils.encode(ksalt))))
					put(COL_KEY, String(kcphr))
					put(COL_CONTENT_SALT, String(CryptoUtils.convert(CryptoUtils.encode(csalt))))
					put(COL_CONTENT, String(ccphr))
					put(COMMON_MODF, Date().time)
				}

				db.beginTransactionNonExclusive()
				try {
					val cursor = db.query(TABLE, KEYS, null, null, null, null, null)
					with(cursor) {
						while (moveToNext()) {
							val pid = getLong((getColumnIndexOrThrow(PKEY)))
							val slt = getString(getColumnIndexOrThrow(COL_KEY_SALT))
							val ttl = getString(getColumnIndexOrThrow(COL_KEY))
							val txt = helper.crypto.decrypt(ttl.toCharArray(), CryptoUtils.decode(CryptoUtils.convert(slt.toCharArray())))
									?: run {
										close()
										return null
									} //incorrect password

							if (String(txt).trim().toLowerCase() == kcomp) {
								close()
								return -1 * pid
							} // found duplicated key
						}
						close()
					}

					val nid = db.insertOrThrow(TABLE, null, newRow)
					if (nid >= 0) {
						var failed = 0
						for (tid in tags) {
							if (NoteTags.insert(db, nid, tid) < 0) failed++
						}
						if (failed > 0)
							return null

						db.setTransactionSuccessful()
					}
					return nid
				} finally {
					db.endTransaction()
				}
			}

			/**
			 * Update the content of a note, and delete/insert all associated tag relations, by the note ID.
			 */
			fun update(helper: DbHelper, nid: Long, content: String, tags: List<Long>): Int {
				val args = arrayOf(nid.toString())
				val db = helper.writableDatabase
				var ret = -1

				val salt = CryptoUtils.generateSalt()
				val cphr = helper.crypto.encrypt(content.toCharArray(), salt)

				val newRow = ContentValues().apply {
					put(COL_CONTENT_SALT, String(CryptoUtils.convert(CryptoUtils.encode(salt))))
					put(COL_CONTENT, String(cphr))
					put(COMMON_MODF, Date().time)
				}

				db.beginTransactionNonExclusive()
				try {
					val count = db.delete(NoteTags.TABLE, "${NoteTags.COL_NID} = ?", args)
					if (count >= 0) {
						var failed = 0
						for (tid in tags) {
							if (NoteTags.insert(db, nid, tid) < 0) failed ++
						}
						if (failed == 0) {
							ret = db.update(TABLE, newRow, COMMON_PKEY, args)
							if (ret >= 0) {
								db.setTransactionSuccessful()
							}
						}
					}
					return ret
				} finally {
					db.endTransaction()
				}
			}

			/**
			 * Delete a note, and all its associated tag relations, by its ID.
			 */
			fun delete(helper: DbHelper, nid: Long): Int {
				val args = arrayOf(nid.toString())
				val db = helper.writableDatabase
				var ret = -1

				db.beginTransactionNonExclusive()
				try {
					val count = db.delete(NoteTags.TABLE, "${NoteTags.COL_NID} = ?", args)
					if (count >= 0) {
						ret = db.delete(TABLE, COMMON_PKEY, args)
						if (ret >= 0) {
							db.setTransactionSuccessful()
						}
					}
					return ret
				} finally {
					db.endTransaction()
				}
			}

			/**
			 * Export notes in encrypted format.
			 */
			fun doExport(helper: DbHelper, out: PrintWriter): Int {
				val cursor = helper.readableDatabase
						.query(TABLE, EXPORTS, null, null, null, null, null)

				val buff = StringBuilder()
				var count = 0
				with(cursor) {
					while (moveToNext()) {
						buff.setLength(0)
						val pid = getLong(getColumnIndexOrThrow(PKEY))
						val kslt = getString(getColumnIndexOrThrow(COL_KEY_SALT))
						val key = getString(getColumnIndexOrThrow(COL_KEY))
						val cslt = getString(getColumnIndexOrThrow(COL_CONTENT_SALT))
						val ctn = getString(getColumnIndexOrThrow(COL_CONTENT))
						val modified = getLong(getColumnIndexOrThrow(COMMON_MODF))
						buff.append(kslt)
								.append(ContextFragment.TAB).append(key)
								.append(ContextFragment.TAB).append(cslt)
								.append(ContextFragment.TAB).append(ctn)
								.append(ContextFragment.TAB).append(modified)

						val tags = NoteTags.select(helper, pid)
						for (record in tags) {
							buff.append(ContextFragment.TAB).append(record.tag)
						}

						out.println(buff.toString())
						count ++
					}
				}

				cursor.close()
				return count
			}

			fun doOldImport(helper: DbHelper, crypto: DbHelper.Crypto, input: Array<String>, smart: SmartConverter?): Int {
				if (input.size == OLD_FORMAT_COLUMN_COUNT) {
					val db = helper.writableDatabase

					// Decrypt using old password and old methods, return if decryption fail, possibly incorrect password
					val dttl = crypto.decrypt(input[3].toCharArray(), CryptoUtils.decode(CryptoUtils.convert(input[1].toCharArray())))
							?: return -3
					val dctn = crypto.decrypt(input[4].toCharArray(), CryptoUtils.decode(CryptoUtils.convert(input[1].toCharArray())))
							?: return -2

					var count = 0
					val data: List<Array<String>>
					data = if (smart != null)
						smart.convert(input[2], String(dttl), String(dctn))
					else
						listOf(arrayOf(String(dttl), String(dctn), input[2]))

					db.beginTransactionNonExclusive()
					try {
						for (datum in data) {
							val kslt = CryptoUtils.generateSalt()
							val cslt = CryptoUtils.generateSalt()

							// Encrypt using new password
							val kcph = crypto.encrypt(datum[0].toCharArray(), kslt)
							val ccph = crypto.encrypt(datum[1].toCharArray(), cslt)

							val newRow = ContentValues().apply {
								put(COL_KEY_SALT, String(CryptoUtils.convert(CryptoUtils.encode(kslt))))
								put(COL_KEY, String(kcph))
								put(COL_CONTENT_SALT, String(CryptoUtils.convert(CryptoUtils.encode(cslt))))
								put(COL_CONTENT, String(ccph))
								put(COMMON_MODF, input[5].toLong())
							}

							val nid = helper.writableDatabase.insertOrThrow(TABLE, null, newRow)
							if (nid >= 0) {
								var tagCount = 0
								if (datum.size > CONVERTED_MIN_COLUMN) {
									for (i in CONVERTED_MIN_COLUMN until datum.size) {
										val tags = Tags.search(helper, datum[i])
										val tid = if (tags.isNotEmpty())
											tags[0]
										else
											Tags.insert(helper, datum[i])?.pid

										if ((tid != null) && (tid >= 0)) {
											if (NoteTags.insert(helper, nid, tid) >= 0)
												tagCount++
										}
									}
								}
								if (tagCount == (datum.size - CONVERTED_MIN_COLUMN))
									count ++
							}
						}
						if (count == data.size)
							db.setTransactionSuccessful()
						return (count - data.size)
					} finally {
						db.endTransaction()
					}
				}
				return -1 // Incoming data with invalid format, or decryption failed
			}

			/**
			 * Import one note with tag relations and (if any) new tags.
			 */
			fun doImport(helper: DbHelper, crypto: DbHelper.Crypto, data: Array<String>): Long {
				if (data.size >= EXPORT_FORMAT_MIN_COLUMN) {
					val db = helper.writableDatabase

					db.beginTransactionNonExclusive()
					try {
						// Decrypt using old password and old methods, return if decryption fail, possibly incorrect password
						val dkey = crypto.decrypt(data[1].toCharArray(), CryptoUtils.decode(CryptoUtils.convert(data[0].toCharArray())))
								?: return -3
						val dctn = crypto.decrypt(data[3].toCharArray(), CryptoUtils.decode(CryptoUtils.convert(data[2].toCharArray())))
								?: return -2

						val kslt = CryptoUtils.generateSalt()
						val cslt = CryptoUtils.generateSalt()

						// Encrypt using new password
						val kcph = crypto.encrypt(dkey, kslt)
						val ccph = crypto.encrypt(dctn, cslt)

						val newRow = ContentValues().apply {
							put(COL_KEY_SALT, String(CryptoUtils.convert(CryptoUtils.encode(kslt))))
							put(COL_KEY, String(kcph))
							put(COL_CONTENT_SALT, String(CryptoUtils.convert(CryptoUtils.encode(cslt))))
							put(COL_CONTENT, String(ccph))
							put(COMMON_MODF, data[4].toLong())
						}

						val nid = helper.writableDatabase.insertOrThrow(TABLE, null, newRow)
						if (nid >= 0) {
							var count = 0
							if (data.size > EXPORT_FORMAT_MIN_COLUMN) {
								for (i in EXPORT_FORMAT_MIN_COLUMN until data.size) {
									val tags = Tags.search(helper, data[i])
									val tid = if (tags.isNotEmpty())
										tags[0]
									else
										Tags.insert(helper, data[i])?.pid

									if ((tid != null) && (tid >= 0)) {
										if (NoteTags.insert(helper, nid, tid) >= 0)
											count++
									}
								}
							}
							if (count == (data.size - EXPORT_FORMAT_MIN_COLUMN))
								db.setTransactionSuccessful()
						}
						return nid
					} finally {
						db.endTransaction()
					}
				}
				return -1 // Incoming data with invalid format, or decryption failed
			}

			/**
			 * Re-encrypt, thus effectively changing the password.
			 * @return 0 if successful, negative otherwise.
			 */
			fun passwd(helper: DbHelper, crypto: DbHelper.Crypto): Int {
				val db = helper.writableDatabase
				var count = 0
				var succd = 0

				db.beginTransactionNonExclusive()
				try {
					val cursor = db.query(TABLE, EXPORTS, null, null, null, null, null)

					with(cursor) {
						while (moveToNext()) {
							val pid = getLong((getColumnIndexOrThrow(PKEY)))
							val kslt = getString(getColumnIndexOrThrow(COL_KEY_SALT))
							val ekey = getString(getColumnIndexOrThrow(COL_KEY))
							val cslt = getString(getColumnIndexOrThrow(COL_CONTENT_SALT))
							val ectn = getString(getColumnIndexOrThrow(COL_CONTENT))

							// Decrypt using old password, break if decryption fail, possibly incorrect password
							val ckey = crypto.decrypt(ekey.toCharArray(), CryptoUtils.decode(CryptoUtils.convert(kslt.toCharArray())))
									?: break
							val cctn = crypto.decrypt(ectn.toCharArray(), CryptoUtils.decode(CryptoUtils.convert(cslt.toCharArray())))
									?: break

							val nkst = CryptoUtils.generateSalt()
							val ncst = CryptoUtils.generateSalt()

							// Encrypt using new password
							val nkey = crypto.encrypt(ckey, nkst)
							val nctn = crypto.encrypt(cctn, ncst)

							val args = arrayOf(pid.toString())
							val newRow = ContentValues().apply {
								put(COL_KEY_SALT, String(CryptoUtils.convert(CryptoUtils.encode(nkst))))
								put(COL_KEY, String(nkey))
								put(COL_CONTENT_SALT, String(CryptoUtils.convert(CryptoUtils.encode(ncst))))
								put(COL_CONTENT, String(nctn))
								put(COMMON_MODF, Date().time)
							}

							if (db.update(TABLE, newRow, COMMON_PKEY, args) == 1) succd ++
							count ++
						}

						if (succd == count)
							db.setTransactionSuccessful()
					}
					cursor.close()
				} finally {
					db.endTransaction()
				}
				return (succd - count)
			}
		}
	}

	class NoteTags : BaseColumns {
		companion object {
			const val TABLE = "NoteTags"
			const val COL_NID = "noteId"
			const val COL_TID = "tagId"

			const val SQL_CREATE =
					"create table $TABLE (" +
					"$PKEY integer primary key autoincrement," +
					"$COL_NID integer not null," +
					"$COL_TID integer not null," +
					"$COMMON_MODF integer," +
					"foreign key($COL_NID) references ${Notes.TABLE}($PKEY)," +
					"foreign key($COL_TID) references ${Tags.TABLE}($PKEY))"
			const val SQL_DROP = "drop table if exists $TABLE"

			private const val QUERY_CONTENT =
					"select nt.$COL_NID, nt.$COL_TID, t.${Tags.COL_TAG_NAME}, nt.$COMMON_MODF" +
					"  from $TABLE as nt" +
					" inner join ${Tags.TABLE} as t on nt.$COL_TID = t.$PKEY" +
					" where nt.$COL_NID = ?" +
					" order by t.${Tags.COL_TAG_NAME}"

			/**
			 * Select one note by its ID and return all tags associate with it.
			 */
			fun select(helper: DbHelper, nid: Long): List<TagRecord> {
				val args = arrayOf(nid.toString())
				val cursor = helper.readableDatabase.rawQuery(QUERY_CONTENT, args)

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
			fun selectIds(helper: DbHelper, nid: Long): List<Long> {
				val args = arrayOf(nid.toString())
				val cursor = helper.readableDatabase.rawQuery(QUERY_CONTENT, args)

				val result = mutableListOf<Long>()
				with(cursor) {
					while (moveToNext()) {
						val tid = getLong(getColumnIndexOrThrow(COL_TID))
						result.add(tid)
					}
				}

				cursor.close()
				return result
			}

			/**
			 * Add a note/tag relationship.
			 */
			fun insert(helper: DbHelper, nid: Long, tid: Long): Long {
				return insert(helper.writableDatabase, nid, tid)
			}
			fun insert(db: SQLiteDatabase, nid: Long, tid: Long): Long {
				val newRow = ContentValues().apply {
					put(COL_NID, nid)
					put(COL_TID, tid)
					put(COMMON_MODF, Date().time)
				}
				return db.insertOrThrow(TABLE, null, newRow)
			}

		}
	}
}