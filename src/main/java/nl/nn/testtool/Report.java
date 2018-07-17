package nl.nn.testtool;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.transform.MessageTransformer;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.EscapeUtil;
import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

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
	private long startTime;
	private long endTime;
	private String correlationId;
	private String name;
	private String description;
	private String path;
	private String stubStrategy;
	private List checkpoints = new ArrayList();
	private transient Report originalReport;
	private transient List threads = new ArrayList();
	private transient Map threadIndex = new HashMap();
	private transient Map threadFirstLevel = new HashMap();
	private transient Map threadLevel = new HashMap();
	private transient Map threadParent = new HashMap();
	private long estimatedMemoryUsage = 0;
	// TODO bij storage interface documenteren dat storage setStorage aan moet roepen?
	private transient Storage storage;
	private transient Integer storageId;
	private transient Long storageSize;
	private transient Report counterpart;
	private transient String xml;
	private transient ReportXmlTransformer reportXmlTransformer;
	private transient boolean differenceChecked = false;
	private transient boolean differenceFound = false;
	
	public Report() {
		String threadName = Thread.currentThread().getName();
		threads.add(threadName);
		threadIndex.put(threadName, new Integer(0));
		threadFirstLevel.put(threadName, new Integer(0));
		threadLevel.put(threadName, new Integer(0));
	}
	
	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setStorage(Storage storage) {
		this.storage = storage;
	}

	public Storage getStorage() {
		return storage;
	}

	public void setStorageId(Integer storageId) {
		this.storageId = storageId;
	}

	public Integer getStorageId() {
		return storageId;
	}
	
	public void setStorageSize(Long storageSize) {
		this.storageSize = storageSize;
	}
	
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

	public void setStubStrategy(String stubStrategy) {
		this.stubStrategy = stubStrategy;
	}
	
	public String getStubStrategy() {
		return stubStrategy;
	}
	
	public void setOriginalReport(Report originalReport) {
		this.originalReport = originalReport;
	}

	protected Object checkpoint(String threadId, String sourceClassName, String name, Object message,
			int checkpointType, int levelChangeNextCheckpoint) {
		if (checkpointType == Checkpoint.TYPE_THREADCREATEPOINT) {
			String threadName = Thread.currentThread().getName();
			threads.add(threads.indexOf(threadName), threadId);
			threadIndex.put(threadId, threadIndex.get(threadName));
			threadFirstLevel.put(threadId, (Integer)threadLevel.get(threadName));
			threadLevel.put(threadId, (Integer)threadLevel.get(threadName));
			threadParent.put(threadId, threadName);
		} else {
			message = addCheckpoint(threadId, sourceClassName, name, message, checkpointType, levelChangeNextCheckpoint);
		}
		return message;
	}

	private Object addCheckpoint(String threadId, String sourceClassName, String name, Object message, int checkpointType, int levelChangeNextCheckpoint) {
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
				message = addCheckpoint(threadName, sourceClassName, name, message, checkpointType, index, level, levelChangeNextCheckpoint);
				if (checkpointType == Checkpoint.TYPE_ABORTPOINT && checkpoints.size() < testTool.getMaxCheckpoints()) {
					int firstLevel = ((Integer)threadFirstLevel.get(threadName)).intValue();
					List checkpoints = getCheckpoints();
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
									message = addCheckpoint(threadName, sourceClassName, name, message, checkpointType, index, level, levelChangeNextCheckpoint);
								}
							}
						}
					}
				}
			}
		}
		return message;
	}

	private Object addCheckpoint(String threadName, String sourceClassName, String name, Object message, int checkpointType, Integer index, Integer level, int levelChangeNextCheckpoint) {
		if (checkpoints.size() < testTool.getMaxCheckpoints()) {
			Checkpoint checkpoint = new Checkpoint(this, threadName, sourceClassName, name, message, checkpointType, level.intValue());
			checkpoints.add(index.intValue(), checkpoint);
			if (originalReport != null) {
				Path lastCheckpointPath = checkpoint.getPath();
				Checkpoint originalCheckpoint = (Checkpoint)originalReport.getCheckpoint(lastCheckpointPath);
				boolean stub = false;
				if (originalCheckpoint == null) {
					stub = testTool.stub(checkpoint, originalReport.getStubStrategy());
				} else {
					checkpoint.setStub(originalCheckpoint.getStub());
					if (originalCheckpoint.getStub() == Checkpoint.STUB_FOLLOW_REPORT_STRATEGY) {
						stub = testTool.stub(originalCheckpoint, originalCheckpoint.getReport().getStubStrategy());
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
						message = originalCheckpoint.getMessage();
					}
					checkpoint.setMessage(message);
					checkpoint.setMessageHasBeenStubbed(true);
				}
			}
			estimatedMemoryUsage = estimatedMemoryUsage + checkpoint.getEstimatedMemoryUsage();
			if (log.isDebugEnabled()) {
				log.debug("Added checkpoint " + getCheckpointLogDescription(name, checkpointType, level));
			}
		} else {
			log.warn("Maximum number of checkpoints exceeded, ignored checkpoint " + getCheckpointLogDescription(name, checkpointType, level));
		}
		for (int i = threads.indexOf(threadName); i < threads.size(); i++) {
			Object key = threads.get(i);
			Integer value = (Integer)threadIndex.get(key);
			threadIndex.put(key, new Integer(value.intValue() + 1));
		}
		level = new Integer(level.intValue() + levelChangeNextCheckpoint);
		threadLevel.put(threadName, level);
		return message;
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

	private boolean isParent(String parentThreadName, String childThreadName) {
		String directParentThreadName = (String)threadParent.get(childThreadName);
		if (directParentThreadName == null) {
			return false;
		} else {
			return parentThreadName.equals(directParentThreadName) || isParent(parentThreadName, directParentThreadName);
		}
	}
	
	private boolean isChild(String childThreadName, String parentThreadName) {
		return isParent(parentThreadName, childThreadName);
	}

	protected boolean finished() {
		boolean finished = true;
		for (int i = 0; i < threads.size(); i++) {
			Object value = threadLevel.get(threads.get(i));
			if (!value.equals(threadFirstLevel.get(threads.get(i)))) {
				finished = false;
				break;
			}
		}
		return finished;
	}

	public Checkpoint getCheckpoint(Path path) {
		Checkpoint result = null;
		Iterator iterator = checkpoints.iterator();
		while (result == null && iterator.hasNext()) {
			Checkpoint checkpoint = (Checkpoint)iterator.next();
			if (path.equals(checkpoint.getPath())) {
				result = checkpoint;
			}
		}
		return result;
	}

	public void setCheckpoints(List checkpoints) {
		this.checkpoints = checkpoints;
	}

	public List getCheckpoints() {
		return checkpoints;
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
	
	public void setCounterpart(Report report) {
		counterpart = report;
	}
	
	public Report getCounterpart() {
		return counterpart;
	}
	
	public void setDifferenceChecked(boolean differenceChecked) {
		this.differenceChecked = differenceChecked;
	}
	
	public boolean getDifferenceChecked() {
		return differenceChecked;
	}

	public void setDifferenceFound(boolean differenceFound) {
		this.differenceFound = differenceFound;
	}
	
	public boolean getDifferenceFound() {
		return differenceFound;
	}

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
		List checkpoints = new ArrayList();
		for (int i = 0; i < this.checkpoints.size(); i++) {
			Checkpoint checkpoint = (Checkpoint)this.checkpoints.get(i);
			checkpoint = (Checkpoint)checkpoint.clone();
			checkpoint.setReport(report);
			checkpoints.add(checkpoint);
		}
		report.setCheckpoints(checkpoints);
		return report;
	}
	 
	public String toString() {
		return name;
//		String result = null;
//		if (reportXmlTransformer == null) {
//			result = name;
//		} else {
//			ReportNameHandler reportNameHandler = new ReportNameHandler();
//			try {
//				XMLReader parser = XMLReaderFactory.createXMLReader();
//				parser.setContentHandler(reportNameHandler);
//				parser.parse(new InputSource(new StringReader(toXml())));
//			} catch (SAXException e) {
//				log.debug("SAXException reading report name: " + e.getMessage());
//				result = reportNameHandler.getReportName();
//			} catch (IOException e) {
//				log.error("IOException reading report name: " + e.getMessage());
//			}
//			if (result == null) {
//				result = name;
//			}
//		}
//		return result;
	}

//	private class ReportNameHandler extends DefaultHandler {
//		String reportName;
//
//		public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
//			if ("Report".equals(localName)) {
//				reportName = attributes.getValue("Name");
//			}
//			throw new SAXException("No further reading needed");
//		}
//		
//		public String getReportName() {
//			return reportName;
//		}
//
//	}
	
	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}
	
	public String toXml() {
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
			Iterator iterator = checkpoints.iterator();
			while (iterator.hasNext()) {
				Checkpoint checkpoint = (Checkpoint)iterator.next();
	
				Object object = checkpoint.getMessage();
				if (object != null) {
					stringBuffer.append("<Checkpoint");
					stringBuffer.append(" Name=\"" + EscapeUtil.escapeXml(checkpoint.getName()) + "\"");
					stringBuffer.append(" Type=\"" + EscapeUtil.escapeXml(checkpoint.getTypeAsString()) + "\"");
					stringBuffer.append(" Level=\"" + checkpoint.getLevel() + "\"");
					String message = object.toString();
					Document document;
					try {
						document = DocumentHelper.parseText(message);
					} catch (DocumentException e) {
						document = null;
					}
					if (document == null) {
						stringBuffer.append(">");
						stringBuffer.append(EscapeUtil.escapeXml(message));
						stringBuffer.append("</Checkpoint>");
					} else {
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
						stringBuffer.append("</Checkpoint>");
					}
				}
			}
			stringBuffer.append("</Report>");
			xml = stringBuffer.toString();
			if (reportXmlTransformer != null) {
				xml = reportXmlTransformer.transform(xml);
			}
		}
		return xml;
	}
	
	private String getCheckpointLogDescription(String name, int type, Integer level) {
		return "(name: " + name + ", type: " + Checkpoint.getTypeAsString(type) + ", level: " + level + ", correlationId: " + correlationId + ")";
	}

}
