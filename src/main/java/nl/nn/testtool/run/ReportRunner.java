/*
   Copyright 2020, 2022-2023 WeAreFrank!, 2018-2019 Nationale-Nederlanden

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

import lombok.Setter;
import nl.nn.testtool.Checkpoint;
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
	private @Setter TestTool testTool;
	private @Setter Storage debugStorage;
	private @Setter SecurityContext securityContext;
	private List<Report> reportsTodo = new ArrayList<Report>();
	private int maximum = 1;
	private Map<Integer, RunResult> results = Collections.synchronizedMap(new HashMap<Integer, RunResult>());
	private boolean running = false;

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

	public Report getRunResultReport(String runResultCorrelationId) throws StorageException {
		return getRunResultReport(debugStorage, runResultCorrelationId);
	}

	public static Report getRunResultReport(Storage storage, String runResultCorrelationId) throws StorageException {
		Report report = null;
		List<String> metadataNames = new ArrayList<String>();
		metadataNames.add("storageId");
		metadataNames.add("correlationId");
		List<String> searchValues = new ArrayList<String>();
		searchValues.add(null);
		searchValues.add(runResultCorrelationId);
		List<List<Object>> metadata = null;
		// TODO in Reader.getMetadata kun je ook i < numberOfRecords veranderen in result.size() < numberOfRecords zodat je hier 1 i.p.v. -1 mee kunt geven maar als je dan zoekt op iets dat niet te vinden is gaat hij alle records door. misschien debugStorage.getMetadata een extra paremter geven, numberOfRecordsToConsider en numberOfRecordsToReturn i.p.v. numberOfRecords? (let op: logica ook in mem storage aanpassen)
		metadata = storage.getMetadata(-1, metadataNames, searchValues,
				MetadataExtractor.VALUE_TYPE_OBJECT);
		if (metadata != null && metadata.size() > 0) {
			Integer runResultStorageId = (Integer)((List<Object>)metadata.get(0)).get(0);
			report = storage.getReport(runResultStorageId);
		}
		return report;
	}

	public static String getRunResultInfo(Report report, Report runResultReport) {
		int stubbedOrig = 0;
		int stubsNotFoundOrig = 0;
		for (Checkpoint checkpoint : report.getCheckpoints()) {
			if (checkpoint.isStubbed()) {
				stubbedOrig++;
			}
			if (checkpoint.getStubNotFound() != null) {
				stubsNotFoundOrig++;
			}
		}
		int stubbedResult = 0;
		int stubsNotFoundResult = 0;
		String alternativeStubInfo = "";
		int alternativeStubSkipped = 0;
		for (Checkpoint checkpoint : runResultReport.getCheckpoints()) {
			if (checkpoint.isStubbed()) {
				stubbedResult++;
				if (checkpoint.getStubNotFound() != null) {
					if (alternativeStubInfo.length() == 0) {
						alternativeStubInfo = " (Alternative stub used for: "
								+ checkpoint.getStubNotFound();
					} else if (alternativeStubInfo.length() > 200) {
						alternativeStubSkipped++;
					} else {
						alternativeStubInfo = alternativeStubInfo + ", "
								+ checkpoint.getStubNotFound();
					}
				}
			}
			if (checkpoint.getStubNotFound() != null) {
				stubsNotFoundResult++;
			}
		}
		int totalOrig = report.getCheckpoints().size();
		int totalResult = runResultReport.getCheckpoints().size();
		String info = "(" + (report.getEndTime() - report.getStartTime()) + " >> "
				+ (runResultReport.getEndTime() - runResultReport.getStartTime()) + " ms)"
				+ " (" + stubbedOrig + "/" + totalOrig + " >> " + stubbedResult + "/" + totalResult + " stubbed)";
		if (stubsNotFoundOrig > 0 || stubsNotFoundResult > 0) {
			info = info + " (" + stubsNotFoundOrig + "/" + totalOrig + " >> " + stubsNotFoundResult + "/" + totalResult
					+ " stubs not found)";
		}
		if (alternativeStubInfo.length() > 0) {
			info = info + alternativeStubInfo;
			if (alternativeStubSkipped > 0) {
				info = info + " and " + alternativeStubSkipped + " more";
			}
			info = info + ")";
		}
		return info;
	}

}
