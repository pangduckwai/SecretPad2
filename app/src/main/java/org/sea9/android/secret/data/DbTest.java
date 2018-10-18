package org.sea9.android.secret.data;

import android.database.DatabaseUtils;
import android.util.Log;

public class DbTest {
	public static void cleanup(DbHelper helper) {
		helper.deleteDatabase();
	}

	public void run(DbHelper helper) {
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
}
