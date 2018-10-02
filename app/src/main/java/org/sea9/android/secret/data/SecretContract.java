package org.sea9.android.secret.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.Date;

public final class SecretContract {
	static final String TAG = "secret.db_contract";
	private SecretContract() {}

	// Common names
	public static final String COL_MODF = "modified";

	public static class Tags implements BaseColumns {
		public static final String TABLE = "Tags";
		public static final String COL_NAME = "tagName";

		public static final String[] COLS = { _ID, COL_NAME, COL_MODF };

		public static final String SQL_CREATE =
				"create table " + TABLE + " ( "
					+ _ID + " integer primary key autoincrement, "
					+ COL_NAME + " text not null, "
					+ COL_MODF + " integer);";

		public static Cursor select(SQLiteOpenHelper helper) {
			return helper.getReadableDatabase().query(TABLE, COLS, null, null, null, null, COL_NAME);
		}

		public static long insert(SQLiteOpenHelper helper, String tagName) {
			ContentValues newRow = new ContentValues();
			newRow.put(COL_NAME, tagName);
			newRow.put(COL_MODF, (new Date()).getTime());
			SQLiteDatabase db = helper.getWritableDatabase();
			return db.insert(TABLE, null, newRow);
		}

		public static int delete(SQLiteOpenHelper helper) {
			String SQL = "delete from " + TABLE + " as t where not exists "
						+ "(select 1 from " + NoteTags.TABLE + " as nt where nt.tagId = t." + _ID + ")";
			SQLiteDatabase db = helper.getWritableDatabase();
			Cursor cursor = db.rawQuery(SQL, null);
			return 1;
		}
	}

	public static class Notes implements BaseColumns {
		public static final String TABLE = "Notes";
		public static final String COL_NKEY = "noteKey";
		public static final String COL_CTNT = "content";
		public static final String COL_SLT1 = "salt1";
		public static final String COL_SLT2 = "salt2";

		public static final String[] COLS = { _ID, COL_SLT1, COL_NKEY, COL_SLT2, COL_CTNT, COL_MODF };

		public static final String SQL_CREATE =
				"create table " + TABLE + " ( "
					+ _ID + " integer primary key autoincrement, "
					+ COL_SLT1 + " text not null, "
					+ COL_NKEY + " text not null, "
					+ COL_SLT2 + " text not null, "
					+ COL_CTNT + " text not null, "
					+ COL_MODF + " integer);";

	}

	public static class NoteTags implements BaseColumns {
		public static final String TABLE = "NoteTags";
		public static final String COL_NID = "noteId";
		public static final String COL_TID = "tagId";

		public static final String[] COLS = { _ID, COL_NID, COL_TID, COL_MODF };

		public static final String SQL_CREATE =
				"create table " + TABLE + " ( "
					+ _ID + " integer primary key autoincrement, "
					+ COL_NID + " integer not null, "
					+ COL_TID + " integer not null, "
					+ COL_MODF + " integer);";
	}

	static class DbHelper extends SQLiteOpenHelper {
		static final int DB_VER = 1;
		static final String DB_NAME = "Secret.db";

		DbHelper(Context context) {
			super(context, DB_NAME, null, DB_VER);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(Notes.SQL_CREATE);
			db.execSQL(Tags.SQL_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + Tags.TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + Notes.TABLE);
			onCreate(db);
		}
	}
	static final SQLiteOpenHelper getHelper(Context context) {
		return new DbHelper(context);
	}
}
