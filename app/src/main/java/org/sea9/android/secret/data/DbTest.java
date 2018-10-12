package org.sea9.android.secret.data;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.util.Log;

import org.sea9.android.secret.ContextFragment;

import java.util.List;

public class DbTest {

	public void run(Context context, ContextFragment ctxFrag, boolean cleanUp) {
		DbHelper helper = ctxFrag.getDbHelper();
		if (cleanUp) {
			cleanup(context, helper);
		}

		prepare(helper);
		test001(helper);
		test002(helper);
		test003(helper);
		test004(helper);
		test007(helper);
		test008(helper);
		test007(helper);
		test009(helper);
	}

	private void prepare(DbHelper helper) {
		TagRecord tags[] = new TagRecord[6];
		long cnt = DatabaseUtils.queryNumEntries(helper.getReadableDatabase(), "Tags");
		if (cnt > 0) {
			Log.w("secret.db_test", "Number of records in Tags: " + cnt);
		} else {
			Log.w("secret.db_test", "Inserting new tags...");
			tags[0] = DbContract.Tags.Companion.insert(helper, "ONE");
			tags[1] = DbContract.Tags.Companion.insert(helper, "TWO");
			tags[2] = DbContract.Tags.Companion.insert(helper, "SIX");
			tags[3] = DbContract.Tags.Companion.insert(helper, "XXX");
			tags[4] = DbContract.Tags.Companion.insert(helper, "TEN");
			tags[5] = DbContract.Tags.Companion.insert(helper, "NINE"); //!!!
		}

		NoteRecord notes[] = new NoteRecord[15];
		cnt = DatabaseUtils.queryNumEntries(helper.getReadableDatabase(), "Notes");
		if (cnt > 0) {
			Log.w("secret.db_test", "Number of records in Notes: " + cnt);
		} else {
			Log.w("secret.db_test", "Inserting new notes...");
			notes[0] = DbContract.Notes.Companion.insert(helper, "K0003", "NOTE0003");
			notes[1] = DbContract.Notes.Companion.insert(helper, "K0005", "NOTE0005");
			notes[2] = DbContract.Notes.Companion.insert(helper, "K0001", "NOTE0001");
			notes[3] = DbContract.Notes.Companion.insert(helper, "K0004", "NOTE0004");
			notes[4] = DbContract.Notes.Companion.insert(helper, "K0002", "NOTE0002");
			notes[5] = DbContract.Notes.Companion.insert(helper, "K0007", "NOTE0007");
			notes[6] = DbContract.Notes.Companion.insert(helper, "K0008", "NOTE0008");
			notes[7] = DbContract.Notes.Companion.insert(helper, "K0006", "NOTE0006");
			notes[8] = DbContract.Notes.Companion.insert(helper, "K0010", "NOTE0010");
			notes[9] = DbContract.Notes.Companion.insert(helper, "K0009", "NOTE0009");
			notes[10] = DbContract.Notes.Companion.insert(helper, "K0015", "NOTE0015");
			notes[11] = DbContract.Notes.Companion.insert(helper, "K0014", "NOTE0014");
			notes[12] = DbContract.Notes.Companion.insert(helper, "K0013", "NOTE0013");
			notes[13] = DbContract.Notes.Companion.insert(helper, "K0012", "NOTE0012");
			notes[14] = DbContract.Notes.Companion.insert(helper, "K0011", "NOTE0011");
		}

		cnt = DatabaseUtils.queryNumEntries(helper.getReadableDatabase(), "NoteTags");
		if (cnt > 0) {
			Log.w("secret.db_test", "Number of records in NoteTags: " + cnt);
		} else {
			Log.w("secret.db_test", "Inserting new note/tag relations...");
			DbContract.NoteTags.Companion.insert(helper, notes[0].getPid(), tags[0].getPid()); //ONE
			DbContract.NoteTags.Companion.insert(helper, notes[0].getPid(), tags[1].getPid()); //TWO
			DbContract.NoteTags.Companion.insert(helper, notes[1].getPid(), tags[2].getPid()); //SIX
			DbContract.NoteTags.Companion.insert(helper, notes[1].getPid(), tags[3].getPid()); //XXX
			DbContract.NoteTags.Companion.insert(helper, notes[1].getPid(), tags[5].getPid()); //NINE !!!
			DbContract.NoteTags.Companion.insert(helper, notes[2].getPid(), tags[4].getPid()); //TEN
			DbContract.NoteTags.Companion.insert(helper, notes[2].getPid(), tags[0].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[3].getPid(), tags[1].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[3].getPid(), tags[2].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[4].getPid(), tags[3].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[4].getPid(), tags[4].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[5].getPid(), tags[0].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[5].getPid(), tags[1].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[6].getPid(), tags[2].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[6].getPid(), tags[3].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[7].getPid(), tags[4].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[7].getPid(), tags[0].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[8].getPid(), tags[1].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[8].getPid(), tags[2].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[9].getPid(), tags[3].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[9].getPid(), tags[4].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[10].getPid(), tags[0].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[10].getPid(), tags[1].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[11].getPid(), tags[2].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[11].getPid(), tags[3].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[12].getPid(), tags[4].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[12].getPid(), tags[0].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[13].getPid(), tags[1].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[13].getPid(), tags[2].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[14].getPid(), tags[3].getPid());
			DbContract.NoteTags.Companion.insert(helper, notes[14].getPid(), tags[4].getPid());
		}
	}

	public static void cleanup(Context context, DbHelper helper) {
		helper.getWritableDatabase().execSQL(DbContract.NoteTags.SQL_DROP);
		helper.getWritableDatabase().execSQL(DbContract.Notes.SQL_DROP);
		helper.getWritableDatabase().execSQL(DbContract.Tags.SQL_DROP_IDX);
		helper.getWritableDatabase().execSQL(DbContract.Tags.SQL_DROP);
		if (context != null) context.deleteDatabase("Secret.db");
	}

	private static final String SQL_TEST0 =
			"select n._id, n.noteKey, n.noteContent, t._id, t.tagName" +
			"  from NoteTags as nt" +
			" inner join Notes as n on n._id = nt.noteId" +
			" inner join Tags  as t on t._id = nt.tagId";
	private static final String SQL_TEST9 =
			"select * from Tags where _id not in " +
			"(select nt.tagId from NoteTags as nt inner join Tags as t on t._id = nt.tagId)";
	private static final String SQL_TEST8 =
			"delete from NoteTags where tagId in" +
			"(select _id from Tags where tagName = 'NINE')";

	// Test foreign keys - add
	private void test001(DbHelper helper) {
		try {
			long rid = DbContract.NoteTags.Companion.insert(helper, 1, 99);
			Log.w("secret.db_test001", "Relation " + rid + " added");
		} catch (SQLException e) {
			Log.w("secret.db_test001", e.getMessage());
		}
	}

	// Test foreign keys - delete
	private void test002(DbHelper helper) {
		try {
			int count = DbContract.Notes.Companion.delete(helper, 2);
			Log.w("secret.db_test002", "Deleted " + count + " notes");
		} catch (SQLException e) {
			Log.w("secret.db_test002", e.getMessage());
		}
	}

	// List notes with tags
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
				builder.append(cursor.getString(i)).append('\t');
			}
			builder.append('\n');
		}
		cursor.close();
		Log.w("secret.db_test003", builder.toString());
	}

	// Test unique index
	private void test004(DbHelper helper) {
		try {
			TagRecord dup = DbContract.Tags.Companion.insert(helper, "Nine");
			if (dup != null) Log.w("secret.db_test1", "Tag " + dup.getPid() + " added");
		} catch (SQLException e) {
			Log.w("secret.db_test004", e.getMessage());
		}
	}

	// List unused tags
	private void test007(DbHelper helper) {
		Cursor cursor = helper.getReadableDatabase().rawQuery(SQL_TEST9, null);
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

	// Delete one of the tags ('NINE') to test 'delete unused tags'
	private void test008(DbHelper helper) {
		int count = helper.getWritableDatabase().compileStatement(SQL_TEST8).executeUpdateDelete();
		Log.w("secret.db_test008", count + " rows deleted from NoteTags");
	}

	// Test delete unused tags
	private void test009(DbHelper helper) {
		List<TagRecord> result = DbContract.Tags.Companion.select(helper);
		StringBuilder builder = new StringBuilder("List of tags before clean-up unused tags:");
		builder.append('\n');
		for (TagRecord rec : result) {
			builder.append(rec.getTag()).append('\n');
		}
		Log.w("secret.db_test009", builder.toString());

		Log.w("secret.db_test009", "Cleaned up " + DbContract.Tags.Companion.delete(helper) + " tags");

		result = DbContract.Tags.Companion.select(helper);
		builder = new StringBuilder("List of tags after clean-up unused tags:");
		builder.append('\n');
		for (TagRecord rec : result) {
			builder.append(rec.getTag()).append('\n');
		}
		Log.w("secret.db_test009", builder.toString());
	}

}
