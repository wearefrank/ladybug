/*
   Copyright 2021, 2023-2025 WeAreFrank!

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
package org.wearefrank.ladybug.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.test.junit.util.TestExport;
import org.wearefrank.ladybug.test.junit.util.TestImport;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;

/**
 * @author Jaco de Groot
 */
@RunWith(Parameterized.class)
public class ReportRelatedTestCase {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{ "File storage", Common.CONTEXT_FILE_STORAGE },
				{ "Memory storage", Common.CONTEXT_MEM_STORAGE },
				{ "Database storage", Common.CONTEXT_DB_STORAGE }
		});
	}

	@Parameter(0)
	public String storageDescription;

	@Parameter(1)
	public ApplicationContext context;

	public static final String FILESYSTEM_PATH = "src/test/resources/";
	public static final String RESOURCE_PATH = "org/wearefrank/ladybug/test/junit/";
	public static final String EXPECTED_SUFFIX = "-expected.xml";
	public static final String ACTUAL_SUFFIX = "-actual.xml";
	public static final String LOG_SUFFIX = "-FAILED.txt";
	public static final String ASSERT_REPORT_XSLT = "transformReport.xslt";
	public static final String DEFAULT_CHARSET = "UTF-8";
	protected TestTool testTool;
	private TestTool testTestTool;
	protected ListAppender<ILoggingEvent> listAppender;
	public String resourcePath = "Override this value!";
	protected String reportName;

	@Rule
	public TestName name = new TestName();

	@Before
	public void setUp() {
		File fileStorageDir = new File("data/file-storage");
		if (!fileStorageDir.isDirectory()) {
			fileStorageDir.mkdirs();
		}
		assertTrue(
				"File storage dir not available: " + fileStorageDir.getAbsolutePath(),
				fileStorageDir.isDirectory()
		);
		ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.wearefrank.ladybug");
		listAppender = new ListAppender<>();
		listAppender.start();
		log.addAppender(listAppender);
		testTool = (TestTool) context.getBean("testTool");
		testTestTool = (TestTool) context.getBean("testTool");
		reportName = Common.methodNameWithoutTestParameter(name.getMethodName());
	}

	@After
	public void tearDown() throws StorageException {
		if (testTool.getNumberOfReportsInProgress() > 0) {
			Report report = testTool.getReportInProgress(0);
			log.error("Checkpoints of report in progress '" + report.getName() + "':");
			for (Checkpoint checkpoint : report.getCheckpoints()) {
				log.error("Name: " + checkpoint.getName()
						+ ", Level: " + checkpoint.getLevel()
						+ ", Message: " + checkpoint.getMessage());
			}
		}
		assertEquals("Found report(s) in progress", 0, testTool.getNumberOfReportsInProgress());
		assertNotNull("No list appender found, setup failed?", listAppender);
		List<ILoggingEvent> loggingEvents = listAppender.list;
		int count = 0;
		for (ILoggingEvent loggingEvent : loggingEvents) {
			if (loggingEvent.getLoggerName().startsWith("org.wearefrank.ladybug.test.junit")) {
				count++;
			} else {
				assertNull(loggingEvent.getMessage()); // Shows log message when it fails
			}
		}
		assertEquals(count, loggingEvents.size());
	}

	@Test
	public void testTestTool() {
		// Assert prototype is used for bean so settings are reset between tests
		assertNotEquals(testTestTool, testTool);
	}

	public static String getCorrelationId() {
		return UUID.randomUUID().toString();
	}

	protected Report assertReport(String correlationId) throws StorageException, IOException {
		return assertReport(correlationId, reportName, false, false, false, false, false);
	}

	protected Report assertReport(String correlationId, String name) throws StorageException, IOException {
		return assertReport(correlationId, name, false, false, false, false, false);
	}

	protected Report assertReport(String correlationId, boolean applyXmlEncoderIgnores,
								  boolean applyEpochTimestampIgnore, boolean applyStackTraceIgnores, boolean applyCorrelationIdIgnores,
								  boolean assertExport) throws StorageException, IOException {
		return assertReport(correlationId, reportName, applyXmlEncoderIgnores, applyEpochTimestampIgnore,
				applyStackTraceIgnores, applyCorrelationIdIgnores, assertExport
		);
	}

	protected Report assertReport(String correlationId, String name, boolean applyXmlEncoderIgnores,
								  boolean applyEpochTimestampIgnore, boolean applyStackTraceIgnores, boolean applyCorrelationIdIgnores,
								  boolean assertExport) throws StorageException, IOException {
		assertEquals("Found report(s) in progress", 0, testTool.getNumberOfReportsInProgress());
		Storage storage = testTool.getDebugStorage();
		Report report = findAndGetReport(testTool, storage, correlationId);
		return assertReport(report, resourcePath, name, applyXmlEncoderIgnores, applyEpochTimestampIgnore,
				applyStackTraceIgnores, applyCorrelationIdIgnores, assertExport
		);
	}

	public static Report assertReport(Report report, String resourcePath, String name)
			throws StorageException, IOException {
		return assertReport(report, resourcePath, name, false, false, false, false, false);
	}

	public static Report assertReport(Report report, String resourcePath, String name, boolean applyXmlEncoderIgnores,
									  boolean applyEpochTimestampIgnore, boolean applyStackTraceIgnores, boolean applyCorrelationIdIgnores,
									  boolean assertExport) throws StorageException, IOException {
		assertNotNull("Report is null", report);
		ReportXmlTransformer reportXmlTransformer = new ReportXmlTransformer();
		reportXmlTransformer.setXslt(getResource(RESOURCE_PATH, ASSERT_REPORT_XSLT));
		report.setReportXmlTransformer(reportXmlTransformer);
		String actual = report.toXml();
		if (applyXmlEncoderIgnores) {
			actual = applyXmlEncoderIgnores(actual);
		}
		if (applyEpochTimestampIgnore) {
			actual = applyEpochTimestampIgnores(actual);
		}
		if (applyStackTraceIgnores) {
			actual = applyStackTraceIgnores(actual);
		}
		if (applyCorrelationIdIgnores) {
			actual = applyCorrelationIdIgnores(actual, report.getCorrelationId());
		}
		assertXml(resourcePath, name, actual);
		if (assertExport) {
			TestExport.assertExport(resourcePath, name, report, true, applyEpochTimestampIgnore,
					applyStackTraceIgnores, false
			);
			TestImport.assertImport(resourcePath, name);
		}
		return report;
	}

	public static Report findAndGetReport(TestTool testTool, Storage storage, String correlationId)
			throws StorageException {
		return findAndGetReport(testTool, storage, correlationId, true);
	}

	public static Report findAndGetReport(TestTool testTool, Storage storage, String correlationId,
										  boolean assertFound) throws StorageException {
		List<Report> reports = findAndGetReports(testTool, storage, correlationId, assertFound);
		Report report = null;
		if (reports.size() > 0) {
			report = reports.get(0);
			assertEquals(correlationId, report.getCorrelationId());
		}
		return report;
	}

	public static List<Report> findAndGetReports(TestTool testTool, Storage storage, String correlationId,
												 boolean assertExactlyOne) throws StorageException {
		// In the Spring config for the JUnit tests bean testTool has scope prototype (see comment in config), hence use
		// the same instance here instead of using getBean()
		assertNull("Report should not be in progress", testTool.getReportInProgress(correlationId));
		List<String> metadataNames = new ArrayList<String>();
		metadataNames.add("storageId");
		metadataNames.add("correlationId");
		List<String> searchValues = new ArrayList<String>();
		searchValues.add(null);
		searchValues.add(correlationId);
		// TODO: Fix that memory storage and file storage store reports in opposite sequences.
		// Then enable the below lines to test that you search only the first (or last) reports
		// for the missing metadata.
		//
		// List<List<Object>> metadata = storage.getMetadata(2, metadataNames, searchValues,
		// 		MetadataExtractor.VALUE_TYPE_OBJECT);
		List<List<Object>> metadata = storage.getMetadata(-1, metadataNames, searchValues,
				MetadataExtractor.VALUE_TYPE_OBJECT
		);
		if (assertExactlyOne) {
			assertEquals("Didn't find exactly 1 report with correlationId '" + correlationId + "'", 1, metadata.size());
		}
		List<Report> reports = new ArrayList<Report>();
		for (int i = 0; i < metadata.size(); i++) {
			Report report = storage.getReport((Integer) metadata.get(i).get(0));
			reports.add(report);
		}
		return reports;
	}

	public static Integer getMaxStorageId(TestTool testTool, Storage storage) throws StorageException {
		List<Integer> storageIds = storage.getStorageIds();
		Integer firstStorageId = storageIds.get(0);
		Integer lastStorageId = storageIds.get(storageIds.size() - 1);
		// Memory storage and file storage store reports in opposite sequences
		if (firstStorageId > lastStorageId) {
			return firstStorageId;
		} else {
			return lastStorageId;
		}
	}

	public static void assertXml(String path, String testCaseName, String actual) throws StorageException, IOException {
		File expectedfile = new File(FILESYSTEM_PATH + path + testCaseName + EXPECTED_SUFFIX);
		File actualFile = new File(FILESYSTEM_PATH + path + testCaseName + ACTUAL_SUFFIX);
		File logFile = new File(FILESYSTEM_PATH + path + testCaseName + LOG_SUFFIX);
		String expected = getResource(path, testCaseName + EXPECTED_SUFFIX, true);
		if (!expected.equals(actual)) {
			StringBuilder builder = new StringBuilder();
			builder.append("===\n");
			builder.append("=== " + testCaseName + " ===\n");
			builder.append("===\n");
			builder.append("expected:\n");
			builder.append("[" + expected + "]\n");
			builder.append("actual:\n");
			builder.append("[" + actual + "]\n");
			builder.append("equal part:\n");
			builder.append("[\n");
			int i = 0;
			for (; i < expected.length() && i < actual.length() && expected.charAt(i) == actual.charAt(i); i++) {
				builder.append(expected.charAt(i));
			}
			builder.append("]\n");
			if (i > expected.length()) {
				builder.append("expected next char: " + expected.charAt(i) + " (" + (int) expected.charAt(i) + ")\n");
			}
			if (i > actual.length()) {
				builder.append("actual next char: " + actual.charAt(i) + " (" + (int) actual.charAt(i) + ")\n");
			}
			writeFile(expectedfile, expected, false);
			System.err.println("===>>> See " + expectedfile.getCanonicalPath());
			writeFile(actualFile, actual, true);
			System.err.println("===>>> See " + actualFile.getCanonicalPath());
			writeFile(logFile, builder.toString(), true);
			System.err.println("===>>> See " + logFile.getCanonicalPath());
		} else {
			// Clean up previous run (file will only exist when previous run has failed)
			actualFile.delete();
			logFile.delete();
		}
		assertEquals(expected, actual);
	}

	public static String applyToXmlIgnores(String xml, Report report) {
		// Correlation id sometimes contains start time, hence replace correlation id first
		xml = xml.replaceFirst(report.getCorrelationId(), "IGNORE-CORRELATIONID");
		// End time is sometimes the same as start time, hence replace in correct order
		if (xml.indexOf("tartTime") < xml.indexOf("ndTime")) {
			xml = xml.replaceFirst("" + report.getStartTime(), "IGNORE-START-TIME");
			xml = xml.replaceFirst("" + report.getEndTime(), "IGNORE-END-TIME");
		} else {
			xml = xml.replaceFirst("" + report.getEndTime(), "IGNORE-END-TIME");
			xml = xml.replaceFirst("" + report.getStartTime(), "IGNORE-START-TIME");
		}
		int i = xml.indexOf("<void property=\"storageId\">");
		if (i >= 0) {
			String firstPart = xml.substring(0, i);
			String secondPart = xml.substring(i);
			secondPart = secondPart.replaceFirst("<int>" + report.getStorageId() + "</int>", "<int>" + "IGNORE-STORAGE-ID" + "</int>");
			xml = firstPart + secondPart;
		} else {
			xml = xml.replaceFirst("<int>" + report.getStorageId() + "</int>", "<int>" + "IGNORE-STORAGE-ID" + "</int>");
		}
		return xml;
	}

	public static String applyXmlEncoderIgnores(String xml) {
		xml = xml.replaceAll("java version=\".*\" class", "java version=\"IGNORE-JAVA-VERSION\" class");
		xml = xml.replaceAll("java version=&quot;.*&quot; class", "java version=&quot;IGNORE-JAVA-VERSION&quot; class");
		return xml;
	}

	public static String applyEpochTimestampIgnores(String string) {
		return string.replaceFirst("1970-01-01T..:..:00\\.000\\+....", "1970-01-01TXX:XX:00.000+XXXX");
	}

	public static String applyStackTraceIgnores(String string) {
		// (?s) enables dotall so the expression . will also match line terminator
		if (string.startsWith("<Report")) {
			return applyEstimatedMemoryUsageIgnore(string)
					.replaceAll(
							"(?s)at org.wearefrank.ladybug.test.junit..[^<]*\\)\n</Checkpoint>",
							"at org.wearefrank.ladybug.test.junit.IGNORE)\n</Checkpoint>"
					);
		} else {
			return string.replaceAll(
					"(?s)java.io.IOException: Test with strange object.[^<]*\\)(&#13;)?\n</string>",
					"java.io.IOException: Test with strange objectIGNORE)\n</string>"
			);
		}
	}

	public static String ignoreStorageId(String xml, Report report) {
		xml = xml.replaceFirst("<int>" + report.getStorageId() + "</int>", "<int>" + "IGNORE-STORAGE-ID" + "</int>");
		return xml;
	}

	public static String applyEstimatedMemoryUsageIgnore(String string) {
		return string.replaceFirst(
				"EstimatedMemoryUsage=\"\\d*\">",
				"EstimatedMemoryUsage=\"IGNORE\">"
		);
	}

	public static String applyCorrelationIdIgnores(String xml, String correlationId) {
		return xml.replaceAll(correlationId, "IGNORE-CORRELATIONID");
	}

	public static String getResource(String path, String name) throws IOException {
		return getResource(path, name, false);
	}

	public static String getResource(String path, String name, boolean createResourceWithMessageWhenNotFound)
			throws IOException {
		StringBuilder result = new StringBuilder();
		String resourceName = path + name;
		InputStream stream = ReportRelatedTestCase.class.getClassLoader().getResourceAsStream(resourceName);
		if (stream == null) {
			if (createResourceWithMessageWhenNotFound) {
				String fileName = FILESYSTEM_PATH + path + name;
				String message = "Replace content of " + fileName + " with expected value";
				writeFile(new File(fileName), message, false);
				return message;
			} else {
				throw new junit.framework.AssertionFailedError("Could not find resource '" + resourceName + "'");
			}
		}
		byte[] bytes = new byte[1024];
		int i;
		try {
			i = stream.read(bytes);
			while (i != -1) {
				result.append(new String(bytes, 0, i, DEFAULT_CHARSET));
				i = stream.read(bytes);
			}
		} catch (UnsupportedEncodingException e1) {
			result.append("UnsupportedEncodingException reading xslt: ").append(e1.getMessage());
		} catch (IOException e2) {
			result.append("IOException reading xslt: ").append(e2.getMessage());
		}
		return result.toString();
	}

	private static void writeFile(File file, String content, boolean overwrite) throws IOException {
		if (!(file.exists() && !overwrite)) {
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.append(content);
			fileWriter.close();
		}
	}
}
