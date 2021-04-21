/*
   Copyright 2021 WeAreFrank!

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

import nl.nn.testtool.Report;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.run.RunResult;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/runner")
public class RunApi extends ApiBase {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Rerun the given reports, and save the output the target storage.
	 *
	 * @param debugStorageParam Target storage to use as debug storage for re-runner.
	 * @param sources Map containing storage names and storage ids for reports that will re-run.
	 */
	@POST
	@Path("/run/{debugStorage}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response runReport(@PathParam("debugStorage") String debugStorageParam, Map<String, List<Integer>> sources) {
		Storage debugStorage = getBean(debugStorageParam);
		List<Report> reports = new ArrayList<>();
		List<String> exceptions = new ArrayList<>();
		for (String storageParam : sources.keySet()) {
			Storage storage = getBean(storageParam);
			for (int storageId : sources.get(storageParam)) {
				try {
					reports.add(storage.getReport(storageId));
				} catch (StorageException e) {
					String message = "Exception for report in [" + storageParam + "] with storage id [" + storageId + "]: " + e.getMessage();
					exceptions.add(message);
					logger.error(message, e);
				}
			}
		}

		ReportRunner runner = getRunner(debugStorage);
		String exception = runner.run(reports, true, true);

		if (exceptions.size() > 0) {
			String message = "Following exceptions were thrown, causing the related reports not to run. " + String.join(". ", exceptions);
			return Response.serverError().entity(message).build();
		} else if (exception != null) {
			return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(exception).build();
		}
		return Response.ok(runner.getResults()).build();
	}

	/**
	 * Get the results from the re-runner for the given debug storage.
	 * @param debugStorageParam Name of the debug storage that runner uses.
	 */
	@GET
	@Path("/result/{debugStorage}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResults(@PathParam("debugStorage") String debugStorageParam) {
		Storage debugStorage = getBean(debugStorageParam);
		ReportRunner runner = getRunner(debugStorage);

		Map<Integer, RunResult> results = runner.getResults();
		if (results == null || results.size() == 0)
			return Response.noContent().build();

		return Response.ok(results).build();
	}

	/**
	 * Resets all the report runners.
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
	 * @param storageParam Name of the debug storage.
	 */
	@POST
	@Path("/reset/{debugStorage}")
	public Response resetRunner(@PathParam("debugStorage") String storageParam) {
		Storage storage = getBean(storageParam);
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
			runner.setTestTool(getBean("testTool"));
			runner.setDebugStorage(debugStorage);
			runner.setSecurityContext(this);
			runners.put(debugStorage, runner);
		}

		return runner;
	}
}
