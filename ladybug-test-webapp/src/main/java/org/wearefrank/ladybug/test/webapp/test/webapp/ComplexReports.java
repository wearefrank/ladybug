package org.wearefrank.ladybug.test.webapp.test.webapp;

import org.wearefrank.ladybug.TestTool;

public class ComplexReports {
	private ComplexReports() {
	}

	public static void fillComplexSuccessReport(String correlationId, String reportName, TestTool testTool) {
		testTool.startpoint(correlationId, null, reportName, "Message for parent");
		testTool.infopoint(correlationId, null, reportName, "Information about the parent");
		testTool.inputpoint(correlationId, null, "First input of parent", "Value of first input of parent");
		testTool.inputpoint(correlationId, null, "Second input of parent", "Value of second input of parent");
		testTool.startpoint(correlationId, null, "First child", "Message for first child");
		testTool.infopoint(correlationId, null, "First child", "Info about first child");
		testTool.inputpoint(correlationId, null, "First input of first child", "Value of first input of first child");
		testTool.threadStartpoint(correlationId, null, "new-thread", null);
		testTool.threadEndpoint(correlationId, null, "new-thread", null);
		testTool.outputpoint(correlationId, null, "First output of first child", "Value of first output of first child");
		testTool.endpoint(correlationId, null, "First child", "Outgoing message from first child");
		testTool.outputpoint(correlationId, null, "First output of parent", "Value of first output of parent");
		testTool.endpoint(correlationId, null, reportName, "Outgoing message from parent");
	}

	public static void fillComplexErrorReport(String correlationId, String reportName, TestTool testTool) {
		testTool.startpoint(correlationId, null, reportName, "Input message");
		testTool.infopoint(correlationId, null, reportName, "Information about the parent");
		testTool.inputpoint(correlationId, null, "First input of parent", "Value of first input of parent");
		testTool.inputpoint(correlationId, null, "Second input of parent", "Value of second input of parent");
		testTool.startpoint(correlationId, null, "First child", "Input message of first child");
		testTool.infopoint(correlationId, null, "First child", "Info about first child");
		testTool.inputpoint(correlationId, null, "First input of first child", "Value of first input of first child");
		testTool.outputpoint(correlationId, null, "First output of first child", "Value of first output of first child");
		testTool.abortpoint(correlationId, null, "First child", "We simulate that something went wrong in first child");
		testTool.abortpoint(correlationId, null, reportName, "And the error propagates up");
	}
}
