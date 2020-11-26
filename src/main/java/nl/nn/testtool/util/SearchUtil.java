/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.testtool.util;

import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Jaco de Groot
 */
public class SearchUtil {

	public static boolean matches(List values, List searchValues) {
		if (searchValues != null) {
			for (int i = 0; i < values.size(); i++) {
				Object value = values.get(i);
				String searchValue = (String)searchValues.get(i);
				if (!matches(value, searchValue)) {
					return false;
				}
			}
		}
		return true;
	}

	public static String getUserHelp() {
		return "When the search value starts with ( and ends with ) the search values is considered a regular expression."
			+ " When the search value consists of the 2 characters \"\" it will match the empty string."
			+ " When the search value consists of the 4 characters null it will match when the object searched for is null."
			+ " Otherwise the search value is considered a case insensitive wildcard search using * as the wildcard character"
			+ " (if the search value starts [ and ends with ] the search value is considered case sensitive)"
			+ " (if the search value doesn't contain the wildcard character it is interpreted as having a wildcard at the beginning and the end)."
			+ " In wildcard mode and regular expression mode the null object equals the empty string."
			+ " The toString() method will be called on the object the search value is matched against.";
	}

	public static boolean matches(Object value, String query) {
		if (query != null && !"".equals(query)) {
			if (query.startsWith("(") && query.endsWith(")")) {
				// Regex search
				if (value == null) {
					value = "";
				}
				// TODO Catch PatternSyntaxException and/or inform the user about it?
				if (!value.toString().matches(query)) {
					return false;
				}
			} else {
				if (query.equals("null")) {
					// Special value search
					if (value != null) {
						return false;
					}
				} else if (query.equals("\"\"")) {
					// Special value search
					if (value == null || !value.toString().equals("")) {
						return false;
					}
				} else {
					// Wildcard search
					boolean caseInsensitive = true;
					if (query.startsWith("[") && query.endsWith("]")) {
						query = query.substring(1, query.length() -1);
						caseInsensitive = false;
					}
					if (query.indexOf('*') == -1) {
						query = "*" + query + "*";
					}
					String valueAsString;
					if (value == null) {
						valueAsString = "";
					} else {
						valueAsString = value.toString();
					}
					if (caseInsensitive) {
						query = query.toLowerCase();
						valueAsString = valueAsString.toLowerCase();
					}
					boolean queryStartsWithWildcard = query.startsWith("*");
					boolean queryEndsWithWildcard = query.endsWith("*");
					int j = 0;
					StringTokenizer stringTokenizer = new StringTokenizer(query, "*");
					while (stringTokenizer.hasMoreTokens()) {
						String token = stringTokenizer.nextToken();
						if (!queryStartsWithWildcard) {
							if (!valueAsString.startsWith(token)) {
								return false;
							}
							queryStartsWithWildcard = true;
						} else if (!queryEndsWithWildcard && !stringTokenizer.hasMoreTokens()) {
							if (!valueAsString.substring(j).endsWith(token)) {
								return false;
							}
						} else if (token.length() != 0) {
							if (j + token.length() > valueAsString.length()) {
								return false;
							}
							j = valueAsString.indexOf(token, j);
							if (j == -1) {
								return false;
							}
							j = j + token.length();
						}
					}
				}
			}
		}
		return true;
	}

}
