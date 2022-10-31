/*
   Copyright 2020, 2022 WeAreFrank!, 2018 Nationale-Nederlanden

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
package nl.nn.testtool;

/**
 * @author Jaco de Groot
 */
public class Path {
	String[] names;
	int[] counts;

	protected Path(int length) {
		names = new String[length];
		counts = new int[length];
		for (int i = 0; i < length; i++) {
			counts[i] = 0;
		}
	}

	protected void setName(int index, String name) {
		names[index] = name;
	}

	protected void incrementCount(int index) {
		counts[index]++;
	}

	public boolean equals(Path path) {
		if (names.length != path.names.length) {
			return false;
		} else {
			for (int i = 0; i < names.length; i++) {
				if (!names[i].equals(path.names[i]) || counts[i] != path.counts[i]) {
					return false;
				}
			}
		}
		return true;
	}

	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < names.length; i++) {
			stringBuffer.append("/" + names[i] + "[" + counts[i] + "]");
		}
		return stringBuffer.toString();
	}
}
