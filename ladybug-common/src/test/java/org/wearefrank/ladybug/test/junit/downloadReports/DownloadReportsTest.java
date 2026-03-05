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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.wearefrank.ladybug.util.ExportResult;
import org.wearefrank.ladybug.web.common.ReportApiImpl;

public class DownloadReportsTest {
	private ApplicationContext context = Common.CONTEXT_MEM_STORAGE;

	private Resource reportTransformationForEmptyReports =
			new ClassPathResource("/org/wearefrank/ladybug/test/junit/reportTransformationForEmptyXml.xslt");
	private Resource reportTransformationThatLeavesTrace =
			new ClassPathResource("/org/wearefrank/ladybug/test/junit/reportTransformationTheLeavesTrace.xslt");

	private String xsltForEmptyReports;
	private String xsltThatLeavesTrace;
	private TestTool testTool = (TestTool)context.getBean("testTool");
	private ReportApiImpl reportApiImpl;

	private int storageIdOfFirst = 0;
	private int storageIdOfSecond = 0;

	public DownloadReportsTest() {
		try {
			xsltForEmptyReports = FileCopyUtils.copyToString(
					new InputStreamReader(reportTransformationForEmptyReports.getInputStream(), StandardCharsets.UTF_8));
			xsltThatLeavesTrace = FileCopyUtils.copyToString(
					new InputStreamReader(reportTransformationThatLeavesTrace.getInputStream(), StandardCharsets.UTF_8));
			// ReportApiImpl is not created as a bean when test Spring configuration is read.
			reportApiImpl = new ReportApiImpl();
			reportApiImpl.setTestTool(testTool);
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

	@Test
	public void whenDownloadingOneReportSummaryRequestedThenXmlFile() throws Exception {
		ExportResult result = reportApiImpl.downloadFile(
				"testStorage", "false", "with_default_xslt", Arrays.asList(new Integer[] {storageIdOfFirst}));
		Assert.assertTrue(result.getSuggestedFilename().endsWith(".xml"));
	}

	@Test
	public void whenDownloadingOneReportRequestedThenTtsFile() throws Exception {
		ExportResult result = reportApiImpl.downloadFile(
				"testStorage", "true", "omit", Arrays.asList(new Integer[] {storageIdOfFirst}));
		Assert.assertTrue(result.getSuggestedFilename().endsWith(".ttr"));
	}

	@Test
	public void whenDownloadingReportAndSummaryThenZipFile() throws Exception {
		ExportResult result = reportApiImpl.downloadFile(
				"testStorage", "true", "with_default_xslt", Arrays.asList(new Integer[] {storageIdOfFirst}));
		Assert.assertTrue(result.getSuggestedFilename().endsWith(".zip"));
		List<String> zipEntries = getZipEntries(result.getTempFile());
		Assert.assertEquals(2, zipEntries.size());
		boolean hasTtr = zipEntries.stream().anyMatch((s) -> s.endsWith("ttr"));
		boolean hasXml = zipEntries.stream().anyMatch((s) -> s.endsWith("xml"));
		Assert.assertTrue(hasTtr && hasXml);
	}

	@Test
	public void whenDownloadingTwoSummariesThenZipWithTwoXml() throws Exception {
		ExportResult result = reportApiImpl.downloadFile(
				"testStorage", "false", "with_default_xslt", Arrays.asList(new Integer[] {storageIdOfFirst, storageIdOfSecond}));
		Assert.assertTrue(result.getSuggestedFilename().endsWith(".zip"));
		List<String> zipEntries = getZipEntries(result.getTempFile());
		Assert.assertEquals(2, zipEntries.size());
		Assert.assertTrue(zipEntries.stream().allMatch(s -> s.endsWith(".xml")));
		boolean hasFirst = zipEntries.stream().anyMatch(s -> s.contains("first"));
		boolean hasSecond = zipEntries.stream().anyMatch(s -> s.contains("second"));
		Assert.assertTrue(hasFirst && hasSecond);
	}

	private List<String> getZipEntries(File f) throws IOException {
		List<String> result = new ArrayList<>();
		InputStream is = new FileInputStream(f);
		ZipInputStream z = new ZipInputStream(is);
		ZipEntry e;
		while((e = z.getNextEntry()) != null) {
			result.add(e.getName());
			z.closeEntry();
		}
		z.close();
		return result;
	}
}
