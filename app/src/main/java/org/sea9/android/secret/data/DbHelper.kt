package org.sea9.android.secret.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DbHelper(private val callback: Listener, private val isTest: Boolean):
		SQLiteOpenHelper(callback.getContext()
				, DB_NAME + (if (isTest) "_test" else "")
				, null
				, DB_VERN) {
	constructor(callback: Listener): this(callback, false)

	companion object {
		const val TAG = "secret.db_contract"
		const val DB_NAME = DbContract.DATABASE
		const val DB_VERN = 1
	}

	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL(DbContract.Tags.SQL_CREATE)
		db.execSQL(DbContract.Tags.SQL_CREATE_IDX)
		db.execSQL(DbContract.Notes.SQL_CREATE)
		db.execSQL(DbContract.NoteTags.SQL_CREATE)
		Log.i(TAG, "Database ${db.path} version ${db.version} created")
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		Log.i(TAG, "Upgrading database from version $oldVersion to $newVersion, which will destroy all old data")
		db.execSQL(DbContract.NoteTags.SQL_DROP)
		db.execSQL(DbContract.Notes.SQL_DROP)
		db.execSQL(DbContract.Tags.SQL_DROP_IDX)
		db.execSQL(DbContract.Tags.SQL_DROP)
		onCreate(db)
	}

	fun deleteDatabase() {
		val dbName = databaseName
		writableDatabase.execSQL(DbContract.NoteTags.SQL_DROP)
		writableDatabase.execSQL(DbContract.Notes.SQL_DROP)
		writableDatabase.execSQL(DbContract.Tags.SQL_DROP_IDX)
		writableDatabase.execSQL(DbContract.Tags.SQL_DROP)
		callback.getContext()?.deleteDatabase(databaseName)
		Log.i(TAG, "Database $dbName deleted")
	}

	var ready: Boolean = false

	override fun onOpen(db: SQLiteDatabase?) {
		super.onOpen(db)
		ready = true
		callback.onReady()
	}

	fun encrypt(input: CharArray, salt: ByteArray): CharArray {
		return callback.encrypt(input, salt)
	}

	fun decrypt(input: CharArray, salt: ByteArray): CharArray {
		return callback.decrypt(input, salt)
	}

	interface Listener {
		fun getContext(): Context?
		fun onReady()
		fun encrypt(input: CharArray, salt: ByteArray): CharArray
		fun decrypt(input: CharArray, salt: ByteArray): CharArray
	}
}