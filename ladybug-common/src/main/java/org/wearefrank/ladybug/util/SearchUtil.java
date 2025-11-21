/*
   Copyright 2020, 2022-2023, 2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
import java.util.StringTokenizer;

/**
 * @author Jaco de Groot
 */
public class SearchUtil {

	public static boolean matches(List<Object> values, List<String> searchValues) {
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
		return getUserHelpWildcards() + getUserHelpRegex() + getUserHelpNullAndEmpty();
	}

	public static String getUserHelpWildcards() {
		return "Search case insensitive using * as the wildcard character."
			+ " Wildcards are automatically added at the beginning and the end unless the search value starts with [ and ends with ] or already starts or ends with the wildcard."
			+ " The search is done case sensitive when the search value starts with [[ and ends with ]].";
	}

	public static String getUserHelpRegex() {
		return " A regular expression search is done when the search value starts with ( and ends with )."
			+ " In regular expression mode the null object is the same as the empty string.";
	}

	public static String getUserHelpNullAndEmpty() {
		return " When the search value consists of the 4 characters null it will match when the object searched for is null."
			+ " Use [] to match the empty string and [null] to match the string of the 4 characters null.";
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
				} else {
					// Wildcard search
					boolean caseInsensitive = true;
					if (query.startsWith("[[") && query.endsWith("]]")) {
						query = query.substring(2, query.length() - 2);
						caseInsensitive = false;
					} else if (query.startsWith("[") && query.endsWith("]")) {
						query = query.substring(1, query.length() - 1);
					} else if (!(query.startsWith("*") || query.endsWith("*"))){
						query = "*" + query + "*";
					}
					String valueAsString;
					if (value == null) {
						valueAsString = "";
					} else {
						valueAsString = value.toString();
					}
					if (query.equals("")) {
						if (value == null || !valueAsString.equals("")) {
							return false;
						}
					} else {
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
		}
		return true;
	}

}
