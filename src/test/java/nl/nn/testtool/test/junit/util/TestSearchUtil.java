/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.testtool.test.junit.util;

import java.util.Arrays;

import junit.framework.TestCase;
import nl.nn.testtool.util.SearchUtil;

/**
 * @author Jaco de Groot
 */
public class TestSearchUtil extends TestCase {

	public void testSearchUtil() {
		String[] values;
		String[] search;
		
		values = new String[]{"a"};
		search = new String[]{"a"};
		assertTrue(values, search);

		values = new String[]{"a"};
		search = new String[]{"*"};
		assertTrue(values, search);

		values = new String[]{"abc"};

		search = new String[]{"(abc)"};
		assertTrue(values, search);

		search = new String[]{"b"};
		assertTrue(values, search);

		search = new String[]{"a*"};
		assertTrue(values, search);

		search = new String[]{"*bc"};
		assertTrue(values, search);

		search = new String[]{"b*"};
		assertFalse(values, search);

		search = new String[]{"*b"};
		assertFalse(values, search);

		assertTrue(SearchUtil.matches(null, "null"));
		assertTrue(SearchUtil.matches("", "\"\""));
		assertTrue(SearchUtil.matches(null, "()"));
		assertTrue(SearchUtil.matches("", "()"));
		assertTrue(SearchUtil.matches("null", "(null)"));
		assertTrue(SearchUtil.matches("\"\"", "(\"\")"));
		assertTrue(SearchUtil.matches(null, "*"));
		assertTrue(SearchUtil.matches("", "*"));
		assertFalse(SearchUtil.matches(null, "(.+)"));
		assertFalse(SearchUtil.matches("", "(.+)"));

	}

	private void assertTrue(String[] values, String[] searchValues) {
		assertTrue(SearchUtil.matches(Arrays.asList(values), Arrays.asList(searchValues)));
	}

	private void assertFalse(String[] values, String[] searchValues) {
		assertFalse(SearchUtil.matches(Arrays.asList(values), Arrays.asList(searchValues)));
	}

}
