package org.sea9.android.secret.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmartConverter {
	private SmartConverter() { }

	public static SmartConverter getInstance() {
		return new SmartConverter();
	}

	private static final String NEWLINES = "\n\\s*\n";
	private static final String NEWLINE = "\n";
	private static final String SPACE = " ";
	private static final String ORIGINAL = " [ORGL]";
	private static final String SUFFIX = ":";
	/**
	 * <code>
	 * Index:      0       1       2         3         4         5         6   ,...
	 * Old format: ID    , salt  , category, title*  , content*, modified
	 * New format: salt1 , key*  , salt2   , content*, modified, TAG1    , TAG2, ...
	 * Default   : old[1], old[3], old[1]  , old[4]  , old[5]  , old[2]
	 * </code>
	 * Definitions:
	 * 1. Subgroup - part of the old content delimited by an empty line. A subgroup will be imported as
	 * 		another note if the first line of the subgroup contains a tag
	 * 2. Main note - the new note to be imported which has its key the same as the old note's title. The
	 * 		main note contains only subgroups with no associated tag
	 */
	public final List<String[]> convert(String category, String title, String content) {
		Map<String, List<String>> result = new HashMap<>(); //format is {key: [key, content, tags...]}

		// Add the original note just in case...
		List<String> orign = new ArrayList<>();
		orign.add(title);
		orign.add(content);
		orign.add(category);
		result.put(title + ORIGINAL, orign);

		String[] contents = content.split(NEWLINES);
		StringBuilder ctnt = new StringBuilder();
		int splitCount = 0;

		for (String group : contents) {
			String[] lines = group.split(NEWLINE);
			if ((lines.length > 1) && (lines[0].endsWith(SUFFIX))) {
				splitCount ++;
				String key = title + SPACE + lines[0].substring(0, lines[0].length() - 1);
				if (result.containsKey(key)) key += SUFFIX;

				StringBuilder ctn = new StringBuilder();
				for (int i = 1; i < lines.length; i++) {
					if (i > 1) ctn.append(NEWLINE);
					ctn.append(lines[i]);
				}

				List<String> values = new ArrayList<>();
				values.add(key);
				values.add(ctn.toString());
				values.add(category);
				result.put(key, values);
			} else {
				if (ctnt.length() > 0) ctnt.append(NEWLINE).append(NEWLINE);
				for (int i = 0; i < lines.length; i ++) {
					if (i > 0) ctnt.append(NEWLINE);
					ctnt.append(lines[i]);
				}
			}
		}

		// Case 2 - If all subgroup has associated tag, main note will be empty, only add the main
		// note to the result if that is not the case.
		// Also should not add if no tag is found at all. In this case importing the original already
		// have this covered.
		if ((ctnt.length() > 0) && (splitCount > 0)) {
			result.get(title + ORIGINAL).set(0, title + ORIGINAL);
			result.put(title, Arrays.asList(title, ctnt.toString(), category));
		}

		List<String[]> ret = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : result.entrySet()) {
			ret.add(entry.getValue().toArray(new String[0]));
		}
		return ret;
	}
}