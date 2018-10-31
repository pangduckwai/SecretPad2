package org.sea9.android.secret.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmartConverter {
	private List<String> topics;

	private SmartConverter(List<String> records) {
		topics = records;
	}

	public static SmartConverter getInstance(List<String> records) {
		return new SmartConverter((records != null) ? records : new ArrayList<>());
	}

	private static final String NEWLINES = "\n\\s*\n";
	private static final String NEWLINE = "\n";
	private static final String SPACE = " ";
	/**
	 * <code>
	 * Index:      0       1       2         3         4         5         6   ,...
	 * Old format: ID    , salt  , category, title*  , content*, modified
	 * New format: salt1 , key*  , salt2   , content*, modified, TAG1    , TAG2, ...
	 * Default   : old[1], old[3], old[1]  , old[4]  , old[5]  , old[2]
	 * </code>
	 */
	public final Map<String, List<String>> convert(String category, String title, String content) {
		Map<String, List<String>> result = new HashMap<>(); //format is {key: [content, tags...]}

		String[] contents = content.split(NEWLINES);
		StringBuilder ctnt = new StringBuilder();
		for (String group : contents) {
			boolean found = false;
			String[] lines = group.split(NEWLINE);
			for (String topic : topics) {
				if (lines[0].trim().toLowerCase().contains(topic.toLowerCase())) {
					found = true;
					String key = title + SPACE + lines[0];
					StringBuilder ctn = new StringBuilder();
					if (result.containsKey(key)) {
						result.get(key).add(topic);
					} else {
						if (lines.length == 1) {
							ctn.append(group);
						} else { // lines.length > 1
							for (int i = 1; i < lines.length; i++) {
								if (i > 1) ctn.append(NEWLINE);
								ctn.append(lines[i]);
							}
						}
						List<String> values = new ArrayList<>();
						values.add(ctn.toString());
						values.add(topic);
						values.add(category);
						result.put(key, values);
					}
				}
			}
			if (!found) {
				if (ctnt.length() > 0) ctnt.append(NEWLINES);
				ctnt.append(group);
			}
		}
		result.put(title, Arrays.asList(ctnt.toString(), category));
		return result;
	}
}