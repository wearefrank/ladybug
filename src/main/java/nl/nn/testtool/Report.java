/*
   Copyright 2018-2020 WeAreFrank!

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

import java.beans.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;

import lombok.SneakyThrows;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.transform.MessageTransformer;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.CsvUtil;
import nl.nn.testtool.util.EscapeUtil;
import nl.nn.testtool.util.LogUtil;
import nl.nn.testtool.util.XmlUtil;

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
	// ObjectInputStream, it doesn't effect reading objects through XMLDecoder.
	private transient static final long serialVersionUID = 5;
	private transient static Logger log = LogUtil.getLogger(Report.class);
	private transient TestTool testTool;
	// Please note that the set method should return void for XmlEncoder to
	// store the property (hence the setVariableCsvWithoutException method)
	private long startTime;
	private long endTime;
	private String correlationId;
	private String name;
	private String description;
	private String path;
	private String stubStrategy;
	private List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
	private long estimatedMemoryUsage = 0;
	private String transformation;
	private String variableCsv;
	// Please note that the get and set methods need @Transient annotation for
	// XmlEncoder to not store the property.
	private transient Report originalReport;
	private transient List<String> threads = new ArrayList<String>();
	private transient Map<String, Integer> threadIndex = new HashMap<String, Integer>();
	private transient Map<String, Integer> threadFirstLevel = new HashMap<String, Integer>();
	private transient Map<String, Integer> threadLevel = new HashMap<String, Integer>();
	private transient Map<String, String> threadParent = new HashMap<String, String>();
	private transient Storage storage;
	private transient Integer storageId;
	private transient Long storageSize;
	private transient ReportXmlTransformer reportXmlTransformer;
	private transient ReportXmlTransformer globalReportXmlTransformer;
	private transient String xml;
	private transient boolean differenceChecked = false;
	private transient boolean differenceFound = false;
	private transient Map<String, String> truncatedMessageMap = new RefCompareMap<String, String>();
	private transient boolean reportFilterMatching = true;
	private transient boolean logReportFilterMatching = true;
	private transient boolean logMaxCheckpoints = true;
	private transient boolean logMaxMemoryUsage = true;

	public Report() {
		String threadName = Thread.currentThread().getName();
		threads.add(threadName);
		threadIndex.put(threadName, new Integer(0));
		threadFirstLevel.put(threadName, new Integer(0));
		threadLevel.put(threadName, new Integer(0));
	}

	@Transient
	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	@Transient
	public TestTool getTestTool() {
		return testTool;
	}

	@Transient
	public void setStorage(Storage storage) {
		this.storage = storage;
	}

	@Transient
	public Storage getStorage() {
		return storage;
	}

	public void setStorageId(Integer storageId) {
		this.storageId = storageId;
	}

	public Integer getStorageId() {
		return storageId;
	}

	@Transient
	public void setStorageSize(Long storageSize) {
		this.storageSize = storageSize;
	}

	@Transient
	public Long getStorageSize() {
		return storageSize;
	}
	
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
	
	public String getCorrelationId() {
		return correlationId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public String getFullPath() {
		return (StringUtils.isNotEmpty(getPath()) ? getPath() : "/") + getName();
	}
	
	public void setStubStrategy(String stubStrategy) {
		this.stubStrategy = stubStrategy;
	}

	public String getStubStrategy() {
		return stubStrategy;
	}

	public void setTransformation(String transformation) {
		this.transformation = transformation;
	}

	public String getTransformation() {
		return transformation;
	}

	@Transient
	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}

	@Transient
	public ReportXmlTransformer getReportXmlTransformer() {
		return reportXmlTransformer;
	}

	@Transient
	public void setGlobalReportXmlTransformer(ReportXmlTransformer globalReportXmlTransformer) {
		this.globalReportXmlTransformer = globalReportXmlTransformer;
	}

	@Transient
	public ReportXmlTransformer getGlobalReportXmlTransformer() {
		return globalReportXmlTransformer;
	}

	@Transient
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

	protected Object checkpoint(String threadId, String sourceClassName, String name, Object message,
			StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
			Set<String> matchingStubStrategies, int checkpointType, int levelChangeNextCheckpoint) {
		if (checkpointType == Checkpoint.TYPE_THREADCREATEPOINT) {
			String threadName = Thread.currentThread().getName();
			int index=threads.indexOf(threadName);
			if (index<0) {
				log.warn("Cannot create thread threadId ["+threadId+"], threadName ["+threadName+"] not found");
			} else {
				threads.add(threads.indexOf(threadName), threadId);
				threadIndex.put(threadId, threadIndex.get(threadName));
				threadFirstLevel.put(threadId, (Integer)threadLevel.get(threadName));
				threadLevel.put(threadId, (Integer)threadLevel.get(threadName));
				threadParent.put(threadId, threadName);
			}
		} else {
			message = addCheckpoint(threadId, sourceClassName, name, message, stubableCode, stubableCodeThrowsException,
					matchingStubStrategies, checkpointType, levelChangeNextCheckpoint);
		}
		return message;
	}

	private Object addCheckpoint(String threadId, String sourceClassName, String name, Object message,
			StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
			Set<String> matchingStubStrategies, int checkpointType, int levelChangeNextCheckpoint) {
		String threadName = Thread.currentThread().getName();
		Integer index = (Integer)threadIndex.get(threadName);
		Integer level = (Integer)threadLevel.get(threadName);
		if (name == null) {
			log.warn("Ignored checkpoint with null name " + getCheckpointLogDescription(name, checkpointType, level));
		} else {
			if (index == null) {
				index = (Integer)threadIndex.remove(threadId);
				if (index != null) {
					threadIndex.put(threadName, index);
					level = (Integer)threadFirstLevel.remove(threadId);
					threadFirstLevel.put(threadName, level);
					threadLevel.remove(threadId);
					String parent = (String)threadParent.remove(threadId);
					threads.add(threads.indexOf(threadId), threadName);
					threads.remove(threadId);
					threadParent.put(threadName, parent);
				}
			}
			if (index == null) {
				log.warn("Unknown thread, ignored checkpoint " + getCheckpointLogDescription(name, checkpointType, level));
			} else {
				message = addCheckpoint(threadName, sourceClassName, name, message, stubableCode,
						stubableCodeThrowsException, matchingStubStrategies, checkpointType, index, level,
						levelChangeNextCheckpoint);
				if (checkpointType == Checkpoint.TYPE_ABORTPOINT && checkpoints.size() < testTool.getMaxCheckpoints()) {
					int firstLevel = ((Integer)threadFirstLevel.get(threadName)).intValue();
					List<Checkpoint> checkpoints = getCheckpoints();
					for (int i = index.intValue() - 1; i > -1; i--) {
						Checkpoint checkpoint = (Checkpoint)checkpoints.get(i);
						if (level.intValue() <= firstLevel + 1) {
							i = -1;
						} else {
							if (checkpoint.getType() == Checkpoint.TYPE_STARTPOINT
									&& checkpoint.getLevel() < level.intValue()) {
								if (checkpoint.getName().equals(name)) {
									i = -1;
								} else {
									index = (Integer)threadIndex.get(threadName);
									level = (Integer)threadLevel.get(threadName);
									message = addCheckpoint(threadName, sourceClassName, name, message, stubableCode,
											stubableCodeThrowsException, matchingStubStrategies, checkpointType, index,
											level, levelChangeNextCheckpoint);
								}
							}
						}
					}
				}
			}
		}
		return message;
	}

	@SneakyThrows
	private Object addCheckpoint(String threadName, String sourceClassName, String name, Object message,
			StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
			Set<String> matchingStubStrategies, int checkpointType, Integer index, Integer level,
			int levelChangeNextCheckpoint) {
		if (!isReportFilterMatching()) {
			if (logReportFilterMatching) {
				log.debug("Report name doesn't match report filter regex, ignore checkpoint "
						+ getCheckpointLogDescription(name, checkpointType, level) + " "
						+ getOtherCheckpointsLogDescription());
				logReportFilterMatching = false;
			}
		} else if (checkpoints.size() >= testTool.getMaxCheckpoints()) {
			if (logMaxCheckpoints) {
				log.warn("Maximum number of checkpoints exceeded, ignore checkpoint "
						+ getCheckpointLogDescription(name, checkpointType, level) + " "
						+ getOtherCheckpointsLogDescription());
				logMaxCheckpoints = false;
			}
		} else if (getEstimatedMemoryUsage() >= testTool.getMaxMemoryUsage()) {
			if (logMaxMemoryUsage) {
				log.warn("Maximum memory usage reached for this report, ignore checkpoint "
						+ getCheckpointLogDescription(name, checkpointType, level) + " "
						+ getOtherCheckpointsLogDescription());
				logMaxMemoryUsage = false;
			}
		} else {
			Checkpoint checkpoint = new Checkpoint(this, threadName, sourceClassName, name, message, checkpointType, level.intValue());
			checkpoints.add(index.intValue(), checkpoint);
			boolean stub = false;
			if (originalReport != null) {
				Path lastCheckpointPath = checkpoint.getPath();
				Checkpoint originalCheckpoint = (Checkpoint)originalReport.getCheckpoint(lastCheckpointPath);
				if (originalCheckpoint == null) {
					if (matchingStubStrategies != null) {
						if (matchingStubStrategies.contains(originalReport.getStubStrategy())) {
							stub = true;
						}
					} else if (testTool.getDebugger() != null) {
						stub = testTool.stub(checkpoint, originalReport.getStubStrategy());
					}
				} else {
					checkpoint.setStub(originalCheckpoint.getStub());
					if (originalCheckpoint.getStub() == Checkpoint.STUB_FOLLOW_REPORT_STRATEGY) {
						if (matchingStubStrategies != null) {
							if (matchingStubStrategies.contains(originalReport.getStubStrategy())) {
								stub = true;
							}
						} else if (testTool.getDebugger() != null) {
							stub = testTool.stub(originalCheckpoint, originalReport.getStubStrategy());
						}
					} else if (originalCheckpoint.getStub() == Checkpoint.STUB_NO) {
						stub = false;
					} else if (originalCheckpoint.getStub() == Checkpoint.STUB_YES) {
						stub = true;
					}
				}
				if (stub) {
					if (originalCheckpoint == null) {
						message = "<stub>Could not find stub message for '" + lastCheckpointPath + "'</stub>";
					} else {
						message = originalCheckpoint.getMessageAsObject();
					}
					checkpoint.setMessage(message);
					checkpoint.setMessageHasBeenStubbed(true);
				}
			}
			if (!stub) {
				try {
					if (stubableCode != null) {
						message = stubableCode.execute();
					}
					if (stubableCodeThrowsException != null) {
						message = stubableCodeThrowsException.execute();
					}
					checkpoint.setMessage(message);
				} catch(Throwable t) {
					checkpoints.remove(index.intValue());
					testTool.abortpoint(correlationId, sourceClassName, name, t.getMessage());
					throw t;
				}
			}
			if(testTool.getMaxMessageLength() > 0 && message != null && message.toString().length() > testTool.getMaxMessageLength()) {
				checkpoint.setMessage(truncateMessage(checkpoint, message.toString()));
			}
			estimatedMemoryUsage += checkpoint.getEstimatedMemoryUsage();
			if (log.isDebugEnabled()) {
				log.debug("Added checkpoint " + getCheckpointLogDescription(name, checkpointType, level));
			}
		}
		for (int i = threads.indexOf(threadName); i < threads.size(); i++) {
			String key = threads.get(i);
			Integer value = (Integer)threadIndex.get(key);
			threadIndex.put(key, new Integer(value.intValue() + 1));
		}
		level = new Integer(level.intValue() + levelChangeNextCheckpoint);
		threadLevel.put(threadName, level);
		return message;
	}
	
	private String truncateMessage(Checkpoint checkpoint, String message) {
		// For a message that is referenced by multiple checkpoints, have one truncated message that is
		// referenced by those checkpoints, to prevent creating multiple String objects representing the
		// same string and occupying unnecessary memory.
		checkpoint.setPreTruncatedMessageLength(message.length());
		if(truncatedMessageMap.containsKey(message)) {
			checkpoint.setEstimatedMemoryUsage(0L);
			return truncatedMessageMap.get(message);
		} else {
			String truncatedMessage = message.substring(0, testTool.getMaxMessageLength())
				+ "... ("+(message.length() - testTool.getMaxMessageLength())+" more characters)";
			
			truncatedMessageMap.put(message, truncatedMessage);
			checkpoint.setEstimatedMemoryUsage(2 * truncatedMessage.length());
			return truncatedMessage;
		}
	}

	public Checkpoint getOriginalEndpointOrAbortpointForCurrentLevel() {
		Checkpoint result = null;
		if (originalReport != null) {
			Checkpoint lastCheckpoint = (Checkpoint)checkpoints.get(checkpoints.size() - 1);
			Checkpoint checkpoint = originalReport.getCheckpoint(lastCheckpoint.getPath());
			if (checkpoint != null) {
				int i = originalReport.checkpoints.indexOf(checkpoint) + 1;
				while (checkpoint.getType() != Checkpoint.TYPE_ENDPOINT
						&& checkpoint.getType() != Checkpoint.TYPE_ABORTPOINT
						&& i < originalReport.checkpoints.size()) {
					checkpoint = (Checkpoint)originalReport.checkpoints.get(i);
					i++;
				}
				if (checkpoint.getType() == Checkpoint.TYPE_ENDPOINT
						|| checkpoint.getType() == Checkpoint.TYPE_ABORTPOINT) {
					result = checkpoint;
				}
			}
		}
		return result;
	}

	protected boolean finished() {
		boolean finished = true;
		for (String threadName : threads) {
			Integer level = threadLevel.get(threadName);
			if (!level.equals(threadFirstLevel.get(threadName))) {
				finished = false;
				break;
			}
		}
		return finished;
	}

	public Checkpoint getCheckpoint(Path path) {
		Checkpoint result = null;
		Iterator<Checkpoint> iterator = checkpoints.iterator();
		while (result == null && iterator.hasNext()) {
			Checkpoint checkpoint = (Checkpoint)iterator.next();
			if (path.equals(checkpoint.getPath())) {
				result = checkpoint;
			}
		}
		return result;
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

	public void setEstimatedMemoryUsage(long estimatedMemoryUsage) {
		this.estimatedMemoryUsage = estimatedMemoryUsage;
	}

	public long getEstimatedMemoryUsage() {
		return estimatedMemoryUsage;
	}

	@Transient
	public void setDifferenceChecked(boolean differenceChecked) {
		this.differenceChecked = differenceChecked;
	}

	@Transient
	public boolean getDifferenceChecked() {
		return differenceChecked;
	}

	@Transient
	public void setDifferenceFound(boolean differenceFound) {
		this.differenceFound = differenceFound;
	}

	@Transient
	public boolean getDifferenceFound() {
		return differenceFound;
	}

	@Transient
	public MessageTransformer getMessageTransformer() {
		return testTool.getMessageTransformer();
	}

	public Object clone() throws CloneNotSupportedException {
		Report report = new Report();
		report.setTestTool(testTool);
		report.setStartTime(startTime);
		report.setEndTime(endTime);
		report.setCorrelationId(correlationId);
		report.setName(name);
		report.setDescription(description);
		report.setPath(path);
		report.setStubStrategy(stubStrategy);
		report.setEstimatedMemoryUsage(estimatedMemoryUsage);
		report.setTransformation(transformation);
		report.setVariableCsv(variableCsv);
		List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
		for (Checkpoint checkpoint : this.checkpoints) {
			checkpoint = (Checkpoint)checkpoint.clone();
			checkpoint.setReport(report);
			checkpoints.add(checkpoint);
		}
		report.setCheckpoints(checkpoints);
		return report;
	}

	public String toString() {
		return name;
	}

	public String toXml() {
		return toXml(null);
	}
	
	public String toXml(ReportRunner reportRunner) {
		if (xml == null) {
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append("<Report");
			stringBuffer.append(" Name=\"" + EscapeUtil.escapeXml(name) + "\"");
			stringBuffer.append(" Description=\"" + EscapeUtil.escapeXml(description) + "\"");
			stringBuffer.append(" Path=\"" + EscapeUtil.escapeXml(path) + "\"");
			stringBuffer.append(" CorrelationId=\"" + EscapeUtil.escapeXml(correlationId) + "\"");
			stringBuffer.append(" StartTime=\"" + startTime + "\"");
			stringBuffer.append(" EndTime=\"" + endTime + "\"");
			stringBuffer.append(" NumberOfCheckpoints=\"" + getNumberOfCheckpoints() + "\"");
			stringBuffer.append(" EstimatedMemoryUsage=\"" + estimatedMemoryUsage + "\"");
			stringBuffer.append(">");
			for (Checkpoint checkpoint : checkpoints) {
				String message;
				if(reportRunner != null && checkpoint.containsVariables()) {
					message = checkpoint.getMessageWithResolvedVariables(reportRunner);
				} else {
					message = checkpoint.getMessage();
				}
				stringBuffer.append("<Checkpoint");
				stringBuffer.append(" Name=\"" + EscapeUtil.escapeXml(checkpoint.getName()) + "\"");
				stringBuffer.append(" Type=\"" + EscapeUtil.escapeXml(checkpoint.getTypeAsString()) + "\"");
				stringBuffer.append(" Level=\"" + checkpoint.getLevel() + "\"");
				if (checkpoint.getEncoding() != Checkpoint.ENCODING_NONE) {
					stringBuffer.append(" Encoding=\"" + checkpoint.getEncodingAsString() + "\"");
				}
				if (message == null) {
					stringBuffer.append(" Null=\"true\"/>");
					} else {
					if (XmlUtil.isXml(message)) {
						String textDecl = null;
						if (message.startsWith("<?")) {
							int i = message.indexOf("?>") + 2;
							textDecl = message.substring(0, i);
							stringBuffer.append(" TextDecl=\"");
							stringBuffer.append(EscapeUtil.escapeXml(textDecl));
							stringBuffer.append("\">");
							message = message.substring(i);
						} else {
							stringBuffer.append(">");
						}
						stringBuffer.append(message);
					} else {
						stringBuffer.append(">");
						stringBuffer.append(EscapeUtil.escapeXml(message));
					}
					stringBuffer.append("</Checkpoint>");
				}
			}
			stringBuffer.append("</Report>");
			xml = stringBuffer.toString();
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
		return "(name: " + name + ", type: " + Checkpoint.getTypeAsString(type) + ", level: " + level + ", correlationId: " + correlationId + ")";
	}

	private String getOtherCheckpointsLogDescription() {
		return "(next checkpoints for this report will be ignored without any logging)";
	}

	public String getVariableCsv() {
		return variableCsv;
	}

	// XMLEncoder requires a void setter function.
	public void setVariableCsv(String variableCsv) {
		if (StringUtils.isEmpty(variableCsv)) {
			this.variableCsv = null;
			return;
		}
		String errorMessage = CsvUtil.validateCsv(variableCsv, ";", 2);
		if (errorMessage != null)
			throw new IllegalArgumentException(errorMessage);

		this.variableCsv = variableCsv;
	}

	public String setVariableCsvWithoutException(String variableCsv) {
		try {
			setVariableCsv(variableCsv);
			return null;
		} catch (IllegalArgumentException e) {
			return e.getMessage();
		}
	}

	public Map<String, String> getVariablesAsMap() {
		if(StringUtils.isEmpty(variableCsv)) {
			return null;
		}
		Map<String, String> variableMap = new LinkedHashMap<String, String>();
		Scanner scanner = new Scanner(variableCsv);
		List<String> lines = new ArrayList<String>();
		while(scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();
			if(StringUtils.isNotEmpty(nextLine) && !nextLine.startsWith("#")) {
				lines.add(nextLine);
			}
		}
		scanner.close();
		
		List<String> params = Arrays.asList(lines.get(0).split(";"));
		for(String key : params) {
			String value = lines.get(1).split(";")[params.indexOf(key)];
			variableMap.put(key, value);
		}
		return variableMap;
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
		for(Object o : keys) {
			if(o == key) {
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
		for(Object o : keys) {
			if(o == key) {
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
	public Set<Map.Entry<K,V>> entrySet() {
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
	public void putAll(Map<? extends K,? extends V> m) {
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