/*
   Copyright 2023, 2025 WeAreFrank!

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
package nl.nn.testtool.echo2.test;

import java.util.Comparator;
import java.util.List;

public class MetadataRecordComparator implements Comparator<List<Object>> {
	private int pathPosition;
	private int namePosition;

	MetadataRecordComparator(int pathPosition, int namePosition) {
		this.pathPosition = pathPosition;
		this.namePosition = namePosition;
	}

	public int compare(List<Object> arg0, List<Object> arg1) {
		String string0 = (String)arg0.get(pathPosition);
		if (namePosition > -1) {
			string0 = string0 + (String)arg0.get(2);
		}
		if (string0 == null) {
			string0 = "/";
		}
		String string1 = (String)arg1.get(pathPosition);
		if (namePosition > -1) {
			string1 = string1 + (String)arg1.get(2);
		}
		if (string1 == null) {
			string1 = "/";
		}
		return string0.compareTo(string1);
	}
	
}