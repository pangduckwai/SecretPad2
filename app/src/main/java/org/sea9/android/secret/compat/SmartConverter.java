package org.sea9.android.secret.compat;

import java.util.ArrayList;
import java.util.List;

public class SmartConverter {
	private List<String> topics;

	private SmartConverter(List<String> records) {
		topics = new ArrayList<>();
		for (String record : records)
			topics.add(record.toLowerCase());
	}

	public static SmartConverter getInstance(List<String> records) {
		return new SmartConverter((records != null) ? records : new ArrayList<>());
	}

	private static final String NEWLINES = "\n\\s*\n";
	/**
	 * <code>
	 * Index:      0       1       2         3         4         5         6   ,...
	 * Old format: ID    , salt  , category, title*  , content*, modified
	 * New format: salt1 , key*  , salt2   , content*, modified, TAG1    , TAG2, ...
	 * Default   : old[1], old[3], old[1]  , old[4]  , old[5]  , old[2]
	 * </code>
	 */
	public final String[][] convert(String category, String title, String content) {
//		List<String[]> result = new ArrayList<>();

		String[] contents = content.split(NEWLINES);
		for (String topic : topics) {
			for (String group : contents) {
				if (group.trim().toLowerCase().startsWith(topic)) {

				}
			}
		}

		System.out.println("Category: " + category);
		System.out.println("Title   : " + title);
		System.out.println("Content :");
		for (String content1 : contents)
			System.out.println(">>> " + content1);

		return null;
	}
}
