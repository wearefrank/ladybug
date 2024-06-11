/*
   Copyright 2021-2024 WeAreFrank!

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
package nl.nn.testtool.web.api;

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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Setter;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.run.RunResult;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.web.ApiServlet;

@Path("/" + ApiServlet.LADYBUG_API_PATH + "/runner")
public class RunApi extends ApiBase {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter @Inject @Autowired TestTool testTool;
	private @Setter @Inject @Autowired ReportXmlTransformer reportXmlTransformer;

	/**
	 * Rerun the given report, and save the output the target storage.
	 *
	 * @param storageId the id of the report in the testStorage to run
	 * @param testStorageName Name of the test storage.
	 * @param debugStorageName Name of the debug storage.
	 * @return The response of running the report.
	 */
	@POST
	@Path("/run/{testStorageName}/{debugStorageName}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response runReport(@PathParam("storageId") int storageId, @PathParam("testStorageName") String testStorageName, @PathParam("debugStorageName") String debugStorageName) {
		Storage testStorage = testTool.getStorage(testStorageName);
		ReportRunner runner = getRunner(testTool.getStorage(debugStorageName));
		Map<String, Object> result = new HashMap<>();
		// Reran reports will allow us to keep track of old reran reports. This will later be used in replace and result.
		HashMap<Integer, Report> reranReports = getSessionAttr("reranReports", false);
		if (reranReports == null) {
			reranReports = new HashMap<>();
			setSessionAttr("reranReports", reranReports);
		}
		String errorMessage = null;
		try {
			Report report = testStorage.getReport(storageId);
			if (report != null) {
				report.setTestTool(testTool);
				reranReports.put(storageId, report);
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

	@PUT
	@Path("/replace/{debugStorage}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response runReport(@PathParam("debugStorage") String storageName, @PathParam("storageId") int storageId) {
		try {
			// Get run result report.
			// TODO: Rename debugStorageStorageParam to debugStorageName and use testTool.getStorage(debugStorageName)
			// (frontend needs to be changed to use storage name instead of bean name)
			Storage debugStorage = testTool.getStorage(storageName);
			ReportRunner reportRunner = getRunner(debugStorage);
			Report runResultReport = reportRunner.getRunResultReport(reportRunner.getResults().get(storageId).correlationId);

			// Get original report.
			HashMap<Integer, Report> reranReports = getSessionAttr("reranReports", true);
			Report report = reranReports.get(storageId);

			// Apply transformations, etc
			log.debug("Replacing report [" + report.getStorage().getName() + ":" + report.getStorageId() + "] " +
					"with [" + debugStorage.getName() + ":" + runResultReport.getStorageId() + "]");
			runResultReport.setTestTool(report.getTestTool());
			runResultReport.setName(report.getName());
			runResultReport.setDescription(report.getDescription());
			if (report.getCheckpoints().get(0).containsVariables()) {
				runResultReport.getCheckpoints().get(0).setMessage(report.getCheckpoints().get(0).getMessage());
			}
			runResultReport.setPath(report.getPath());
			runResultReport.setTransformation(report.getTransformation());
			runResultReport.setReportXmlTransformer(report.getReportXmlTransformer());
			runResultReport.setVariableCsvWithoutException(report.getVariableCsv());
			runResultReport.setStorageId(report.getStorageId());

			((CrudStorage) report.getStorage()).update(runResultReport);
			reportRunner.getResults().remove(storageId);
			reranReports.remove(storageId);
			return Response.ok(runResultReport).build();
		} catch (StorageException e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Exception while replacing report with storage id [" + storageId + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}
	/**
	 * Resets all the report runners.
	 * @return The response after resetting the reports.
	 */
	@POST
	@Path("/reset")
	public Response resetAll() {
		Object sessionAttr = getSessionAttr("reportRunner", false);
		if (!(sessionAttr instanceof Map)) {
			setSessionAttr("reportRunner", new HashMap<Object, Object>());
			return Response.ok().build();
		}

		Map<Object, Object> map = (Map) sessionAttr;
		for (Object key : map.keySet()) {
			Object val = map.get(key);
			if (val instanceof ReportRunner) {
				ReportRunner runner = (ReportRunner) val;
				runner.reset();
			} else {
				map.remove(key);
			}
		}
		return Response.ok().build();
	}

	/**
	 * Resets the re-runner with the given debug storage.
	 * @param storageName Name of the debug storage.
	 * @return The response after resetting.
	 */
	@POST
	@Path("/reset/{debugStorage}")
	public Response resetRunner(@PathParam("debugStorage") String storageName) {
		Storage storage = testTool.getStorage(storageName);
		ReportRunner runner = getRunner(storage);
		runner.reset();
		return Response.ok().build();
	}

	/**
	 * Return from the map, or generate report re-runner with the given debug storage.
	 * With the API, each session can create multiple re-runners for different debug storages.
	 *
	 * @param debugStorage Debug storage.
	 * @return ReportRunner that uses the given debug storage.
	 */
	private ReportRunner getRunner(Storage debugStorage) {
		Map<Object, Object> runners;

		Object sessionAttr = getSessionAttr("reportRunner", false);
		if (sessionAttr instanceof Map) {
			runners = (Map) sessionAttr;
		} else {
			runners = new HashMap<>();
			setSessionAttr("reportRunner", runners);
		}

		ReportRunner runner = (ReportRunner) runners.get(debugStorage);
		if (runner == null) {
			runner = new ReportRunner();
			runner.setTestTool(testTool);
			runner.setDebugStorage(debugStorage);
			runner.setSecurityContext(this);
			runners.put(debugStorage, runner);
		}

		return runner;
	}
}
