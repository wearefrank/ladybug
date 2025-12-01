/*
   Copyright 2024, 2025 WeAreFrank!

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
package org.wearefrank.ladybug;

public enum CheckpointType {
	STARTPOINT,
	ENDPOINT,
	ABORTPOINT,
	INPUTPOINT,
	OUTPUTPOINT,
	INFOPOINT,
	THREAD_CREATEPOINT,
	THREAD_STARTPOINT,
	THREAD_ENDPOINT;

	// Cannot override the implicit valueOf(String)
	public static CheckpointType valueOfString(String string) {
		for (CheckpointType checkpointType : values()) {
			if (checkpointType.toString().equals(string)) return checkpointType;
		}
		return null;
	}

	public static CheckpointType valueOf(int number) {
		CheckpointType[] values = CheckpointType.values();
		for (int i = 0; i < values.length; i++) {
			if (values[i].toInt() == number) {
				return values[i];
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return toString(name(), true);
	}

	public static String toString(String name, boolean camelCase) {
		// Based on https://stackoverflow.com/a/18031576/17193564
		String result = name.charAt(0) + name.substring(1).toLowerCase();
		int i = result.indexOf("_");
		while (i != -1) {
			result = result.substring(0, i) + (camelCase ? "" : " ")
					+ (camelCase ? Character.toUpperCase(result.charAt(i + 1)) : result.charAt(i + 1))
					+ result.substring(i + 2);
			i = result.indexOf("_");
		}
		return result;
	}

	public static String toString(int number) {
		return valueOf(number).toString();
	}

	public int toInt() {
		return ordinal() + 1;
	}
}
