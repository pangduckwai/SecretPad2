package org.sea9.android.secret.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DbHelper(private val caller: Caller, val crypto: Crypto, isTest: Boolean):
		SQLiteOpenHelper(caller.getContext()
				, DB_NAME + (if (isTest) "_test" else "")
				, null
				, DB_VERN) {
	constructor(caller: Caller, crypto: Crypto): this(caller, crypto, false)

	companion object {
		const val TAG = "secret.db_helper"
		const val DB_NAME = DbContract.DATABASE
		const val DB_VERN = 100
	}

	override fun close() {
		super.close()
		ready = false
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
		caller.getContext()?.deleteDatabase(databaseName)
		Log.i(TAG, "Database $dbName deleted")
	}

	var ready: Boolean = false

	override fun onOpen(db: SQLiteDatabase?) {
		super.onOpen(db)
		ready = true
		caller.onReady()
	}

	/*=========================================
	 * Access interfaces to the ContextFragment
	 */
	interface Caller {
		fun getContext(): Context?
		fun onReady()
	}

	interface Crypto {
		fun encrypt(input: CharArray, salt: ByteArray): CharArray
		fun decrypt(input: CharArray, salt: ByteArray): CharArray?
	}
}