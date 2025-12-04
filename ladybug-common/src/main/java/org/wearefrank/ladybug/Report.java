/*
   Copyright 2018-2025 WeAreFrank!

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

import java.beans.Transient;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import org.wearefrank.ladybug.MessageEncoder.ToStringResult;
import org.wearefrank.ladybug.run.ReportRunner;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.storage.Storage;
import org.wearefrank.ladybug.transform.MessageTransformer;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;
import org.wearefrank.ladybug.util.CsvUtil;
import org.wearefrank.ladybug.util.EscapeUtil;
import org.wearefrank.ladybug.util.XmlUtil;

/**
 * @author Jaco de Groot
 */
public class Report implements Serializable {
	// The serialVersionUID only needs to be changed when throwing an exception
	// on reading an older version needs to be forced. As long as Java is able
	// to read the older version there's probably no reason to force an
	// exception. In the case Java isn't able to read the older version it will
	// throw an exception, hence no reason either to force throwing an
	// exception. The serialVersionUID also only effects reading objects through
	// ObjectInputStream, it doesn't affect reading objects through XMLDecoder.
	private static final long serialVersionUID = 5;
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public static final long TIME_NOT_SET_VALUE = Long.MIN_VALUE;
	// Please note that the set method should return void for XmlEncoder to store the property
	private @Setter @Getter long startTime;
	private @Setter @Getter long endTime = TIME_NOT_SET_VALUE;
	private @Setter @Getter String correlationId;
	private @Setter @Getter String name;
	private @Setter @Getter String description;
	private @Setter @Getter String path;
	private @Setter @Getter String stubStrategy;
	private @Setter @Getter String linkMethod;
	// See Checkpoint also for properties that will be stored by XmlEncoder and
	// serialization / ObjectOutputStream
	private List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
	private @Setter @Getter String transformation;
	private Map<String, String> variables;
	// Please note that the get and set methods need @Transient annotation for XmlEncoder to not store the property.
	// This is in contrast to serialization / ObjectOutputStream that is using variables (and doesn't look at get and
	// set methods) and needs a variable to be declared transient to not store the field.
	// Also note that ObjectInputStream will use default values 0 and false for transient primitives, see
	// https://stackoverflow.com/questions/10531076/serialization-via-objectinputstream-and-transient-fields
	private transient String mainThread;
	private transient long mainThreadFinishedTime = TIME_NOT_SET_VALUE;
	private transient List<String> threads = new ArrayList<>();
	private transient List<String> threadsWithThreadCreatepoint = new ArrayList<>();
	private transient Map<String, Integer> threadCheckpointIndex = new HashMap<>();
	private transient Map<String, Integer> threadFirstLevel = new HashMap<>();
	private transient Map<String, Integer> threadLevel = new HashMap<>();
	private transient Map<String, String> threadParent = new HashMap<>();
	private transient int threadsActiveCount = 0;
	private transient TestTool testTool;
	private transient boolean closed;
	private transient Storage storage;
	// Property storageId will not be exposed by JSON-B when using storageId instead of transientStorageId, see also:
	//   https://github.com/jakartaee/jsonb-api/issues/269
	// For Jackson this is not the case (it will expose the property in both situations).
	// Other differences between JSON-B and Jackson:
	//   - Property names in the json response:
	//     - JSON-B:
	//       - Checkpoint.getUID() -> "UID":"0#0"
	//       - Checkpoint.getUid() -> "uid":"0#0"
	//     - Jackson:
	//       - Checkpoint.getUID() -> "uid":"0#0"
	//       - Checkpoint.getUid() -> "uid":"0#0"
	//   - Properties with null values are not present in the json response with JSON-B while with Jackson the response
	//     would for example contain "description":null
	// When using Quarkus use either the quarkus-resteasy-jackson dependency or the quarkus-resteasy-jsonb dependency
	// to enable Jackson and JSON-B. 
	// When using CXF see jsonProvider in cxf-beans.xml
	private transient Integer transientStorageId;
	private transient long storageSize;
	// When set to a different value then 0 it will still be 0 after being initialized by ObjectInputStream, see
	// comment above about default values for ObjectInputStream
	private transient long estimatedMemoryUsage = 0L;
	private transient ReportXmlTransformer reportXmlTransformer;
	private transient ReportXmlTransformer globalReportXmlTransformer;
	private transient String xml;
	private transient Report originalReport;
	private transient boolean differenceChecked = false;
	private transient boolean differenceFound = false;
	private transient Map<String, String> truncatedMessageMap = new RefCompareMap<>();
	private transient boolean reportFilterMatching = true;
	private transient boolean logReportFilterMatching = true;
	private transient boolean logMaxCheckpoints = true;
	private transient boolean logMaxMemoryUsage = true;
	private transient Map<Object, Set<Checkpoint>> streamingMessageListeners = new HashMap<>();
	private transient Map<Object, StreamingMessageResult> streamingMessageResults = new HashMap<>();

	@Transient
	@JsonIgnore
	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	@Transient
	@JsonIgnore
	public TestTool getTestTool() {
		return testTool;
	}

	@Transient
	@JsonIgnore
	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	@Transient
	@JsonIgnore
	public boolean isClosed() {
		return closed;
	}

	@Transient
	@JsonIgnore
	public void setStorage(Storage storage) {
		this.storage = storage;
	}

	@Transient
	@JsonIgnore
	public Storage getStorage() {
		return storage;
	}

	/**
	 * Convenient method (for the frontend) to check (without calling {@link #getStorage()}) whether the underlying
	 * storage of this report is a {@link CrudStorage}. Based on this the frontend determines whether it should be
	 * possible to edit the report or not.
	 *
	 * @return true when underlying storage is a {@link CrudStorage}, false otherwise
	 */
	public boolean isCrudStorage() {
		return storage.isCrudStorage();
	}

	public void setStorageId(Integer storageId) {
		transientStorageId = storageId;
	}

	public Integer getStorageId() {
		return transientStorageId;
	}

	@Transient
	@JsonIgnore
	public void setStorageSize(long storageSize) {
		this.storageSize = storageSize;
	}

	@Transient
	@JsonIgnore
	public Long getStorageSize() {
		return storageSize;
	}

	@Transient
	@JsonIgnore
	public void setMainThreadFinishedTime(long mainThreadFinishedTime) {
		this.mainThreadFinishedTime = mainThreadFinishedTime;
	}

	@Transient
	@JsonIgnore
	public long getMainThreadFinishedTime() {
		return mainThreadFinishedTime;
	}

	public String getFullPath() {
		return (StringUtils.isNotEmpty(getPath()) ? getPath() : "/") + getName();
	}

	@Transient
	@JsonIgnore
	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}

	@Transient
	@JsonIgnore
	public ReportXmlTransformer getReportXmlTransformer() {
		return reportXmlTransformer;
	}

	@Transient
	@JsonIgnore
	public void setGlobalReportXmlTransformer(ReportXmlTransformer globalReportXmlTransformer) {
		this.globalReportXmlTransformer = globalReportXmlTransformer;
	}

	@Transient
	@JsonIgnore
	public ReportXmlTransformer getGlobalReportXmlTransformer() {
		return globalReportXmlTransformer;
	}

	@Transient
	@JsonIgnore
	public void setOriginalReport(Report originalReport) {
		this.originalReport = originalReport;
	}

	@Transient
	public Report getOriginalReport() {
		return originalReport;
	}

	@Transient
	public void setReportFilterMatching(boolean reportFilterMatching) {
		this.reportFilterMatching = reportFilterMatching;
	}

	@Transient
	public boolean isReportFilterMatching() {
		return reportFilterMatching;
	}

	protected void init() {
		mainThread = Thread.currentThread().getName();
		threads.add(mainThread);
		threadCheckpointIndex.put(mainThread, 0);
		threadFirstLevel.put(mainThread, 0);
		threadLevel.put(mainThread, 0);
		threadsActiveCount++;
	}

	protected <T> T checkpoint(String childThreadId, String sourceClassName, String name, T message, Map<String, Object> messageContext,
							   StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
							   Set<String> matchingStubStrategies, int checkpointType, int levelChangeNextCheckpoint) {
		if (checkpointType == CheckpointType.THREAD_CREATEPOINT.toInt()) {
			String parentThreadName = Thread.currentThread().getName();
			if (!threads.contains(parentThreadName)) {
				log.warn("Unknown parent thread '" + parentThreadName + "' for child thread '" + childThreadId
						+ "' , ignored checkpoint " + getCheckpointLogDescription(name, checkpointType, null));
			} else {
				name = "Waiting for thread '" + childThreadId + "' to start...";
				threadCreatepoint(parentThreadName, childThreadId);
				threadsWithThreadCreatepoint.add(childThreadId);
			}
		} else {
			if (checkpointType == CheckpointType.THREAD_STARTPOINT.toInt() && !threads.contains(childThreadId)) {
				if (threads.size() == 0) {
					// This can happen when a report is still open because not all message capturers are closed while
					// all threads are finished
					warnNewChildThreadDetected(childThreadId, null, false, name, checkpointType, true);
					return message;
				} else {
					String parentThreadName = threads.get(threads.size() - 1);
					threadCreatepoint(parentThreadName, childThreadId);
					warnNewChildThreadDetected(childThreadId, parentThreadName, false, name, checkpointType, false);
				}
			} else if (checkpointType == CheckpointType.STARTPOINT.toInt()
					&& !threads.contains(Thread.currentThread().getName())) {
				checkpointType = CheckpointType.THREAD_STARTPOINT.toInt();
				childThreadId = Thread.currentThread().getName();
				if (threads.size() == 0 || checkpoints.size() == 0) {
					// No threads can happen when a report is still open because not all message capturers are closed
					// while all threads are finished
					// No checkpoint can happen when two threads start a new report for the same correlationId and one
					// of them is the first to run the synchronized(reportsInProgress) in class TestTool and the other 
					// is the first to run the synchronized(report) in class TestTool (see also devMode in TestTool)
					warnNewChildThreadDetected(childThreadId, null, true, name, checkpointType, true);
					return message;
				} else {
					String parentThreadName = threads.get(threads.size() - 1);
					threadCreatepoint(parentThreadName, childThreadId);
					warnNewChildThreadDetected(childThreadId, parentThreadName, true, name, checkpointType, true);
				}
			}
		}
		message = addCheckpoint(childThreadId, sourceClassName, name, message, messageContext, stubableCode, stubableCodeThrowsException,
				matchingStubStrategies, checkpointType, levelChangeNextCheckpoint
		);
		return message;
	}

	private void warnNewChildThreadDetected(String childThreadId, String parentThreadName,
											boolean threadStartpointNotUsed, String checkpointName, int checkpointType, boolean ignored) {
		String parentThreadWarning = " for guessed parent thread '" + parentThreadName + "'";
		if (parentThreadName == null) {
			parentThreadWarning = " for unknown parent thread";
		}
		String startpointWarning = " before threadStartpoint()";
		if (threadStartpointNotUsed) {
			startpointWarning = " and threadStartpoint() instead of startpoint()";
		}
		String ignoredWarning = "";
		if (ignored) {
			ignoredWarning = " ignored";
		}
		log.warn("New child thread '" + childThreadId + "'" + parentThreadWarning + " detected, use threadCreatepoint()"
				+ startpointWarning + " for" + ignoredWarning + " checkpoint "
				+ getCheckpointLogDescription(checkpointName, checkpointType, null));
	}

	private void threadCreatepoint(String parentThreadName, String childThreadId) {
		threads.add(threads.indexOf(parentThreadName), childThreadId);
		threadCheckpointIndex.put(childThreadId, threadCheckpointIndex.get(parentThreadName));
		threadFirstLevel.put(childThreadId, threadLevel.get(parentThreadName));
		threadLevel.put(childThreadId, threadLevel.get(parentThreadName));
		threadParent.put(childThreadId, parentThreadName);
		threadsActiveCount++;
	}

	/**
	 * For threadCreatepoints a checkpoint is added to the report and removed when a threadStartpoint is added to
	 * visualize the status of waiting for a thread to start. A threadCreatepoint should be visualized as an error as a
	 * thread was expected to start but didn't start.
	 *
	 * @param index
	 * @param threadName
	 */
	private void removeThreadCreatepoint(int index, String childThreadId) {
		if (threadsWithThreadCreatepoint.remove(childThreadId)) {
			// When testTool.getMaxCheckpoints() or testTool.getMaxMemoryUsage() is reached method threadCreatepoint()
			// will still be called but no checkpoint is added, hence check index < checkpoints.size()
			if (index < checkpoints.size()) {
				checkpoints.remove(index);
				for (int i = threads.indexOf(childThreadId) + 1; i < threads.size(); i++) {
					String key = threads.get(i);
					Integer value = threadCheckpointIndex.get(key);
					threadCheckpointIndex.put(key, value - 1);
				}
			}
		}
	}

	private <T> T addCheckpoint(String childThreadId, String sourceClassName, String name, T message, Map<String, Object> messageContext,
								StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
								Set<String> matchingStubStrategies, int checkpointType, int levelChangeNextCheckpoint) {
		String threadName = Thread.currentThread().getName();
		Integer index = threadCheckpointIndex.get(threadName);
		Integer level = threadLevel.get(threadName);
		if (checkpointType == CheckpointType.THREAD_STARTPOINT.toInt()) {
			// At this point index will already be != null when name of the child thread was used as childThreadId
			// when calling threadCreatepoint() (no rename of child thread id in the relevant maps needed in that case)
			if (index == null) {
				index = threadCheckpointIndex.remove(childThreadId);
				if (index != null) {
					// Rename child thread id in the relevant lists and maps to the actual thread name of the child
					// thread (which at this point is the current thread (calling it's first checkpoint with
					// threadStartpoint()) for as far as they are not already the same (in which case index will be
					// initialized with a non null value at the beginning of this method)
					threads.add(threads.indexOf(childThreadId), threadName);
					threads.remove(childThreadId);
					if (threadsWithThreadCreatepoint.remove(childThreadId)) {
						threadsWithThreadCreatepoint.add(threadName);
					}
					threadCheckpointIndex.put(threadName, index);
					level = threadFirstLevel.remove(childThreadId);
					threadFirstLevel.put(threadName, level);
					level = threadLevel.remove(childThreadId);
					threadLevel.put(threadName, level);
					String parent = threadParent.remove(childThreadId);
					threadParent.put(threadName, parent);
				} else {
					log.warn("Unknown childThreadId '" + childThreadId
							+ "', use the same childThreadId when calling threadCreatepoint() and threadStartpoint()");
				}
			}
			if (index != null) {
				removeThreadCreatepoint(index, threadName);
			}
		}
		if (index == null) {
			log.warn("Unknown thread '" + threadName + "', ignored checkpoint "
					+ getCheckpointLogDescription(name, checkpointType, level));
		} else {
			if (!isReportFilterMatching()) {
				message = TestTool.execute(stubableCode, stubableCodeThrowsException, message);
				if (logReportFilterMatching) {
					log.debug("Report name doesn't match report filter regex, ignored checkpoint "
							+ getCheckpointLogDescription(name, checkpointType, level) + " "
							+ getOtherCheckpointsLogDescription());
					logReportFilterMatching = false;
				}
			} else if (checkpoints.size() >= testTool.getMaxCheckpoints()) {
				message = TestTool.execute(stubableCode, stubableCodeThrowsException, message);
				if (logMaxCheckpoints) {
					log.warn("Maximum number of checkpoints (" + testTool.getMaxCheckpoints()
							+ ") exceeded, ignored checkpoint "
							+ getCheckpointLogDescription(name, checkpointType, level) + " "
							+ getOtherCheckpointsLogDescription());
					logMaxCheckpoints = false;
				}
			} else if (getEstimatedMemoryUsage() >= testTool.getMaxMemoryUsage()) {
				message = TestTool.execute(stubableCode, stubableCodeThrowsException, message);
				if (logMaxMemoryUsage) {
					log.warn("Maximum memory usage reached for this report, ignored checkpoint "
							+ getCheckpointLogDescription(name, checkpointType, level) + " "
							+ getOtherCheckpointsLogDescription());
					logMaxMemoryUsage = false;
				}
			} else {
				message = addCheckpoint(threadName, sourceClassName, name, message, messageContext, stubableCode,
						stubableCodeThrowsException, matchingStubStrategies, checkpointType, index, level
				);
			}
			Integer newLevel = level + levelChangeNextCheckpoint;
			threadLevel.put(threadName, newLevel);
			if (newLevel.equals(threadFirstLevel.get(threadName))) {
				// threadCreatepoint has already been removed on first checkpoint for thread, hence use false
				// for removeThreadCreatepoint
				closeThread(threadName, false);
			}
		}
		return message;
	}

	@SneakyThrows
	private <T> T addCheckpoint(String threadName, String sourceClassName, String name, T message, Map<String, Object> messageContext,
								StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
								Set<String> matchingStubStrategies, int checkpointType, Integer index, Integer level) {
		Checkpoint checkpoint = new Checkpoint(this, threadName, sourceClassName, name, checkpointType, level);
		checkpoint.setMessageContext(messageContext);
		if (testTool.getOpenTelemetryTracer() != null) {
			SpanBuilder checkpointSpanBuilder = testTool.getOpenTelemetryTracer().spanBuilder("checkpoint - " + name);
			for (Checkpoint checkpointInList : checkpoints) {
				if (checkpointInList.getType() == 1 && checkpointInList.getLevel() == checkpoint.getLevel() - 1) {
					checkpointSpanBuilder.setParent(Context.current().with(checkpointInList.getSpan()));
				}
			}
			Span checkpointSpan = checkpointSpanBuilder.startSpan();
			checkpointSpan.setAttribute("checkpointType", checkpoint.getType());
			checkpointSpan.setAttribute("checkpointTypeAsString", checkpoint.getTypeAsString());
			checkpointSpan.setAttribute("checkpointLevel", checkpoint.getLevel());
			checkpoint.setSpan(checkpointSpan);
		}
		boolean stub = false;
		if (originalReport != null) {
			Checkpoint originalCheckpoint = originalReport.getCheckpoint(checkpoint, linkMethod, true);
			if (originalCheckpoint == null) {
				if (matchingStubStrategies != null) {
					if (matchingStubStrategies.contains(originalReport.getStubStrategy())) {
						stub = true;
					}
				} else {
					stub = testTool.stub(checkpoint, originalReport.getStubStrategy());
				}
				if (stub) {
					checkpoint.setStubNotFound("Could not find stub message with link method " + linkMethod);
				} else {
					checkpoint.setStubNotFound("Counterpart not found with link method " + linkMethod);
				}
			} else {
				checkpoint.setStub(originalCheckpoint.getStub());
				if (originalCheckpoint.getStub() == StubType.FOLLOW_REPORT_STRATEGY.toInt()) {
					if (matchingStubStrategies != null) {
						if (matchingStubStrategies.contains(originalReport.getStubStrategy())) {
							stub = true;
						}
					} else {
						stub = testTool.stub(originalCheckpoint, originalReport.getStubStrategy());
					}
				} else if (originalCheckpoint.getStub() == StubType.NO.toInt()) {
					stub = false;
				} else if (originalCheckpoint.getStub() == StubType.YES.toInt()) {
					stub = true;
				}
			}
			if (stub) {
				checkpoint.setStubbed(true);
				message = getMessageEncoder().toObject(originalCheckpoint, message);
				message = checkpoint.setMessage(message);
			}
		}
		if (!stub) {
			try {
				message = TestTool.execute(stubableCode, stubableCodeThrowsException, message);
			} catch (Throwable t) {
				testTool.abortpoint(correlationId, sourceClassName, name, t.getMessage());
				throw t;
			}
			message = checkpoint.setMessage(message);
		}
		if (index > checkpoints.size()) {
			// This code should not be necessary anymore (see testRemoveThreadCreatepoint() for the issue that has been
			// fixed). Keep the following code for now and remove it somewhere in the future
			String warning = "Ladybug adjustment of checkpoint index to prevent IndexOutOfBoundsException."
					+ " For unknown reason index is " + index + " while checkpoints size is " + checkpoints.size() + "."
					+ " Please create an issue at https://github.com/wearefrank/ladybug/issues/new\n"
					+ getThreadInfo();
			log.warn(warning);
			Checkpoint warningCheckpoint = new Checkpoint(this, threadName, this.getClass().getCanonicalName(),
					"WARNING", CheckpointType.INFOPOINT.toInt(), level
			);
			warningCheckpoint.setMessage(warning);
			threadCheckpointIndex.put(threadName, checkpoints.size());
			index = checkpoints.size() - 1;
			checkpoints.add(index, warningCheckpoint);
			index++;
		}
		// Add checkpoint to the list after stubable code has been executed. Otherwise when a report in progress is
		// opened it might give the impression that the stubable code is already executed
		checkpoints.add(index, checkpoint);
		for (int i = threads.indexOf(threadName); i < threads.size(); i++) {
			String key = threads.get(i);
			Integer value = threadCheckpointIndex.get(key);
			threadCheckpointIndex.put(key, value + 1);
		}
		estimatedMemoryUsage += checkpoint.getEstimatedMemoryUsage();
		if (log.isDebugEnabled()) {
			log.debug("Added checkpoint " + getCheckpointLogDescription(name, checkpointType, level));
		}
		if (testTool.getOpenTelemetryTracer() != null) {
			if (checkpointType != 1) {
				checkpoint.getSpan().end();
			}
			if (checkpointType == 2) {
				for (Checkpoint checkpointInList : checkpoints) {
					if (checkpointInList.getType() == 1 && checkpointInList.getLevel() == checkpoint.getLevel() - 1) {
						checkpointInList.getSpan().end();
					}
				}
			}
		}

		return message;
	}

	public String getThreadInfo() {
		return "\nmainThread: " + mainThread
				+ "\nmainThreadFinishedTime: " + mainThreadFinishedTime
				+ "\nthreads: " + threads
				+ "\nthreadCheckpointIndex: " + threadCheckpointIndex
				+ "\nthreadFirstLevel: " + threadFirstLevel
				+ "\nthreadLevel: " + threadLevel
				+ "\nthreadParent: " + threadParent
				+ "\nthreadsActiveCount: " + threadsActiveCount
				+ "\nstreamingMessageListeners: " + streamingMessageListeners
				+ "\ncloseThreads: " + testTool.isCloseThreads()
				+ "\ncloseNewThreadsOnly: " + testTool.isCloseNewThreadsOnly()
				+ "\ncloseMessageCapturers: " + testTool.isCloseMessageCapturers();
	}

	protected String truncateMessage(Checkpoint checkpoint, String message) {
		if (testTool.getMaxMessageLength() > -1 && message != null
				&& message.toString().length() > testTool.getMaxMessageLength()) {
			// For a message that is referenced by multiple checkpoints, have one truncated message that is
			// referenced by those checkpoints, to prevent creating multiple String objects representing the
			// same string and occupying unnecessary memory.
			checkpoint.setPreTruncatedMessageLength(message.length());
			if (truncatedMessageMap.containsKey(message)) {
				return truncatedMessageMap.get(message);
			} else {
				String truncatedMessage = message.substring(0, testTool.getMaxMessageLength());
				truncatedMessageMap.put(message, truncatedMessage);
				return truncatedMessage;
			}
		}
		return message;
	}

	public Checkpoint getOriginalEndpointOrAbortpointForCurrentLevel() {
		Checkpoint result = null;
		if (originalReport != null) {
			Checkpoint lastCheckpoint = (Checkpoint) checkpoints.get(checkpoints.size() - 1);
			Checkpoint checkpoint = originalReport.getCheckpoint(lastCheckpoint.getPath());
			if (checkpoint != null) {
				int i = originalReport.checkpoints.indexOf(checkpoint) + 1;
				while (checkpoint.getType() != CheckpointType.ENDPOINT.toInt()
						&& checkpoint.getType() != CheckpointType.ABORTPOINT.toInt()
						&& i < originalReport.checkpoints.size()) {
					checkpoint = (Checkpoint) originalReport.checkpoints.get(i);
					i++;
				}
				if (checkpoint.getType() == CheckpointType.ENDPOINT.toInt()
						|| checkpoint.getType() == CheckpointType.ABORTPOINT.toInt()) {
					result = checkpoint;
				}
			}
		}
		return result;
	}

	protected void closeThreads(boolean closeNewThreadsOnly) {
		if (closeNewThreadsOnly) {
			// Don't close new threads while other threads are active (and don't close other threads either when new
			// threads aren't closed)
			for (String threadName : threads) {
				int firstLevel = threadFirstLevel.get(threadName);
				int level = threadLevel.get(threadName);
				if (level > firstLevel) {
					return;
				}
			}
		}
		while (threads.size() > 0) {
			closeThread(threads.get(0), false);
		}
	}

	protected void closeThread(String threadName, boolean removeThreadCreatepoint) {
		Integer index = threadCheckpointIndex.get(threadName);
		if (index != null) {
			if (removeThreadCreatepoint) {
				removeThreadCreatepoint(index, threadName);
			}
			threads.remove(threadName);
			threadCheckpointIndex.remove(threadName);
			threadFirstLevel.remove(threadName);
			threadLevel.remove(threadName);
			threadParent.remove(threadName);
			threadsActiveCount--;
		} else {
			log.warn("Thread '" + threadName + "' to close for report with correlationId '" + correlationId + "' not found");
		}
	}

	protected boolean threadsFinished() {
		return threadsActiveCount < 1;
	}

	protected boolean mainThreadFinished() {
		return !threads.contains(mainThread);
	}

	protected boolean streamingMessageListenersFinished() {
		return streamingMessageListeners.size() < 1;
	}

	protected void addStreamingMessageListener(Object streamingMessage, Checkpoint checkpoint) {
		synchronized (streamingMessageListeners) {
			Set<Checkpoint> checkpoints = streamingMessageListeners.get(streamingMessage);
			if (checkpoints == null) {
				checkpoints = new HashSet<Checkpoint>();
			}
			checkpoints.add(checkpoint);
			streamingMessageListeners.put(streamingMessage, checkpoints);
		}
	}

	protected void removeStreamingMessageListener(Object streamingMessage, Checkpoint checkpoint) {
		synchronized (streamingMessageListeners) {
			Set<Checkpoint> checkpoints = streamingMessageListeners.remove(streamingMessage);
			if (checkpoints != null) {
				checkpoints.remove(checkpoint);
				if (checkpoints.size() > 0) {
					streamingMessageListeners.put(streamingMessage, checkpoints);
				}
			}
		}
	}

	protected void closeMessageCapturers() {
		for (Checkpoint checkpoint : checkpoints) {
			checkpoint.closeMessageCapturer();
		}
	}

	protected boolean isKnownStreamingMessage(Object streamingMessage) {
		synchronized (streamingMessageListeners) {
			return streamingMessageListeners.get(streamingMessage) != null;
		}
	}

	protected void closeStreamingMessage(String messageClassName, Object streamingMessage, String streamingType,
										 String charset, Object message, int preTruncatedMessageLength, Throwable exception) {
		StreamingMessageResult streamingMessageResult = new StreamingMessageResult();
		streamingMessageResult.setMessageClassName(messageClassName);
		streamingMessageResult.setStreamingType(streamingType);
		streamingMessageResult.setCharset(charset);
		streamingMessageResult.setMessage(message);
		streamingMessageResult.setPreTruncatedMessageLength(preTruncatedMessageLength);
		streamingMessageResult.setException(exception);
		synchronized (streamingMessageListeners) {
			// Check whether TestTool.close() already closed the message capturer
			if (streamingMessageListeners.containsKey(streamingMessage)) {
				streamingMessageResults.put(streamingMessage, streamingMessageResult);
			}
			closeStreamingMessageListeners();
		}
		getTestTool().closeReportIfFinished(this);
	}

	/**
	 * Close all streaming message listeners for which the streaming message has been closed
	 */
	private void closeStreamingMessageListeners() {
		synchronized (streamingMessageListeners) {
			Set<Object> finishedStreamingMessage = new HashSet<Object>();
			for (Object streamingMessage : streamingMessageListeners.keySet()) {
				StreamingMessageResult streamingMessageResult = streamingMessageResults.remove(streamingMessage);
				if (streamingMessageResult != null) {
					finishedStreamingMessage.add(streamingMessage);
					for (Checkpoint checkpoint : streamingMessageListeners.get(streamingMessage)) {
						checkpoint.setWaitingForStream(false);
						checkpoint.setStreaming(streamingMessageResult.getStreamingType());
						if (streamingMessageResult.getException() != null) {
							checkpoint.setMessage(streamingMessageResult.getException());
						} else {
							Object message = streamingMessageResult.getMessage();
							String charset = streamingMessageResult.getCharset();
							ToStringResult toStringResult = getMessageEncoder().toString(message, charset);
							checkpoint.setMessage(toStringResult.getString());
							checkpoint.setEncoding(toStringResult.getEncoding());
							checkpoint.setMessageClassName(streamingMessageResult.getMessageClassName());
							checkpoint.setPreTruncatedMessageLength(streamingMessageResult.getPreTruncatedMessageLength());
						}
					}
				}
			}
			for (Object streamingMessage : finishedStreamingMessage) {
				streamingMessageListeners.remove(streamingMessage);
			}
		}
	}

	public Checkpoint getCheckpoint(Path path) {
		Checkpoint result = null;
		Iterator<Checkpoint> iterator = checkpoints.iterator();
		while (result == null && iterator.hasNext()) {
			Checkpoint checkpoint = (Checkpoint) iterator.next();
			if (path.equals(checkpoint.getPath())) {
				result = checkpoint;
			}
		}
		return result;
	}

	public Checkpoint getCheckpoint(Checkpoint counterpartCheckpoint) {
		return getCheckpoint(counterpartCheckpoint, linkMethod, false);
	}

	public Checkpoint getCheckpoint(Checkpoint counterpartCheckpoint, String linkMethod) {
		return getCheckpoint(counterpartCheckpoint, linkMethod, false);
	}

	public Checkpoint getCheckpoint(Checkpoint counterpartCheckpoint, String linkMethod,
									boolean counterpartCheckpointInProgress) {
		// linkMethod is null when report has been created with a Ladybug version before link method was introduced
		if (LinkMethodType.PATH_AND_TYPE.toString().equals(linkMethod)) {
			Path path = counterpartCheckpoint.getPath(counterpartCheckpointInProgress);
			Checkpoint checkpoint = getCheckpoint(path);
			if (checkpoint != null && counterpartCheckpoint.getType() == checkpoint.getType()) {
				return checkpoint;
			}
		} else if (LinkMethodType.CHECKPOINT_NR.toString().equals(linkMethod)) {
			int i = counterpartCheckpoint.getIndex();
			if (i == -1 && counterpartCheckpointInProgress) {
				// Checkpoint constructed but not added to list of checkpoints yet
				i = counterpartCheckpoint.getReport().getCheckpoints().size();
			}
			return checkpoints.get(i);
		} else if (LinkMethodType.NTH_NAME_AND_TYPE.toString().equals(linkMethod)) {
			int counterpartCount = 0;
			for (Checkpoint checkpoint : counterpartCheckpoint.getReport().getCheckpoints()) {
				if (Objects.equals(counterpartCheckpoint.getName(), checkpoint.getName())
						&& counterpartCheckpoint.getType() == checkpoint.getType()) {
					counterpartCount++;
				}
			}
			if (counterpartCheckpointInProgress) {
				counterpartCount++;
			}
			int count = 0;
			for (Checkpoint checkpoint : checkpoints) {
				if (Objects.equals(counterpartCheckpoint.getName(), checkpoint.getName())
						&& counterpartCheckpoint.getType() == checkpoint.getType()) {
					count++;
				}
				if (counterpartCount == count) {
					return checkpoint;
				}
			}
		}
		return null;
	}

	public void setCheckpoints(List<Checkpoint> checkpoints) {
		this.checkpoints = checkpoints;
	}

	public List<Checkpoint> getCheckpoints() {
		return checkpoints;
	}

	public Checkpoint getInputCheckpoint() {
		return checkpoints.get(0);
	}

	public int getNumberOfCheckpoints() {
		return checkpoints.size();
	}

	public long getEstimatedMemoryUsage() {
		// Variable estimatedMemoryUsage is transient so it needs to be recalculated after the Report object has been
		// loaded from storage. In other situations it should not be recalculated because this method is being called
		// every time a checkpoint is added (to check whether the max memory usage has been exceeded) so recalculating
		// it every time would be bad for performance.
		if (estimatedMemoryUsage == 0L && mainThread == null) {
			for (Checkpoint checkpoint : checkpoints) {
				estimatedMemoryUsage += checkpoint.getEstimatedMemoryUsage();
			}
		}
		return estimatedMemoryUsage;
	}

	@Transient
	@JsonIgnore
	public void setDifferenceChecked(boolean differenceChecked) {
		this.differenceChecked = differenceChecked;
	}

	@Transient
	@JsonIgnore
	public boolean isDifferenceChecked() {
		return differenceChecked;
	}

	@Transient
	@JsonIgnore
	public void setDifferenceFound(boolean differenceFound) {
		this.differenceFound = differenceFound;
	}

	@Transient
	@JsonIgnore
	public boolean isDifferenceFound() {
		return differenceFound;
	}

	@Transient
	@JsonIgnore
	public MessageTransformer getMessageTransformer() {
		return testTool.getMessageTransformer();
	}

	@Transient
	@JsonIgnore
	public MessageEncoder getMessageEncoder() {
		return testTool.getMessageEncoder();
	}

	@Transient
	@JsonIgnore
	public MessageCapturer getMessageCapturer() {
		return testTool.getMessageCapturer();
	}

	@Override
	public Report clone() throws CloneNotSupportedException {
		Report report = new Report();
		report.setTestTool(testTool);
		report.setStartTime(startTime);
		report.setEndTime(endTime);
		report.setCorrelationId(correlationId);
		report.setName(name);
		report.setDescription(description);
		report.setPath(path);
		report.setStubStrategy(stubStrategy);
		report.setLinkMethod(linkMethod);
		report.setTransformation(transformation);
		if (getVariables() != null) {
			Map<String, String> variables = new LinkedHashMap<String, String>();
			variables.putAll(getVariables());
			report.setVariables(variables);
		}
		List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
		for (Checkpoint checkpoint : this.checkpoints) {
			checkpoint = checkpoint.clone();
			checkpoint.setReport(report);
			checkpoints.add(checkpoint);
		}
		report.setCheckpoints(checkpoints);
		return report;
	}

	@Override
	public String toString() {
		return name;
	}

	public String toXml() {
		return toXml(null);
	}

	public String toXml(ReportRunner reportRunner) {
		if (xml == null) {
			StringBuilder builder = new StringBuilder();
			builder.append("<Report");
			builder.append(" Name=\"" + EscapeUtil.escapeXml(name) + "\"");
			builder.append(" Description=\"" + EscapeUtil.escapeXml(description) + "\"");
			builder.append(" Path=\"" + EscapeUtil.escapeXml(path) + "\"");
			builder.append(" CorrelationId=\"" + EscapeUtil.escapeXml(correlationId) + "\"");
			builder.append(" StartTime=\"" + startTime + "\"");
			builder.append(" EndTime=\"" + endTime + "\"");
			builder.append(" NumberOfCheckpoints=\"" + getNumberOfCheckpoints() + "\"");
			builder.append(" EstimatedMemoryUsage=\"" + getEstimatedMemoryUsage() + "\"");
			builder.append(">");
			for (Checkpoint checkpoint : checkpoints) {
				String message;
				if (reportRunner != null && checkpoint.containsVariables()) {
					message = checkpoint.getMessageWithResolvedVariables(reportRunner);
				} else {
					message = checkpoint.getMessage();
				}
				builder.append("<Checkpoint");
				builder.append(" Name=\"" + EscapeUtil.escapeXml(checkpoint.getName()) + "\"");
				builder.append(" Type=\"" + EscapeUtil.escapeXml(checkpoint.getTypeAsString()) + "\"");
				builder.append(" Level=\"" + checkpoint.getLevel() + "\"");
				if (checkpoint.getSourceClassName() != null) {
					builder.append(" SourceClassName=\"" + EscapeUtil.escapeXml(checkpoint.getSourceClassName()) + "\"");
				}
				if (checkpoint.getMessageClassName() != null) {
					builder.append(" MessageClassName=\"" + EscapeUtil.escapeXml(checkpoint.getMessageClassName()) + "\"");
				}
				if (checkpoint.getPreTruncatedMessageLength() != -1) {
					builder.append(" PreTruncatedMessageLength=\"" + checkpoint.getPreTruncatedMessageLength() + "\"");
				}
				if (checkpoint.getEncoding() != null) {
					builder.append(" Encoding=\"" + EscapeUtil.escapeXml(checkpoint.getEncoding()) + "\"");
				}
				if (checkpoint.getStreaming() != null) {
					builder.append(" Streaming=\"" + EscapeUtil.escapeXml(checkpoint.getStreaming()) + "\"");
				}
				if (checkpoint.isWaitingForStream()) {
					builder.append(" WaitingForStream=\"" + checkpoint.isWaitingForStream() + "\"");
				}
				if (checkpoint.getStub() != StubType.FOLLOW_REPORT_STRATEGY.toInt()) {
					builder.append(" Stub=\"" + checkpoint.getStub() + "\"");
				}
				if (checkpoint.isStubbed()) {
					builder.append(" Stubbed=\"" + checkpoint.isStubbed() + "\"");
				}
				if (checkpoint.getStubNotFound() != null) {
					builder.append(" StubNotFound=\"" + checkpoint.getStubNotFound() + "\"");
				}
				if (message == null) {
					builder.append(" Null=\"true\"/>");
				} else {
					if (XmlUtil.isXml(message)) {
						String textDecl = null;
						if (message.startsWith("<?")) {
							int i = message.indexOf("?>") + 2;
							textDecl = message.substring(0, i);
							builder.append(" TextDecl=\"");
							builder.append(EscapeUtil.escapeXml(textDecl));
							builder.append("\">");
							message = message.substring(i);
						} else {
							builder.append(">");
						}
						builder.append(message);
					} else {
						builder.append(">");
						builder.append(EscapeUtil.escapeXml(message));
					}
					builder.append("</Checkpoint>");
				}
			}
			builder.append("</Report>");
			xml = builder.toString();
			if (reportXmlTransformer != null || (transformation != null && transformation.trim().length() > 0)) {
				if (reportXmlTransformer == null) {
					reportXmlTransformer = new ReportXmlTransformer();
					reportXmlTransformer.setXslt(transformation);
				}
				xml = reportXmlTransformer.transform(xml);
			} else if (globalReportXmlTransformer != null) {
				xml = globalReportXmlTransformer.transform(xml);
			}
		}
		return xml;
	}

	public void flushCachedXml() {
		reportXmlTransformer = null;
		xml = null;
	}

	private String getCheckpointLogDescription(String name, int type, Integer level) {
		return getCheckpointLogDescription(name, type, level, correlationId);
	}

	protected static String getCheckpointLogDescription(String name, int type, Integer level, String correlationId) {
		return "(name: " + name + ", type: " + CheckpointType.toString(type) + ", level: " + level
				+ ", correlationId: " + correlationId + ")";
	}

	private String getOtherCheckpointsLogDescription() {
		return "(next checkpoints for this report will be ignored without any logging)";
	}

	public Map<String, String> getVariables() {
		return variables;
	}

	public void setVariables(Map<String, String> variables) {
		this.variables = variables;
	}

	/**
	 * Fill variables map with the header of the csv as keys and the first line of values from the csv as values. The
	 * GUI will prevent the user from adding more then one line of values to the csv.
	 */
	@Transient
	@JsonIgnore
	public String setVariablesCsv(String variablesCsv) {
		if (StringUtils.isEmpty(variablesCsv)) {
			variables = null;
			return null;
		}
		String errorMessage = CsvUtil.validateCsv(variablesCsv, ";", 2);
		if (errorMessage != null) {
			return errorMessage;
		}
		variables = new LinkedHashMap<String, String>();
		Scanner scanner = new Scanner(variablesCsv);
		List<String> lines = new ArrayList<String>();
		while (scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();
			if (StringUtils.isNotEmpty(nextLine) && !nextLine.startsWith("#")) {
				lines.add(nextLine);
			}
		}
		scanner.close();
		// For each column name from the header of the csv
		List<String> columns = Arrays.asList(lines.get(0).split(";"));
		for (String key : columns) {
			// Read the value from the first line of values
			String value = lines.get(1).split(";")[columns.indexOf(key)];
			variables.put(key, value);
		}
		return null;
	}

	@Transient
	@JsonIgnore
	public String getVariablesCsv() {
		if (variables == null) {
			return "";
		}
		String csv = "";
		for (String key : variables.keySet()) {
			csv = csv + key + ";";
		}
		csv = csv.substring(0, csv.length() - 1);
		csv = csv + "\n";
		for (String key : variables.keySet()) {
			csv = csv + variables.get(key) + ";";
		}
		csv = csv.substring(0, csv.length() - 1);
		return csv;
	}
}

/**
 * A custom implementation of the map interface that compares keys based on their object reference, i.e. comparing with 'o1 == o2' rather than 'o1.equals(o2)'.
 * This greatly enhances the performance of maps with large objects as keys.
 * <p>
 * Note: Since this implementation was written with a specific use case in mind, most methods are not implemented and will throw an exception when called.
 */
class RefCompareMap<K, V> implements Map<K, V> {

	private List<K> keys = new ArrayList<K>();
	private List<V> values = new ArrayList<V>();

	@Override
	public V get(Object key) {
		int i = 0;
		for (Object o : keys) {
			if (o == key) {
				return values.get(i);
			}
			i++;
		}
		return null;
	}

	@Override
	public V put(K key, V value) {
		keys.add(key);
		values.add(value);

		return value;
	}

	@Override
	public boolean containsKey(Object key) {
		for (Object o : keys) {
			if (o == key) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void clear() {
		throw new NotImplementedException();
	}

	@Override
	public boolean containsValue(Object value) {
		throw new NotImplementedException();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isEmpty() {
		throw new NotImplementedException();
	}

	@Override
	public Set<K> keySet() {
		throw new NotImplementedException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new NotImplementedException();
	}

	@Override
	public V remove(Object key) {
		throw new NotImplementedException();
	}

	@Override
	public int size() {
		throw new NotImplementedException();
	}

	@Override
	public Collection<V> values() {
		throw new NotImplementedException();
	}
}

@Data
class StreamingMessageResult {
	String messageClassName;
	String streamingType;
	String charset;
	Object message;
	int preTruncatedMessageLength;
	Throwable exception;
}
