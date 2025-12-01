/*
   Copyright 2020-2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug.util;

import java.util.List;

/**
 * @author Jaco de Groot
 */
public class EscapeUtil {

	public static String escapeXml(String value) {
		if (value == null) {
			return "";
		} else {
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < value.length(); i++) {
				if (value.charAt(i) == '<') {
					result.append("&lt;");
				} else if (value.charAt(i) == '>') {
					result.append("&gt;");
				} else if (value.charAt(i) == '\"') {
					result.append("&quot;");
				} else if (value.charAt(i) == '&') {
					result.append("&amp;");
				} else {
					result.append(value.charAt(i));
				}
			}
			return result.toString();
		}
	}

	public static String escapeCsv(String value) {
		if (value == null) {
			value = "";
		} else {
			boolean specialCharsFound = false;
			if (value.length() == 0) {
				specialCharsFound = true;
			} else {
				for (int i = 0; i < value.length(); i++) {
					int c = value.charAt(i);
					if (c == '"') {
						specialCharsFound = true;
						value = value.substring(0, i) + "\"" + value.substring(i);
						i++;
					} else if (c == ',' ||c == '\n' || c == '\r') {
						specialCharsFound = true;
					}
				}
			}
			if (specialCharsFound) {
				value = "\"" + value + "\"";
			}
		}
		return value;
	}
	
	public static String escapeCsv(List<String> values) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.size() - 1; i++) {
			builder.append(escapeCsv(values.get(i)));
			builder.append(",");
		}
		builder.append(escapeCsv(values.get(values.size() - 1)));
		return builder.toString();
	}

}
