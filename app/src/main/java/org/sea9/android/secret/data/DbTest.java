package org.sea9.android.secret.data;

import android.database.Cursor;
import android.util.Log;

public class DbTest {
	public DbTest(DbHelper helper) {
		prepare(helper);
		test1(helper);
		test2(helper);
		test3(helper);
		test2(helper);
		teardown(helper);
	}

	private void prepare(DbHelper dbHelper) {
		TagRecord tags[] = new TagRecord[5];
		tags[0] = DbContract.Tags.Companion.insert(dbHelper, "ONE");
		tags[1] = DbContract.Tags.Companion.insert(dbHelper, "TWO");
		tags[2] = DbContract.Tags.Companion.insert(dbHelper, "SIX");
		tags[3] = DbContract.Tags.Companion.insert(dbHelper, "NINE");
		tags[4] = DbContract.Tags.Companion.insert(dbHelper, "TEN");

		NoteRecord notes[] = new NoteRecord[5];
		notes[0] = DbContract.Notes.Companion.insert(dbHelper, "K0001", "NOTE0001");
		notes[1] = DbContract.Notes.Companion.insert(dbHelper, "K0002", "NOTE0002");
		notes[2] = DbContract.Notes.Companion.insert(dbHelper, "K0003", "NOTE0003");
		notes[3] = DbContract.Notes.Companion.insert(dbHelper, "K0004", "NOTE0004");
		notes[4] = DbContract.Notes.Companion.insert(dbHelper, "K0005", "NOTE0005");

		DbContract.NoteTags.Companion.insert(dbHelper, notes[0].getPid(), tags[0].getPid()); //ONE
		DbContract.NoteTags.Companion.insert(dbHelper, notes[0].getPid(), tags[1].getPid()); //TWO
		DbContract.NoteTags.Companion.insert(dbHelper, notes[1].getPid(), tags[2].getPid()); //SIX
		DbContract.NoteTags.Companion.insert(dbHelper, notes[1].getPid(), tags[3].getPid()); //NINE
		DbContract.NoteTags.Companion.insert(dbHelper, notes[2].getPid(), tags[4].getPid()); //TEN
		DbContract.NoteTags.Companion.insert(dbHelper, notes[2].getPid(), tags[0].getPid());
		DbContract.NoteTags.Companion.insert(dbHelper, notes[3].getPid(), tags[1].getPid());
		DbContract.NoteTags.Companion.insert(dbHelper, notes[3].getPid(), tags[2].getPid());
		DbContract.NoteTags.Companion.insert(dbHelper, notes[4].getPid(), tags[4].getPid());
		DbContract.NoteTags.Companion.insert(dbHelper, notes[4].getPid(), tags[0].getPid());
	}

	private void teardown(DbHelper helper) {
		helper.getWritableDatabase().execSQL("drop table if exists NoteTags");
		helper.getWritableDatabase().execSQL("drop table if exists Notes");
		helper.getWritableDatabase().execSQL("drop table if exists Tags");
	}

	private void test1(DbHelper helper) {
		String SQL =
			"select n._id, n.noteKey, n.noteContent, t._id, t.tagName" +
			"  from Notes as n" +
			" inner join NoteTags as nt on nt.noteId = n._id" +
			" inner join Tags as t on t._id = nt.tagId";
		Cursor cursor = helper.getReadableDatabase().rawQuery(SQL, null);
		while (cursor.moveToNext()) {
			String c0 = cursor.getString(0);
			String c1 = cursor.getString(1);
			String c2 = cursor.getString(2);
			String c3 = cursor.getString(3);
			String c4 = cursor.getString(4);
			Log.d("secret.db_test1", c0 + "  " + c1 + "  " + c2 + "  " + c3 + "  " + c4);
		}
		cursor.close();
	}

	private void test2(DbHelper helper) {
		String SQL = "select distinct t.tagName from Tags as t left join NoteTags as nt on t._id = nt.tagId";
		Cursor cursor = helper.getReadableDatabase().rawQuery(SQL, null);
		while (cursor.moveToNext()) {
			Log.d("secret.db_test2", cursor.getString(0));
		}
		cursor.close();
	}

	private void test3(DbHelper helper) {
		String SQL = "delete from NoteTags where noteId = 2 and tagId = 4";
		helper.getWritableDatabase().execSQL(SQL);
//		String cols[] = cursor.getColumnNames();
//		Log.d("secret.db_test3", "Cols " + cols.length);
//		for (String s : cols) {
//			Log.d("secret.db_test3a", s);
//		}
//		while (cursor.moveToNext()) {
//			Log.d("secret.db_test3b", cursor.getString(0));
//		}
//		cursor.close();
	}
}
