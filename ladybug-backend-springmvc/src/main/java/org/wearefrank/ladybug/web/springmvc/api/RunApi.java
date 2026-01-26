/*
   Copyright 2025 WeAreFrank!

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
package org.wearefrank.ladybug.web.springmvc.api;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.run.ReportRunner;
import org.wearefrank.ladybug.run.RunResult;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.wearefrank.ladybug.web.common.Util.fullMessage;

@RestController
@RequestMapping("/runner")
@RolesAllowed("IbisTester")
public class RunApi {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter @Autowired TestTool testTool;
	private @Setter @Autowired ReportXmlTransformer reportXmlTransformer;

	@PostMapping(value = "/run/{storageName}/{storageId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> runReport(@PathVariable("storageName") String storageName, @PathVariable("storageId") int storageId) {
		Map<String, Object> result = new HashMap<>();
		String errorMessage = null;
		try {
			Report report = testTool.getStorage(storageName).getReport(storageId);
			if (report != null) {
				report.setTestTool(testTool);
				ReportRunner runner = new ReportRunner();
				runner.setTestTool(testTool);
				runner.setDebugStorage(testTool.getDebugStorage());
				errorMessage = runner.run(Collections.singletonList(report), true, true);
				if (errorMessage == null) {
					RunResult runResult = runner.getResults().get(storageId);
					if (runResult.errorMessage == null) {
						Report runResultReport = runner.getRunResultReport(runResult.correlationId);
						if (runResultReport == null) {
							errorMessage = "Rerunning did not produce a new report because the report generator was disabled";
						} else {
							runResultReport.setTestTool(testTool);
							result = extractRunResult(report, runResultReport, runner);
						}
					} else {
						errorMessage = runResult.errorMessage;
					}
				}
			}
		} catch (StorageException e) {
			errorMessage = "Storage exception: " + fullMessage(e);
			log.error(errorMessage, e);
		}
		if (errorMessage != null) {
			return ResponseEntity.internalServerError().body(errorMessage);
		}
		return ResponseEntity.ok(result);
	}

	private Map<String, Object> extractRunResult(Report report, Report runResultReport, ReportRunner runner) {
		Map<String, Object> res = new HashMap<>();
		report.setGlobalReportXmlTransformer(reportXmlTransformer);
		runResultReport.setGlobalReportXmlTransformer(reportXmlTransformer);
		runResultReport.setTransformation(report.getTransformation());
		runResultReport.setReportXmlTransformer(report.getReportXmlTransformer());
		res.put("info", ReportRunner.getRunResultInfo(report, runResultReport));
		res.put("equal", report.toXml(runner).equals(runResultReport.toXml(runner)));
		res.put("originalReport", report);
		res.put("runResultReport", runResultReport);
		res.put("originalXml", report.toXml(runner));
		res.put("runResultXml", runResultReport.toXml(runner));
		return res;
	}
}
