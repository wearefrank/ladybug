package org.wearefrank.ladybug.web.common;

import java.util.Map;

public final class Util {
	private Util() {
	}

	public static boolean mapContainsOnly(Map<String, ?> map, String[] mandatory, String[] optional) {
		int count = 0;
		if (mandatory != null) {
			for (String field : mandatory) {
				if (!map.containsKey(field)) return false;
			}
			count = mandatory.length;
		}
		if (optional != null) {
			for (String field: optional) {
				if (map.containsKey(field)) count++;
			}
		}
		return map.size() == count;
	}

}
