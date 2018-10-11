package org.sea9.android.secret.data;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.util.Log;

import org.sea9.android.secret.ContextFragment;

import java.util.ArrayList;
import java.util.List;

public class DbTest {
	public DbTest(Context context, ContextFragment ctxFrag, DbHelper helper, boolean cleanUp) {
		prepare(helper);
		test(helper);
//		test1(ctxFrag);
//		test2(helper);
//		test3(helper);
//		test4(helper);
//		test2(helper);
//		test1(ctxFrag);
		if (cleanUp) {
			helper.getWritableDatabase().execSQL(DbContract.NoteTags.SQL_DROP);
			helper.getWritableDatabase().execSQL(DbContract.Notes.SQL_DROP);
			helper.getWritableDatabase().execSQL(DbContract.Tags.SQL_DROP);
			if (context != null) context.deleteDatabase("Secret.db");
		}
	}

	private void prepare(DbHelper dbHelper) {
		TagRecord tags[] = new TagRecord[5];
		long cnt = DatabaseUtils.queryNumEntries(dbHelper.getReadableDatabase(), "Tags");
		if (cnt > 0) {
			Log.w("secret.db_test", "Number of records in Tags: " + cnt);
		} else {
			tags[0] = DbContract.Tags.Companion.insert(dbHelper, "ONE");
			tags[1] = DbContract.Tags.Companion.insert(dbHelper, "TWO");
			tags[2] = DbContract.Tags.Companion.insert(dbHelper, "SIX");
			tags[3] = DbContract.Tags.Companion.insert(dbHelper, "NINE");
			tags[4] = DbContract.Tags.Companion.insert(dbHelper, "TEN");
		}

		NoteRecord notes[] = new NoteRecord[15];
		cnt = DatabaseUtils.queryNumEntries(dbHelper.getReadableDatabase(), "Notes");
		if (cnt > 0) {
			Log.w("secret.db_test", "Number of records in Notes: " + cnt);
		} else {
			notes[0] = DbContract.Notes.Companion.insert(dbHelper, "K0003", "NOTE0003");
			notes[1] = DbContract.Notes.Companion.insert(dbHelper, "K0005", "NOTE0005");
			notes[2] = DbContract.Notes.Companion.insert(dbHelper, "K0001", "NOTE0001");
			notes[3] = DbContract.Notes.Companion.insert(dbHelper, "K0004", "NOTE0004");
			notes[4] = DbContract.Notes.Companion.insert(dbHelper, "K0002", "NOTE0002");
			notes[5] = DbContract.Notes.Companion.insert(dbHelper, "K0007", "NOTE0007");
			notes[6] = DbContract.Notes.Companion.insert(dbHelper, "K0008", "NOTE0008");
			notes[7] = DbContract.Notes.Companion.insert(dbHelper, "K0006", "NOTE0006");
			notes[8] = DbContract.Notes.Companion.insert(dbHelper, "K0010", "NOTE0010");
			notes[9] = DbContract.Notes.Companion.insert(dbHelper, "K0009", "NOTE0009");
			notes[10] = DbContract.Notes.Companion.insert(dbHelper, "K0015", "NOTE0015");
			notes[11] = DbContract.Notes.Companion.insert(dbHelper, "K0014", "NOTE0014");
			notes[12] = DbContract.Notes.Companion.insert(dbHelper, "K0013", "NOTE0013");
			notes[13] = DbContract.Notes.Companion.insert(dbHelper, "K0012", "NOTE0012");
			notes[14] = DbContract.Notes.Companion.insert(dbHelper, "K0011", "NOTE0011");
		}

		cnt = DatabaseUtils.queryNumEntries(dbHelper.getReadableDatabase(), "NoteTags");
		if (cnt > 0) {
			Log.w("secret.db_test", "Number of records in NoteTags: " + cnt);
		} else {
			DbContract.NoteTags.Companion.insert(dbHelper, notes[0].getPid(), tags[0].getPid()); //ONE
			DbContract.NoteTags.Companion.insert(dbHelper, notes[0].getPid(), tags[1].getPid()); //TWO
			DbContract.NoteTags.Companion.insert(dbHelper, notes[1].getPid(), tags[2].getPid()); //SIX
			DbContract.NoteTags.Companion.insert(dbHelper, notes[1].getPid(), tags[3].getPid()); //NINE
			DbContract.NoteTags.Companion.insert(dbHelper, notes[2].getPid(), tags[4].getPid()); //TEN
			DbContract.NoteTags.Companion.insert(dbHelper, notes[2].getPid(), tags[0].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[3].getPid(), tags[1].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[3].getPid(), tags[2].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[4].getPid(), tags[3].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[4].getPid(), tags[4].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[5].getPid(), tags[0].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[5].getPid(), tags[1].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[6].getPid(), tags[2].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[6].getPid(), tags[3].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[7].getPid(), tags[4].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[7].getPid(), tags[0].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[8].getPid(), tags[1].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[8].getPid(), tags[2].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[9].getPid(), tags[3].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[9].getPid(), tags[4].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[10].getPid(), tags[0].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[10].getPid(), tags[1].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[11].getPid(), tags[2].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[11].getPid(), tags[3].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[12].getPid(), tags[4].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[12].getPid(), tags[0].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[13].getPid(), tags[1].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[13].getPid(), tags[2].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[14].getPid(), tags[3].getPid());
			DbContract.NoteTags.Companion.insert(dbHelper, notes[14].getPid(), tags[4].getPid());
		}
	}

	public static void cleanup(Context context, DbHelper helper) {
		helper.getWritableDatabase().execSQL(DbContract.NoteTags.SQL_DROP);
		helper.getWritableDatabase().execSQL(DbContract.Notes.SQL_DROP);
		helper.getWritableDatabase().execSQL(DbContract.Tags.SQL_DROP);
		if (context != null) context.deleteDatabase("Secret.db");
	}

	private void test(DbHelper helper) {
		String SQL =
				"select n.noteKey, n.noteContent, t.tagName" +
				"  from NoteTags as nt" +
				" inner join Notes as n on n._id = nt.noteId" +
				" inner join Tags  as t on t._id = nt.tagId";

		Cursor cursor = helper.getReadableDatabase().rawQuery(SQL, null);
		StringBuilder builder = new StringBuilder("x");
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
		Log.w("secret.db_test", builder.toString());
	}

//	private void test0(ContextFragment ctxFrag) {
//		List<Integer> tags = new ArrayList<>();
//		tags.add(1);
//		NoteRecord x = ctxFrag.createNote("K9001", "NOTE9001", tags);
//		Log.w("secret.db_test0", x.getPid() + " " + x.getKey());
//	}
//
//	private void test1(ContextFragment ctxFrag) {
//		List<NoteRecord> list = ctxFrag.retrieveNotes();
//		StringBuilder builder = new StringBuilder("x");
//		builder.append('\n');
//		for (NoteRecord rec : list) {
//			builder.append(rec.getKey()).append('\t');
//			for (Long tag : rec.getTags()) {
//				builder.append(tag).append('/');
//			}
//			builder.append('\n');
//		}
//		builder.append("\n").append(list.get(0).getKey()).append('\t').append(ctxFrag.retrieveNote(list.get(0)));
//		Log.w("secret.db_test1", builder.toString());
//	}

	private void test2(DbHelper helper) {
		Log.w("secret.db_test2", "Deleted " + DbContract.Tags.Companion.delete(helper) + " rows");

		List<TagRecord> result = DbContract.Tags.Companion.select(helper);
		StringBuilder builder = new StringBuilder("x");
		builder.append('\n');
		for (TagRecord rec : result) {
			builder.append(rec.getTag()).append('\n');
		}
		Log.w("secret.db_test2", builder.toString());
	}

	private void test3(DbHelper helper) {
		int count = DbContract.NoteTags.Companion.delete(helper, 2, 4);
		Log.w("secret.db_test3", count + " rows deleted from NoteTags");
	}

	private void test4(DbHelper helper) {
		String SQL = "select * from Tags where _id not in " +
				"(select nt.tagId from NoteTags as nt inner join Tags as t on t._id = nt.tagId)";
		Cursor cursor = helper.getReadableDatabase().rawQuery(SQL, null);
		StringBuilder builder = new StringBuilder("x");
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
		Log.w("secret.db_test4", builder.toString());
	}


}
