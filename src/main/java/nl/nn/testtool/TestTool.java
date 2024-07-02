/*
   Copyright 2019-2024 WeAreFrank!, 2018 Nationale-Nederlanden

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
package nl.nn.testtool;

import java.lang.invoke.MethodHandles;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.filter.Views;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.transform.MessageTransformer;

/**
 * @author Jaco de Groot
 */
@ApplicationScoped
public class TestTool {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static Logger securityLog;
	private String configName;
	private String configVersion;
	private int maxCheckpoints = 2500;
	private int maxMessageLength = 10000000;
	private long maxMemoryUsage = 100000000L;
	private Debugger debugger;
	private Rerunner rerunner;
	private boolean reportGeneratorEnabled = true;
	private boolean defaultReportGeneratorEnabled = true;
	private List<Report> reportsInProgress = new ArrayList<Report>();
	private Map<String, Report> reportsInProgressByCorrelationId = new HashMap<String, Report>();
	private long numberOfReportsInProgress = 0;
	private Map<String, Report> originalReports = new HashMap<String, Report>();
	private @Setter @Getter @Inject @Autowired LogStorage debugStorage;
	private @Setter @Getter @Inject @Autowired CrudStorage testStorage;
	private MessageEncoder messageEncoder = new MessageEncoderImpl();
	private MessageCapturer messageCapturer = new MessageCapturerImpl();
	private MessageTransformer messageTransformer;
	private String regexFilter;
	private String defaultRegexFilter;
	public static final String STUB_STRATEGY_STUB_ALL_EXTERNAL_CONNECTION_CODE = "Stub all external connection code";
	public static final String STUB_STRATEGY_NEVER = "Never";
	public static final String STUB_STRATEGY_ALWAYS = "Always";
	private String defaultStubStrategy = STUB_STRATEGY_STUB_ALL_EXTERNAL_CONNECTION_CODE;
	private List<String> stubStrategies = new ArrayList<String>();
		{
			stubStrategies.add(STUB_STRATEGY_STUB_ALL_EXTERNAL_CONNECTION_CODE);
			stubStrategies.add(STUB_STRATEGY_NEVER);
			stubStrategies.add(STUB_STRATEGY_ALWAYS);
		}
	private Set<String> matchingStubStrategiesForExternalConnectionCode = new HashSet<>();
		{
			matchingStubStrategiesForExternalConnectionCode.add(STUB_STRATEGY_STUB_ALL_EXTERNAL_CONNECTION_CODE);
			matchingStubStrategiesForExternalConnectionCode.add(STUB_STRATEGY_ALWAYS);
		}
	public static final String DEFAULT_STUB_MESSAGE =
			"Default Ladybug stub message (counterpart checkpoint in original report not found?)";
	private @Getter boolean closeThreads = false;
	private @Getter boolean closeNewThreadsOnly = false;
	private @Getter boolean closeMessageCapturers = false;
	private @Setter @Getter @Inject @Autowired Views views;
	private @Setter @Getter int reportsInProgressThreshold = 300000;
	boolean devMode = false; // See testConcurrentLastEndpointAndFirstStartpointForSameCorrelationId()

	@PostConstruct
	public void init() {
		defaultRegexFilter = regexFilter;
		defaultReportGeneratorEnabled = reportGeneratorEnabled;
	}

	public void reset() {
		regexFilter = defaultRegexFilter;
		reportGeneratorEnabled = defaultReportGeneratorEnabled;
	}

	public void setSecurityLoggerName(String securityLoggerName) {
		securityLog = LoggerFactory.getLogger(securityLoggerName);
	}

	public Logger getSecurityLog() {
		return securityLog;
	}

	public void setConfigName(String configName) {
		this.configName = configName;
	}

	public String getConfigName() {
		return configName;
	}

	public void setConfigVersion(String configVersion) {
		this.configVersion = configVersion;
	}

	public String getConfigVersion() {
		return configVersion;
	}

	public void setMaxCheckpoints(int maxCheckpoints) {
		this.maxCheckpoints = maxCheckpoints;
	}

	public int getMaxCheckpoints() {
		return maxCheckpoints;
	}

	public void setMaxMessageLength(int maxMessageLength) {
		this.maxMessageLength = maxMessageLength;
	}

	public int getMaxMessageLength() {
		return maxMessageLength;
	}
	
	public void setMaxMemoryUsage(long maxMemoryUsage) {
		this.maxMemoryUsage = maxMemoryUsage;
	}
	
	public long getMaxMemoryUsage() {
		return maxMemoryUsage;
	}

	public void setDebugger(Debugger debugger) {
		this.debugger = debugger;
	}
	
	public Debugger getDebugger() {
		return debugger;
	}

	public void setRerunner(Rerunner rerunner) {
		this.rerunner = rerunner;
	}
	
	public Rerunner getRerunner() {
		return rerunner;
	}

	public void setReportGeneratorEnabled(boolean reportGeneratorEnabled) {
		this.reportGeneratorEnabled = reportGeneratorEnabled;
	}
	
	public boolean isReportGeneratorEnabled() {
		return reportGeneratorEnabled;
	}
	
	/**
	 * Sends the result of <code>isReportGeneratorEnabled()</code> to the Debugger
	 * implementation of the application using the Ladybug.
	 */
	public void sendReportGeneratorStatusUpdate() {
		if (debugger != null) {
			debugger.updateReportGeneratorStatus(isReportGeneratorEnabled());
		}
	}

	public void setMessageEncoder(MessageEncoder messageEncoder) {
		this.messageEncoder = messageEncoder;
	}

	public MessageEncoder getMessageEncoder() {
		return messageEncoder;
	}

	public void setMessageTransformer(MessageTransformer messageTransformer) {
		this.messageTransformer = messageTransformer;
	}

	public MessageTransformer getMessageTransformer() {
		return messageTransformer;
	}

	public void setMessageCapturer(MessageCapturer messageCapturer) {
		this.messageCapturer = messageCapturer;
	}

	public MessageCapturer getMessageCapturer() {
		return messageCapturer;
	}

	public void setRegexFilter(String regexFilter) {
		this.regexFilter = regexFilter;
	}

	public String getRegexFilter() {
		return regexFilter;
	}

	public void setDefaultStubStrategy(String defaultStubStrategy) {
		this.defaultStubStrategy = defaultStubStrategy;
	}

	public String getDefaultStubStrategy() {
		if (debugger == null) {
			return defaultStubStrategy;
		} else {
			return debugger.getDefaultStubStrategy();
		}
	}

	public void setStubStrategies(List<String> stubStrategies) {
		this.stubStrategies = stubStrategies;
	}

	public List<String> getStubStrategies() {
		if (debugger == null) {
			return stubStrategies;
		} else {
			return debugger.getStubStrategies();
		}
	}

	public void setMatchingStubStrategiesForExternalConnectionCode(Set<String> matchingStubStrategiesForExternalConnectionCode) {
		this.matchingStubStrategiesForExternalConnectionCode = matchingStubStrategiesForExternalConnectionCode;
	}

	public Set<String> getMatchingStubStrategiesForExternalConnectionCode() {
		return matchingStubStrategiesForExternalConnectionCode;
	}

	/**
	 * Close child threads when main thread is finished (top level endpoint has been called) to prevent threads from
	 * keeping reports in progress in case they call checkpoints that aren't properly surrounded with a try/catch, see
	 * {@link TestTool#close(String)}. Setting this to true will risk checkpoints not being added for child threads that
	 * are still running after the main thread is finished. The use of {@link CloseReportsTask} can lower this risk
	 * 
	 * @see CloseReportsTask
	 * @see TestTool#close(String)
	 * @param closeThreads ...
	 */
	public void setCloseThreads(boolean closeThreads) {
		this.closeThreads = closeThreads;
	}

	/**
	 * Only close threads when all of them haven't started and aren't cancelled {@link TestTool#close(String, String)}).
	 * This way threads can continue to add checkpoints to the report after the main thread has finished until all
	 * threads are finished while threads that will not start and aren't cancelld will not keep the report in progress.
	 * See {@link TestTool#close(String)} on how to properly surrounded checkpoint with a try/catch to prevent threads
	 * from keeping reports in progress.
	 * 
	 * @param closeNewThreadsOnly ...
	 */
	public void setCloseNewThreadsOnly(boolean closeNewThreadsOnly) {
		this.closeNewThreadsOnly = closeNewThreadsOnly;
	}

	/**
	 * Close message capturers when main thread is finished (top level endpoint has been called) to prevent streams for
	 * which the close method isn't called to keep reports in progress. Setting this to true will risk streams not
	 * being captured when they are still active after the main thread is finished. The use of {@link CloseReportsTask}
	 * can lower this risk
	 * 
	 * @see CloseReportsTask
	 * @see TestTool#close(String)
	 * @param closeMessageCapturers ...
	 */
	public void setCloseMessageCapturers(boolean closeMessageCapturers) {
		this.closeMessageCapturers = closeMessageCapturers;
	}

	private <T> T checkpoint(String correlationId, String childThreadId, String sourceClassName, String name,
			T message, StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
			Set<String> matchingStubStrategies, int checkpointType, int levelChangeNextCheckpoint) {
		boolean executeStubableCode = true;
		if (reportGeneratorEnabled) {
			Report report;
			// Blocking for all threads for all reports
			synchronized(reportsInProgress) {
				report = getReportInProgress(correlationId);
				if (report == null) {
					report = createReport(correlationId, name, checkpointType);
				}
			}
			if (devMode) randomSleep();
			while (report != null) {
				// "synchronized(report)" is only blocking for threads writing to the same report (which is only the
				// case when multiple threads use the same correlationId)
				synchronized(report) {
					// "synchronized(report)" is used instead of "synchronized(reportsInProgress)" and separate from the
					// "synchronized(reportsInProgress)" in getReportInProgress() to prevent threads from being blocked
					// as much as possible. But in the very rare/unusual case that one thread calls the last endpoint of
					// a report (which will close the report) and another thread in parallel calls a startpoint for the
					// same correlationId this last thread can receive the report object from getReportInProgress() and
					// start waiting for a lock on the report object while the first thread is executing
					// report.checkpoint() below and closing the report. Hence double check that the report isn't
					// closed.
					if (report.isClosed()) {
						synchronized(reportsInProgress) {
							report = getReportInProgress(correlationId);
							if (report == null) {
								report = createReport(correlationId, name, checkpointType);
							}
						}
						// Synchronize and check isClosed() on report again as it will now point to a different report
						continue;
					}
					executeStubableCode = false;
					message = report.checkpoint(childThreadId, sourceClassName, name, message, stubableCode,
							stubableCodeThrowsException, matchingStubStrategies, checkpointType,
							levelChangeNextCheckpoint);
					closeReportIfFinished(report);
				}
				report = null;
			}
		}
		if (executeStubableCode) {
			message = execute(stubableCode, stubableCodeThrowsException, message);
		}
		return message;
	}

	private Report createReport(String correlationId, String name, int checkpointType) {
		Report report = null;
		if (checkpointType == Checkpoint.TYPE_STARTPOINT) {
			log.debug("Create new report for '" + correlationId + "'");
			report = new Report();
			report.setStartTime(System.currentTimeMillis());
			report.setTestTool(this);
			report.setCorrelationId(correlationId);
			report.setName(name);
			if (StringUtils.isNotEmpty(regexFilter)) {
				String nameToMatch = name;
				if (nameToMatch == null) {
					nameToMatch = ""; // Same behavior as SearchUtil.matches()
				}
				if (!nameToMatch.matches(regexFilter)) {
					report.setReportFilterMatching(false);
				}
			}
			Report originalReport;
			synchronized(originalReports) {
				originalReport = (Report)originalReports.remove(correlationId);
			}
			if (originalReport == null) {
				report.setStubStrategy(getDefaultStubStrategy());
			} else {
				report.setStubStrategy(originalReport.getStubStrategy());
				report.setOriginalReport(originalReport);
			}
			report.init();
			reportsInProgress.add(0, report);
			reportsInProgressByCorrelationId.put(correlationId, report);
			numberOfReportsInProgress++;
		} else {
			log.warn("No report in progress for correlationId and checkpoint not a startpoint, ignored checkpoint "
					+ Report.getCheckpointLogDescription(name, checkpointType, null, correlationId));
		}
		return report;
	}

	@SuppressWarnings("unchecked")
	@SneakyThrows
	protected static <T> T execute(StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
			T message) {
		if (stubableCode != null) {
			message = (T)stubableCode.execute();
		}
		if (stubableCodeThrowsException != null) {
			message = (T)stubableCodeThrowsException.execute();
		}
		return message;
	}

	protected void closeReportIfFinished(Report report) {
		synchronized(report) {
			if (!report.isClosed()) {
				if (report.mainThreadFinished()) {
					if (!report.threadsFinished() && closeThreads) {
						report.closeThreads(closeNewThreadsOnly);
					}
					if (report.getMainThreadFinishedTime() == Report.TIME_NOT_SET_VALUE) {
						report.setMainThreadFinishedTime(System.currentTimeMillis());
					}
				}
				if (report.threadsFinished()) {
					if (report.getEndTime() == Report.TIME_NOT_SET_VALUE) {
						report.setEndTime(System.currentTimeMillis());
					}
					if (!report.streamingMessageListenersFinished()
							&& closeMessageCapturers) {
						report.closeMessageCapturers();
					}
					if (!report.isClosed() && report.streamingMessageListenersFinished()) {
						report.setClosed(true);
						log.debug("Report is finished for '" + report.getCorrelationId() + "'");
						synchronized(reportsInProgress) {
							reportsInProgress.remove(report);
							reportsInProgressByCorrelationId.remove(report.getCorrelationId());
							numberOfReportsInProgress--;
						}
						if (report.isReportFilterMatching()) {
							debugStorage.storeWithoutException(report);
						}
					}
				}
			}
		}
	}

	private void randomSleep() {
		try {
			Thread.sleep(ThreadLocalRandom.current().nextLong(0, 10));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean warnReportsInProgress() {
		synchronized(reportsInProgress) {
			for (Report report : reportsInProgress) {
				if (!messageCapturerWaitingForClose(report)
						&& report.getStartTime() + (5 * 60 * 1000) < System.currentTimeMillis()) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean warnMessageCapturerWaitingForClose() {
		synchronized(reportsInProgress) {
			for (Report report : reportsInProgress) {
				if (messageCapturerWaitingForClose(report)
						&& report.getEndTime() + (30 * 1000) < System.currentTimeMillis()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean messageCapturerWaitingForClose(Report report) {
		synchronized(reportsInProgress) {
			return report.threadsFinished() && !report.streamingMessageListenersFinished();
		}
	}

	public <T> T startpoint(String correlationId, String sourceClassName, String name, T message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_STARTPOINT, 1);
	}

	public <T> T startpoint(String correlationId, String sourceClassName, String name, T message,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, matchingStubStrategies,
				Checkpoint.TYPE_STARTPOINT, 1);
	}

	public <T> T startpoint(String correlationId, String sourceClassName, String name, StubableCode stubableCode,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, null, stubableCode, null, matchingStubStrategies,
				Checkpoint.TYPE_STARTPOINT, 1);
	}

	/**
	 * Parameter throwsException determines the type of exception thrown. E.g. when set to (IOException)null the
	 * compiler will report this method to throw an IOException which needs to be handled. When set to null the compiler
	 * will not report an exception being thrown by this method.
	 * 
	 * @param <E> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param stubableCodeThrowsException ...
	 * @param matchingStubStrategies ...
	 * @param throwsException ...
	 * @return ...
	 * @throws E ...
	 */
	public <E extends Exception> Object startpoint(String correlationId, String sourceClassName, String name,
			StubableCodeThrowsException stubableCodeThrowsException, Set<String> matchingStubStrategies,
			E throwsException) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, stubableCodeThrowsException,
				matchingStubStrategies, Checkpoint.TYPE_STARTPOINT, 1);
	}

	public <T> T endpoint(String correlationId, String sourceClassName, String name, T message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_ENDPOINT, -1);
	}

	public <T> T endpoint(String correlationId, String sourceClassName, String name, T message,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, matchingStubStrategies,
				Checkpoint.TYPE_ENDPOINT, -1);
	}

	public <T> T endpoint(String correlationId, String sourceClassName, String name,
			StubableCode stubableCode, Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, null, stubableCode, null, matchingStubStrategies,
				Checkpoint.TYPE_ENDPOINT, -1);
	}

	/**
	 * See description of parameter throwsException at startpoint method.
	 * 
	 * @param <T> ...
	 * @param <E> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param stubableCodeThrowsException ...
	 * @param matchingStubStrategies ...
	 * @param throwsException ...
	 * @return ...
	 * @throws E ...
	 */
	public <T, E extends Exception> T endpoint(String correlationId, String sourceClassName, String name,
			StubableCodeThrowsException stubableCodeThrowsException, Set<String> matchingStubStrategies,
			E throwsException) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, stubableCodeThrowsException,
				matchingStubStrategies, Checkpoint.TYPE_ENDPOINT, -1);
	}

	/**
	 * Convenient method for the most common use case for stubable code, stubbing all external connections related code.
	 * The default stubbing strategy (Stub all external connection code) will be used as matching stub strategies.
	 * 
	 * @param <T> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param externalConnectionCode ...
	 * @return ...
	 */
	public <T> T endpoint(String correlationId, String sourceClassName, String name,
			ExternalConnectionCode externalConnectionCode) {
		return checkpoint(correlationId, null, sourceClassName, name, null, externalConnectionCode, null,
				matchingStubStrategiesForExternalConnectionCode, Checkpoint.TYPE_ENDPOINT, -1);
	}

	/**
	 * Convenient method for the most common use case for stubable code that throws exception, stubbing all external
	 * connections related code.
	 * The default stubbing strategy (Stub all external connection code) will be used as matching stub strategies.
	 * See description of parameter throwsException at startpoint method.
	 * 
	 * @param <T> ...
	 * @param <E> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param externalConnectionCodeThrowsException ...
	 * @param throwsException ...
	 * @return ...
	 * @throws E ...
	 */
	public <T, E extends Exception> T endpoint(String correlationId, String sourceClassName, String name,
			ExternalConnectionCodeThrowsException externalConnectionCodeThrowsException, E throwsException) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, externalConnectionCodeThrowsException,
				matchingStubStrategiesForExternalConnectionCode, Checkpoint.TYPE_ENDPOINT, -1);
	}

	public <T> T inputpoint(String correlationId, String sourceClassName, String name, T message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_INPUTPOINT, 0);
	}

	public <T> T inputpoint(String correlationId, String sourceClassName, String name, T message,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, matchingStubStrategies,
				Checkpoint.TYPE_INPUTPOINT, 0);
	}

	public <T> T inputpoint(String correlationId, String sourceClassName, String name,
			StubableCode stubableCode, Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, null, stubableCode, null, matchingStubStrategies,
				Checkpoint.TYPE_INPUTPOINT, 0);
	}

	/**
	 * See description of parameter throwsException at startpoint method.
	 * 
	 * @param <T> ...
	 * @param <E> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param stubableCodeThrowsException ...
	 * @param matchingStubStrategies ...
	 * @param throwsException ...
	 * @return ...
	 * @throws E ...
	 */
	public <T, E extends Exception> T inputpoint(String correlationId, String sourceClassName,
			String name, StubableCodeThrowsException stubableCodeThrowsException, Set<String> matchingStubStrategies,
			E throwsException) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, stubableCodeThrowsException,
				matchingStubStrategies, Checkpoint.TYPE_INPUTPOINT, 0);
	}

	public <T> T outputpoint(String correlationId, String sourceClassName, String name, T message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	public <T> T outputpoint(String correlationId, String sourceClassName, String name, T message,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, matchingStubStrategies,
				Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	public <T> T outputpoint(String correlationId, String sourceClassName, String name,
			StubableCode stubableCode, Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, null, stubableCode, null, matchingStubStrategies,
				Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	/**
	 * See description of parameter throwsException at startpoint method.
	 * 
	 * @param <T> ...
	 * @param <E> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param stubableCodeThrowsException ...
	 * @param matchingStubStrategies ...
	 * @param throwsException ...
	 * @return ...
	 * @throws E ...
	 */
	public <T, E extends Exception> T outputpoint(String correlationId, String sourceClassName,
			String name, StubableCodeThrowsException stubableCodeThrowsException, Set<String> matchingStubStrategies,
			E throwsException) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, stubableCodeThrowsException,
				matchingStubStrategies, Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	/**
	 * Convenient method for the most common use case for stubable code, stubbing all external connections related code.
	 * The default stubbing strategy (Stub all external connection code) will be used as matching stub strategies.
	 * 
	 * @param <T> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param externalConnectionCode ...
	 * @return ...
	 */
	public <T> T outputpoint(String correlationId, String sourceClassName, String name,
			ExternalConnectionCode externalConnectionCode) {
		return checkpoint(correlationId, null, sourceClassName, name, null, externalConnectionCode, null,
				matchingStubStrategiesForExternalConnectionCode, Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	/**
	 * Convenient method for the most common use case for stubable code that throws exception, stubbing all external
	 * connections related code.
	 * The default stubbing strategy (Stub all external connection code) will be used as matching stub strategies.
	 * See description of parameter throwsException at startpoint method.
	 * 
	 * @param <T> ...
	 * @param <E> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param externalConnectionCodeThrowsException ...
	 * @param throwsException ...
	 * @return ...
	 * @throws E ...
	 */
	public <T, E extends Exception> T outputpoint(String correlationId, String sourceClassName,
			String name, ExternalConnectionCodeThrowsException externalConnectionCodeThrowsException, E throwsException
			) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, externalConnectionCodeThrowsException,
				matchingStubStrategiesForExternalConnectionCode, Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	public <T> T infopoint(String correlationId, String sourceClassName, String name, T message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_INFOPOINT, 0);
	}

	/**
	 * Use abortpoint instead of endpoint in case an exception is thrown after a startpoint. The exception object can
	 * be passed as the message parameter.
	 * 
	 * @param <T> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param message ...
	 * @return ...
	 */
	public <T> T abortpoint(String correlationId, String sourceClassName, String name, T message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_ABORTPOINT, -1);
	}

	/**
	 * Set a marker in the report for a child thread to appear. This method
	 * should be called by the parent thread. Specify a childThreadId that will also
	 * be used by the child thread when calling threadStartpoint. The name of the
	 * child thread can be used as childThreadId (when unique and known at this point).
	 * 
	 * @param correlationId ...
	 * @param childThreadId ...
	 */
	public void threadCreatepoint(String correlationId, String childThreadId) {
		checkpoint(correlationId, childThreadId, null, null, null, null, null, null,
				Checkpoint.TYPE_THREADCREATEPOINT, 0);
	}

	/**
	 * Startpoint for a child thread. Specify a childThreadId that was also used when
	 * calling threadCreatepoint.
	 * 
	 * @param <T> ...
	 * @param correlationId ...
	 * @param childThreadId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param message ...
	 * @return ...
	 */
	public <T> T threadStartpoint(String correlationId, String childThreadId, String sourceClassName,
			String name, T message) {
		return checkpoint(correlationId, childThreadId, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_THREADSTARTPOINT, 1);
	}

	/**
	 * Startpoint for a child thread. This method can be used when the name of
	 * the child thread was used as childThreadId when calling threadCreatepoint.
	 * 
	 * @param <T> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param message ...
	 * @return ...
	 */
	public <T> T threadStartpoint(String correlationId, String sourceClassName, String name, T message) {
		return threadStartpoint(correlationId, Thread.currentThread().getName(), sourceClassName, name, message);
	}

	public <T> T threadEndpoint(String correlationId, String sourceClassName, String name, T message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_THREADENDPOINT, -1);
	}

	/**
	 * Mark all threads and message capturers as finished and close the report, hence write the report to storage. When
	 * all necessary checkpoints are properly surrounded with a try/catch that calls an abortpoint and all threads are
	 * properly surrounded with a try/catch it should not be necessary to call this method. On the other hand, to be on
	 * the safe side call this method in the finally of the root startpoint of the report to prevent reports from
	 * staying in progress and consuming memory. Hence for the top level startpoint use:
	 * 
	 * <code>
	 * try {
	 *     startpoint();
	 *     inputpoint();
	 *     infopoint();
	 *     outputpoint();
	 *     endpoint();
	 * } catch(Throwable t) {
	 *     abortpoint();
	 *     throw t;
	 * } finally {
	 *     close();
	 * }
	 *</code>
	 *
	 * Don't use this when threads and/or {@link MessageCapturer}s can continue to live after the main thread has
	 * finished (after it called the top level endpoint) and you want to wait for them to finish. In this case make sure
	 * that all necessary checkpoints are properly surrounded with a try/catch that calls an abortpoint and all threads
	 * are properly ended and all streams properly closed or use {@link CloseReportsTask}
	 * 
	 * @see CloseReportsTask
	 * @see TestTool#setCloseThreads(boolean)
	 * @see TestTool#setCloseMessageCapturers(boolean)
	 * @param correlationId ...
	 */
	public void close(String correlationId) {
		close(correlationId, true, true);
	}

	/**
	 * @see TestTool#close(String)
	 * 
	 * @param correlationId ...
	 * @param closeThreads ...
	 * @param closeMessageCapturers ...
	 */
	public void close(String correlationId, boolean closeThreads, boolean closeMessageCapturers) {
		close(null, correlationId, closeThreads, closeMessageCapturers);
	}

	private void close(Report report, String correlationId, boolean closeThreads, boolean closeMessageCapturers) {
		if (closeThreads) {
			close(correlationId, null);
		}
		if (closeMessageCapturers) {
			if (report == null) {
				synchronized(reportsInProgress) {
					report = (Report)reportsInProgressByCorrelationId.get(correlationId);
				}
			}
			if (report != null) {
				synchronized(report) {
					if (!report.isClosed()) {
						report.closeMessageCapturers();
						closeReportIfFinished(report);
					}
				}
			}
		}
	}

	/**
	 * Mark a thread as finished. When a threadCreatepoint is called but it is not certain whether this thread will
	 * execute this method can be used to mark this thread as finished / cancel it when it is certain that this thread
	 * will not start.
	 *
	 * @see #close(String)
	 * @param correlationId ...
	 * @param threadName    child thread id or name of the thread to close or null to close all threads (name of a
	 *                      thread cannot be null as Thread.setName(null) will result in:
	 *                      java.lang.NullPointerException: name cannot be null).
	 *                      When closing all threads the threadCreatepoints are left behind for the user to see where
	 *                      threads were supposed to start. This will warn the user that the thread didn't start and
	 *                      that the thread wasn't cancelled either (by explicitly calling this method with the specific
	 *                      thread name)
	 */
	public void close(String correlationId, String threadName) {
		Report report;
		synchronized(reportsInProgress) {
			report = (Report)reportsInProgressByCorrelationId.get(correlationId);
		}
		if (report != null) {
			synchronized(report) {
				if (threadName == null) {
					report.closeThreads(false);
				} else {
					report.closeThread(threadName, true);
				}
				closeReportIfFinished(report);
			}
		}
	}

	/**
	 * Close threads and/or message capturers when not already closed within a certain amount of time. Set
	 * <code>waitForMainThreadToFinish</code> to <code>false</code> when there's a risk for reports to stay in progress
	 * because some checkpoints handled by the main thread are not properly surrounded with a try/catch that calls an
	 * abortpoint. This method is used by {@link CloseReportsTask}
	 * 
	 * @see CloseReportsTask
	 * @param threadsTime                the time in milliseconds that needs to be passed for threads to be closed. Set
	 *                                   to -1 to not close threads
	 * @param messageCapturersTime       the time in milliseconds that needs to be passed for message capturers to be
	 *                                   closed. Set to -1 to not close threads
	 * @param waitForMainThreadToFinish  whether or not to wait for the main thread to finish. When <code>true</code>
	 *                                   time for threads and message capturers is counted from the time the main thread
	 *                                   has finished otherwise from the start of the report
	 * @param logThreadInfoBeforeClose   whether or not to log thread info on info level before calling close on a
	 *                                   report
	 * @param logThreadInfoMinReportAge  log thread info at info level for reports with an age above this minimum age
	 *                                   (disabled when minimum age and maximum age is the same)
	 * @param logThreadInfoMaxReportAge  log thread info at info level for reports with an age below this maximum age
	 *                                   (disabled when minimum age and maximum age is the same)
	 */
	public final void close(long threadsTime, long messageCapturersTime, boolean waitForMainThreadToFinish,
			boolean logThreadInfoBeforeClose, long logThreadInfoMinReportAge, long logThreadInfoMaxReportAge) {
		// Lock reportsInProgress as less as possible, synchronize on each report individually
		Set<Report> reports = new HashSet<Report>();
		synchronized(reportsInProgress) {
			for (Report report : reportsInProgress) {
				reports.add(report);
			}
		}
		for (Report report : reports) {
			synchronized (report) {
				boolean closeThreads = false;
				boolean closeMessageCapturers = false;
				if (waitForMainThreadToFinish) {
					if (report.mainThreadFinished()) {
						if (report.getMainThreadFinishedTime() + threadsTime <= System.currentTimeMillis()
								&& threadsTime != -1) {
							closeThreads = true;
						}
						if (report.getMainThreadFinishedTime() + messageCapturersTime <= System.currentTimeMillis()
								&& messageCapturersTime != -1) {
							closeMessageCapturers = true;
						}
					}
				} else {
					if (report.getStartTime() + threadsTime <= System.currentTimeMillis()
							&& threadsTime != -1) {
						closeThreads = true;
					}
					if (report.getStartTime() + messageCapturersTime <= System.currentTimeMillis()
							&& messageCapturersTime != -1) {
						closeMessageCapturers = true;
					}
				}
				boolean logThreadInfoBecauseOfAge = false;
				if (report.getStartTime() + logThreadInfoMinReportAge < System.currentTimeMillis()
						&& report.getStartTime() + logThreadInfoMaxReportAge > System.currentTimeMillis()) {
					logThreadInfoBecauseOfAge = true;
				}
				if (closeThreads || closeMessageCapturers || logThreadInfoBecauseOfAge) {
					String message = "Thread info for report in progress '" + report.getName() + "' (closeThreads="
							+ closeThreads + ",closeMessageCapturers=" + closeMessageCapturers + "): "
							+ report.getThreadInfo();
					if (logThreadInfoBeforeClose || logThreadInfoBecauseOfAge) {
						log.info(message);
					} else {
						log.debug(message);
					}
				}
				close(report, report.getCorrelationId(), closeThreads, closeMessageCapturers);
				closeReportIfFinished(report);
			}
		}
	}

	public static String getCorrelationId() {
		String name = getName();
		if (name == null) {
			name = "Ladybug";
		}
		String version = getVersion();
		if (version == null) {
			version = "unknown-version";
		}
		return name.replaceAll(" ", "_") + "-" + version.replaceAll(" ", "_") + "-" + new UID().toString();
	}

	/**
	 * See {@link Rerunner#rerun(String, Report, SecurityContext, ReportRunner)}
	 * 
	 * @param correlationId ...
	 * @param report ...
	 * @param securityContext ...
	 * @return ...
	 */
	public String rerun(String correlationId, Report report, SecurityContext securityContext) {
		return rerun(correlationId, report, securityContext, null);
	}

	/**
	 * See {@link Rerunner#rerun(String, Report, SecurityContext, ReportRunner)}
	 * 
	 * @param correlationId ...
	 * @param report ...
	 * @param securityContext ...
	 * @param reportRunner ...
	 * @return ...
	 */
	public String rerun(String correlationId, Report report, SecurityContext securityContext, ReportRunner reportRunner) {
		String errorMessage = null;
		if (rerunner == null && debugger == null) {
			errorMessage = "No rerunner or debugger configured";
		} else if (rerunner != null && debugger != null) {
			errorMessage = "Both rerunner and debugger configured";
		} else {
			boolean reportGeneratorEnabled = isReportGeneratorEnabled();
			if (reportGeneratorEnabled) {
				synchronized(originalReports) {
					originalReports.put(correlationId, report);
				}
			}
			try {
				if (rerunner != null) {
					errorMessage = rerunner.rerun(correlationId, report, securityContext, reportRunner);
				} else {
					errorMessage = debugger.rerun(correlationId, report, securityContext, reportRunner);
				}
			} finally {
				if (reportGeneratorEnabled) {
					// Verify that originalReport has been removed from originalReports by checkpoint()
					Report originalReport;
					synchronized(originalReports) {
						originalReport = (Report)originalReports.remove(correlationId);
					}
					if (errorMessage == null && originalReport != null) {
						errorMessage = "Rerun didn't trigger any checkpoint or new report didn't get correlationId '"
								+ correlationId + "'";
					}
				}
			}
		}
		return errorMessage;
	}

    /**
     * Get the endpoint for the current level from the original report. The
     * optional found and returned checkpoint can be used to check whether the
     * next endpoint will be stubbed, hence code until the next endpoint can
     * be skipped. This method will always return null when report is not in
     * rerun.
     * 
     * @param correlationId ...
     * @return ...
     */
	public Checkpoint getOriginalEndpointOrAbortpointForCurrentLevel(String correlationId) {
		Checkpoint result = null;
		synchronized(reportsInProgress) {
			Report report = (Report)reportsInProgressByCorrelationId.get(correlationId);
			if (report != null) {
				result = report.getOriginalEndpointOrAbortpointForCurrentLevel();
			}
		}
		return result;
	}
	// TODO vorige methode niet meer nodig?! hier nog documentern dat je met geturnde report voorzicht moet zijn omdat het nog in progress is? 
	public Report getReportInProgress(String correlationId) {
		synchronized(reportsInProgress) {
			return (Report)reportsInProgressByCorrelationId.get(correlationId);
		}
	}

	/**
	 * Check whether the checkpoint should be stubbed for the given stub strategy in case no matchingStubStrategies for
	 * the checkpoint is known
	 * 
	 * @param checkpoint ...
	 * @param strategy ...
	 * @return whether the checkpoint should be stubbed
	 */	
	public boolean stub(Checkpoint checkpoint, String strategy) {
		if (debugger == null) {
			if (STUB_STRATEGY_ALWAYS.equals(strategy)) {
				return true;
			} else {
				return false;
			}
		} else {
			return debugger.stub(checkpoint, strategy);
		}
	}

	public Report getReportInProgress(int index) {
		Report reportClone = null;
		synchronized(reportsInProgress) {
			if (index > -1 && index < reportsInProgress.size()) {
				Report report = reportsInProgress.get(index);
				try {
					reportClone = report.clone();
				} catch (CloneNotSupportedException e) {
					log.error("Unable to clone report in progress", e);
				}
			}
		}
		return reportClone;
	}

	public Report removeReportInProgress(int index) {
		Report report = null;
		synchronized(reportsInProgress) {
			if (index > -1 && index < reportsInProgress.size()) {
				report = reportsInProgress.remove(index);
				numberOfReportsInProgress--;
			}
		}
		return report;
	}

	public long getNumberOfReportsInProgress() {
		return numberOfReportsInProgress;
	}

	public long getReportsInProgressEstimatedMemoryUsage() {
		long reportsInProgressEstimatedMemoryUsage = 0;
		synchronized(reportsInProgress) {
			for (Report report : reportsInProgress) {
				reportsInProgressEstimatedMemoryUsage += report.getEstimatedMemoryUsage();
			}
		}
		return reportsInProgressEstimatedMemoryUsage;
	}

	public Storage getStorage(String name) {
		for (View view : views) {
			Storage storage = view.getDebugStorage();
			if (name.equals(storage.getName())) {
				return storage;
			}
		}
		// TODO: Introduce views for test tab also and replace getViews() in TestToolApi with getTabs() (for now the
		// frontend is using hardcoded storage name Test for test tab)
		if (name.equals("Test")) {
			return getTestStorage();
		}
		return null;
	}

	public static String getName() {
		return Package.getPackage("nl.nn.testtool").getSpecificationTitle();
	}

	public static String getVersion() {
		return getImplementationVersion();
	}

	public static String getSpecificationVersion() {
		return Package.getPackage("nl.nn.testtool").getSpecificationVersion();
	}

	public static String getImplementationVersion() {
		return Package.getPackage("nl.nn.testtool").getImplementationVersion();
	}
}