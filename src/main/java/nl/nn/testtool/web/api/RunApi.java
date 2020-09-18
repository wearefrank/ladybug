package nl.nn.testtool.web.api;

import nl.nn.testtool.Report;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.run.RunResult;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.LogUtil;
import org.apache.log4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunApi extends ApiBase {
	private static final Logger logger = LogUtil.getLogger(RunApi.class);

	@Context
	private HttpServletRequest httpRequest;

	@POST
	@Path("/runner/run/{debugStorage}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
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
		runner.run(reports, false, false); // ???
		if (exceptions.size() > 0) {
			String message = "Following exceptions were thrown, causing the related reports not to run. " + String.join(". ", exceptions);
			return Response.serverError().entity(message).build();
		}

		return Response.ok().build();
	}

	@GET
	@Path("/runner/result/{debugStorage}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResults(@PathParam("debugStorage") String debugStorageParam) {
		Storage debugStorage = getBean(debugStorageParam);
		ReportRunner runner = getRunner(debugStorage);

		Map<Integer, RunResult> results = runner.getResults();
		if (results == null || results.size() == 0)
			return Response.noContent().build();

		return Response.ok(results).build();
	}

	@POST
	@Path("/runner/reset")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Response resetAll() {
		Object sessionAttr = httpRequest.getSession().getAttribute("reportRunner");
		if (!(sessionAttr instanceof Map)) {
			httpRequest.getSession().setAttribute("reportRunner", new HashMap<Object, Object>());
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

	@POST
	@Path("/runner/reset/{debugStorage}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Response resetRunner(@PathParam("debugStorage") String storageParam) {
		Storage storage = getBean(storageParam);
		ReportRunner runner = getRunner(storage);
		runner.reset();
		return Response.ok().build();
	}

	private ReportRunner getRunner(Storage debugStorage) {
		Map<Object, Object> runners;

		Object sessionAttr = httpRequest.getSession().getAttribute("reportRunner");
		if (sessionAttr instanceof Map) {
			runners = (Map) sessionAttr;
		} else {
			runners = new HashMap<>();
			httpRequest.getSession().setAttribute("reportRunner", runners);
		}

		ReportRunner runner = (ReportRunner) runners.get(debugStorage);
		if (runner == null) {
			runner = new ReportRunner();
			runner.setTestTool(getBean("testTool"));
			runner.setDebugStorage(debugStorage);
			runner.setSecurityContext(null); // ????
			runners.put(debugStorage, runner);
		}

		return runner;
	}
}
