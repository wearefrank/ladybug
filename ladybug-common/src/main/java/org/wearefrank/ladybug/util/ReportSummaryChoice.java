/*
   Copyright 2026 WeAreFrank!

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

/*
	This choice also applies when reports are downloaded without report summaries.
	In that case the XML of every report is generated. If a report has an empty
	XML, then the report is omitted.

	Still there is no need for a choice SKIP_DEFAULT_XSLT_AND_OMIT. If reports are
	downloaded and the default XSLT is to be neglected, then reports cannot be empty
	so skipping empty reports should not be requested.

	Reports in the test tab can have an empty XML because of the report specific
	transformation. But the test only allows you to download one report at a time.
	If you are downloading a single report, there is no option to omit reports
	with empty XML.
 */
public enum ReportSummaryChoice {
	OMIT,
	NO_DEFAULT_XSLT,
	WITH_DEFAULT_XSLT;

	static ReportSummaryChoice fromBoolean(boolean v) {
		if (v) {
			return WITH_DEFAULT_XSLT;
		} else {
			return OMIT;
		}
	}

	public static ReportSummaryChoice fromString(String s) {
		if (s.equalsIgnoreCase("true")) {
			return WITH_DEFAULT_XSLT;
		} else if (s.equalsIgnoreCase("false")) {
			return OMIT;
		} else {
			for (ReportSummaryChoice e: values()) {
				if (s.equalsIgnoreCase(e.toString())) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException(String.format("Cannot convert [%s] to ReportSummaryChoice", s));
	}
}
