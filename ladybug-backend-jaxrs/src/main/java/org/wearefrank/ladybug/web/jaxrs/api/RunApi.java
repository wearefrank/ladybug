/*
   Copyright 2021-2025 WeAreFrank!

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
package org.wearefrank.ladybug.web.jaxrs.api;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Setter;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.run.ReportRunner;
import org.wearefrank.ladybug.run.RunResult;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;

import org.wearefrank.ladybug.web.common.Constants;
import org.wearefrank.ladybug.web.jaxrs.api.ApiBase;

@Path("/" + Constants.LADYBUG_API_PATH + "/runner")
public class RunApi extends ApiBase {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter @Inject @Autowired TestTool testTool;
	private @Setter @Inject @Autowired ReportXmlTransformer reportXmlTransformer;

	@POST
	@Path("/run/{storageName}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response runReport(@PathParam("storageName") String storageName, @PathParam("storageId") int storageId) {
		Map<String, Object> result = new HashMap<>();
		String errorMessage = null;
		try {
			Report report = testTool.getStorage(storageName).getReport(storageId);
			if (report != null) {
				report.setTestTool(testTool);
				ReportRunner runner = new ReportRunner();
				runner.setTestTool(testTool);
				runner.setDebugStorage(testTool.getDebugStorage());
				runner.setSecurityContext(this);
				errorMessage = runner.run(Collections.singletonList(report), true, true);
				if (errorMessage == null) {
					RunResult runResult = runner.getResults().get(storageId);
					if (runResult.errorMessage == null) {
						Report runResultReport = runner.getRunResultReport(runResult.correlationId);
						runResultReport.setTestTool(testTool);
						result = extractRunResult(report, runResultReport, runner);
					} else {
						errorMessage = runResult.errorMessage;
					}
				}
			}
		} catch (StorageException e) {
			errorMessage = "Storage exception: " + e.getMessage();
			log.error(errorMessage, e);
		}
		if (errorMessage != null) {
			return Response.serverError().entity(errorMessage).build();
		}
		return Response.ok(result).build();
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
