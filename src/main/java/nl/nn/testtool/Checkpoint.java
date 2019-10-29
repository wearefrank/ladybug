/*
   Copyright 2018 Nationale-Nederlanden

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

import java.io.Serializable;

import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;
import org.apache.ws.security.util.DOM2Writer;
import org.w3c.dom.Node;

/**
 * @author Jaco de Groot
 */
public class Checkpoint implements Serializable, Cloneable {
	// See comment above field serialVersionUID on class Report
	private transient static final long serialVersionUID = 4;
	private transient static Logger log = LogUtil.getLogger(Checkpoint.class);
	private Report report;
	private String threadName;
	private String sourceClassName;
	private String name;
	private String message;
	private int type;
	private int level = 0;
	private boolean messageHasBeenStubbed = false;
	private int stub = STUB_FOLLOW_REPORT_STRATEGY;

	public transient static final int TYPE_NONE = 0;
	public transient static final int TYPE_STARTPOINT = 1;
	public transient static final int TYPE_ENDPOINT = 2;
	public transient static final int TYPE_ABORTPOINT = 3;
	public transient static final int TYPE_INPUTPOINT = 4;
	public transient static final int TYPE_OUTPUTPOINT = 5;
	public transient static final int TYPE_INFOPOINT = 6;
	public transient static final int TYPE_THREADCREATEPOINT = 7;
	public transient static final int TYPE_THREADSTARTPOINT = 8;
	public transient static final int TYPE_THREADENDPOINT = 9;
	public transient static final int STUB_FOLLOW_REPORT_STRATEGY = -1;
	public transient static final int STUB_NO = 0;
	public transient static final int STUB_YES = 1;
	
	public Checkpoint() {
		// Only for Java XML encoding/decoding! Use other constructor instead.
	}

	public Checkpoint(Report report, String threadName, String sourceClassName,
			String name, Object message, int type, int level) {
		this.report = report;
		this.threadName = threadName;
		this.sourceClassName = sourceClassName;
		this.name = name;
		setMessage(message);
		this.type = type;
		this.level = level;
	}

	public void setReport(Report report) {
		this.report = report;
	}
	
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

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setMessage(String message) {
		// report is null when called by XMLDecoder
		if (report != null) {
			int maxMsgLength = report.getTestTool().getMaxMessageLength();
			if(message.length() > maxMsgLength) {
				String cappedMsg = null;
				
				for(Checkpoint checkpoint : report.getCheckpoints()) {
					String olderMsg = checkpoint.getMessage();
					
					if(olderMsg != null && message.startsWith(olderMsg)) {
						cappedMsg = olderMsg;
					}
				}
				if(cappedMsg == null) {
					cappedMsg = message.substring(0, maxMsgLength);
				}
				
				message = cappedMsg + "... (" + (message.length()-maxMsgLength) + " more characters)";
			}
			if(report.getMessageTransformer() != null) {
				message = report.getMessageTransformer().transform(message);
			}
		}
		
		this.message = message;
	}

	public void setMessage(Object message) {
		if (message != null) {
			if (message instanceof Node) {
				Node node = (Node)message;
				setMessage(DOM2Writer.nodeToString(node));
			} else {
				setMessage(message.toString());
			}
		}
	}

	public String getMessage() {
		return message;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public String getTypeAsString() {
		return getTypeAsString(getType());
	}

	public static String getTypeAsString(int type) {
		String typeAsString = null;
		switch (type) {
			case TYPE_NONE : typeAsString = "None"; break;
			case TYPE_STARTPOINT : typeAsString = "Startpoint"; break;
			case TYPE_ENDPOINT : typeAsString = "Endpoint"; break;
			case TYPE_ABORTPOINT : typeAsString = "Abortpoint"; break;
			case TYPE_INPUTPOINT : typeAsString = "Inputpoint"; break;
			case TYPE_OUTPUTPOINT : typeAsString = "Outputpoint"; break;
			case TYPE_INFOPOINT : typeAsString = "Infopoint"; break;
			case TYPE_THREADSTARTPOINT : typeAsString = "ThreadStartpoint"; break;
			case TYPE_THREADENDPOINT : typeAsString = "ThreadEndpoint"; break;
		}
		return typeAsString;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}
	
	public void setMessageHasBeenStubbed(boolean messageHasBeenStubbed) {
		this.messageHasBeenStubbed = messageHasBeenStubbed;
	}

	public boolean getMessageHasBeenStubbed() {
		return messageHasBeenStubbed;
	}

	public void setStub(int stub) {
		this.stub = stub;
	}
	
	public int getStub() {
		return stub;
	}

	public Path getPath() {
		Path path = new Path(level + 1);
		path.setName(level, name);
		int currentLevel = level;
		for (int i = report.getCheckpoints().indexOf(this) - 1; i >= 0; i--) {
			Checkpoint currentCheckpoint = (Checkpoint)report.getCheckpoints().get(i);
			if (currentCheckpoint.getLevel() == currentLevel && currentCheckpoint.getName().equals(name)) {
				path.incrementCount(currentLevel);
			} else if (currentCheckpoint.getLevel() < currentLevel) {
				currentLevel = currentCheckpoint.getLevel();
				path.setName(currentLevel, currentCheckpoint.getName());
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
			return message.length() * 2;
		}
	}

	public Object clone() throws CloneNotSupportedException {
		Checkpoint checkpoint = (Checkpoint)super.clone();
		checkpoint.setReport(null);
		return checkpoint;
	}

	public String toString() {
		return name;
	}

}
