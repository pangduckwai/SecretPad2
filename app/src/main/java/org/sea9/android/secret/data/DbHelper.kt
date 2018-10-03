package org.sea9.android.secret.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DbHelper(context: Context): SQLiteOpenHelper(context, DB_NAME, null, DB_VERN) {
	val dbHelper = DbHelper(context)

	companion object {
		const val TAG = "secret.db_contract"
		const val DB_NAME = "Secret.db"
		const val DB_VERN = 1
	}

	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL(DbContract.Notes.SQL_CREATE)
		db.execSQL(DbContract.Tags.SQL_CREATE)
		db.execSQL(DbContract.NoteTags.SQL_CREATE)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		Log.i(TAG, "Upgrading database from version $oldVersion to $newVersion, which will destroy all old data")
		db.execSQL("DROP TABLE IF EXISTS ${DbContract.NoteTags.TABLE}")
		db.execSQL("DROP TABLE IF EXISTS ${DbContract.Tags.TABLE}")
		db.execSQL("DROP TABLE IF EXISTS ${DbContract.Notes.TABLE}")
		onCreate(db)
	}
}