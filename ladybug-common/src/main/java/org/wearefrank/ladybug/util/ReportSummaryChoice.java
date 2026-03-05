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

import lombok.Getter;

public enum ReportSummaryChoice {
	// When omitting reports with empty XML, do apply the default XSLT transformation to see if empty.
	OMIT(true, false),
	// When omitting reports with empty XML, do NOT apply the default XSLT transformation to see if empty.
	OMIT_NO_DEFAULT_XSLT(false, false),
	NO_DEFAULT_XSLT(false, true),
	WITH_DEFAULT_XSLT(true, true);

	private @Getter boolean globalXsltApplied;
	private @Getter boolean summaryExported;

	ReportSummaryChoice(boolean globalXsltApplied, boolean summaryExported) {
		this.globalXsltApplied = globalXsltApplied;
		this.summaryExported = summaryExported;
	}

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
