package org.wearefrank.ladybug.test.junit.prepareReports;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import org.springframework.context.ApplicationContext;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.test.junit.Common;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;
import org.wearefrank.ladybug.web.common.shownreport.ShownCheckpoint;
import org.wearefrank.ladybug.web.common.shownreport.ShownReport;
import org.wearefrank.ladybug.web.common.shownreport.ShownReportBuilder;
import org.wearefrank.ladybug.filter.View;

public class ShownReportBuilderTest {
	private ApplicationContext context = Common.CONTEXT_MEM_STORAGE;
	private TestTool testTool = (TestTool)context.getBean("testTool");

	private View testView;

	private ShownReportBuilder instance;

	private String correlationId;

	public ShownReportBuilderTest() {
		// Is stateless, no need to construct / destruct for every test
		instance = new ShownReportBuilder();
	}

	@Before
	public void setUp() {
		testView = new View();
		correlationId = ReportRelatedTestCase.getCorrelationId();
	}

	@Test
	public void whenReportHasOnlyStartpointEndpointThenEndpointOneLevelDeeper() throws Exception {
		startpoint("start");
		endpoint("end");
		ShownReport actual = getShownReport();
		Assert.assertEquals("start", actual.getName());
		Assert.assertEquals(correlationId, actual.getCorrelationId());
		Assert.assertEquals(1, actual.getChildren().size());
		ShownCheckpoint child = actual.getChildren().get(0);
		Assert.assertEquals("start", child.getName());
		Assert.assertEquals(1, child.getChildren().size());
		ShownCheckpoint grandChild = child.getChildren().get(0);
		Assert.assertEquals("end", grandChild.getName());
		Assert.assertNull(grandChild.getChildren());
	}

	@Test
	public void whenReportHasInputPointThenNothingInside() throws Exception {
		startpoint("start");
		inputpoint("input");
		endpoint("end");
		ShownReport actual = getShownReport();
		Assert.assertEquals("start", actual.getName());
		Assert.assertEquals(correlationId, actual.getCorrelationId());
		Assert.assertEquals(1, actual.getChildren().size());
		ShownCheckpoint child = actual.getChildren().get(0);
		Assert.assertEquals("start", child.getName());
		Assert.assertEquals(2, child.getChildren().size());
		ShownCheckpoint grandChild = child.getChildren().get(0);
		Assert.assertEquals("input", grandChild.getName());
		Assert.assertNull(grandChild.getChildren());
		grandChild = child.getChildren().get(1);
		Assert.assertEquals("end", grandChild.getName());
		Assert.assertNull(grandChild.getChildren());
	}

	private ShownReport getShownReport() throws Exception {
		Report report = ReportRelatedTestCase.findAndGetReport(testTool, testTool.getDebugStorage(), correlationId);
		return instance.transform(report, testView);
	}

	private void startpoint(String name) {
		testTool.startpoint(correlationId, "dummySourceClass", name, "Dummy message");

	}

	private void endpoint(String name) {
		testTool.endpoint(correlationId, "dummySourceClass", name, "Dummy message");
	}

	private void inputpoint(String name) {
		testTool.inputpoint(correlationId, "dummySourceClass", name, "Dummy message");
	}
}
