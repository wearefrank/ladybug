/*
   Copyright 2018-2019 Nationale-Nederlanden

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
package nl.nn.testtool.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;

/**
 * @author Jaco de Groot
 */
public class ReportRunner implements Runnable {
	private TestTool testTool;
	private SecurityContext securityContext;
	private List<Report> reportsTodo = new ArrayList<Report>();
	private int maximum = 1;
	private Map<Integer, RunResult> results = Collections.synchronizedMap(new HashMap<Integer, RunResult>());
	private boolean running = false;
	private Storage debugStorage;

	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setDebugStorage(Storage debugStorage) {
		this.debugStorage = debugStorage;
	}

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	public synchronized String run(List<Report> reports, boolean reset, boolean wait) {
		if (running) {
			return "Already running!";
		} else {
			if (reset) {
				reset();
			}
			running = true;
			reportsTodo.addAll(reports);
			maximum = reportsTodo.size();
			if (wait) {
				run();
			} else {
				Thread thread = new Thread(this);
				thread.start();
			}
			return null;
		}
	}

	public synchronized String reset() {
		if (running) {
			return "Still running!";
		} else {
			results.clear();
			return null;
		}
	}

	@Override
	public void run() {
		for (Report report : reportsTodo) {
			run(report);
		}
		reportsTodo.clear();
		running = false;
	}

	private void run(Report report) {
		RunResult runResult = new RunResult();
		runResult.correlationId = TestTool.getCorrelationId();
 		runResult.errorMessage = testTool.rerun(runResult.correlationId, report, securityContext, this);
 		runResult.fullPath = report.getFullPath();
		results.put(report.getStorageId(), runResult);
	}

	public int getMaximum() {
		return maximum;
	}

	public int getProgressValue() {
		return results.size();
	}

	public Map<Integer, RunResult> getResults() {
		return results;
	}

	public Report getRunResultReport(String runResultCorrelationId)
			throws StorageException {
		Report report = null;
		List<String> metadataNames = new ArrayList<String>();
		metadataNames.add("storageId");
		metadataNames.add("correlationId");
		List<String> searchValues = new ArrayList<String>();
		searchValues.add(null);
		searchValues.add(runResultCorrelationId);
		List<List<Object>> metadata = null;
		// TODO in Reader.getMetadata kun je ook i < numberOfRecords veranderen in result.size() < numberOfRecords zodat je hier 1 i.p.v. -1 mee kunt geven maar als je dan zoekt op iets dat niet te vinden is gaat hij alle records door. misschien debugStorage.getMetadata een extra paremter geven, numberOfRecordsToConsider en numberOfRecordsToReturn i.p.v. numberOfRecords? (let op: logica ook in mem storage aanpassen)
		metadata = debugStorage.getMetadata(-1, metadataNames, searchValues,
				MetadataExtractor.VALUE_TYPE_OBJECT);
		if (metadata != null && metadata.size() > 0) {
			Integer runResultStorageId = (Integer)((List<Object>)metadata.get(0)).get(0);
			report = debugStorage.getReport(runResultStorageId);
		}
		return report;
	}

	public Storage getDebugStorage() {
		return debugStorage;
	}
}
