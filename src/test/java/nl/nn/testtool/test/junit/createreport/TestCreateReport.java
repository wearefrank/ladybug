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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;

/**
 * @author Jaco de Groot
 */
public class TestCreateReport extends TestCase {

	private static TestTool testTool = (TestTool) new ClassPathXmlApplicationContext("springTestToolTestJUnit.xml").getBean("testTool");

	// TODO [junit] overal checken of reports in progress?
	
	/*
	ook met een thread testen die null als naam heeft
	zelfde corr. id en andere thread
	createThreadpoint met de subthread aanroepen
	*/

	/* GUI op zelfde storage laten werken als junit?
	Controleren in GUI:
	- Worden reports goed weergegeven?
	- Wordt abort goed weergegeven?
*/

	private ListAppender<ILoggingEvent> listAppender;

	@Override
	public void setUp() {
		testTool.setRegexFilter(null);
		Logger log = (Logger)LoggerFactory.getLogger("nl.nn.testtool");
		listAppender = new ListAppender<>();
		listAppender.start();
		log.addAppender(listAppender);
	}

	@Override
	public void tearDown() {
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(0, loggingEvents.size());
	}

	public void testSingleStartAndEndPointPlainMessage() {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, "startclassname", "startname", "startmessage");
		testTool.endpoint(correlationId, "endclassname", "endname", "endmessage");
		assertReport(correlationId, "test1.xslt");
	}

	public void testTwoStartAndEndPointPlainMessages() {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, "startclassname", "startname1", "startmessage1");
		testTool.startpoint(correlationId, "startclassname", "startname2", "startmessage2");
		testTool.endpoint(correlationId, "endclassname", "endname2", "endmessage2");
		testTool.endpoint(correlationId, "endclassname", "endname1", "endmessage1");
		assertReport(correlationId, "test1.xslt");
	}

	public void testThread() {
		testThread(getCorrelationId(), true, true);
	}

	public void testThreadWithoutThreadCreatepoint() {
		String correlationId = getCorrelationId();
		testThread(correlationId, false, true);
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(Level.WARN, loggingEvents.get(0).getLevel());
		assertEquals("New child thread '" + correlationId
				+ "-ChildThreadId' for parent thread 'main' detected, use threadCreatepoint() before threadStartpoint() for checkpoint (name: startname2, type: ThreadStartpoint, level: null, correlationId: "
				+ correlationId + ")", loggingEvents.get(0).getMessage());
		loggingEvents.remove(0);
	}

	public void testThreadWithoutThreadCreatepointAndThreadStartpoint() {
		String correlationId = getCorrelationId();
		testThread(correlationId, false, false);
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(Level.WARN, loggingEvents.get(0).getLevel());
		assertEquals("New child thread '" + correlationId
				+ "-ThreadName' for parent thread 'main' detected, use threadCreatepoint() and threadStartpoint() instead of startpoint() for checkpoint (name: startname3, type: ThreadStartpoint, level: null, correlationId: "
				+ correlationId + ")", loggingEvents.get(0).getMessage());
		loggingEvents.remove(0);
	}

	private void testThread(String correlationId, boolean useThreadCreatepoint, boolean useThreadStartpoint) {
		String childThreadId = correlationId + "-ChildThreadId";
		String threadName = correlationId + "-ThreadName";
		testTool.startpoint(correlationId, "startclassname", "startname1", "startmessage1");
		if (useThreadCreatepoint) {
			testTool.threadCreatepoint(correlationId, childThreadId);
		}
		String originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		if (useThreadStartpoint) {
			testTool.threadStartpoint(correlationId, childThreadId, "threadstartclassname", "startname2", "startmessage2");
		}
		testTool.startpoint(correlationId, "startclassname", "startname3", "startmessage3");
		testTool.endpoint(correlationId, "endclassname", "endname3", "endmessage3");
		if (useThreadStartpoint) {
			testTool.threadEndpoint(correlationId, "threadendclassname", "endname2", "endmessage2");
		}
		Thread.currentThread().setName(originalThreadName);
		testTool.endpoint(correlationId, "endclassname", "endname1", "endmessage1");
		assertReport(correlationId, "test1.xslt");
	}

	public void testAbort() {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, "startclassname", "startname1", "startmessage1");
		testTool.startpoint(correlationId, "startclassname", "startname2", "startmessage2");
		testTool.abortpoint(correlationId, "startclassname", "startname1", "endmessage2");
		assertReport(correlationId, "test1.xslt");
	}

	public void testAbortWithNonExistingStartname() {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, "startclassname", "startname1", "startmessage1");
		testTool.startpoint(correlationId, "startclassname", "startname2", "startmessage2");
		testTool.abortpoint(correlationId, "abortclassname", "wrong", "endmessage2");
		assertReport(correlationId, "test1.xslt");
	}

	public void testAbortThread() {
		String correlationId = getCorrelationId();
		String childThreadId = correlationId + "-ChildThreadId";
		String threadName = correlationId + "-ThreadName";
		testTool.startpoint(correlationId, "startclassname", "startname1", "startmessage1");
		testTool.threadCreatepoint(correlationId, childThreadId);
		String originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		testTool.threadStartpoint(correlationId, childThreadId, "threadstartclassname", "startname2", "startmessage2");
		testTool.abortpoint(correlationId, "abortclassname", "wrong", "endmessage2");
		Thread.currentThread().setName(originalThreadName);
		testTool.endpoint(correlationId, "endclassname", "endname1", "endmessage1");
		assertReport(correlationId, "test1.xslt");
	}

	public void testIgnoreReport() throws StorageException {
		Storage storage = testTool.getDebugStorage();
		testTool.setRegexFilter("^(?!testIgnoreReport).*");
		Report report1 = storage.getReport((Integer)storage.getStorageIds().get(0));
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, "startclassname", "testIgnoreReport", "startmessage1");
		testTool.startpoint(correlationId, "startclassname", "level2", "startmessage2");
		testTool.endpoint(correlationId, "endclassname", "level2", "endmessage2");
		testTool.endpoint(correlationId, "endclassname", "testIgnoreReport", "endmessage1");
		Report report2 = storage.getReport((Integer)storage.getStorageIds().get(0));
		try {
			assertEquals(report1.toXml(), report2.toXml());
		} catch(AssertionFailedError e) {
			System.err.println("Following report should have been ignored: " + report2.toXml());
			throw e;
		}
	}

	public void testReportFilter() throws StorageException {
		testTool.setRegexFilter("startname1");
		setName("testTwoStartAndEndPointPlainMessages");
		testTwoStartAndEndPointPlainMessages();
	}

	private Report assertReport(String correlationId, String xslt) {
		Storage storage = testTool.getDebugStorage();
//		List metadataNames = new ArrayList();
//		metadataNames.add("storageId");
//		List metadata = null;
//		try {
//			metadata = storage.getMetadata(1, metadataNames, null, MetadataExtractor.VALUE_TYPE_OBJECT);
//		} catch (StorageException e) {
//		}
//		assertNotNull(metadata);

//TODO [junit] nette oplossing zoeken
//Deze methode aan TestTool.java toevoegen:
//		public static List getReportsInProgress() {
//			return reportsInProgress;
//		}
//Dan kan dit worden gebruikt:
//		List list = TestTool.getReportsInProgress();
//		Iterator iterator = list.iterator();
//		while (iterator.hasNext()) {
//			Report report = (Report)iterator.next();
//			ReportXmlTransformer reportXmlTransformer = new ReportXmlTransformer();
//			assertNull(reportXmlTransformer.setXslt(getResource(xslt)));
//			report.setReportXmlTransformer(reportXmlTransformer);
//			System.out.println(getName() + " in progress: [" + report.toXml() + "]");
//		}
//		assertTrue(list.size() == 0);

		Report report = null;
		try {
//exceptie laten gooien (try/catch weghalen)?
//als er geen report is gemaakt klopt get(0) niet. in setUp() opslaan wat laatste storageId was en hier checken dat die niet hetzelfde is?
			report = storage.getReport((Integer)storage.getStorageIds().get(0));
		} catch (StorageException e) {
		}
		assertNotNull(report);
		ReportXmlTransformer reportXmlTransformer = new ReportXmlTransformer();
		reportXmlTransformer.setXslt(getResource(xslt));
		report.setReportXmlTransformer(reportXmlTransformer);
		String found = report.toXml();
		String expected = getResource(getName() + ".xml");
		if (!found.equals(expected)) {
			System.err.println("===");
			System.err.println("=== " + getName() + " ===");
			System.err.println("===");
			System.err.println("found:");
			System.err.println("[" + found + "]");
			System.err.println("expected:");
			System.err.println("[" + expected + "]");
			System.err.println("equal part:");
			System.err.print("[");
			int i = 0;
			for (; i < found.length() && i < expected.length() && found.charAt(i) == expected.charAt(i); i++) {
				System.err.print(found.charAt(i));
			}
			System.err.println("]");
			System.err.println("found next char: " + found.charAt(i) + " (" + (int)found.charAt(i) + ")");
			System.err.println("expected next char: " + expected.charAt(i) + " (" + (int)expected.charAt(i) + ")");
		}
		assertEquals(found, expected);
		return report;
	}

	private String getCorrelationId() {
		return getName() + "-" + System.currentTimeMillis();
	}

	private String getResource(String name) {
		StringBuffer result = new StringBuffer();
		String resourceName = "nl/nn/testtool/test/junit/createreport/" + name;
		InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName);
		if (stream == null) {
			throw new junit.framework.AssertionFailedError("Could not find resource '" + resourceName + "'");
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
}
