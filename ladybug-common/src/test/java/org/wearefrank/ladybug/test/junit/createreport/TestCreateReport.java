/*
   Copyright 2021-2023, 2025 WeAreFrank!

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
package org.wearefrank.ladybug.test.junit.createreport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.CloseReportsTask;
import org.wearefrank.ladybug.MessageCapturerImpl;
import org.wearefrank.ladybug.MessageEncoder;
import org.wearefrank.ladybug.MessageEncoderImpl;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.test.junit.ReportRelatedTestCase;

/**
 * @author Jaco de Groot
 */
public class TestCreateReport extends ReportRelatedTestCase {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private enum EndParentThreadAfter {
		CHILD_THREAD_CREATEPOINT, CHILD_THREAD_STARTPOINT, CHILD_THREAD_ENDPOINT
	}

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
	}

	@Test
	public void testSingleStartAndEndPointPlainMessageWithStubableCode() throws StorageException, IOException {
		String correlationId = getCorrelationId();
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
		testThreads(getCorrelationId(), true, true, true, false, false, EndParentThreadAfter.CHILD_THREAD_ENDPOINT, true);
		testThreads(getCorrelationId(), true, true, true, false, false, EndParentThreadAfter.CHILD_THREAD_CREATEPOINT, true);
	}

	@Test
	public void testThreadsWithoutThreadCreatepoint() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThreads(correlationId, false, true, true, false, false, EndParentThreadAfter.CHILD_THREAD_ENDPOINT, true);
		assertWarningsUseThreadCreatepointBeforeThreadStartpoint(listAppender, correlationId,
				Thread.currentThread().getName());
	}

	@Test
	public void testThreadsWithoutThreadCreatepointAndThreadStartpoint() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThreads(correlationId, false, false, true, false, false, EndParentThreadAfter.CHILD_THREAD_ENDPOINT, true);
		assertWarningsUseThreadCreatepointAndThreadStartpoint(listAppender, correlationId,
				Thread.currentThread().getName());
	}

	@Test
	public void testThreadsWithThreadCreatepointOnlyAndCancelThreads() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThreads(correlationId, true, false, false, false, false, EndParentThreadAfter.CHILD_THREAD_CREATEPOINT, false);
		assertEquals("Report should be in progress (waiting for threadStartpoint)", 1,
				testTool.getNumberOfReportsInProgress());
		testTool.close(correlationId, Thread.currentThread().getName() + "-ChildThreadIdA");
		testTool.close(correlationId, Thread.currentThread().getName() + "-ChildThreadIdB");
		assertReport(correlationId);
		testTool.close(correlationId, null);
	}

	@Test
	public void testCloseThreads() throws StorageException, IOException  {
		String correlationId = getCorrelationId();
		testTool.setCloseThreads(true);
		testThreads(correlationId, true, true, true, true, false, EndParentThreadAfter.CHILD_THREAD_STARTPOINT, false);
		assertEquals("Report should not be in progress", 0, testTool.getNumberOfReportsInProgress());
		assertWarningInLog(listAppender,
				"No report in progress for correlationId and checkpoint not a startpoint, ignored checkpoint (name: name2a, type: ThreadEndpoint, level: null, correlationId: "
				+ correlationId + ")");
		assertWarningInLog(listAppender,
				"No report in progress for correlationId and checkpoint not a startpoint, ignored checkpoint (name: name2b, type: ThreadEndpoint, level: null, correlationId: "
				+ correlationId + ")");
		testTool.setCloseNewThreadsOnly(true);
		testThreads(correlationId, true, true, true, true, true, EndParentThreadAfter.CHILD_THREAD_STARTPOINT, false);
		assertEquals("Report should be in progress", 1, testTool.getNumberOfReportsInProgress());
		testTool.close(correlationId);
	}

	@Test
	public void testThreadsWithThreadCreatepointOnlyAndCloseThreads() throws StorageException, IOException {
		testThreadsWithThreadCreatepointOnlyAndCloseThreads(true, false, false, false);
		testThreadsWithThreadCreatepointOnlyAndCloseThreads(false, true, false, false);
		testThreadsWithThreadCreatepointOnlyAndCloseThreads(false, true, true, false);
		testThreadsWithThreadCreatepointOnlyAndCloseThreads(false, false, false, true);
		testThreadsWithThreadCreatepointOnlyAndCloseThreads(true, true, false, true);
		testThreadsWithThreadCreatepointOnlyAndCloseThreads(true, true, true, true);
	}

	public void testThreadsWithThreadCreatepointOnlyAndCloseThreads(boolean withCloseMethod, boolean withSetMethod,
			boolean closeNewThreadsOnly, boolean withTask) throws StorageException, IOException {
		if (withSetMethod) {
			testTool.setCloseThreads(true);
			testTool.setCloseNewThreadsOnly(closeNewThreadsOnly);
		}
		String correlationId = getCorrelationId();
		testThreads(correlationId, true, false, false, false, false, EndParentThreadAfter.CHILD_THREAD_CREATEPOINT, false);
		if (!withSetMethod) {
			assertEquals("Report should be in progress (waiting for threadStartpoint)", 1, testTool.getNumberOfReportsInProgress());
		}
		if (withCloseMethod) {
			testTool.close(correlationId);
		}
		if (withTask) {
			CloseReportsTask task = new CloseReportsTask();
			task.setTestTool(testTool);
			task.setThreadsTime(-1);
			task.closeReports();
			if (!withCloseMethod) {
				assertEquals("Report should be in progress (waiting for threadStartpoint)", 1, testTool.getNumberOfReportsInProgress());
			}
			task.setThreadsTime(0);
			task.closeReports();
		}
		assertReport(correlationId, false, false, false, true, false);
		if (withSetMethod) {
			testTool.setCloseThreads(false);
			testTool.setCloseNewThreadsOnly(false);
		}
	}

	private static void assertWarningsUseThreadCreatepointBeforeThreadStartpoint(
			ListAppender<ILoggingEvent> listAppender, String correlationId, String parentThreadName) {
		List<String> names = new ArrayList<String>();
		names.add("A");
		names.add("B");
		for (String name : names) {
			assertWarningUseThreadCreatepointBeforeThreadStartpoint(listAppender, correlationId,
					parentThreadName, parentThreadName + "-ChildThreadId" + name , "name2" + name.toLowerCase());
		}
	}

	private static void assertWarningUseThreadCreatepointBeforeThreadStartpoint(ListAppender<ILoggingEvent> listAppender,
			String correlationId, String parentThreadName, String childThreadName, String checkpointName) {
		assertWarningInLog(listAppender, "New child thread '" + childThreadName
				+ "' for guessed parent thread '" + parentThreadName
				+ "' detected, use threadCreatepoint() before threadStartpoint() for checkpoint (name: "
				+ checkpointName + ", type: ThreadStartpoint, level: null, correlationId: " + correlationId + ")");
	}

	private static void assertWarningsUseThreadCreatepointAndThreadStartpoint(ListAppender<ILoggingEvent> listAppender,
			String correlationId, String parentThreadName) {
		List<String> names = new ArrayList<String>();
		names.add("A");
		names.add("B");
		for (String name : names) {
			assertWarningUseThreadCreatepointAndThreadStartpoint(listAppender, correlationId,
					parentThreadName, parentThreadName + "-ChildThreadName" + name, "name3" + name.toLowerCase());
		}
	}

	private static void assertWarningUseThreadCreatepointAndThreadStartpoint(ListAppender<ILoggingEvent> listAppender,
			String correlationId, String parentThreadName, String childThreadName, String checkpointName) {
		assertWarningUseThreadCreatepointAndThreadStartpoint(listAppender, correlationId, parentThreadName,
				childThreadName, checkpointName, false);
	}

	private static void assertWarningUseThreadCreatepointAndThreadStartpoint(ListAppender<ILoggingEvent> listAppender,
			String correlationId, String parentThreadName, String childThreadName, String checkpointName,
			boolean removeSecondWarning) {
		String parentThreadSentence = " for guessed parent thread '" + parentThreadName + "'";
		if (parentThreadName == null) {
			parentThreadSentence = " for unknown parent thread";
		}
		assertWarningInLog(listAppender, "New child thread '" + childThreadName + "'" + parentThreadSentence
				+ " detected, use threadCreatepoint() and threadStartpoint() instead of startpoint() for ignored checkpoint (name: "
				+ checkpointName + ", type: ThreadStartpoint, level: null, correlationId: " + correlationId + ")",
				removeSecondWarning);
	}

	private static void assertWarningInLog(ListAppender<ILoggingEvent> listAppender, String warning) {
		assertWarningInLog(listAppender, warning, false);
	}

	private static void assertWarningInLog(ListAppender<ILoggingEvent> listAppender, String warning,
			boolean removeSecondWarning) {
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(warning, loggingEvents.get(0).getMessage());
		assertEquals(Level.WARN, loggingEvents.get(0).getLevel());
		loggingEvents.remove(0);
		if (removeSecondWarning) {
			loggingEvents.remove(0);
		}
	}

	private static void ignoreWarningsInLog(ListAppender<ILoggingEvent> listAppender, String warningsStartsWith) {
		List<ILoggingEvent> loggingEvents = listAppender.list;
		for (int i = 0; i < loggingEvents.size(); i++) {
			ILoggingEvent loggingEvent = loggingEvents.get(i);
			if (loggingEvent.getMessage().startsWith(warningsStartsWith)) {
				loggingEvents.remove(i);
				i--;
			}
		}
	}

	private void testThreads(String correlationId, boolean useThreadCreatepoint, boolean useThreadStartpoint,
			boolean useChildCheckpoints, boolean keepChildThreadOpen, boolean delaySecondChildThread,
			EndParentThreadAfter endParentThreadAfter, boolean assertReport) throws StorageException, IOException {
		String parentThreadName = Thread.currentThread().getName();
		String childThreadIdA = parentThreadName + "-ChildThreadIdA";
		String childThreadIdB = parentThreadName + "-ChildThreadIdB";
		String childThreadNameA = parentThreadName + "-ChildThreadNameA";
		String childThreadNameB = parentThreadName + "-ChildThreadNameB";
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		if (useThreadCreatepoint) {
			testTool.threadCreatepoint(correlationId, childThreadIdA);
			testTool.threadCreatepoint(correlationId, childThreadIdB);
		}
		if (endParentThreadAfter == EndParentThreadAfter.CHILD_THREAD_CREATEPOINT) {
			testTool.endpoint(correlationId, null, reportName, "endmessage1");
		}
		if (useThreadStartpoint) {
			Thread.currentThread().setName(childThreadNameA);
			testTool.threadStartpoint(correlationId, childThreadIdA, null, "name2a", "startmessage2");
			if (!delaySecondChildThread) {
				Thread.currentThread().setName(childThreadNameB);
				testTool.threadStartpoint(correlationId, childThreadIdB, null, "name2b", "startmessage2");
			}
		}
		if (useChildCheckpoints) {
			Thread.currentThread().setName(childThreadNameA);
			testTool.startpoint(correlationId, null, "name3a", "startmessage3");
			if (!delaySecondChildThread) {
				Thread.currentThread().setName(childThreadNameB);
				testTool.startpoint(correlationId, null, "name3b", "startmessage3");
			}
			if (!keepChildThreadOpen) {
				Thread.currentThread().setName(childThreadNameB);
				testTool.endpoint(correlationId, null, "name3b", "endmessage3");
				if (!delaySecondChildThread) {
					Thread.currentThread().setName(childThreadNameA);
					testTool.endpoint(correlationId, null, "name3a", "endmessage3");
				}
			}
		}
		if (endParentThreadAfter == EndParentThreadAfter.CHILD_THREAD_STARTPOINT) {
			Thread.currentThread().setName(parentThreadName);
			testTool.endpoint(correlationId, null, reportName, "endmessage1");
		}
		if (delaySecondChildThread) {
			Thread.currentThread().setName(childThreadNameB);
			testTool.threadStartpoint(correlationId, childThreadIdB, null, "name2b", "startmessage2");
		}
		if (useThreadStartpoint) {
			Thread.currentThread().setName(childThreadNameA);
			testTool.threadEndpoint(correlationId, null, "name2a", "endmessage2");
			Thread.currentThread().setName(childThreadNameB);
			testTool.threadEndpoint(correlationId, null, "name2b", "endmessage2");
		}
		if (endParentThreadAfter == EndParentThreadAfter.CHILD_THREAD_ENDPOINT) {
			Thread.currentThread().setName(parentThreadName);
			testTool.endpoint(correlationId, null, reportName, "endmessage1");
		}
		Thread.currentThread().setName(parentThreadName);
		if (assertReport) {
			assertReport(correlationId);
		}
	}

	/**
	 * Test whether synchronization is done properly in TestTool.checkpoint()
	 * 
	 * @throws Throwable ...
	 */
	@Test
	public void testConcurrency() throws Throwable {
		// Disable the following code in Report.java to make the test fail with an ArrayIndexOutOfBoundsException:
		//		if (threads.size() == 0) {
		//			// This can happen when a report is still open because not all message capturers are closed while
		//			// all threads are finished
		//			warnNewChildThreadDetected(childThreadId, null, false/true, checkpointType);
		//			return message;
		//		} else {
		testConcurrency(true);
		// Also disable report.isClosed() check in TestTool.checkpoint() to make the next test also fail with an
		// ArrayIndexOutOfBoundsException (disable the previous test so this test will be executed)
		testConcurrency(false);
	}

	private void testConcurrency(boolean keepReportOpenWithMessageCapturer) throws Throwable {
		int nrOfThreads = 10;
		int nrOfTestsPerThread = 10;
		int nrOfCheckpointsCalledByTestThread = 2;
		if (keepReportOpenWithMessageCapturer) {
			nrOfCheckpointsCalledByTestThread = 3;
		}
		testTool.setMaxCheckpoints(nrOfThreads * nrOfTestsPerThread * nrOfCheckpointsCalledByTestThread);
		String correlationId = getCorrelationId();
		TestThread[] testThreads = new TestThread[nrOfThreads];
		for (int i = 0; i < nrOfThreads; i++) {
			testThreads[i] = new TestThread();
			testThreads[i].setName("Thread-" + i);
			testThreads[i].setTestTool(testTool);
			testThreads[i].setCorrelationId(correlationId);
			testThreads[i].setNrOfTests(nrOfTestsPerThread);
			testThreads[i].setKeepReportOpenWithMessageCapturer(keepReportOpenWithMessageCapturer);
		}
		for (int i = 0; i < nrOfThreads; i++) {
			testThreads[i].start();
		}
		for (int i = 0; i < nrOfThreads; i++) {
			while (testThreads[i].isAlive()) Thread.sleep(10);
		}
		ignoreWarningsInLog(listAppender, "New child thread '");
		ignoreWarningsInLog(listAppender, "Unknown thread 'Thread-");
		ignoreWarningsInLog(listAppender,
				"No report in progress for correlationId and checkpoint not a startpoint, ignored checkpoint (name: Thread-");
		for (int i = 0; i < nrOfThreads; i++) {
			if (testThreads[i].getThrowable() != null) {
				throw new Exception(testThreads[i].getThrowable());
			}
		}
		testTool.close(correlationId);
	}

	/**
	 * ArrayIndexOutOfBoundsException will occur when synchronization isn't done properly in TestTool.checkpoint(). E.g.
	 * disable report.isClosed() check in TestTool.checkpoint() to make this test throw an exception and fail. In
	 * addition to testConcurrency() (which is a simpler way to test for ArrayIndexOutOfBoundsException) this test will
	 * also assert the generated reports.
	 * 
	 * @throws Throwable ...
	 */
	@Test
	public void testConcurrentLastEndpointAndFirstStartpointForSameCorrelationId() throws Throwable {
		int minimumCountOutcome1 = 1;
		int minimumCountOutcome2 = 1;
		int minimumCountOutcome3 = 0;
		int maximumNrOfRuns = 10;
		boolean failWhenMinimumCountsNotReachedBeforeMaximumNrOfRuns = false;
		// Outcome 3 is very rare. To reproduce either set maximumNrOfRuns to something like 100000 and wait a long time
		// or change devMode to true in TestTool.java and enable the following lines:
		// minimumCountOutcome3 = 1;
		// failWhenMinimumCountsNotReachedBeforeMaximumNrOfRuns = true;
		int c1 = 0;
		int c2 = 0;
		int c3 = 0;
		int i = 0;
		while (c1 < minimumCountOutcome1 || c2 < minimumCountOutcome2 || c3 < minimumCountOutcome3) {
			i++;
			if (i > maximumNrOfRuns) {
				if (failWhenMinimumCountsNotReachedBeforeMaximumNrOfRuns) {
					fail("Prevent infinite running test (c1=" + c1 + ", c2=" + c2 + ", c3=" + c3 + ")");
				}
				break;
			}
			int outcome = testConcurrentLastEndpointAndFirstStartpointForSameCorrelationIdSub();
			if (outcome == 1) c1++;
			if (outcome == 2) c2++;
			if (outcome == 3) c3++;
		}
		log.debug(name.getMethodName() + ": c1=" + c1 + ", c2=" + c2 + ", c3=" + c3);
	}

	private int testConcurrentLastEndpointAndFirstStartpointForSameCorrelationIdSub() throws Throwable {
		int outcome = -1;
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
		// Depending on the timing of the threads there are 3 possible outcomes:
		//   - 1 report with 4 checkpoints
		//   - 2 reports with 2 checkpoints
		//   - 1 report with 2 checkpoints
		// And the report and checkpoint names of the report(s) can be a bit different depending on which thread was
		// faster
		List<Report> reports = findAndGetReports(testTool, testTool.getDebugStorage(), correlationId, false);
		if (reports.size() == 1) {
			Report report = reports.get(0);
			assertTrue("Report name incorrect: " + report.getName(),
					report.getName().equals("Thread-0") || report.getName().equals("Thread-1"));
			if (report.getNumberOfCheckpoints() == 4) {
				outcome = 1;
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
				assertWarningUseThreadCreatepointAndThreadStartpoint(listAppender, correlationId, parentThreadName,
						childThreadName, childThreadName);
			} else if (report.getNumberOfCheckpoints() == 2) {
				outcome = 3;
				String nameSuffix = "31";
				String childThreadName = "Thread-1";
				String parentThreadName = null;
				if (report.getName().equals("Thread-1")) {
					// This time second thread was faster, hence use different expected xml
					nameSuffix = "32";
					childThreadName = "Thread-0";
				}
				assertReport(report, resourcePath, reportName + nameSuffix, false, false, false, false, false);
				assertWarningUseThreadCreatepointAndThreadStartpoint(listAppender, correlationId, parentThreadName,
						childThreadName, childThreadName, true);
			} else {
				fail("Didn't find 2 or 4 checkpoints, found " + report.getNumberOfCheckpoints());
			}
		} else if (reports.size() == 2) {
			outcome = 2;
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
		return outcome;
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
		assertEquals("Report should be in progress (number of endpoints + abortpoints doesn't match number of startpoints)", 1, testTool.getNumberOfReportsInProgress());
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
		String parentThreadName = Thread.currentThread().getName();
		String childThreadName = parentThreadName +"-ChildThreadName";
		testTool.startpoint(correlationId, null, reportName, "startmessage1");
		testTool.threadCreatepoint(correlationId, childThreadName);
		Thread.currentThread().setName(childThreadName);
		testTool.threadStartpoint(correlationId, null, "name2", "startmessage2");
		testTool.startpoint(correlationId, null, "name3", "startmessage3");
		testTool.abortpoint(correlationId, null, "name3", "abortmessage3");
		testTool.abortpoint(correlationId, null, "name2", "abortmessage2");
		Thread.currentThread().setName(parentThreadName);
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
		testThreads(correlationId, true, true, true, true, false, EndParentThreadAfter.CHILD_THREAD_CREATEPOINT, false);
		testTool.close(correlationId);
		assertReport(correlationId);
	}

	@Test
	public void testCancelThreads() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThreads(correlationId, true, true, true, true, false, EndParentThreadAfter.CHILD_THREAD_CREATEPOINT, false);
		testTool.close(correlationId, Thread.currentThread().getName() + "-ChildThreadNameA");
		testTool.close(correlationId, Thread.currentThread().getName() + "-ChildThreadNameB");
		assertReport(correlationId);
	}

	/*
	 * This test reproduces IndexOutOfBoundsException on version 2.3-20221102.162219 (commit 3b93527) (on which it was
	 * reported)
	 */
	@Test
	public void testRemoveThreadCreatepoint() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		String parentThreadName = Thread.currentThread().getName();
		String childThreadName1 = "child-1";
		String childThreadName2 = "child-2";
		String childThreadName3 = "child-3";
		String childThreadName4 = "child-4";
		String childThreadName5 = "child-5";
		String childThreadName6 = "child-6";

		testTool.startpoint(correlationId, null, reportName, "startmessage");

		Thread.currentThread().setName(parentThreadName);
		testTool.threadCreatepoint(correlationId, childThreadName1);
		Thread.currentThread().setName(childThreadName1);
		testTool.threadStartpoint(correlationId, null, "start1", "message1");

		Thread.currentThread().setName(parentThreadName);
		testTool.threadCreatepoint(correlationId, childThreadName2);

		Thread.currentThread().setName(parentThreadName);
		testTool.threadCreatepoint(correlationId, childThreadName3);

		Thread.currentThread().setName(parentThreadName);
		testTool.threadCreatepoint(correlationId, childThreadName4);

		Thread.currentThread().setName(parentThreadName);
		testTool.threadCreatepoint(correlationId, childThreadName5);

		Thread.currentThread().setName(parentThreadName);
		testTool.threadCreatepoint(correlationId, childThreadName6);

		testTool.close(correlationId, childThreadName6);
		testTool.close(correlationId, childThreadName5);
		testTool.close(correlationId, childThreadName4);

		String firstException = null;
		try {
			Thread.currentThread().setName(childThreadName1);
			testTool.startpoint(correlationId, null, "start2", "message2");
		} catch (Exception e) {
			firstException = e.getClass() + ": " + e.getMessage();
		}

		String secondException= null;
		try {
			Thread.currentThread().setName(parentThreadName);
			testTool.abortpoint(correlationId, this.getClass().getTypeName(), reportName, "endmessage");
		} catch (Exception e) {
			secondException = e.getClass() + ": " + e.getMessage();
		}

		// Prevent report in progress failure
		testTool.close(correlationId);

		assertNull(firstException);
		assertNull(secondException);
		// On commit 3b93527 instead of the previous assertions the following assertions pass:
		// assertEquals("class java.lang.IndexOutOfBoundsException: Index: -1, Size: 4", firstException);
		// assertEquals("class java.lang.IndexOutOfBoundsException: Index: 5, Size: 4", secondException);
	}

	@Test
	public void testCloseMessageCapturers() throws IOException, StorageException {
		testCloseMessageCapturers(true, false, false);
		testCloseMessageCapturers(false, true, false);
		testCloseMessageCapturers(false, false, true);
		testCloseMessageCapturers(true, true, true);
	}

	private void testCloseMessageCapturers(boolean withCloseMethod, boolean withSetMethod, boolean withTask)
			throws IOException, StorageException {
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
		if (!withSetMethod) {
			assertEquals("Report should be in progress (waiting for message capturer to close)", 1, testTool.getNumberOfReportsInProgress());
		}
		if (withCloseMethod) {
			testTool.close(correlationId, false, true);
		}
		if (withTask) {
			CloseReportsTask task = new CloseReportsTask();
			task.setTestTool(testTool);
			task.setMessageCapturersTime(-1);
			task.closeReports();
			if (!withCloseMethod) {
				assertEquals("Report should be in progress (waiting for message capturer to close)", 1, testTool.getNumberOfReportsInProgress());
			}
			task.setMessageCapturersTime(0);
			task.closeReports();
		}

		testWriterMessage(writerMessage);
		testReaderMessage(readerMessage);
		testOutputStreamMessage(outputStreamMessage);
		testInputStreamMessage(inputStreamMessage);

		assertReport(correlationId);

		if (withSetMethod) {
			testTool.setCloseMessageCapturers(false);
		}
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
		assertWarningInLog(listAppender,
				"Maximum number of checkpoints (2) exceeded, ignored checkpoint (name: name2, type: Endpoint, level: 2, correlationId: "
				+ correlationId + ") (next checkpoints for this report will be ignored without any logging)");
		assertEquals(0, testTool.getNumberOfReportsInProgress());
		assertEquals("startmessage2", startmessage2);
		assertEquals("endmessage2", endmessage2);
	}

	@Test
	public void testMaxCheckpointsWithCloseThread() {
		testTool.setMaxCheckpoints(1);
		String correlationId = getCorrelationId();
		String childThreadName1 = "child-1";
		testTool.startpoint(correlationId, null, reportName, "startmessage");
		testTool.threadCreatepoint(correlationId, childThreadName1);
		// Will give IndexOutOfBoundsException when removeThreadCreatepoint() doesn't check index < checkpoints.size()
		testTool.close(correlationId, childThreadName1);
		testTool.endpoint(correlationId, null, reportName, "endmessage");
		assertWarningInLog(listAppender,
				"Maximum number of checkpoints (1) exceeded, ignored checkpoint (name: Waiting for thread 'child-1' to start..., type: ThreadCreatepoint, level: 1, correlationId: "
				+ correlationId + ") (next checkpoints for this report will be ignored without any logging)");
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
