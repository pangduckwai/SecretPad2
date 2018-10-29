package org.sea9.android.secret.compat;

import org.sea9.android.secret.data.DbContract;
import org.sea9.android.secret.data.DbHelper;
import org.sea9.android.secret.data.TagRecord;

import java.util.ArrayList;
import java.util.List;

public class SmartConverter {
	private static final String TAB = "\t";

	private List<TagRecord> tags;

	private SmartConverter(List<TagRecord> records) {
		tags = records;
	}

	public static SmartConverter getInstance(DbHelper helper) {
		List<TagRecord> records = DbContract.Tags.Companion.select(helper);
		if (records == null) records = new ArrayList<>();
		return new SmartConverter(records);
	}

	/**
	 * <code>
	 * Index:      0       1       2         3         4         5         6   ,...
	 * Old format: ID    , salt  , category, title*  , content*, modified
	 * New format: salt1 , key*  , salt2   , content*, modified, TAG1    , TAG2, ...
	 * Default   : old[1], old[3], old[1]  , old[4]  , old[5]  , old[2]
	 * </code>
	 */
	public final String[][] convert(String line) {
		String inp[] = line.split(TAB);
		List<String[]> result = new ArrayList<>();
		if (inp.length == 6) { // Double check the format
			String[] row = { inp[1], inp[3], inp[1], inp[4], inp[5], inp[2] }; // TODO TEMP
			result.add(row);
		}

//		String[][] ret = new
		return null;
	}
}
