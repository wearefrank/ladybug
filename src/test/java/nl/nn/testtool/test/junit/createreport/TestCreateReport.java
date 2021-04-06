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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import org.apache.xerces.dom.DocumentImpl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.SneakyThrows;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MessageCapturerImpl;
import nl.nn.testtool.MessageEncoderImpl;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.test.junit.ReportRelatedTestCase;

/**
 * @author Jaco de Groot
 */
public class TestCreateReport extends ReportRelatedTestCase {

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
		// Same but with StubableCode
		correlationId = getCorrelationId();
		testTool.startpoint(correlationId, this.getClass().getTypeName(), "startname", () -> {return "startmessage";}, new HashSet<String>());
		testTool.endpoint(correlationId, this.getClass().getTypeName(), "endname", () -> {return "endmessage";}, new HashSet<String>());
		assertReport(correlationId);
	}

	public void testTwoStartAndEndPointPlainMessages() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "name1", "startmessage1");
		testTool.startpoint(correlationId, null, "name2", "startmessage2");
		testTool.endpoint(correlationId, null, "name2", "endmessage2");
		testTool.endpoint(correlationId, null, "name1", "endmessage1");
		assertReport(correlationId);
	}

	public void testSpecialValues() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "name1", null);
		testTool.infopoint(correlationId, null, "infoname1", new Date(0));
		testTool.infopoint(correlationId, null, "infoname2", new IOException("Test with strange object"));
		testTool.infopoint(correlationId, null, "infoname3", 123);
		testTool.infopoint(correlationId, null, "infoname4", new Integer(456));
		testTool.infopoint(correlationId, null, "infoname5", new DocumentImpl().createElement("NodeTest"));
		testTool.endpoint(correlationId, null, "name1", "");
		assertReport(correlationId, true, true, true, true);
	}

	public void testThread() throws StorageException, IOException {
		testThread(getCorrelationId(), true, true, true, true, false, true);
		testThread(getCorrelationId(), true, true, true, false, false, true);
	}

	public void testThreadWithoutThreadCreatepoint() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThread(correlationId, false, true, true, true, false, true);
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(Level.WARN, loggingEvents.get(0).getLevel());
		assertEquals("New child thread '" + correlationId
				+ "-ChildThreadId' for parent thread 'main' detected, use threadCreatepoint() before threadStartpoint() for checkpoint (name: name2, type: ThreadStartpoint, level: null, correlationId: "
				+ correlationId + ")", loggingEvents.get(0).getMessage());
		loggingEvents.remove(0);
	}

	public void testThreadWithoutThreadCreatepointAndThreadStartpoint() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThread(correlationId, false, false, true, true, false, true);
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(Level.WARN, loggingEvents.get(0).getLevel());
		assertEquals("New child thread '" + correlationId
				+ "-ThreadName' for parent thread 'main' detected, use threadCreatepoint() and threadStartpoint() instead of startpoint() for checkpoint (name: name3, type: ThreadStartpoint, level: null, correlationId: "
				+ correlationId + ")", loggingEvents.get(0).getMessage());
		loggingEvents.remove(0);
	}

	public void testThreadWithThreadCreatepointOnly() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThread(correlationId, true, false, false, false, false, false);
		assertEquals("Report should be in progress (waiting for threadStartpoint),", 1, testTool.getNumberOfReportsInProgress());
		testTool.close(correlationId, correlationId + "-ChildThreadId");
		assertReport(correlationId);
	}

	private void testThread(String correlationId, boolean useThreadCreatepoint, boolean useThreadStartpoint,
			boolean useChildCheckpoints, boolean waitForChildThread, boolean keepThreadOpen, boolean assertReport)
			throws StorageException, IOException {
		String childThreadId = correlationId + "-ChildThreadId";
		String threadName = correlationId + "-ThreadName";
		testTool.startpoint(correlationId, null, "name1", "startmessage1");
		if (useThreadCreatepoint) {
			testTool.threadCreatepoint(correlationId, childThreadId);
		}
		if (!waitForChildThread) {
			testTool.endpoint(correlationId, null, "name1", "endmessage1");
		}
		String originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		if (useThreadStartpoint) {
			testTool.threadStartpoint(correlationId, childThreadId, null, "name2", "startmessage2");
		}
		if (useChildCheckpoints) {
			testTool.startpoint(correlationId, null, "name3", "startmessage3");
			if (!keepThreadOpen) {
				testTool.endpoint(correlationId, null, "name3", "endmessage3");
			}
		}
		if (useThreadStartpoint) {
			testTool.threadEndpoint(correlationId, null, "name2", "endmessage2");
		}
		Thread.currentThread().setName(originalThreadName);
		if (waitForChildThread) {
			testTool.endpoint(correlationId, null, "name1", "endmessage1");
		}
		if (assertReport) {
			assertReport(correlationId);
		}
	}

	public void testAbort() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "name1", "startmessage1");
		testTool.startpoint(correlationId, null, "name2", "startmessage2");
		testTool.abortpoint(correlationId, null, "name2", "abortmessage2");
		testTool.abortpoint(correlationId, null, "name1", "abortmessage1");
		assertReport(correlationId);
	}

	public void testAbortWithoutEnoughAbortpoints() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "name1", "startmessage1");
		testTool.startpoint(correlationId, null, "name2", "startmessage2");
		testTool.abortpoint(correlationId, null, "name2", "endmessage2");
		assertEquals("Report should be in progress (number of endpoints + abortpoints doesn't match number of startpoints),", 1, testTool.getNumberOfReportsInProgress());
		testTool.close(correlationId);
		assertReport(correlationId);
	}

	public void testAbortThread() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		String threadName = correlationId + "-ThreadName";
		testTool.startpoint(correlationId, null, "name1", "startmessage1");
		testTool.threadCreatepoint(correlationId, threadName);
		String originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		testTool.threadStartpoint(correlationId, null, "name2", "startmessage2");
		testTool.startpoint(correlationId, null, "name3", "startmessage3");
		testTool.abortpoint(correlationId, null, "name3", "abortmessage3");
		testTool.abortpoint(correlationId, null, "name2", "abortmessage2");
		Thread.currentThread().setName(originalThreadName);
		testTool.endpoint(correlationId, null, "name1", "endmessage1");
		assertReport(correlationId);
	}

	public void testCloseReport() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "name1", "startmessage1");
		testTool.startpoint(correlationId, null, "name2", "startmessage2");
		testTool.close(correlationId);
		assertReport(correlationId);
	}

	public void testCloseReportWithThreadOpen() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThread(correlationId, true, true, true, false, true, false);
		testTool.close(correlationId);
		assertReport(correlationId);
	}

	public void testCloseThread() throws StorageException, IOException {
		String correlationId = getCorrelationId();
		testThread(correlationId, true, true, true, false, true, false);
		testTool.close(correlationId, correlationId + "-ThreadName");
		assertReport(correlationId);
	}

	public void testIgnoreReport() throws StorageException {
		Storage storage = testTool.getDebugStorage();
		testTool.setRegexFilter("^(?!testIgnoreReport).*");
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "testIgnoreReport", "startmessage1");
		String startmessage2 = testTool.startpoint(correlationId, null, "level2", () -> {return "startmessage2";}, new HashSet<String>());
		String endmessage2 = testTool.endpoint(correlationId, null, "level2", () -> {return "endmessage2";}, new HashSet<String>());
		testTool.endpoint(correlationId, null, "testIgnoreReport", "endmessage1");
		Report report = findAndGetReport(testTool, storage, correlationId, false);
		assertNull("Report should have been ignored", report);
		assertEquals("startmessage2", startmessage2);
		assertEquals("endmessage2", endmessage2);
	}

	public void testIgnoreReportAndAbort() throws StorageException {
		Storage storage = testTool.getDebugStorage();
		testTool.setRegexFilter("^(?!testIgnoreReport).*");

		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "testIgnoreReport", "startmessage1");
		testTool.startpoint(correlationId, null, "level2", "startmessage2");
		testTool.abortpoint(correlationId, null, "level2", "abortmessage2");
		testTool.abortpoint(correlationId, null, "testIgnoreReport", "abortmessage1");
		Report report = findAndGetReport(testTool, storage, correlationId, false);
		assertNull("Report should have been ignored", report);

		correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "testIgnoreReport", "startmessage1");
		testTool.startpoint(correlationId, null, "level2", "startmessage2");
		testTool.abortpoint(correlationId, null, "level2", "abortmessage2");
		testTool.close(correlationId);
		report = findAndGetReport(testTool, storage, correlationId, false);
		assertNull("Report should have been ignored", report);
	}

	public void testReportFilter() throws StorageException, IOException {
		testTool.setRegexFilter("name1");
		setName("testTwoStartAndEndPointPlainMessages");
		testTwoStartAndEndPointPlainMessages();
	}

	public void testMaxCheckpoints() throws StorageException, IOException {
		testTool.setMaxCheckpoints(2);
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "name1", "startmessage1");
		String startmessage2 = testTool.startpoint(correlationId, null, "name2", () -> {return "startmessage2";}, new HashSet<String>());
		String endmessage2 = testTool.endpoint(correlationId, null, "name2", () -> {return "endmessage2";}, new HashSet<String>());
		testTool.endpoint(correlationId, null, "name1", "endmessage1");
		assertReport(correlationId);
		List<ILoggingEvent> loggingEvents = listAppender.list;
		assertEquals(Level.WARN, loggingEvents.get(0).getLevel());
		assertEquals("Maximum number of checkpoints exceeded, ignored checkpoint (name: name2, type: Endpoint, level: 2, correlationId: "
				+ correlationId
				+ ") (next checkpoints for this report will be ignored without any logging)",
				loggingEvents.get(0).getMessage());
		loggingEvents.remove(0);
		assertEquals(0, testTool.getNumberOfReportsInProgress());
		assertEquals("startmessage2", startmessage2);
		assertEquals("endmessage2", endmessage2);
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
		testTool.startpoint(correlationId, null, "name", "startmessage");

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

		testTool.endpoint(correlationId, null, "name", "endmessage");

		testWriterMessage(writerMessage); // After report is closed

		testOutputStreamMessage(outputStreamMessage); // After report is closed

		assertReport(correlationId, false, false, false, true);
		Storage storage = testTool.getDebugStorage();
		Report report = findAndGetReport(testTool, storage, correlationId);
		assertEquals(report.getCheckpoints().get(1).getMessage(), writerOriginalMessage.toString().substring(0, maxMessageLength));
		assertArrayEquals(report.getCheckpoints().get(4).getMessage().getBytes(), Arrays.copyOf(outputStreamOriginalMessage.toByteArray(), maxMessageLength));
	}

	public void testStreamsAllClosedBeforeReportIsClosed() throws IOException, StorageException {
		String correlationId = getCorrelationId();
		testTool.startpoint(correlationId, null, "name", "startmessage");

		Writer writerOriginalMessage = new StringWriter();
		Writer writerMessage = testTool.inputpoint(correlationId, null, "writer", writerOriginalMessage);
		assertNotEquals(writerOriginalMessage, writerMessage);
		testWriterMessage(writerMessage);

		testTool.endpoint(correlationId, null, "name", "endmessage");

		assertReport(correlationId);
	}

	public void testStreamWithCharset() throws IOException, StorageException {
		byte[] bytes = new byte[2];
		bytes[0] = (byte)235; // ë
		bytes[1] = (byte)169; // ©
		String actual = testTool.getMessageEncoder().toString(bytes, "ISO-8859-1").getString();
		assertEquals("ë©" , actual);
		testTool.setMessageCapturer(new MessageCapturerImpl() {
			@Override
			@SneakyThrows
			public <T> T toOutputStream(T message, OutputStream outputStream, Consumer<String> charsetNotifier) {
				charsetNotifier.accept("ISO-8859-1");
				return super.toOutputStream(message, outputStream, charsetNotifier);
			}
		});
		String correlationId = getCorrelationId();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		OutputStream outputStream =
				testTool.startpoint(correlationId, "sourceClassName", "name", byteArrayOutputStream);
		testTool.endpoint(correlationId, "sourceClassName", "name", "message");
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
