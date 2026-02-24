package org.wearefrank.ladybug.test.junit.downloadReports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Assert;
import org.springframework.context.ApplicationContext;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.test.junit.Common;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;

public class DownloadReportsTest {
	private ApplicationContext context = Common.CONTEXT_MEM_STORAGE;
	private TestTool testTool = (TestTool)context.getBean("testTool");

	private int storageIdOfFirst = 0;
	private int storageIdOfSecond = 0;

	@BeforeEach
	public void setUp() throws StorageException {
		String correlationIdOfFirst = ReportRelatedTestCase.getCorrelationId();
		testTool.startpoint(correlationIdOfFirst, "FakeClassName", "DownloadReportsTest first report", "Message of DownloadReportsTest first report");
		testTool.endpoint(correlationIdOfFirst, "FakeClassName", "DownloadReportsTest first report", "End of first");
		String correlationIdOfSecond = ReportRelatedTestCase.getCorrelationId();
		testTool.startpoint(correlationIdOfSecond, "FakeClassName", "DownloadReportsTest second report", "Message of DownloadReportsTest second report");
		testTool.endpoint(correlationIdOfSecond, "FakeClassName", "DownloadReportsTest second report", "End of second");
		Report firstReport = ReportRelatedTestCase.findAndGetReport(testTool, testTool.getDebugStorage(), correlationIdOfFirst);
		Report secondReport = ReportRelatedTestCase.findAndGetReport(testTool, testTool.getDebugStorage(), correlationIdOfSecond);
		storageIdOfFirst = firstReport.getStorageId();
		storageIdOfSecond = secondReport.getStorageId();
		Assert.assertEquals(firstReport.getCheckpoints().size(), 2);
		Assert.assertEquals(secondReport.getCheckpoints().size(), 2);
	}
}
