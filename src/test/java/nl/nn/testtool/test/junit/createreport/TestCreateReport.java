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
package nl.nn.testtool.test.junit.createreport;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.xerces.dom.DocumentImpl;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import nl.nn.testtool.MessageEncoderImpl;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.test.junit.util.TestExport;
import nl.nn.testtool.transform.ReportXmlTransformer;

/**
 * @author Jaco de Groot
 */
public class TestCreateReport extends TestCase {
	public static final String FILESYSTEM_PATH = "src/test/resources/";
	public static final String RESOURCE_PATH = "nl/nn/testtool/test/junit/createreport/";
	public static final String EXPECTED_SUFFIX = "-expected.xml";
	public static final String ACTUAL_SUFFIX = "-actual.xml";
	public static final String LOG_SUFFIX = "-FAILED.txt";
	public static final String ASSERT_REPORT_XSLT = "transformReport.xslt";
	private static final ApplicationContext context = new ClassPathXmlApplicationContext("springTestToolTestJUnit.xml");
	private static final TestTool testTestTool = (TestTool)context.getBean("testTool");
	private TestTool testTool;

	/*
	ook met een thread testen die null als naam heeft
	zelfde corr. id en andere thread
	createThreadpoint met de subthread aanroepen
	*/

	private ListAppender<ILoggingEvent> listAppender;

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
		testTool = (TestTool)context.getBean("testTool");
	}

	@Override
	public void tearDown() {
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(0, loggingEvents.size());
	}

	public void testTestTool() {
		// Assert prototype is used for bean so settings are reset between tests
		assertNotEquals(testTestTool, testTool);
	}

	public void testSingleStartAndEndPointPlainMessage() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, this.getClass().getTypeName(), "startname", "startmessage");
		testTool.endpoint(correlationId, this.getClass().getTypeName(), "endname", "endmessage");
		assertReport(correlationId);
	}

	public void testTwoStartAndEndPointPlainMessages() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "startname1", "startmessage1");
		testTool.startpoint(correlationId, null, "startname2", "startmessage2");
		testTool.endpoint(correlationId, null, "endname2", "endmessage2");
		testTool.endpoint(correlationId, null, "endname1", "endmessage1");
		assertReport(correlationId);
	}

	public void testSpecialValues() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "startname1", null);
		testTool.infopoint(correlationId, null, "infoname1", new Date(0));
		testTool.infopoint(correlationId, null, "infoname2", new IOException("Test with strange object"));
		testTool.infopoint(correlationId, null, "infoname3", 123);
		testTool.infopoint(correlationId, null, "infoname4", new Integer(456));
		testTool.infopoint(correlationId, null, "infoname5", new DocumentImpl().createElement("NodeTest"));
		testTool.endpoint(correlationId, null, "endname1", "");
		assertReport(correlationId, true);
	}

	public void testThread() throws StorageException, IOException {
		testThread(getCorrelationId(), true, true);
	}

	public void testThreadWithoutThreadCreatepoint() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThread(correlationId, false, true);
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(Level.WARN, loggingEvents.get(0).getLevel());
		assertEquals("New child thread '" + correlationId
				+ "-ChildThreadId' for parent thread 'main' detected, use threadCreatepoint() before threadStartpoint() for checkpoint (name: startname2, type: ThreadStartpoint, level: null, correlationId: "
				+ correlationId + ")", loggingEvents.get(0).getMessage());
		loggingEvents.remove(0);
	}

	public void testThreadWithoutThreadCreatepointAndThreadStartpoint() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThread(correlationId, false, false);
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(Level.WARN, loggingEvents.get(0).getLevel());
		assertEquals("New child thread '" + correlationId
				+ "-ThreadName' for parent thread 'main' detected, use threadCreatepoint() and threadStartpoint() instead of startpoint() for checkpoint (name: startname3, type: ThreadStartpoint, level: null, correlationId: "
				+ correlationId + ")", loggingEvents.get(0).getMessage());
		loggingEvents.remove(0);
	}

	private void testThread(String correlationId, boolean useThreadCreatepoint, boolean useThreadStartpoint) throws StorageException, IOException {
		String childThreadId = correlationId + "-ChildThreadId";
		String threadName = correlationId + "-ThreadName";
		testTool.startpoint(correlationId, null, "startname1", "startmessage1");
		if (useThreadCreatepoint) {
			testTool.threadCreatepoint(correlationId, childThreadId);
		}
		String originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		if (useThreadStartpoint) {
			testTool.threadStartpoint(correlationId, childThreadId, null, "startname2", "startmessage2");
		}
		testTool.startpoint(correlationId, null, "startname3", "startmessage3");
		testTool.endpoint(correlationId, null, "endname3", "endmessage3");
		if (useThreadStartpoint) {
			testTool.threadEndpoint(correlationId, null, "endname2", "endmessage2");
		}
		Thread.currentThread().setName(originalThreadName);
		testTool.endpoint(correlationId, null, "endname1", "endmessage1");
		assertReport(correlationId);
	}

	public void testAbort() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "startname1", "startmessage1");
		testTool.startpoint(correlationId, null, "startname2", "startmessage2");
		testTool.abortpoint(correlationId, null, "startname1", "endmessage2");
		assertReport(correlationId);
	}

	public void testAbortWithNonExistingStartname() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "startname1", "startmessage1");
		testTool.startpoint(correlationId, null, "startname2", "startmessage2");
		testTool.abortpoint(correlationId, null, "wrong", "endmessage2");
		assertReport(correlationId);
	}

	public void testAbortThread() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		String childThreadId = correlationId + "-ChildThreadId";
		String threadName = correlationId + "-ThreadName";
		testTool.startpoint(correlationId, null, "startname1", "startmessage1");
		testTool.threadCreatepoint(correlationId, childThreadId);
		String originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		testTool.threadStartpoint(correlationId, childThreadId, null, "startname2", "startmessage2");
		testTool.abortpoint(correlationId, null, "wrong", "endmessage2");
		Thread.currentThread().setName(originalThreadName);
		testTool.endpoint(correlationId, null, "endname1", "endmessage1");
		assertReport(correlationId);
	}

	public void testIgnoreReport() throws StorageException {
		Storage storage = testTool.getDebugStorage();
		testTool.setRegexFilter("^(?!testIgnoreReport).*");
		Report report1 = storage.getReport((Integer)storage.getStorageIds().get(0));
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "testIgnoreReport", "startmessage1");
		testTool.startpoint(correlationId, null, "level2", "startmessage2");
		testTool.endpoint(correlationId, null, "level2", "endmessage2");
		testTool.endpoint(correlationId, null, "testIgnoreReport", "endmessage1");
		Report report2 = storage.getReport((Integer)storage.getStorageIds().get(0));
		try {
			assertEquals(report1.toXml(), report2.toXml());
		} catch(AssertionFailedError e) {
			System.err.println("Following report should have been ignored: " + report2.toXml());
			throw e;
		}
	}

	public void testReportFilter() throws StorageException, IOException {
		testTool.setRegexFilter("startname1");
		setName("testTwoStartAndEndPointPlainMessages");
		testTwoStartAndEndPointPlainMessages();
	}

	public void testStreams() throws IOException, StorageException {
		String correlationId = getCorrelationId();
		int maxMessageLength = 50;
		testTool.setMaxMessageLength(maxMessageLength);
		// Make byte array readable in expected and actual xml
		testTool.setMessageEncoder(
				new MessageEncoderImpl() {
					@Override
					public ToStringResult toString(Object message) {
						if (message instanceof byte[]) {
							return new ToStringResult(new String((byte[])message), "new String((byte[])message)");
						}
						return super.toString(message);
					}
				}
		);
		testTool.startpoint(correlationId, null, "startname", "startmessage");

		Writer writerOriginalMessage = new StringWriter();
		Writer writerMessage = (Writer)testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
		assertNotEquals(writerOriginalMessage, writerMessage);
		testWriterMessage(writerMessage); // Before report is closed

		writerOriginalMessage = new StringWriter();
		writerMessage = (Writer)testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
		assertNotEquals(writerOriginalMessage, writerMessage);

		// Assert no wrapping of message when same message is used again
		assertEquals(writerMessage, testTool.inputpoint(correlationId, null, "writer", writerMessage));

		ByteArrayOutputStream outputStreamOriginalMessage = new ByteArrayOutputStream();
		OutputStream outputStreamMessage = (OutputStream)testTool.inputpoint(correlationId, null, "outputstream", outputStreamOriginalMessage);
		assertNotEquals(outputStreamOriginalMessage, outputStreamMessage);
		testOutputStreamMessage(outputStreamMessage); // Before report is closed

		outputStreamOriginalMessage = new ByteArrayOutputStream();
		outputStreamMessage = (OutputStream)testTool.inputpoint(correlationId, null, "outputstream", outputStreamOriginalMessage);
		assertNotEquals(outputStreamOriginalMessage, outputStreamMessage);

		// Assert no wrapping of message when used again
		assertEquals(outputStreamMessage, testTool.inputpoint(correlationId, null, "outputstream", outputStreamMessage));

		testTool.endpoint(correlationId, null, "endname", "endmessage");

		testWriterMessage(writerMessage); // After report is closed

		testOutputStreamMessage(outputStreamMessage); // After report is closed

		assertReport(correlationId, true);
		Storage storage = testTool.getDebugStorage();
		Report report = storage.getReport((Integer)storage.getStorageIds().get(0));
		assertEquals(report.getCheckpoints().get(1).getMessage(), writerOriginalMessage.toString().substring(0, maxMessageLength));
		assertArrayEquals(report.getCheckpoints().get(4).getMessage().getBytes(), Arrays.copyOf(outputStreamOriginalMessage.toByteArray(), maxMessageLength));
	}

	public void testStreamsAllClosedBeforeReportIsClosed() throws IOException, StorageException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "startname", "startmessage");

		Writer writerOriginalMessage = new StringWriter();
		Writer writerMessage = (Writer)testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
		assertNotEquals(writerOriginalMessage, writerMessage);
		testWriterMessage(writerMessage);

		testTool.endpoint(correlationId, null, "endname", "endmessage");

		assertReport(correlationId);
	}

	private void testWriterMessage(Writer writerMessage) throws IOException {
		writerMessage.write("Test Writer");
		writerMessage.write(" write", 0, 6);
		writerMessage.write(" write".toCharArray());
		writerMessage.write("  writ".toCharArray(), 1, 5);
		writerMessage.write(101);
		writerMessage.append(' ');
		writerMessage.append(" app ", 1, 4);
		writerMessage.append("end random random random random random");
		writerMessage.close();
	}

	private void testOutputStreamMessage(OutputStream outputStreamMessage) throws IOException {
		outputStreamMessage.write("Test OutputStream ".getBytes());
		outputStreamMessage.write(119);
		String s = "rite random random random random random";
		outputStreamMessage.write(s.getBytes(), 0, s.length());
		outputStreamMessage.close();
	}

	private Report assertReport(String correlationId) throws StorageException, IOException {
		return assertReport(correlationId, false);
	}

	private Report assertReport(String correlationId, boolean assertExport) throws StorageException, IOException {
		assertEquals(0, testTool.getNumberOfReportsInProgress());
		Storage storage = testTool.getDebugStorage();
		Report report = storage.getReport((Integer)storage.getStorageIds().get(0));
		assertEquals(correlationId, report.getCorrelationId());
		assertNotNull(report);
		ReportXmlTransformer reportXmlTransformer = new ReportXmlTransformer();
		reportXmlTransformer.setXslt(getResource(RESOURCE_PATH, ASSERT_REPORT_XSLT, null));
		report.setReportXmlTransformer(reportXmlTransformer);
		String actual = report.toXml();
		assertXml(RESOURCE_PATH, getName(), actual);
		if (assertExport) {
			TestExport.assertExport(RESOURCE_PATH, getName() + "Export", correlationId, report, true);
		}
		return report;
	}

	public static void assertXml(String path, String testCaseName, String actual) throws StorageException, IOException {
		File expectedfile = new File(FILESYSTEM_PATH + path + testCaseName + EXPECTED_SUFFIX);
		File actualFile = new File(FILESYSTEM_PATH + path + testCaseName + ACTUAL_SUFFIX);
		File logFile = new File(FILESYSTEM_PATH + path + testCaseName + LOG_SUFFIX);
		String expected = getResource(path, testCaseName + EXPECTED_SUFFIX, "Replace with real expected string");
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

	private String getCorrelationId() {
		return getName() + "-" + System.currentTimeMillis();
	}

	public static String getResource(String path, String name, String valueToWriteWhenNotFound) throws IOException {
		StringBuffer result = new StringBuffer();
		String resourceName = path + name;
		InputStream stream = TestCreateReport.class.getClassLoader().getResourceAsStream(resourceName);
		if (stream == null) {
			if (valueToWriteWhenNotFound == null) {
				throw new junit.framework.AssertionFailedError("Could not find resource '" + resourceName + "'");
			} else {
				writeFile(new File(FILESYSTEM_PATH + path + name),
						valueToWriteWhenNotFound, false);
				return valueToWriteWhenNotFound;
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
