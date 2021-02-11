/*
   Copyright 2018 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.SneakyThrows;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.transform.MessageTransformer;

/**
 * @author Jaco de Groot
 */
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
	private List<Report> reportsInProgress = new ArrayList<Report>();
	private Map<String, Report> reportsInProgressByCorrelationId = new HashMap<String, Report>();
	private long numberOfReportsInProgress = 0;
	private long reportsInProgressEstimatedMemoryUsage = 0;
	private Map<String, Report> originalReports = new HashMap<String, Report>();
	private List<StartpointProvider> startpointProviders = new ArrayList<StartpointProvider>();
	private List<String> startpointProviderNames = new ArrayList<String>();
	private LogStorage debugStorage;
	private MessageEncoder messageEncoder = new MessageEncoderImpl();
	private MessageCapturer messageCapturer = new MessageCapturerImpl();
	private MessageTransformer messageTransformer;
	private String regexFilter;
	private String defaultStubStrategy = "Stub all external connection code";
	private List<String> stubStrategies = new ArrayList<String>(); { stubStrategies.add(defaultStubStrategy); }
	private Set<String> matchingStubStrategiesForExternalConnectionCode = new HashSet<>(stubStrategies);

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
	
	public void setDebugStorage(LogStorage debugStorage) {
		this.debugStorage = debugStorage;
	}

	public LogStorage getDebugStorage() {
		return debugStorage;
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

	@SneakyThrows
	private Object checkpoint(String correlationId, String childThreadId, String sourceClassName, String name,
			Object message, StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
			Set<String> matchingStubStrategies, int checkpointType, int levelChangeNextCheckpoint) {
		boolean executeStubableCode = true;
		if (reportGeneratorEnabled) {
			Report report;
			synchronized(reportsInProgress) {
				report = (Report)reportsInProgressByCorrelationId.get(correlationId);
				if (report == null) {
					if (checkpointType == Checkpoint.TYPE_STARTPOINT) {
						log.debug("Create new report for '" + correlationId + "'");
						report = new Report();
						report.setStartTime(System.currentTimeMillis());
						report.setTestTool(this);
						report.setCorrelationId(correlationId);
						report.setName(name);
						if (StringUtils.isNotEmpty(regexFilter) && !name.matches(regexFilter)) {
							report.setReportFilterMatching(false);
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
						reportsInProgress.add(0, report);
						reportsInProgressByCorrelationId.put(correlationId, report);
						numberOfReportsInProgress++;
					} else {
						log.warn("Report for '" + correlationId + "' is null, could not add checkpoint '" + name + "'");
					}
				}
			}
			if (report != null) {
				synchronized(report) {
					executeStubableCode = false;
					long oldMemoryUsage = report.getEstimatedMemoryUsage();
					message = report.checkpoint(childThreadId, sourceClassName, name, message, stubableCode,
							stubableCodeThrowsException, matchingStubStrategies, checkpointType, levelChangeNextCheckpoint);
					report.setEndTime(System.currentTimeMillis());
					synchronized(reportsInProgress) {
						reportsInProgressEstimatedMemoryUsage = reportsInProgressEstimatedMemoryUsage
								+ report.getEstimatedMemoryUsage() - oldMemoryUsage;
					}
					closeReport(report);
				}
			}
		}
		if (executeStubableCode) {
			if (stubableCode != null) {
				message = stubableCode.execute();
			}
			if (stubableCodeThrowsException != null) {
				message = stubableCodeThrowsException.execute();
			}
		}
		return message;
	}

	protected void closeReport(Report report) {
		synchronized(report) {
			if (report.finished() && !report.isClosed()) {
				report.closeStreamingMessages();
				report.setClosed(true);
				log.debug("Report is finished for '" + report.getCorrelationId() + "'");
				synchronized(reportsInProgress) {
					reportsInProgress.remove(report);
					reportsInProgressByCorrelationId.remove(report.getCorrelationId());
					numberOfReportsInProgress--;
					reportsInProgressEstimatedMemoryUsage = reportsInProgressEstimatedMemoryUsage - report.getEstimatedMemoryUsage();
				}
				if (report.isReportFilterMatching()) {
					debugStorage.storeWithoutException(report);
				}
			}
		}
	}

	public boolean messageCapturerWaitingForClose() {
		synchronized(reportsInProgress) {
			for (Report report : reportsInProgress) {
				if (report.threadsFinished() && !report.messageCapturersFinished()
						&& report.getEndTime() + 30000 < System.currentTimeMillis()) {
					return true;
				}
			}
		}
		return false;
	}

	public Object startpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_STARTPOINT, 1);
	}

	public Object startpoint(String correlationId, String sourceClassName, String name, Object message,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, matchingStubStrategies,
				Checkpoint.TYPE_STARTPOINT, 1);
	}

	public Object startpoint(String correlationId, String sourceClassName, String name, StubableCode stubableCode,
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

	public Object endpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_ENDPOINT, -1);
	}

	public Object endpoint(String correlationId, String sourceClassName, String name, Object message,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, matchingStubStrategies,
				Checkpoint.TYPE_ENDPOINT, -1);
	}

	public Object endpoint(String correlationId, String sourceClassName, String name, StubableCode stubableCode,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, null, stubableCode, null, matchingStubStrategies,
				Checkpoint.TYPE_ENDPOINT, -1);
	}

	/**
	 * See description of parameter throwsException at startpoint method.
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
	public <E extends Exception> Object endpoint(String correlationId, String sourceClassName, String name,
			StubableCodeThrowsException stubableCodeThrowsException, Set<String> matchingStubStrategies,
			E throwsException) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, stubableCodeThrowsException,
				matchingStubStrategies, Checkpoint.TYPE_ENDPOINT, -1);
	}

	/**
	 * Convenient method for the most common use case for stubable code, stubbing all external connections related code.
	 * The default stubbing strategy (Stub all external connection code) will be used as matching stub strategies.
	 * 
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param externalConnectionCode ...
	 * @return ...
	 */
	public Object endpoint(String correlationId, String sourceClassName, String name, ExternalConnectionCode externalConnectionCode) {
		return checkpoint(correlationId, null, sourceClassName, name, null, externalConnectionCode, null, matchingStubStrategiesForExternalConnectionCode, Checkpoint.TYPE_ENDPOINT, -1);
	}

	/**
	 * Convenient method for the most common use case for stubable code that throws exception, stubbing all external
	 * connections related code.
	 * The default stubbing strategy (Stub all external connection code) will be used as matching stub strategies.
	 * See description of parameter throwsException at startpoint method.
	 * 
	 * @param <E> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param externalConnectionCodeThrowsException ...
	 * @param throwsException ...
	 * @return ...
	 * @throws E ...
	 */
	public <E extends Exception> Object endpoint(String correlationId, String sourceClassName, String name,
			ExternalConnectionCodeThrowsException externalConnectionCodeThrowsException, E throwsException) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, externalConnectionCodeThrowsException,
				matchingStubStrategiesForExternalConnectionCode, Checkpoint.TYPE_ENDPOINT, -1);
	}

	public Object inputpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_INPUTPOINT, 0);
	}

	public Object inputpoint(String correlationId, String sourceClassName, String name, Object message,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, matchingStubStrategies,
				Checkpoint.TYPE_INPUTPOINT, 0);
	}

	public Object inputpoint(String correlationId, String sourceClassName, String name, StubableCode stubableCode,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, null, stubableCode, null, matchingStubStrategies,
				Checkpoint.TYPE_INPUTPOINT, 0);
	}

	/**
	 * See description of parameter throwsException at startpoint method.
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
	public <E extends Exception> Object inputpoint(String correlationId, String sourceClassName, String name,
			StubableCodeThrowsException stubableCodeThrowsException, Set<String> matchingStubStrategies,
			E throwsException) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, stubableCodeThrowsException,
				matchingStubStrategies, Checkpoint.TYPE_INPUTPOINT, 0);
	}

	public Object outputpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	public Object outputpoint(String correlationId, String sourceClassName, String name, Object message,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, matchingStubStrategies,
				Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	public Object outputpoint(String correlationId, String sourceClassName, String name, StubableCode stubableCode,
			Set<String> matchingStubStrategies) {
		return checkpoint(correlationId, null, sourceClassName, name, null, stubableCode, null, matchingStubStrategies,
				Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	/**
	 * See description of parameter throwsException at startpoint method.
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
	public <E extends Exception> Object outputpoint(String correlationId, String sourceClassName, String name,
			StubableCodeThrowsException stubableCodeThrowsException, Set<String> matchingStubStrategies,
			E throwsException) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, stubableCodeThrowsException,
				matchingStubStrategies, Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	/**
	 * Convenient method for the most common use case for stubable code, stubbing all external connections related code.
	 * The default stubbing strategy (Stub all external connection code) will be used as matching stub strategies.
	 * 
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param externalConnectionCode ...
	 * @return ...
	 */
	public Object outputpoint(String correlationId, String sourceClassName, String name,
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
	 * @param <E> ...
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param externalConnectionCodeThrowsException ...
	 * @param throwsException ...
	 * @return ...
	 * @throws E ...
	 */
	public <E extends Exception> Object outputpoint(String correlationId, String sourceClassName, String name,
			ExternalConnectionCodeThrowsException externalConnectionCodeThrowsException, E throwsException) throws E {
		return checkpoint(correlationId, null, sourceClassName, name, null, null, externalConnectionCodeThrowsException,
				matchingStubStrategiesForExternalConnectionCode, Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	public Object infopoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null,
				Checkpoint.TYPE_INFOPOINT, 0);
	}

	/**
	 * Specify the name of a previous startpoint to abort to or a unique name
	 * to finish the report or thread.
	 * 
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param message ...
	 * @return ...
	 */
	public Object abortpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null, Checkpoint.TYPE_ABORTPOINT, -1);
	}

	/**
	 * Set a marker in the report for a child thread to appear. This method
	 * should be called by the parent thread. Specify a childThreadId that will also
	 * be used by the child thread when calling threadStartpoint. The name of the
	 * child thread can be used as childThreadId (when known at this point).
	 * 
	 * @param correlationId ...
	 * @param childThreadId ...
	 */
	public void threadCreatepoint(String correlationId, String childThreadId) {
		checkpoint(correlationId, childThreadId, null, null, null, null, null, null, Checkpoint.TYPE_THREADCREATEPOINT, 0);
	}

	/**
	 * Startpoint for a child thread. Specify a childThreadId that was also used when
	 * calling threadStartpoint.
	 * 
	 * @param correlationId ...
	 * @param childThreadId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param message ...
	 * @return ...
	 */
	public Object threadStartpoint(String correlationId, String childThreadId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, childThreadId, sourceClassName, name, message, null, null, null, Checkpoint.TYPE_THREADSTARTPOINT, 1);
	}

	/**
	 * Startpoint for a child thread. This method can be used when the name of
	 * the child thread was used as childThreadId when calling threadCreatepoint.
	 * 
	 * @param correlationId ...
	 * @param sourceClassName ...
	 * @param name ...
	 * @param message ...
	 * @return ...
	 */
	public Object threadStartpoint(String correlationId, String sourceClassName, String name, Object message) {
		return threadStartpoint(correlationId, Thread.currentThread().getName(), sourceClassName, name, message);
	}

	public Object threadEndpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, null, null, null, Checkpoint.TYPE_THREADENDPOINT, -1);
	}

	public static String getCorrelationId() {
		return getName().replaceAll(" ", "_")
				+ "-" + getVersion().replaceAll(" ", "_")
				+ "-" + new UID().toString();
	}

	public String rerun(Report report, SecurityContext securityContext) {
		return rerun(null, report, securityContext, null);
	}

	public String rerun(String correlationId, Report report, SecurityContext securityContext, ReportRunner reportRunner) {
		String errorMessage = null;
		if (rerunner == null && debugger == null) {
			errorMessage = "No rerunner or debugger configured";
		} else if (rerunner != null && debugger != null) {
			errorMessage = "Both rerunner and debugger configured";
		} else {
			if (correlationId == null) {
				correlationId = getCorrelationId();
			}
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
	 * Check whether the checkpoint should be stubbed for the given stub
	 * strategy.
	 * 
	 * @param checkpoint ...
	 * @param strategy ...
	 * @return whether the checkpoint should be stubbed
	 */	
	public boolean stub(Checkpoint checkpoint, String strategy) {
		return debugger.stub(checkpoint, strategy);
	}

	public Report getReportInProgress(int index) {
		Report reportClone = null;
		synchronized(reportsInProgress) {
			if (index > -1 && index < reportsInProgress.size()) {
				Report report = (Report)reportsInProgress.get(index);
				try {
					reportClone = (Report)report.clone();
				} catch (CloneNotSupportedException e) {
					log.error("Unable to clone report in progress", e);
				}
			}
		}
		return reportClone;
	}

	public long getNumberOfReportsInProgress() {
		return numberOfReportsInProgress;
	}

	public long getReportsInProgressEstimatedMemoryUsage() {
		return reportsInProgressEstimatedMemoryUsage;
	}
	
	public void register(StartpointProvider startpointProvider) {
		synchronized(startpointProviders) {
			startpointProviders.add(startpointProvider);
			startpointProviderNames.add(startpointProvider.getName());
		}
	}
	
	public List<String> getStartpointProviderNames() {
		synchronized(startpointProviders) {
			return startpointProviderNames;
		}
	}
	
	public StartpointProvider getStartpointProvider(String name) {
		synchronized(startpointProviders) {
			int i = startpointProviderNames.indexOf(name);
			if (i == -1) {
				return null;
			} else {
				return (StartpointProvider)startpointProviders.get(i);
			}
		}
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