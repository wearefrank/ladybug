/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.testtool.test.junit;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import junit.framework.TestCase;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.test.junit.util.TestExport;
import nl.nn.testtool.transform.ReportXmlTransformer;

/**
 * @author Jaco de Groot
 */
public class ReportRelatedTestCase extends TestCase {
	public static final String FILESYSTEM_PATH = "src/test/resources/";
	public static final String RESOURCE_PATH = "nl/nn/testtool/test/junit/";
	public static final String EXPECTED_SUFFIX = "-expected.xml";
	public static final String ACTUAL_SUFFIX = "-actual.xml";
	public static final String LOG_SUFFIX = "-FAILED.txt";
	public static final String ASSERT_REPORT_XSLT = "transformReport.xslt";
	// Use this context at least in all tests that use debug storage otherwise when more then one context is creating
	// storage beans the storageId's are likely to not be unique anymore which will give unexpected results
	public static final ApplicationContext CONTEXT = new ClassPathXmlApplicationContext("springTestToolTestJUnit.xml");
	private static final TestTool testTestTool = (TestTool)CONTEXT.getBean("testTool");
	protected TestTool testTool;
	protected ListAppender<ILoggingEvent> listAppender;
	public String resourcePath = "Override this value!";

	@Override
	public void setUp() {
		File logsDir = new File("logs");
		if (!logsDir.isDirectory()) {
			logsDir.mkdir();
		}
		assertTrue(logsDir.isDirectory());
		Logger log = (Logger)LoggerFactory.getLogger("nl.nn.testtool");
		listAppender = new ListAppender<>();
		listAppender.start();
		log.addAppender(listAppender);
		testTool = (TestTool)CONTEXT.getBean("testTool");
	}

	@Override
	public void tearDown() {
		List<ILoggingEvent> loggingEvents = listAppender.list;
		String logMessage = null;
		if (loggingEvents.size() > 0) {
			logMessage = loggingEvents.get(0).getMessage();
			assertNull(logMessage); // Shows log message when it fails
		}
		assertEquals(0, loggingEvents.size());
	}

	public void testTestTool() {
		// Assert prototype is used for bean so settings are reset between tests
		assertNotEquals(testTestTool, testTool);
	}

	protected String getCorrelationId() {
		return getCorrelationId(getName());
	}

	public static String getCorrelationId(String name) {
		return name + "-" + UUID.randomUUID();
	}

	protected Report assertReport(String correlationId) throws StorageException, IOException {
		return assertReport(correlationId, false, false, false, false);
	}

	protected Report assertReport(String correlationId, boolean applyXmlEncoderIgnores,
			boolean applyEpochTimestampIgnore, boolean applyStackTraceIgnores, boolean assertExport)
			throws StorageException, IOException {
		assertEquals("Found report(s) in progress,", 0, testTool.getNumberOfReportsInProgress());
		Storage storage = testTool.getDebugStorage();
		Report report = findAndGetReport(testTool, storage, correlationId);
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
		assertXml(resourcePath, getName(), actual);
		if (assertExport) {
			TestExport.assertExport(resourcePath, getName() + "Export", correlationId, report, true
					, applyEpochTimestampIgnore, applyStackTraceIgnores);
		}
		return report;
	}

	public static Report findAndGetReport(TestTool testTool, Storage storage, String correlationId) throws StorageException {
		return findAndGetReport(testTool, storage, correlationId, true);
	}

	public static Report findAndGetReport(TestTool testTool, Storage storage, String correlationId, boolean assertFound)
			throws StorageException {
		// In the Spring config for the JUnit tests bean testTool has scope prototype (see comment in config), hence use
		// the same instance here instead of using getBean()
		assertNull("Report should not be in progress", testTool.getReportInProgress(correlationId));
		List<String> metadataNames = new ArrayList<String>();
		metadataNames.add("storageId");
		metadataNames.add("correlationId");
		List<String> searchValues = new ArrayList<String>();
		searchValues.add(null);
		searchValues.add(correlationId);
		List<List<Object>> metadata = storage.getMetadata(2, metadataNames, searchValues,
				MetadataExtractor.VALUE_TYPE_OBJECT);
		if (assertFound) {
			assertEquals("Didn't find exactly 1 report with correlationId " + correlationId + ",", 1, metadata.size());
		}
		Report report = null;
		if (metadata.size() > 0) {
			report = storage.getReport((Integer)metadata.get(0).get(0));
			assertEquals(correlationId, report.getCorrelationId());
		}
		return report;
	}

	public static void assertXml(String path, String testCaseName, String actual) throws StorageException, IOException {
		File expectedfile = new File(FILESYSTEM_PATH + path + testCaseName + EXPECTED_SUFFIX);
		File actualFile = new File(FILESYSTEM_PATH + path + testCaseName + ACTUAL_SUFFIX);
		File logFile = new File(FILESYSTEM_PATH + path + testCaseName + LOG_SUFFIX);
		String expected = getResource(path, testCaseName + EXPECTED_SUFFIX, true);
		if (!expected.equals(actual)) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("===\n");
			buffer.append("=== " + testCaseName + " ===\n");
			buffer.append("===\n");
			buffer.append("expected:\n");
			buffer.append("[" + expected + "]\n");
			buffer.append("actual:\n");
			buffer.append("[" + actual + "]\n");
			buffer.append("equal part:\n");
			buffer.append("[\n");
			int i = 0;
			for (; i < expected.length() && i < actual.length() && expected.charAt(i) == actual.charAt(i); i++) {
				buffer.append(expected.charAt(i));
			}
			buffer.append("]\n");
			if (i > expected.length()) {
				buffer.append("expected next char: " + expected.charAt(i) + " (" + (int)expected.charAt(i) + ")\n");
			}
			if (i > actual.length()) {
				buffer.append("actual next char: " + actual.charAt(i) + " (" + (int)actual.charAt(i) + ")\n");
			}
			writeFile(expectedfile, expected, false);
			System.err.println("===>>> See " + expectedfile.getCanonicalPath());
			writeFile(actualFile, actual, true);
			System.err.println("===>>> See " + actualFile.getCanonicalPath());
			writeFile(logFile, buffer.toString(), true);
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
		xml = xml.replaceFirst("" + report.getStorageId(), "IGNORE-STORAGE-ID");
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
			return string.replaceFirst("EstimatedMemoryUsage=\".*\">",
									   "EstimatedMemoryUsage=\"IGNORE\">")
					.replaceFirst("(?s)at nl.nn.testtool.test.junit..*\\)\n</Checkpoint>",
									  "at nl.nn.testtool.test.junit.IGNORE)\n</Checkpoint>");
		} else {
			return string.replaceFirst("estimatedMemoryUsage\">\n   <long>.*</long>",
									   "estimatedMemoryUsage\">\n   <long>IGNORE</long>")
					.replaceFirst("(?s)java.io.IOException: Test with strange object.*\\)(&#13;)?\n</string>",
									  "java.io.IOException: Test with strange objectIGNORE)\n</string>");
		}
	}

	public static String getResource(String path, String name) throws IOException {
		return getResource(path, name, false);
	}

	public static String getResource(String path, String name, boolean createResourceWithMessageWhenNotFound)
			throws IOException {
		StringBuffer result = new StringBuffer();
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
				result.append(new String(bytes, 0, i, "UTF-8"));
				i = stream.read(bytes);
			}
		} catch (UnsupportedEncodingException e1) {
			result.append("UnsupportedEncodingException reading xslt: " + e1.getMessage());
		} catch (IOException e2) {
			result.append("IOException reading xslt: " + e2.getMessage());
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
