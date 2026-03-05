package org.wearefrank.ladybug.util;

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
