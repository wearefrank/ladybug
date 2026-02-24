package org.wearefrank.ladybug.test.junit.downloadReports;

import io.micrometer.common.util.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.test.junit.Common;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class DownloadReportsTest {
	private ApplicationContext context = Common.CONTEXT_MEM_STORAGE;

	private Resource reportTransformationForEmptyReports =
			new ClassPathResource("/org/wearefrank/ladybug/test/junit/reportTransformationForEmptyXml.xslt");
	private String transformerXslt;
	private TestTool testTool = (TestTool)context.getBean("testTool");

	private int storageIdOfFirst = 0;
	private int storageIdOfSecond = 0;

	public DownloadReportsTest() {
		try {
			transformerXslt = FileCopyUtils.copyToString(
					new InputStreamReader(reportTransformationForEmptyReports.getInputStream(), StandardCharsets.UTF_8));
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
		storageIdOfFirst = firstReport.getStorageId();
		storageIdOfSecond = secondReport.getStorageId();
	}

	@Test
	public void testReportsExistAndAreAsNeeded() throws StorageException {
		Report firstReport = testTool.getDebugStorage().getReport(storageIdOfFirst);
		Report secondReport = testTool.getDebugStorage().getReport(storageIdOfSecond);
		Assert.assertEquals(2, firstReport.getCheckpoints().size());
		Assert.assertEquals(2, secondReport.getCheckpoints().size());
		firstReport.setTransformation(transformerXslt);
		secondReport.setTransformation(transformerXslt);
		Assert.assertTrue(StringUtils.isNotEmpty(firstReport.toXml()));
		Assert.assertEquals("", secondReport.toXml());
	}
}
