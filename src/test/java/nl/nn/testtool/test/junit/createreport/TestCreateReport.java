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
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.xerces.dom.DocumentImpl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import junit.framework.AssertionFailedError;
import lombok.SneakyThrows;
import nl.nn.testtool.MessageEncoderImpl;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.test.junit.ReportRelatedTestCase;

/**
 * @author Jaco de Groot
 */
public class TestCreateReport extends ReportRelatedTestCase {

	/*
	ook met een thread testen die null als naam heeft
	zelfde corr. id en andere thread
	createThreadpoint met de subthread aanroepen
	*/
	@Override
	public void setUp() {
		resourcePath = RESOURCE_PATH + "createreport/";
		super.setUp();
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
		assertReport(correlationId, true, true, true);
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
		testTool.startpoint(correlationId, null, "startname", "startmessage");

		Writer writerOriginalMessage = new StringWriter();
		Writer writerMessage = testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
		assertNotEquals(writerOriginalMessage, writerMessage);
		testWriterMessage(writerMessage); // Before report is closed

		writerOriginalMessage = new StringWriter();
		writerMessage = testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
		assertNotEquals(writerOriginalMessage, writerMessage);

		// Assert no wrapping of message when same message is used again
		assertEquals(writerMessage, testTool.inputpoint(correlationId, null, "writer", writerMessage));

		ByteArrayOutputStream outputStreamOriginalMessage = new ByteArrayOutputStream();
		OutputStream outputStreamMessage = testTool.inputpoint(correlationId, null, "outputstream", outputStreamOriginalMessage);
		assertNotEquals(outputStreamOriginalMessage, outputStreamMessage);
		testOutputStreamMessage(outputStreamMessage); // Before report is closed

		outputStreamOriginalMessage = new ByteArrayOutputStream();
		outputStreamMessage = testTool.inputpoint(correlationId, null, "outputstream", outputStreamOriginalMessage);
		assertNotEquals(outputStreamOriginalMessage, outputStreamMessage);

		// Assert no wrapping of message when used again
		assertEquals(outputStreamMessage, testTool.inputpoint(correlationId, null, "outputstream", outputStreamMessage));

		testTool.endpoint(correlationId, null, "endname", "endmessage");

		testWriterMessage(writerMessage); // After report is closed

		testOutputStreamMessage(outputStreamMessage); // After report is closed

		assertReport(correlationId, false, false, true);
		Storage storage = testTool.getDebugStorage();
		Report report = storage.getReport((Integer)storage.getStorageIds().get(0));
		assertEquals(report.getCheckpoints().get(1).getMessage(), writerOriginalMessage.toString().substring(0, maxMessageLength));
		assertArrayEquals(report.getCheckpoints().get(4).getMessage().getBytes(), Arrays.copyOf(outputStreamOriginalMessage.toByteArray(), maxMessageLength));
	}

	public void testStreamsAllClosedBeforeReportIsClosed() throws IOException, StorageException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "startname", "startmessage");

		Writer writerOriginalMessage = new StringWriter();
		Writer writerMessage = testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
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

}
