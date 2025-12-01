/*
   Copyright 2019-2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug;

import java.beans.ExceptionListener;
import java.beans.Transient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.bind.annotation.JsonbTransient;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.opentelemetry.api.trace.Span;
import org.wearefrank.ladybug.MessageCapturer.StreamingType;
import org.wearefrank.ladybug.MessageEncoder.ToStringResult;
import org.wearefrank.ladybug.run.ReportRunner;
import org.wearefrank.ladybug.run.RunResult;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.util.ImportResult;
import org.wearefrank.ladybug.util.XmlUtil;

/**
 * @author Jaco de Groot
 */
public class Checkpoint implements Serializable, Cloneable {
	// See comment above field serialVersionUID on class Report
	private static final long serialVersionUID = 4;
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final Pattern GENERIC_VARIABLE_PATTERN = Pattern.compile("\\$\\{.*?\\}");
	private static final Pattern EXTERNAL_VARIABLE_PATTERN = Pattern.compile("\\$\\{checkpoint\\(([0-9]+#[0-9]+)\\)(\\.xpath\\((.*?)\\)|)\\}");

	private Report report;
	private String threadName;
	private String sourceClassName;
	private String messageClassName;
	private String name;
	private String message;
	private Map<String, Object> messageContext;
	private String encoding;
	private String streaming;
	private boolean waitingForStream = false;
	private boolean noCloseReceivedForStream = false;
	private int type;
	private int level = 0;
	private int stub = StubType.FOLLOW_REPORT_STRATEGY.toInt();
	private boolean stubbed = false;
	private String stubNotFound;
	private int preTruncatedMessageLength = -1;
	// For transient fields see comment above the first transient field in class Report
	private transient StringWriter messageCapturerWriter;
	private transient ByteArrayOutputStream messageCapturerOutputStream;
	private transient Map<String, Pattern> variablesPatternMap;
	private transient Span span = null;

	public Checkpoint() {
		// Only for Java XML encoding/decoding! Use other constructor instead.
	}

	public Checkpoint(Report report, String threadName, String sourceClassName,	String name, int type, int level) {
		this.report = report;
		this.threadName = threadName;
		this.sourceClassName = sourceClassName;
		this.name = name;
		this.type = type;
		this.level = level;
	}

	// JsonIgnore is used so that Jackson will not get into an infinite loop trying to reference report,
	// which already contains checkpoint.
	@JsonIgnore
	public void setReport(Report report) {
		this.report = report;
	}

	@JsonIgnore
	// Quarkus with Jackson without @JsonbTransient on opening a report:
	//   javax.json.bind.JsonbException: Recursive reference has been found in class class nl.nn.testtool.Report.
	@JsonbTransient
	public Report getReport() {
		return report;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public String getThreadName() {
		return threadName;
	}

	public void setSourceClassName(String sourceClassName) {
		this.sourceClassName = sourceClassName;
	}

	public String getSourceClassName() {
		return sourceClassName;
	}

	public void setMessageClassName(String messageClassName) {
		this.messageClassName = messageClassName;
	}

	public String getMessageClassName() {
		return messageClassName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setMessage(String message) {
		// report is null when called by XMLDecoder
		if (report != null) {
			message = report.truncateMessage(this, message);
			if (report.getMessageTransformer() != null) {
				message = report.getMessageTransformer().transform(this, message);
			}
		}
		this.message = message;
	}

	public  <T> T setMessage(T message) {
		// For streams message encoder and setMessage() with param type String will be called a second time in
		// Report.closeStreamingMessageListeners(). The first call here will allow the message encoder to set the
		// message to a value that shows that Ladybug is waiting for the stream to be read, captured and closed or any
		// other message
		ToStringResult toStringResult = report.getMessageEncoder().toString(message, null);
		setMessage(toStringResult.getString());
		setEncoding(toStringResult.getEncoding());
		setMessageClassName(toStringResult.getMessageClassName());
		if (message != null && report.getMessageCapturer() != null) {
			// Use message capturer only once for the same message
			if (report.isKnownStreamingMessage(message)) {
				report.addStreamingMessageListener(message, this);
			} else {
				StreamingType streamingType = report.getMessageCapturer().getStreamingType(message);
				if (streamingType == StreamingType.CHARACTER_STREAM || streamingType == StreamingType.BYTE_STREAM) {
					setWaitingForStream(true);
					setStreaming(streamingType.toString());
					TestTool testTool = report.getTestTool();
					// Listener must be added before calling toWriter() and toOutputStream() as while calling those
					// methods the close() method on the stream can already be called
					report.addStreamingMessageListener(message, this);
					// Use array to work around final scope limitation for anonymous inner class
					Object[] messageToClose = new Object[1];
					messageToClose[0] = message;
					String[] charset = new String[1];
					Throwable[] exception = new Throwable[1];
					if (streamingType == StreamingType.CHARACTER_STREAM) {
						messageCapturerWriter = new StringWriter() {
								int length = 0;
								boolean truncated = false;

								@Override
								public void write(String str) {
									write(str.toCharArray(), 0, str.length());
								}

								@Override
								public void write(String str, int off, int len) {
									write(str.toCharArray(), off, len);
								}

								@Override
								public void write(int c) {
									char[] cbuf = new char[1];
									cbuf[0] = (char)c;
									write(cbuf, 0, 1);
								}

								@Override
								public void write(char[] cbuf) {
									write(cbuf, 0, cbuf.length);
								}

								@Override
								public void write(char[] cbuf, int off, int len) {
									if (length + len > testTool.getMaxMessageLength()) {
										if (!truncated) {
											super.write(cbuf, off, testTool.getMaxMessageLength() - length);
											truncated = true;
										}
									}
									length = length + len;
									if (truncated) {
										return;
									} else {
										super.write(cbuf, off, len);
									}
								}

								@Override
								public void close() throws IOException {
									super.close();
									int preTruncatedMessageLength = -1;
									if (truncated) {
										preTruncatedMessageLength = length;
									}
									messageCapturerWriter = null;
									report.closeStreamingMessage(toStringResult.getMessageClassName(),
											messageToClose[0], streamingType.toString(), charset[0],
											toString(), preTruncatedMessageLength, exception[0]);
								}
						};
						message = report.getMessageCapturer().toWriter(message, messageCapturerWriter,
								exceptionNotifier -> exception[0] = exceptionNotifier);
					} else {
						messageCapturerOutputStream = new ByteArrayOutputStream() {
								int length = 0;
								boolean truncated = false;

								@Override
								public void write(int b) {
									byte[] buf = new byte[1];
									buf[0] = (byte)b;
									write(buf, 0, 1);
								}

								@Override
								public void write(byte[] b) {
									write(b, 0, b.length);
								}

								@Override
								public void write(byte[] b, int off, int len) {
									if (length + len > testTool.getMaxMessageLength()) {
										if (!truncated) {
											super.write(b, off, testTool.getMaxMessageLength() - length);
											truncated = true;
										}
									}
									length = length + len;
									if (truncated) {
										return;
									} else {
										super.write(b, off, len);
									}
								}

								@Override
								public void close() throws IOException {
									super.close();
									int preTruncatedMessageLength = -1;
									if (truncated) {
										preTruncatedMessageLength = length;
									}
									messageCapturerOutputStream = null;
									report.closeStreamingMessage(toStringResult.getMessageClassName(),
											messageToClose[0], streamingType.toString(), charset[0],
											toByteArray(), preTruncatedMessageLength, exception[0]);
								}
						};
						message = report.getMessageCapturer().toOutputStream(message, messageCapturerOutputStream,
								charsetNotifier -> charset[0] = charsetNotifier,
								exceptionNotifier -> exception[0] = exceptionNotifier);
					}
					// When message is wrapped by toWriter() or toOutputStream() start listening to the wrapped message
					// instead of the original message message for which addStreamingMessageListener() was called
					// earlier. Also use the wrapped message on close instead of the original message.
					// Ladybug's default MessageCapturerImpl will always wrap the message but for example the
					// Frank!Framework has a Message object that will wrap the request object internally and the
					// implementation of MessageCapturer by the Frank!Framework doesn't wrap the Message object.
					if (message != messageToClose[0]) {
						// First add listener and then remove listener as close() on messageCapturerWriter or
						// messageCapturerOutputStream may be called in the mean time in a separate thread
						report.addStreamingMessageListener(message, this);
						Object origMessage = messageToClose[0];
						messageToClose[0] = message;
						report.removeStreamingMessageListener(origMessage, this);
					}
				}
			}
		}
		return message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessageContext(Map<String, Object> messageContext) {
		this.messageContext = messageContext;
	}

	public Map<String, Object> getMessageContext() {
		return messageContext;
	}

	public void closeMessageCapturer() {
		try {
			if (messageCapturerWriter != null) {
				noCloseReceivedForStream = true;
				messageCapturerWriter.close();
			}
		} catch (IOException e) {
			log.warn("Could not close messageCapturerWriter", e);
		}
		try {
			if (messageCapturerOutputStream != null) {
				noCloseReceivedForStream = true;
				messageCapturerOutputStream.close();
			}
		} catch (IOException e) {
			log.warn("Could not close messageCapturerOutputStream", e);
		}
	}

	@JsonIgnore
	public Object getMessageAsObject() {
		return report.getMessageEncoder().toObject(this);
	}

	@JsonIgnore
	public <T> T getMessageAsObject(T messageToStub) {
		return report.getMessageEncoder().toObject(this, messageToStub);
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getEncoding() {
		return encoding;
	}

	/**
	 * Indicates whether the message is (being) captured from a stream and when this is the case the
	 * {@link StreamingType} being used
	 * 
	 * @param streaming ...
	 */
	public void setStreaming(String streaming) {
		this.streaming = streaming;
	}

	public String getStreaming() {
		return streaming;
	}

	public void setWaitingForStream(boolean waitingForStream) {
		this.waitingForStream = waitingForStream;
	}

	public boolean isWaitingForStream() {
		return waitingForStream;
	}

	public void setNoCloseReceivedForStream(boolean noCloseReceivedForStream) {
		this.noCloseReceivedForStream = noCloseReceivedForStream;
	}

	public boolean isNoCloseReceivedForStream() {
		return noCloseReceivedForStream;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public String getTypeAsString() {
		return CheckpointType.toString(getType());
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}

	public void setStub(int stub) {
		this.stub = stub;
	}

	public int getStub() {
		return stub;
	}

	public void setStubbed(boolean stubbed) {
		this.stubbed = stubbed;
	}

	public boolean isStubbed() {
		return stubbed;
	}

	public void setStubNotFound(String stubNotFound) {
		this.stubNotFound = stubNotFound;
	}

	public String getStubNotFound() {
		return stubNotFound;
	}

	@JsonIgnore
	public Path getPath() {
		return getPath(false);
	}

	protected Path getPath(boolean checkpointInProgress) {
		int currentLevel = level;
		String currentName = name;
		if (currentLevel < 0) {
			currentName = "[INVALID LEVEL " + currentLevel + "]" + currentName;
			currentLevel = 0;
		}
		Path path = new Path(currentLevel + 1);
		path.setName(currentLevel, currentName);
		int i = report.getCheckpoints().indexOf(this);
		if (i == -1 && checkpointInProgress) {
			// Checkpoint constructed but not added to list of checkpoints yet
			i = report.getCheckpoints().size();
		}
		i--;
		for ( ; i >= 0; i--) {
			Checkpoint currentCheckpoint = report.getCheckpoints().get(i);
			String nextName = currentCheckpoint.getName();
			if (currentCheckpoint.getLevel() == currentLevel &&
					((nextName == null && currentName == null) || nextName.equals(currentName))) {
				path.incrementCount(currentLevel);
			} else if (currentCheckpoint.getLevel() < currentLevel && currentCheckpoint.getLevel() > -1) {
				currentLevel = currentCheckpoint.getLevel();
				currentName = nextName;
				path.setName(currentLevel, currentName);
			}
		}
		return path;
	}

	/**
	 * Estimated memory usage in bytes.
	 * 
	 * @return estimated memory usage in bytes
	 */
	public long getEstimatedMemoryUsage() {
		if (message == null) {
			return 0L;
		} else {
			return message.length() * 2L;
		}
	}

	@Override
	public Checkpoint clone() throws CloneNotSupportedException {
		Checkpoint checkpoint = (Checkpoint)super.clone();
		checkpoint.setReport(null);
		return checkpoint;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Sets the length of the message before it was truncated, so that the Ladybug's UI
	 * can display the amount of characters that were removed from the original message.
	 * 
	 * @param length The length of the message before it was truncated.
	 */
	public void setPreTruncatedMessageLength(int length) {
		preTruncatedMessageLength = length;
	}

	public int getPreTruncatedMessageLength() {
		return preTruncatedMessageLength;
	}

	public String getMessageWithResolvedVariables(ReportRunner reportRunner) {
		String result = getMessage();
		if(getMessage() != null && containsVariables()) {
			// 1. Parse external report variables
			if(reportRunner != null) {
				List<MatchResult> matchResults = new ArrayList<MatchResult>();
				Matcher m = EXTERNAL_VARIABLE_PATTERN.matcher(getMessage());
				while(m.find()) {
					matchResults.add(m.toMatchResult());
				}
				for(MatchResult matchResult : matchResults) {
					int reportStorageId = Integer.parseInt(matchResult.group(1).split("#")[0]);
					int checkpointIndex = Integer.parseInt(matchResult.group(1).split("#")[1]);
					String xpathExpression = null;
					if(StringUtils.isNotEmpty(matchResult.group(2))) {
						xpathExpression = matchResult.group(3);
					}
					
					// Determine the target report
					Report targetReport = null;
					try {
						for(Entry<Integer, RunResult> entry : reportRunner.getResults().entrySet()) {
							if(entry.getKey() == reportStorageId) {
								targetReport = reportRunner.getRunResultReport(entry.getValue().correlationId);
							}
						}
					} catch (StorageException e) {
						log.error(e.getMessage(), e);
					}
					// Attempt to fetch data from xpath in target checkpoint's XML message
					if(targetReport != null) {
						try {
							String targetCheckpointMessage = targetReport.getCheckpoints().get(checkpointIndex).getMessage();
							if(StringUtils.isNotEmpty(targetCheckpointMessage)) {
								if(StringUtils.isNotEmpty(xpathExpression)) {
									try {
										String xpathResult = XmlUtil.createXPathExpression(xpathExpression).evaluate(
												XmlUtil.createXmlSourceFromString(targetCheckpointMessage));
										if(xpathResult != null) {
											try {
												result = result.replace(matchResult.group(), xpathResult);
											} catch (IllegalArgumentException e) {
												if(GENERIC_VARIABLE_PATTERN.matcher(xpathResult).find()) {
													log.warn(warningMessageHeader(matchResult.group())
															+"Specified xpath expression points to incorrectly parsed parameter "+xpathResult+"; "
															+ "see other recent log warnings for a possible cause");
												}
											}
										}
									} catch (XPathExpressionException e) {
										log.warn(warningMessageHeader(matchResult.group())+"Invalid xpath expression or XML message in target checkpoint");
									}
								} else {
									result = result.replaceAll(Pattern.quote(matchResult.group()), targetCheckpointMessage);
								}
							} else {
								log.warn(warningMessageHeader(matchResult.group())+"Target checkpoint ["+targetReport.getCheckpoints().get(checkpointIndex)+"] contains no message");
							}
						} catch (IndexOutOfBoundsException e) {
							log.warn(warningMessageHeader(matchResult.group())+"Index out of bounds: checkpoint with index ["+checkpointIndex+"] does not exist in report with storageId ["+reportStorageId+"]");
						}
					} else {
						log.warn(warningMessageHeader(matchResult.group())+"Run result not found for storageId ["+reportStorageId+"] - please make sure it runs before this report");
					}
				}
			}
			// 2. Parse local variables
			if(report.getVariables() != null) {
				Map<String, String> variablesMap = report.getVariables();
				Map<String, Pattern> variablesPatternMap = getVariablePatternMap(variablesMap);
				for(Entry<String, String> entry : variablesMap.entrySet()) {
					Matcher m = variablesPatternMap.get(entry.getKey()).matcher(getMessage());
					while(m.find()) {
						result = result.replaceAll(Pattern.quote(m.group()), entry.getValue());
					}
				}
			}
		}
		return result;
	}

	private String warningMessageHeader(String parameter) {
		return "Could not parse parameter "+parameter+" found in the input of report ["+report.getFullPath()+"] with storageId ["+report.getStorageId()+"]\n"; 
	}
	
	public boolean containsVariables() {
		if(StringUtils.isEmpty(getMessage())) return false;
		return GENERIC_VARIABLE_PATTERN.matcher(getMessage()).find();
	}

	protected Map<String, Pattern> getVariablePatternMap(Map<String, String> variableMap) {
		if(variablesPatternMap == null) {
			variablesPatternMap = new HashMap<String, Pattern>();
			for(Entry<String, String> entry : variableMap.entrySet()) {
				variablesPatternMap.put(entry.getKey(), Pattern.compile("\\$\\{"+entry.getKey()+"\\}"));
			}
		}
		return variablesPatternMap;
	}

	public int getIndex() {
		return report.getCheckpoints().indexOf(this);
	}

	// Use lowercase to make JSON-B behave the same as Jackson
	// See also comment in Report above Integer transientStorageId
	public String getUid() {
		return report.getStorageId()+"#"+report.getCheckpoints().indexOf(this);
	}

	/**
	 * To be called when reports are uploaded to the Ladybug. Updates variables referring to 
	 * a report that had its storageId changed.
	 * @param importResults ...
	 * @return ...
	 */
	public boolean updateVariables(List<ImportResult> importResults) {
		boolean isVariablesUpdated = false;
		Matcher m = EXTERNAL_VARIABLE_PATTERN.matcher(getMessage());
		List<MatchResult> matchResults = new ArrayList<MatchResult>();
		while(m.find()) {
			matchResults.add(m.toMatchResult());
		}
		for(MatchResult matchResult : matchResults) {
			int matchResultStorageId = Integer.valueOf(matchResult.group(1).split("#")[0]);
			for(ImportResult importResult : importResults) {
				if(matchResultStorageId == importResult.getOldStorageId()) {
					int newStorageId = importResult.getNewStorageId();
					String newVar = matchResult.group().replaceAll(String.valueOf(matchResultStorageId), String.valueOf(newStorageId));
					setMessage(getMessage().replace(matchResult.group(), newVar));
					isVariablesUpdated = true;
				}
			}
		}
		return isVariablesUpdated;
	}

	@Transient
	@JsonIgnore
	public void setSpan(Span span) {
		this.span = span;
	}

	@Transient
	@JsonIgnore
	public Span getSpan() {
		return span;
	}
}

class XMLEncoderExceptionListener implements ExceptionListener {
	boolean exceptionThrown = false;

	@Override
	public void exceptionThrown(Exception e) {
		exceptionThrown = true;
	}

	public boolean isExceptionThrown() {
		return exceptionThrown;
	}

}