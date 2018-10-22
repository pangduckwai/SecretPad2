package org.sea9.android.secret

import android.content.Context
import android.database.SQLException
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.text.Editable
import android.util.Log
import org.junit.*
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.sea9.android.secret.crypto.CryptoUtils
import org.sea9.android.secret.data.DbContract
import org.sea9.android.secret.data.DbHelper
import org.sea9.android.secret.data.NoteRecord
import org.sea9.android.secret.data.TagRecord

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SecretPad2InstrumentedTest {
	companion object {
		private lateinit var password: CharArray
		private lateinit var context: Context
		private lateinit var helper: DbHelper

		private var tid: Long = -1
		private var ti2: Long = -1
		private var nid: Long = -1
		private var aid: Long = -1

		@BeforeClass @JvmStatic
		fun prepare() {
			password = CryptoUtils.convert(CryptoUtils.encode(CryptoUtils.hash(CryptoUtils.convert("abcd1234".toCharArray()))))
			context = InstrumentationRegistry.getTargetContext()
			helper = DbHelper(object : DbHelper.Caller {
				override fun getContext(): Context? {
					return context
				}

				override fun onReady() {
					Log.w("secret.instrumented_test", "DB test connection ready")
				}

				override fun encrypt(input: CharArray, salt: ByteArray): CharArray {
					return CryptoUtils.encrypt(input, password, salt)
				}

				override fun decrypt(input: CharArray, salt: ByteArray): CharArray {
					return CryptoUtils.decrypt(input, password, salt)
				}
			}, true)
			helper.writableDatabase.execSQL(DbContract.SQL_CONFIG)

			val tags = arrayOfNulls<TagRecord>(6)
			tags[0] = DbContract.Tags.insert(helper, "NINE")
			tags[1] = DbContract.Tags.insert(helper, "ONE")
			tags[2] = DbContract.Tags.insert(helper, "TWO")
			tags[3] = DbContract.Tags.insert(helper, "SIX")
			tags[4] = DbContract.Tags.insert(helper, "XXX")
			tags[5] = DbContract.Tags.insert(helper, "TEN")
			tid = tags[0]!!.pid
			ti2 = tags[4]!!.pid

			val notes = arrayOfNulls<NoteRecord>(7)
			notes[0] = DbContract.Notes.insert(helper, "KEY03", "CONTENT03")
			notes[1] = DbContract.Notes.insert(helper, "KEY05", "CONTENT05")
			notes[2] = DbContract.Notes.insert(helper, "KEY01", "CONTENT01")
			notes[3] = DbContract.Notes.insert(helper, "KEY04", "CONTENT04")
			notes[4] = DbContract.Notes.insert(helper, "KEY06", "CONTENT06")
			notes[5] = DbContract.Notes.insert(helper, "KEY07", "CONTENT07")
			notes[6] = DbContract.Notes.insert(helper, "KEY02", "CONTENT02")
			nid = notes[0]!!.pid

			aid = DbContract.NoteTags.insert(helper, notes[0]!!.pid, tags[0]!!.pid)
			DbContract.NoteTags.insert(helper, notes[0]!!.pid, tags[1]!!.pid)
			DbContract.NoteTags.insert(helper, notes[1]!!.pid, tags[2]!!.pid)
			DbContract.NoteTags.insert(helper, notes[1]!!.pid, tags[3]!!.pid)
			DbContract.NoteTags.insert(helper, notes[2]!!.pid, tags[4]!!.pid)
			DbContract.NoteTags.insert(helper, notes[2]!!.pid, tags[5]!!.pid)
			DbContract.NoteTags.insert(helper, notes[3]!!.pid, tags[1]!!.pid)
			DbContract.NoteTags.insert(helper, notes[3]!!.pid, tags[2]!!.pid)
			DbContract.NoteTags.insert(helper, notes[4]!!.pid, tags[3]!!.pid)
			DbContract.NoteTags.insert(helper, notes[4]!!.pid, tags[4]!!.pid)
			DbContract.NoteTags.insert(helper, notes[5]!!.pid, tags[5]!!.pid)
			DbContract.NoteTags.insert(helper, notes[5]!!.pid, tags[1]!!.pid)
			DbContract.NoteTags.insert(helper, notes[6]!!.pid, tags[2]!!.pid)
			DbContract.NoteTags.insert(helper, notes[6]!!.pid, tags[3]!!.pid)
		}

		@AfterClass @JvmStatic
		fun cleanup() {
			helper.deleteDatabase()
		}
	}

	@Test
	fun useAppContext() {
		// Context of the app under test.
		val appContext = InstrumentationRegistry.getTargetContext()
		assertEquals("org.sea9.android.secret", appContext.packageName)
	}

	@Test
	fun testCryptoHash() {
		val factory = Editable.Factory.getInstance()
		val editable = factory.newEditable("abcd1234")
		val length = editable.length
		val message = CharArray(length)
		editable.getChars(0, length, message, 0)
		editable.clear()
		val hashed = CryptoUtils.convert(CryptoUtils.encode(CryptoUtils.hash(CryptoUtils.convert(message))))
		assertTrue(password.contentEquals(hashed))
	}

	@Test
	fun testEncryptDecrypt() {
		val secret = "HellohowAreyoutoday?I'mfineThankYou".toCharArray()
		val salt = CryptoUtils.generateSalt()
		val cipherText = CryptoUtils.encrypt(secret, password, salt)
		val clearText = CryptoUtils.decrypt(cipherText, password, salt)
		assertTrue(secret.contentEquals(clearText))
	}

	@Test(expected = SQLException::class)
	fun testForeignKeyAddAssociation1() {
		DbContract.NoteTags.insert(helper, nid, 999)
	}

	@Test(expected = SQLException::class)
	fun testForeignKeyAddAssociation2() {
		DbContract.NoteTags.insert(helper, 999, ti2)
	}

	@Test(expected = SQLException::class)
	fun testForeignKeyDeleteNote() {
		helper.writableDatabase.delete("Notes", "_id = ?", arrayOf(nid.toString()))
	}

	@Test(expected = SQLException::class)
	fun testForeignKeyDeleteTag() {
		helper.writableDatabase.delete("Tags", "_id = ?", arrayOf(ti2.toString()))
	}

	@Test(expected = SQLException::class)
	fun testUniqueIndex() {
		DbContract.Tags.insert(helper, "One")
	}

	@Test
	fun testRecordCount() {
		var associationCount = 0
		val tagCount = DbContract.Tags.select(helper).size
		val noteList = DbContract.Notes.select(helper)
		for (record in noteList!!) {
			associationCount += DbContract.NoteTags.select(helper, record.pid).size
		}
		assertEquals(5, tagCount)
		assertEquals(7, noteList.size)
		assertEquals(13, associationCount)
	}

	@Test
	fun testReadEncryptedFields() {
		var count = -1
		val list = DbContract.Notes.select(helper)
		count ++
		for (record in list!!) {
			DbContract.Notes.select(helper, record.pid)
			count ++
		}
		assertTrue(count >= 0)
	}

	@Test
	fun testDeleteUnusedTags() {
		val before = DbContract.Tags.select(helper).size
		helper.writableDatabase.delete("NoteTags", "_id = ?", arrayOf(aid.toString()))
		val deleted = DbContract.Tags.delete(helper)
		val after = DbContract.Tags.select(helper).size
		assertTrue((before == (after + deleted)) && (deleted > 0))
	}
/*
	// List notes with tags
	private static final String SQL_TEST0 =
			"select n._id, n.noteKey, n.keySalt, n.noteContent, n.contentSalt, t._id, t.tagName" +
					"  from NoteTags as nt" +
					" inner join Notes as n on n._id = nt.noteId" +
					" inner join Tags  as t on t._id = nt.tagId";
	private void test003(DbHelper helper) {
		Cursor cursor = helper.getReadableDatabase().rawQuery(SQL_TEST0, null);
		StringBuilder builder = new StringBuilder("Notes:");
		builder.append('\n');
		String cols[] = cursor.getColumnNames();
		for (String col : cols) {
			builder.append(col).append('\t');
		}
		builder.append('\n');
		while (cursor.moveToNext()) {
			for (int i = 0; i < cols.length; i ++) {
				String data = cursor.getString(i);
				if (data.length() >= 24) data = data.trim().substring(0, 24);
				builder.append(data).append('\t');
			}
			builder.append('\n');
		}
		cursor.close();
		Log.w("secret.db_test003", builder.toString());
	}

	// List unused tags
	private static final String SQL_TEST7 =
			"select * from Tags where _id not in " +
					"(select nt.tagId from NoteTags as nt inner join Tags as t on t._id = nt.tagId)";
	private void test007(DbHelper helper) {
		Cursor cursor = helper.getReadableDatabase().rawQuery(SQL_TEST7, null);
		StringBuilder builder = new StringBuilder("Unused tags:");
		builder.append('\n');
		String cols[] = cursor.getColumnNames();
		for (String col : cols) {
			builder.append(col).append('\t');
		}
		builder.append('\n');
		while (cursor.moveToNext()) {
			for (int i = 0; i < cols.length; i ++) {
				builder.append(cursor.getString(i)).append('\t');
			}
			builder.append('\n');
		}
		cursor.close();
		Log.w("secret.db_test007", builder.toString());
	}
 */
}
