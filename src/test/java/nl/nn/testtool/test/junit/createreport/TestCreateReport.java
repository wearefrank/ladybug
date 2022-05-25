/*
   Copyright 2021-2022 WeAreFrank!

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.xerces.dom.DocumentImpl;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.SneakyThrows;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MessageCapturerImpl;
import nl.nn.testtool.MessageEncoder;
import nl.nn.testtool.MessageEncoderImpl;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.test.junit.ReportRelatedTestCase;

/**
 * @author Jaco de Groot
 */
public class TestCreateReport extends ReportRelatedTestCase {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Before
	public void setUp() {
		resourcePath = RESOURCE_PATH + "createreport/";
		super.setUp();
	}

	@Test
	public void testSingleStartAndEndPointPlainMessage() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, this.getClass().getTypeName(), reportName, "startmessage");
		testTool.endpoint(correlationId, this.getClass().getTypeName(), reportName, "endmessage");
		assertReport(correlationId);
		// Same but with StubableCode
		correlationId = getCorrelationId();
		testTool.startpoint(correlationId, this.getClass().getTypeName(), reportName, () -> {return "startmessage";}, new HashSet<String>());
		testTool.endpoint(correlationId, this.getClass().getTypeName(), reportName, () -> {return "endmessage";}, new HashSet<String>());
		assertReport(correlationId);
	}

	@Test
	public void testTwoStartAndEndPointPlainMessages() throws StorageException, IOException {
		testTwoStartAndEndPointPlainMessages(reportName);
	}

	private void testTwoStartAndEndPointPlainMessages(String name) throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, name, "startmessage1");
		testTool.startpoint(correlationId, null, "name2", "startmessage2");
		testTool.endpoint(correlationId, null, "name2", "endmessage2");
		testTool.endpoint(correlationId, null, name, "endmessage1");
		assertReport(correlationId, name);
	}

	@Test
	public void testSpecialValues() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, null);
		testTool.infopoint(correlationId, null, "infoname1", new Date(0));
		Set<String> set = new HashSet<String>();
		set.add("Test with strange object");
		testTool.infopoint(correlationId, null, "infoname2", set);
		testTool.infopoint(correlationId, null, "infoname3", 123);
		testTool.infopoint(correlationId, null, "infoname4", new Integer(456));
		testTool.infopoint(correlationId, null, "infoname5", new DocumentImpl().createElement("NodeTest"));
		testTool.endpoint(correlationId, null, reportName, "");
		assertReport(correlationId, true, true, true, false, true);
	}

	@Test
	public void testExceptionAsMessage() throws StorageException, IOException  {
		String correlationId = getCorrelationId();
		Exception exception = new Exception("My Exception");
		// Checkpoints are added on odd and even level to make it possible to visually check the colors in the tree
		// at http://localhost/testtool using ibis-ladybug-test-webapp
		testTool.startpoint(correlationId, null, reportName, exception);
		testTool.inputpoint(correlationId, null, "input", exception);
		testTool.outputpoint(correlationId, null, "output", exception);
		testTool.infopoint(correlationId, null, "info", exception);
		testTool.threadStartpoint(correlationId, null, "thread start", exception);
		testTool.threadEndpoint(correlationId, null, "thread end", exception);
		testTool.startpoint(correlationId, null, "start", exception);
		testTool.inputpoint(correlationId, null, "input", exception);
		testTool.outputpoint(correlationId, null, "output", exception);
		testTool.infopoint(correlationId, null, "info", exception);
		testTool.threadStartpoint(correlationId, null, "thread start", exception);
		testTool.threadEndpoint(correlationId, null, "thread end", exception);
		testTool.endpoint(correlationId, null, "end", exception);
		testTool.endpoint(correlationId, null, reportName, exception);
		assertReport(correlationId, false, false, true, false, false);
	}

	@Test
	public void testThreads() throws StorageException, IOException {
		testThreads(getCorrelationId(), true, true, true, true, false, true);
		testThreads(getCorrelationId(), true, true, true, false, false, true);
	}

	@Test
	public void testThreadsWithoutThreadCreatepoint() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThreads(correlationId, false, true, true, true, false, true);
		assertWarningsUseThreadCreatepointBeforeThreadStartpoint(listAppender, correlationId);
	}

	@Test
	public void testThreadsWithoutThreadCreatepointAndThreadStartpoint() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThreads(correlationId, false, false, true, true, false, true);
		assertWarningsUseThreadCreatepointAndThreadStartpoint(listAppender, correlationId);
	}

	@Test
	public void testThreadsWithThreadCreatepointOnlyAndCancelThreads() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThreads(correlationId, true, false, false, false, false, false);
		assertEquals("Report should be in progress (waiting for threadStartpoint),", 1, testTool.getNumberOfReportsInProgress());
		testTool.close(correlationId, correlationId + "-ChildThreadIdA");
		testTool.close(correlationId, correlationId + "-ChildThreadIdB");
		assertReport(correlationId);
		testTool.close(correlationId);
	}

	@Test
	public void testThreadsWithThreadCreatepointOnlyAndCloseThreads() throws StorageException, IOException {
		testThreadsWithThreadCreatepointOnlyAndCloseThreads(true, false);
		testThreadsWithThreadCreatepointOnlyAndCloseThreads(false, true);
		testThreadsWithThreadCreatepointOnlyAndCloseThreads(true, true);
	}

	public void testThreadsWithThreadCreatepointOnlyAndCloseThreads(boolean withCloseMethod, boolean withSetMethod)
			throws StorageException, IOException {
		if (withSetMethod) {
			testTool.setCloseThreads(true);
		}
		String correlationId = getCorrelationId();
		testThreads(correlationId, true, false, false, false, false, false);
		if (!withSetMethod) {
			assertEquals("Report should be in progress (waiting for threadStartpoint),", 1, testTool.getNumberOfReportsInProgress());
		}
		if (withCloseMethod) {
			testTool.close(correlationId);
		}
		assertReport(correlationId, false, false, false, true, false);
	}

	private static void assertWarningsUseThreadCreatepointBeforeThreadStartpoint(ListAppender<ILoggingEvent> listAppender, String correlationId) {
		List<String> names = new ArrayList<String>();
		names.add("A");
		names.add("B");
		for (String name : names) {
			assertWarningUseThreadCreatepointBeforeThreadStartpoint(listAppender, correlationId,
					correlationId + "-ChildThreadId" + name, "main", "name2" + name.toLowerCase());
		}
	}

	private static void assertWarningUseThreadCreatepointBeforeThreadStartpoint(ListAppender<ILoggingEvent> listAppender,
			String correlationId, String childThreadName, String parentThreadName, String checkpointName) {
		assertWarningInLog(listAppender, correlationId, "New child thread '" + childThreadName
				+ "' for parent thread '" + parentThreadName
				+ "' detected, use threadCreatepoint() before threadStartpoint() for checkpoint (name: "
				+ checkpointName + ", type: ThreadStartpoint, level: null, correlationId: " + correlationId + ")");
	}

	private static void assertWarningsUseThreadCreatepointAndThreadStartpoint(ListAppender<ILoggingEvent> listAppender, String correlationId) {
		List<String> names = new ArrayList<String>();
		names.add("A");
		names.add("B");
		for (String name : names) {
			assertWarningUseThreadCreatepointAndThreadStartpoint(listAppender, correlationId,
					correlationId + "-ThreadName" + name, "main", "name3" + name.toLowerCase());
		}
	}

	private static void assertWarningUseThreadCreatepointAndThreadStartpoint(ListAppender<ILoggingEvent> listAppender,
			String correlationId, String childThreadName, String parentThreadName, String checkpointName) {
		assertWarningInLog(listAppender, correlationId,  "New child thread '" + childThreadName
				+ "' for parent thread '" + parentThreadName
				+ "' detected, use threadCreatepoint() and threadStartpoint() instead of startpoint() for checkpoint (name: "
				+ checkpointName + ", type: ThreadStartpoint, level: null, correlationId: " + correlationId + ")");
	}

	private static void assertWarningInLog(ListAppender<ILoggingEvent> listAppender, String correlationId,
			String warning) {
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(Level.WARN, loggingEvents.get(0).getLevel());
		assertEquals(warning, loggingEvents.get(0).getMessage());
		loggingEvents.remove(0);
	}

	private void testThreads(String correlationId, boolean useThreadCreatepoint, boolean useThreadStartpoint,
			boolean useChildCheckpoints, boolean waitForChildThread, boolean keepThreadOpen, boolean assertReport)
			throws StorageException, IOException {
		String childThreadIdA = correlationId + "-ChildThreadIdA";
		String childThreadIdB = correlationId + "-ChildThreadIdB";
		String threadNameA = correlationId + "-ThreadNameA";
		String threadNameB = correlationId + "-ThreadNameB";
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		if (useThreadCreatepoint) {
			testTool.threadCreatepoint(correlationId, childThreadIdA);
			testTool.threadCreatepoint(correlationId, childThreadIdB);
		}
		if (!waitForChildThread) {
			testTool.endpoint(correlationId, null, reportName, "endmessage1");
		}
		String originalThreadName = Thread.currentThread().getName();
		if (useThreadStartpoint) {
			Thread.currentThread().setName(threadNameA);
			testTool.threadStartpoint(correlationId, childThreadIdA, null, "name2a", "startmessage2");
			Thread.currentThread().setName(threadNameB);
			testTool.threadStartpoint(correlationId, childThreadIdB, null, "name2b", "startmessage2");
		}
		if (useChildCheckpoints) {
			Thread.currentThread().setName(threadNameA);
			testTool.startpoint(correlationId, null, "name3a", "startmessage3");
			Thread.currentThread().setName(threadNameB);
			testTool.startpoint(correlationId, null, "name3b", "startmessage3");
			if (!keepThreadOpen) {
				Thread.currentThread().setName(threadNameB);
				testTool.endpoint(correlationId, null, "name3b", "endmessage3");
				Thread.currentThread().setName(threadNameA);
				testTool.endpoint(correlationId, null, "name3a", "endmessage3");
			}
		}
		if (useThreadStartpoint) {
			Thread.currentThread().setName(threadNameA);
			testTool.threadEndpoint(correlationId, null, "name2a", "endmessage2");
			Thread.currentThread().setName(threadNameB);
			testTool.threadEndpoint(correlationId, null, "name2b", "endmessage2");
		}
		Thread.currentThread().setName(originalThreadName);
		if (waitForChildThread) {
			testTool.endpoint(correlationId, null, reportName, "endmessage1");
		}
		if (assertReport) {
			assertReport(correlationId);
		}
	}

	@Test
	public void testConcurrentLastEndpointAndFirstStartpointForSameCorrelationId() throws Throwable {
		int minimumNrOfRuns = 10;
		int maximumNrOfRuns = 100;
		int count1 = 0;
		int count2 = 0;
		int i = 0;
		while (i < minimumNrOfRuns || count1 == 0 || count2 == 0) {
			int outcome = testConcurrentLastEndpointAndFirstStartpointForSameCorrelationIdSub();
			if (outcome == 1) count1++;
			if (outcome == 2) count2++;
			if (count1 > maximumNrOfRuns || count2 > maximumNrOfRuns) {
				// fail("Prevent infinite running test (count1=" + count1 + " and count2=" + count2 + ")");
				logCounters(count1, count2);
				assumeTrue("This sometimes happens, is it possible to fix this?", false);
			}
			i++;
		}
		logCounters(count1, count2);
	}

	private void logCounters(int count1, int count2) {
		log.debug(name.getMethodName() + ": count1=" + count1 + " and count2=" + count2);
	}

	private int testConcurrentLastEndpointAndFirstStartpointForSameCorrelationIdSub() throws Throwable {
		// Test synchronized statements in TestTool.checkpoint(). More specifically: Test
		// https://github.com/ibissource/ibis-ladybug/commit/9a8b11ad83f09ac4dba8a87f94a4a2273e949f3e
		// To test whether this test case would still trigger the ArrayIndexOutOfBoundsException after being modified
		// disable the following code in TestTool.java:
		//		if (report.isClosed()) {
		//			// Create a new report
		//			report = getReportInProgress(correlationId, name, checkpointType);
		//		}
		String correlationId = getCorrelationId();
		TestThread thread1 = new TestThread();
		TestThread thread2 = new TestThread();
		// Prevent thread names to become Thread-2,3,... when this method is called multiple times
		thread1.setName("Thread-0");
		thread2.setName("Thread-1");
		thread1.setTestTool(testTool);
		thread2.setTestTool(testTool);
		thread1.setCorrelationId(correlationId);
		thread2.setCorrelationId(correlationId);
		thread1.start();
		thread2.start();
		while (thread1.isAlive()) Thread.sleep(10);
		while (thread2.isAlive()) Thread.sleep(10);
		if (thread1.getThrowable() != null) throw thread1.getThrowable();
		if (thread2.getThrowable() != null) throw thread2.getThrowable();
		// Depending on the timing of the threads there are 2 possible outcomes:
		//   - 1 report with 4 checkpoints
		//   - 2 reports with 2 checkpoints
		// And for both outcomes the report and checkpoint names of the report(s) are a bit different depending on
		// which thread was faster
		List<Report> reports = findAndGetReports(testTool, testTool.getDebugStorage(), correlationId, false);
		if (reports.size() == 1) {
			Report report = reports.get(0);
			assertTrue("Report name incorrect: " + report.getName(),
					report.getName().equals("Thread-0") || report.getName().equals("Thread-1"));
			String nameSuffix = "11";
			String childThreadName = "Thread-1";
			String parentThreadName = "Thread-0";
			if (report.getName().equals("Thread-1")) {
				// This time second thread was faster, hence use different expected xml
				nameSuffix = "12";
				childThreadName = "Thread-0";
				parentThreadName = "Thread-1";
			}
			assertReport(report, resourcePath, reportName + nameSuffix, false, false, false, false, false);
			assertWarningUseThreadCreatepointAndThreadStartpoint(listAppender, correlationId, childThreadName,
					parentThreadName, childThreadName);
		} else if (reports.size() == 2) {
			Report report1 = reports.get(0);
			Report report2 = reports.get(1);
			assertTrue("Report names incorrect: " + report1.getName() + " and " + report2.getName(),
					(report1.getName().equals("Thread-0") && report2.getName().equals("Thread-1"))
					|| (report1.getName().equals("Thread-1") && report2.getName().equals("Thread-0")));
			if (report1.getName().equals("Thread-1")) {
				// This time second thread was faster, hence swap places so asserts can be done as if first thread was
				// faster
				Report report3 = report1;
				report1 = report2;
				report2 = report3;
			}
			assertReport(report1, resourcePath, reportName + "21", false, false, false, false, false);
			assertReport(report2, resourcePath, reportName + "22", false, false, false, false, false);
		} else {
			fail("Didn't find 1 or 2 reports, found " + reports.size());
		}
		return reports.size();
	}

	@Test
	public void testAbort() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		testTool.startpoint(correlationId, null, "name2", "startmessage2");
		testTool.abortpoint(correlationId, null, "name2", "abortmessage2");
		testTool.abortpoint(correlationId, null, reportName, "abortmessage1");
		assertReport(correlationId);
	}

	@Test
	public void testAbortWithoutEnoughAbortpoints() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		testTool.startpoint(correlationId, null, "name2", "startmessage2");
		testTool.abortpoint(correlationId, null, "name2", "endmessage2");
		assertEquals("Report should be in progress (number of endpoints + abortpoints doesn't match number of startpoints),", 1, testTool.getNumberOfReportsInProgress());
		testTool.close(correlationId);
		assertReport(correlationId);
	}

	@Test
	public void testAbortLambda() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		testTool.startpoint(correlationId, null, "name2", () -> {return "startmessage2";}, new HashSet<String>());
		assertThrows(RuntimeException.class, () -> {
			testTool.endpoint(correlationId, null, "name2", () -> {if (isTrue()) throw new RuntimeException("abortmessage2"); return "dummy";}, new HashSet<String>());
		});
		testTool.abortpoint(correlationId, null, reportName, "abortmessage1");

		assertReport(correlationId);
	}

	private boolean isTrue() {
		return true;
	}

	@Test
	public void testAbortThread() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		String threadName = correlationId + "-ThreadName";
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		testTool.threadCreatepoint(correlationId, threadName);
		String originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		testTool.threadStartpoint(correlationId, null, "name2", "startmessage2");
		testTool.startpoint(correlationId, null, "name3", "startmessage3");
		testTool.abortpoint(correlationId, null, "name3", "abortmessage3");
		testTool.abortpoint(correlationId, null, "name2", "abortmessage2");
		Thread.currentThread().setName(originalThreadName);
		testTool.endpoint(correlationId, null, reportName, "endmessage1");
		assertReport(correlationId);
	}

	@Test
	public void testCloseReport() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		testTool.startpoint(correlationId, null, "name2", "startmessage2");
		testTool.close(correlationId);
		assertReport(correlationId);
	}

	@Test
	public void testCloseReportWithThreads() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThreads(correlationId, true, true, true, false, true, false);
		testTool.close(correlationId);
		assertReport(correlationId);
	}

	@Test
	public void testCancelThreads() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThreads(correlationId, true, true, true, false, true, false);
		testTool.close(correlationId, correlationId + "-ThreadNameA");
		testTool.close(correlationId, correlationId + "-ThreadNameB");
		assertReport(correlationId);
	}

	@Test
	public void testCloseMessageCapturers() throws IOException, StorageException {
		testCloseMessageCapturers(true, false);
		testCloseMessageCapturers(false, true);
		testCloseMessageCapturers(true, true);
	}

	private void testCloseMessageCapturers(boolean withCloseMethod, boolean withSetMethod) throws IOException, StorageException {
		if (withSetMethod) {
			testTool.setCloseMessageCapturers(true);
		}

		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, "startmessage");

		Writer writerOriginalMessage = new StringWriter();
		Writer writerMessage = testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
		writerMessage.write("Hello Writer World!");
		assertNotEquals(writerOriginalMessage, writerMessage);

		String readerString = "Hello Reader World!";
		Reader readerOriginalMessage = new StringReader(readerString);
		Reader readerMessage = testTool.inputpoint(correlationId, null, "reader", readerOriginalMessage);
		readerMessage.read(new char[readerString.length()]);
		assertNotEquals(readerOriginalMessage, readerMessage);

		OutputStream outputStreamOriginalMessage = new ByteArrayOutputStream();
		OutputStream outputStreamMessage = testTool.inputpoint(correlationId, null, "outputstream", outputStreamOriginalMessage);
		outputStreamMessage.write("Hello OutputStream World!".getBytes());
		assertNotEquals(outputStreamOriginalMessage, outputStreamMessage);

		String inputStreamString = "Hello InputStream World!";
		InputStream inputStreamOriginalMessage = new ByteArrayInputStream(inputStreamString.getBytes());
		InputStream inputStreamMessage = testTool.inputpoint(correlationId, null, "inputstream", inputStreamOriginalMessage);
		inputStreamMessage.read(new byte[inputStreamString.length()]);
		assertNotEquals(inputStreamOriginalMessage, inputStreamMessage);

		testTool.endpoint(correlationId, null, reportName, "endmessage");
		if (withCloseMethod) {
			testTool.close(correlationId, false, true);
		}

		testWriterMessage(writerMessage);
		testReaderMessage(readerMessage);
		testOutputStreamMessage(outputStreamMessage);
		testInputStreamMessage(inputStreamMessage);

		assertReport(correlationId);
	}

	@Test
	public void testIgnoreReport() throws StorageException {
		Storage storage = testTool.getDebugStorage();
		testTool.setRegexFilter("^(?!" + reportName + ").*");
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		String startmessage2 = testTool.startpoint(correlationId, null, "level2", () -> {return "startmessage2";}, new HashSet<String>());
		String endmessage2 = testTool.endpoint(correlationId, null, "level2", () -> {return "endmessage2";}, new HashSet<String>());
		testTool.endpoint(correlationId, null, reportName, "endmessage1");
		Report report = findAndGetReport(testTool, storage, correlationId, false);
		assertNull("Report should have been ignored", report);
		assertEquals("startmessage2", startmessage2);
		assertEquals("endmessage2", endmessage2);
	}

	@Test
	public void testIgnoreReportAndAbort() throws StorageException {
		Storage storage = testTool.getDebugStorage();
		testTool.setRegexFilter("^(?!" + reportName + ").*");

		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		testTool.startpoint(correlationId, null, "level2", "startmessage2");
		testTool.abortpoint(correlationId, null, "level2", "abortmessage2");
		testTool.abortpoint(correlationId, null, "testIgnoreReport", "abortmessage1");
		Report report = findAndGetReport(testTool, storage, correlationId, false);
		assertNull("Report should have been ignored", report);

		correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		testTool.startpoint(correlationId, null, "level2", "startmessage2");
		testTool.abortpoint(correlationId, null, "level2", "abortmessage2");
		testTool.close(correlationId);
		report = findAndGetReport(testTool, storage, correlationId, false);
		assertNull("Report should have been ignored", report);
	}

	@Test
	public void testReportFilter() throws StorageException, IOException {
		String name = "testTwoStartAndEndPointPlainMessages";
		testTool.setRegexFilter(name);
		testTwoStartAndEndPointPlainMessages(name);
	}

	@Test
	public void testMaxCheckpoints() throws StorageException, IOException {
		testTool.setMaxCheckpoints(2);
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		String startmessage2 = testTool.startpoint(correlationId, null, "name2", () -> {return "startmessage2";}, new HashSet<String>());
		String endmessage2 = testTool.endpoint(correlationId, null, "name2", () -> {return "endmessage2";}, new HashSet<String>());
		testTool.endpoint(correlationId, null, reportName, "endmessage1");
		assertReport(correlationId);
		assertWarningInLog(listAppender, correlationId,
				"Maximum number of checkpoints exceeded, ignored checkpoint (name: name2, type: Endpoint, level: 2, correlationId: "
				+ correlationId + ") (next checkpoints for this report will be ignored without any logging)");
		assertEquals(0, testTool.getNumberOfReportsInProgress());
		assertEquals("startmessage2", startmessage2);
		assertEquals("endmessage2", endmessage2);
	}

	@Test
	public void testStreamsWithReaderAndInputStream() throws IOException, StorageException {
		String correlationId = getCorrelationId();
		int maxMessageLength = 15;
		testTool.setMaxMessageLength(maxMessageLength);
		testTool.startpoint(correlationId, null, reportName, "startmessage");

		Reader readerOriginalMessage = new StringReader("Random string 11");
		Reader readerMessage = testTool.inputpoint(correlationId, null, "reader", readerOriginalMessage);
		assertNotEquals(readerOriginalMessage, readerMessage);
		testReaderMessage(readerMessage); // Before report is closed

		readerOriginalMessage = new StringReader("Random string 22");
		readerMessage = testTool.inputpoint(correlationId, null, "reader", readerOriginalMessage);
		assertNotEquals(readerOriginalMessage, readerMessage);

		// Assert no wrapping of message when same message is used again
		assertEquals(readerMessage, testTool.inputpoint(correlationId, null, "reader", readerMessage));

		InputStream inputStreamOriginalMessage = new ByteArrayInputStream("Random string 33".getBytes());
		InputStream inputStreamMessage = testTool.inputpoint(correlationId, null, "inputstream", inputStreamOriginalMessage);
		assertNotEquals(inputStreamOriginalMessage, inputStreamMessage);
		testInputStreamMessage(inputStreamMessage); // Before report is closed

		inputStreamOriginalMessage = new ByteArrayInputStream("Random string 44".getBytes());
		inputStreamMessage = testTool.inputpoint(correlationId, null, "inputstream", inputStreamOriginalMessage);
		assertNotEquals(inputStreamOriginalMessage, inputStreamMessage);

		// Assert no wrapping of message when used again
		assertEquals(inputStreamMessage, testTool.inputpoint(correlationId, null, "inputstream", inputStreamMessage));

		testTool.endpoint(correlationId, null, reportName, "endmessage");

		testReaderMessage(readerMessage); // After report is closed

		testInputStreamMessage(inputStreamMessage); // After report is closed

		assertReport(correlationId, false, false, false, false, true);

		Storage storage = testTool.getDebugStorage();
		Report report = findAndGetReport(testTool, storage, correlationId);
		assertEquals("java.io.StringReader",
				testTool.getMessageEncoder().toObject(report.getCheckpoints().get(1)).getClass().getTypeName());
		assertEquals("java.io.ByteArrayInputStream",
				testTool.getMessageEncoder().toObject(report.getCheckpoints().get(4)).getClass().getTypeName());
	}

	@Test
	public void testStreamsWithWriterAndOutputStream() throws IOException, StorageException {
		String correlationId = getCorrelationId();
		int maxMessageLength = 50;
		testTool.setMaxMessageLength(maxMessageLength);
		MessageEncoder defaultMessageEncoder = testTool.getMessageEncoder();
		// Make byte array readable in expected and actual xml
		testTool.setMessageEncoder(
				new MessageEncoderImpl() {
					@Override
					@SneakyThrows
					public ToStringResult toString(Object message, String charset) {
						if (message instanceof byte[]) {
							String string;
							if (charset == null) {
								string = new String((byte[])message);
							} else {
								string = new String((byte[])message, charset);
							}
							return new ToStringResult(string, "new String((byte[])message)");
						}
						return super.toString(message, charset);
					}
				}
		);
		testTool.startpoint(correlationId, null, reportName, "startmessage");

		Writer writerOriginalMessage = new StringWriter();
		Writer writerMessage = testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
		assertNotEquals(writerOriginalMessage, writerMessage);
		testWriterMessage(writerMessage); // Before report is closed

		writerOriginalMessage = new StringWriter();
		writerMessage = testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
		assertNotEquals(writerOriginalMessage, writerMessage);

		// Assert no wrapping of message when same message is used again
		assertEquals(writerMessage, testTool.inputpoint(correlationId, null, "writer", writerMessage));

		OutputStream outputStreamOriginalMessage = new ByteArrayOutputStream();
		OutputStream outputStreamMessage = testTool.inputpoint(correlationId, null, "outputstream", outputStreamOriginalMessage);
		assertNotEquals(outputStreamOriginalMessage, outputStreamMessage);
		testOutputStreamMessage(outputStreamMessage); // Before report is closed

		outputStreamOriginalMessage = new ByteArrayOutputStream();
		outputStreamMessage = testTool.inputpoint(correlationId, null, "outputstream", outputStreamOriginalMessage);
		assertNotEquals(outputStreamOriginalMessage, outputStreamMessage);

		// Assert no wrapping of message when used again
		assertEquals(outputStreamMessage, testTool.inputpoint(correlationId, null, "outputstream", outputStreamMessage));

		testTool.endpoint(correlationId, null, reportName, "endmessage");
		testWriterMessage(writerMessage); // After report is closed
		testOutputStreamMessage(outputStreamMessage); // After report is closed
		assertReport(correlationId, false, false, false, false, true);

		Storage storage = testTool.getDebugStorage();
		Report report = findAndGetReport(testTool, storage, correlationId);
		Checkpoint checkpoint = report.getCheckpoints().get(1);
		assertEquals(checkpoint.getMessage(), writerOriginalMessage.toString().substring(0, maxMessageLength));
		Object messageToStub = new StringWriter();
		Object message = testTool.getMessageEncoder().toObject(checkpoint, messageToStub);
		assertEquals("java.io.StringWriter", message.getClass().getTypeName());

		// Write new report with default message encoder to test toObject
		testTool.setMessageEncoder(defaultMessageEncoder);
		correlationId = getCorrelationId();
		outputStreamOriginalMessage = new ByteArrayOutputStream();
		outputStreamMessage = testTool.startpoint(correlationId, null, reportName + "2", outputStreamOriginalMessage);
		outputStreamMessage.write("Hello World!".getBytes("UTF-8"));
		outputStreamMessage.close();
		outputStreamOriginalMessage = new ByteArrayOutputStream();
		outputStreamMessage = testTool.endpoint(correlationId, null, reportName + "2", outputStreamOriginalMessage);
		outputStreamMessage.close();
		report = findAndGetReport(testTool, storage, correlationId);
		checkpoint = report.getCheckpoints().get(0);
		messageToStub = new ByteArrayOutputStream();
		message = defaultMessageEncoder.toObject(checkpoint, messageToStub);
		assertEquals("java.io.ByteArrayOutputStream", message.getClass().getTypeName());
		assertEquals("Hello World!", ((ByteArrayOutputStream)message).toString("UTF-8"));
	}

	@Test
	public void testStreamsAllClosedBeforeReportIsClosed() throws IOException, StorageException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, reportName, "startmessage");

		Writer writerOriginalMessage = new StringWriter();
		Writer writerMessage = testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
		assertNotEquals(writerOriginalMessage, writerMessage);
		testWriterMessage(writerMessage);

		testTool.endpoint(correlationId, null, reportName, "endmessage");

		assertReport(correlationId);
	}

	@Test
	public void testStreamWithCharset() throws IOException, StorageException {
		byte[] bytes = new byte[2];
		bytes[0] = (byte)235; // ë in ISO-8859-1 (UTF-8 would need two bytes)
		bytes[1] = (byte)169; // © in ISO-8859-1 (UTF-8 would need two bytes)
		String actual = testTool.getMessageEncoder().toString(bytes, "ISO-8859-1").getString();
		assertEquals("ë©" , actual);
		testTool.setMessageCapturer(new MessageCapturerImpl() {
			@Override
			@SneakyThrows
			public <T> T toOutputStream(T message, OutputStream outputStream, Consumer<String> charsetNotifier,
					Consumer<Throwable> exceptionNotifier) {
				charsetNotifier.accept("ISO-8859-1");
				return super.toOutputStream(message, outputStream, charsetNotifier, exceptionNotifier);
			}
		});
		String correlationId = getCorrelationId();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		OutputStream outputStream =
				testTool.startpoint(correlationId, "sourceClassName", reportName, byteArrayOutputStream);
		testTool.endpoint(correlationId, "sourceClassName", reportName, "endmessage");
		outputStream.write(bytes);
		outputStream.close();
		Report report = assertReport(correlationId);
		report.setTestTool(testTool);
		Checkpoint checkpoint = report.getCheckpoints().get(0);
		assertEquals("CHARSET-ISO-8859-1", checkpoint.getEncoding());
		ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream)checkpoint.getMessageAsObject();
		assertEquals(bytes[0], (byte)byteArrayInputStream.read());
		assertEquals(bytes[1], (byte)byteArrayInputStream.read());
		byteArrayInputStream = checkpoint.getMessageAsObject(new ByteArrayInputStream(new byte[0]));
		assertEquals(235, byteArrayInputStream.read());
		assertEquals(169, byteArrayInputStream.read());
	}

	@Test
	public void testStreamWithException() throws IOException, StorageException {
		testTool.setMessageCapturer(new MessageCapturerImpl() {
			@Override
			public <T> T toOutputStream(T message, OutputStream outputStream, Consumer<String> charsetNotifier,
					Consumer<Throwable> exceptionNotifier) {
				exceptionNotifier.accept(new IOException("Notify exception"));
				return super.toOutputStream(message, outputStream, charsetNotifier, exceptionNotifier);
			}
		});
		String correlationId = getCorrelationId();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		OutputStream outputStream =
				testTool.startpoint(correlationId, "sourceClassName", reportName, byteArrayOutputStream);
		outputStream.write("startmessage".getBytes());
		outputStream.close();
		testTool.endpoint(correlationId, "sourceClassName", reportName, "endmessage");
		assertReport(correlationId, false, false, true, false, false);
	}

	@Test
	public void testStreamSynchronous() throws IOException, StorageException {
		testTool.setMessageCapturer(new MessageCapturerImpl() {
			@Override
			public <T> T toWriter(T message, Writer writer, Consumer<Throwable> exceptionNotifier) {
				try {
					// Immediately write and close message/stream instead of asynchronously writing data to writer after
					// toWriter() has returned
					writer.write("startmessage");
					writer.close();
				} catch (IOException e) {
					exceptionNotifier.accept(e);
				}
				return message;
			}
		});
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, "sourceClassName", reportName, new StringWriter());
		testTool.endpoint(correlationId, "sourceClassName", reportName, "endmessage");
		assertReport(correlationId);
	}

	private void testReaderMessage(Reader readerMessage) throws IOException {
		readerMessage.read();
		readerMessage.read(new char[4]);
		readerMessage.read(new char[20], 5, 10);
		readerMessage.close();
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

	private void testInputStreamMessage(InputStream inputStreamMessage) throws IOException {
		inputStreamMessage.read();
		inputStreamMessage.read(new byte[4]);
		inputStreamMessage.read(new byte[20], 5, 10);
		inputStreamMessage.close();
	}

	private void testOutputStreamMessage(OutputStream outputStreamMessage) throws IOException {
		outputStreamMessage.write("Test OutputStream ".getBytes());
		outputStreamMessage.write(119);
		String s = "rite random random random random random";
		outputStreamMessage.write(s.getBytes(), 0, s.length());
		outputStreamMessage.close();
	}

}
