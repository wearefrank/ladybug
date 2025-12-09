/*
   Copyright 2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
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
