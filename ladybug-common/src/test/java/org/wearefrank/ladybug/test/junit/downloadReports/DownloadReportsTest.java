package org.wearefrank.ladybug.test.junit.downloadReports;

import io.micrometer.common.util.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.test.junit.Common;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;

import org.springframework.core.io.Resource;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class DownloadReportsTest {
	private ApplicationContext context = Common.CONTEXT_MEM_STORAGE;

	private Resource reportTransformationForEmptyReports =
			new ClassPathResource("/org/wearefrank/ladybug/test/junit/reportTransformationForEmptyXml.xslt");
	private Resource reportTransformationThatLeavesTrace =
			new ClassPathResource("/org/wearefrank/ladybug/test/junit/reportTransformationTheLeavesTrace.xslt");

	private String xsltForEmptyReports;
	private String xsltThatLeavesTrace;
	private TestTool testTool = (TestTool)context.getBean("testTool");

	private int storageIdOfFirst = 0;
	private int storageIdOfSecond = 0;

	public DownloadReportsTest() {
		try {
			xsltForEmptyReports = FileCopyUtils.copyToString(
					new InputStreamReader(reportTransformationForEmptyReports.getInputStream(), StandardCharsets.UTF_8));
			xsltThatLeavesTrace = FileCopyUtils.copyToString(
					new InputStreamReader(reportTransformationThatLeavesTrace.getInputStream(), StandardCharsets.UTF_8));
		} catch(Exception e) {
			System.out.println(String.format("Could not read resource for report transformation: %s", e.getStackTrace()));
		}
	}

	@Before
	public void setUp() throws StorageException, IOException {
		String correlationIdOfFirst = ReportRelatedTestCase.getCorrelationId();
		testTool.startpoint(correlationIdOfFirst, "FakeClassName", "DownloadReportsTest first report", "Message of DownloadReportsTest first report");
		testTool.endpoint(correlationIdOfFirst, "FakeClassName", "DownloadReportsTest first report", "End of first");
		String correlationIdOfSecond = ReportRelatedTestCase.getCorrelationId();
		testTool.startpoint(correlationIdOfSecond, "FakeClassName", "DownloadReportsTest second report", "Message of DownloadReportsTest second report");
		testTool.endpoint(correlationIdOfSecond, "FakeClassName", "DownloadReportsTest second report", "End of second");
		Report firstReport = ReportRelatedTestCase.findAndGetReport(testTool, testTool.getDebugStorage(), correlationIdOfFirst);
		Report secondReport = ReportRelatedTestCase.findAndGetReport(testTool, testTool.getDebugStorage(), correlationIdOfSecond);
		CrudStorage crudStorage = (CrudStorage) testTool.getTestStorage();
		crudStorage.store(firstReport);
		crudStorage.store(secondReport);
		storageIdOfFirst = firstReport.getStorageId();
		storageIdOfSecond = secondReport.getStorageId();
	}

	@Test
	public void whenReportHasItsOwnXsltTransformationThenApplied() throws StorageException {
		Report firstReport = testTool.getTestStorage().getReport(storageIdOfFirst);
		Report secondReport = testTool.getTestStorage().getReport(storageIdOfSecond);
		firstReport.setTransformation(xsltForEmptyReports);
		secondReport.setTransformation(xsltForEmptyReports);
		Assert.assertEquals(2, firstReport.getCheckpoints().size());
		Assert.assertEquals(2, secondReport.getCheckpoints().size());
		Assert.assertTrue(StringUtils.isNotEmpty(firstReport.toXml()));
		Assert.assertEquals("", secondReport.toXml());
	}

	@Test
	public void whenNoTransformationAvaliableThenRawXml() throws StorageException {
		Report firstReport = testTool.getTestStorage().getReport(storageIdOfFirst);
		Report secondReport = testTool.getTestStorage().getReport(storageIdOfSecond);
		Assert.assertTrue(StringUtils.isNotEmpty(firstReport.toXml()));
		Assert.assertTrue(StringUtils.isNotEmpty(secondReport.toXml()));
	}

	@Test
	public void whenGlobalXsltSetThenApplied() throws StorageException {
		Report firstReport = testTool.getTestStorage().getReport(storageIdOfFirst);
		Report secondReport = testTool.getTestStorage().getReport(storageIdOfSecond);
		ReportXmlTransformer global = new ReportXmlTransformer();
		global.updateXslt(xsltThatLeavesTrace);
		firstReport.setGlobalReportXmlTransformer(global);
		secondReport.setGlobalReportXmlTransformer(global);
		Assert.assertTrue(firstReport.toXml().contains("TransformedReport"));
		Assert.assertTrue(secondReport.toXml().contains("TransformedReport"));
	}

	@Test
	public void whenReportHasTransformationThenItTakesPrecedenceOverGlobal() throws StorageException {
		Report firstReport = testTool.getTestStorage().getReport(storageIdOfFirst);
		Report secondReport = testTool.getTestStorage().getReport(storageIdOfSecond);
		firstReport.setTransformation(xsltForEmptyReports);
		secondReport.setTransformation(xsltForEmptyReports);
		ReportXmlTransformer global = new ReportXmlTransformer();
		global.updateXslt(xsltThatLeavesTrace);
		firstReport.setGlobalReportXmlTransformer(global);
		secondReport.setGlobalReportXmlTransformer(global);
		Assert.assertTrue(StringUtils.isNotEmpty(firstReport.toXml()));
		Assert.assertEquals("", secondReport.toXml());
	}
}
