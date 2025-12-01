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

public enum StubType {
	FOLLOW_REPORT_STRATEGY,
	NO,
	YES;

	// Cannot override the implicit valueOf(String)
	public static StubType valueOfString(String string) {
		for (StubType stubType : values()) {
			if (stubType.toString().equals(string)) return stubType;
		}
		return null;
	}

	public static StubType valueOf(int number) {
		for (StubType stubType : values()) {
			if (stubType.ordinal() == number - 2) return stubType;
		}
		return null;
	}

	@Override
	public String toString() {
		return CheckpointType.toString(name(), false);
	}

	public static String toString(int number) {
		return valueOf(number).toString();
	}

	public int toInt() {
		// In the past the following was part of Checkpoint:
		// public static final int STUB_FOLLOW_REPORT_STRATEGY = -1;
		// public static final int STUB_NO = 0;
		// public static final int STUB_YES = 1;
		return ordinal() - 1;
	}
}
