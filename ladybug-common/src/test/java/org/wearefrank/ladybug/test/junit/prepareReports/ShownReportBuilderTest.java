package org.wearefrank.ladybug.test.junit.prepareReports;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import org.springframework.context.ApplicationContext;
import org.wearefrank.ladybug.test.junit.Common;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;
import org.wearefrank.ladybug.web.common.shownreport.ShownCheckpoint;
import org.wearefrank.ladybug.web.common.shownreport.ShownReport;
import org.wearefrank.ladybug.web.common.shownreport.ShownReportBuilder;
import org.wearefrank.ladybug.filter.View;

import java.util.List;
import java.util.ArrayList;

public class ShownReportBuilderTest extends ReportRelatedTestCase {
	private View testView;

	private ShownReportBuilder instance;

	private String correlationId;

	private static class TestToolThread extends Thread {
		private TestTool testTool;
		private final String correlationId;

		private List<Runnable> steps = new ArrayList<>();

		TestToolThread(TestTool testTool, String correlationId) {
			this.testTool = testTool;
			this.correlationId = correlationId;
		}

		@Override
		public void run() {
			steps.forEach(Runnable::run);
		}

		void addDelay(int ms) {
			steps.add(() -> {
				try {
					sleep(ms);
				} catch(InterruptedException e) {
					throw new IllegalStateException("Fail test because of InterruptedException", e);
				}
			});
		}

		void addStartpoint(String name) {
			steps.add(() -> testTool.startpoint(correlationId, "DummySourceClass", name, "DummyMessage"));
		}

		void addEndpoint(String name) {
			steps.add(() -> testTool.endpoint(correlationId, "DummySourceClass", name, "DummyMessage"));
		}

		void addInputpoint(String name) {
			steps.add(() -> testTool.inputpoint(correlationId, "DummySourceClass", name, "DummyMessage"));
		}

		void addThreadCreatepoint() {
			steps.add(() -> testTool.threadCreatepoint(correlationId, this.getName()));
		}

		void addThreadEndpoint(String name) {
			steps.add(() -> testTool.threadEndpoint(correlationId, "DummySourceClass", name, "DummyMessage"));
		}
	}

	private TestToolThread secondThread;

	public ShownReportBuilderTest() {
		// Is stateless, no need to construct / destruct for every test
		instance = new ShownReportBuilder();
	}

	@Before
	public void setUp() {
		super.setUp();
		testView = new View();
		correlationId = ReportRelatedTestCase.getCorrelationId();
		secondThread = new TestToolThread(testTool, correlationId);
	}

	@Test
	public void whenReportHasOnlyStartpointEndpointThenEndpointOneLevelDeeper() throws Exception {
		startpoint("start");
		endpoint("end");
		ShownReport actual = getShownReport();
		show(actual);
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

	@Test
	public void whenCheckpointsComeFromMultipleTrheadsThenSortedUnderThreadpoints() throws Exception {
		secondThread.addDelay(100);
		secondThread.addStartpoint("secondStart");
		secondThread.addDelay(200);
		secondThread.addThreadEndpoint("secondEnd");
		startpoint("firstStart");
		threadCreatepoint(secondThread.getName());
		secondThread.start();
		// At 100ms secondStart is created.
		Thread.sleep(200);
		endpoint("firstEnd");
		// At 300ms secondEnd is created.
		secondThread.join();
		ShownReport actual = getShownReport();
		show(actual);
		/*
		Assert.assertEquals("firstStart", actual.getName());
		Assert.assertEquals(1, actual.getChildren().size());
		ShownCheckpoint child_0 = actual.getChildren().get(0);
		Assert.assertEquals("firstStart", child_0.getName());
		Assert.assertEquals(2, child_0.getChildren().size());
		ShownCheckpoint child_00 = child_0.getChildren().get(0);
		Assert.assertEquals(secondThread.getName(), child_00.getName());
		Assert.assertEquals(1, child_00.getChildren().size());
		ShownCheckpoint child_000 = child_00.getChildren().get(0);
		Assert.assertEquals("secondStart", child_000.getName());
		Assert.assertEquals(1, child_000.getChildren().size());
		ShownCheckpoint child_0000 = child_000.getChildren().get(0);
		Assert.assertEquals("secondEnd", child_0000.getName());
		Assert.assertNull(child_0000.getChildren());
		ShownCheckpoint child_01 = child_0.getChildren().get(1);
		Assert.assertEquals("secondEnd", child_01.getName());
		Assert.assertNull(child_01.getChildren());
		 */
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

	void threadCreatepoint(String name) {
		testTool.threadCreatepoint(correlationId, name);
	}

	private void show(ShownReport report) {
		System.out.println(report.getName());
		if (report.getChildren() != null) {
			report.getChildren().stream().forEach(c -> showCheckpointRecursively(c, 2));
		}
	}

	private void showCheckpointRecursively(ShownCheckpoint checkpoint, int indent) {
		System.out.println(String.format("%s%s (%d)", " ".repeat(indent), checkpoint.getName(), checkpoint.getLevel()));
		if (checkpoint.getChildren() != null) {
			checkpoint.getChildren().stream().forEach(c -> showCheckpointRecursively(c, indent + 2));
		}
	}
}
