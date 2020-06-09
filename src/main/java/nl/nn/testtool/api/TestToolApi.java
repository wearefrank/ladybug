package nl.nn.testtool.api;

import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.transform.ReportXmlTransformer;
import org.apache.commons.lang.StringUtils;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Path("/testtool")
public class TestToolApi {
	private static TestTool testTool;
	private static ReportXmlTransformer reportXmlTransformer;

	@GET
	@Path("/")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInfo() {
		HashMap<String, Object> map = new HashMap<>(4);
		map.put("generatorEnabled", testTool.isReportGeneratorEnabled());
		map.put("estMemory", testTool.getReportsInProgressEstimatedMemoryUsage());
		map.put("regexFilter", testTool.getRegexFilter());
		map.put("reportsInProgress", testTool.getNumberOfReportsInProgress());
		return Response.ok(map).build();
	}

	@POST
	@Path("/")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Consumes(MediaType.APPLICATION_JSON)
	public Response setInfo(Map<String, String> map) {
		// TODO: Check user roles.
		String generatorEnabled = map.remove("generatorEnabled");
		String regexFilter = map.remove("regexFilter");
		if (map.size() > 0 || (StringUtils.isEmpty(generatorEnabled) && StringUtils.isEmpty(regexFilter)))
			return Response.status(Response.Status.BAD_REQUEST).build();

		if (StringUtils.isNotEmpty(generatorEnabled)) {
			testTool.setReportGeneratorEnabled("1".equalsIgnoreCase(generatorEnabled) || "true".equalsIgnoreCase(generatorEnabled));
			testTool.sendReportGeneratorStatusUpdate();
		}
		if (StringUtils.isNotEmpty(regexFilter))
			testTool.setRegexFilter(regexFilter);

		return Response.ok().build();
	}

	@GET
	@Path("/in-progress/{count}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReportsInProgress(@PathParam("count") long count) {
		count = Math.min(count, testTool.getNumberOfReportsInProgress());
		if (count == 0)
			return Response.noContent().build();

		ArrayList<Report> reports = new ArrayList<>(((Number) count).intValue());
		for (int i = 0; i < count; i++)
			reports.add(testTool.getReportInProgress(i));

		return Response.ok(reports).build();
	}

	@POST
	@Path("/transformation/")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateReportTransformation(Map<String, String> map) {
		String transformation = map.get("transformation");
		if (StringUtils.isEmpty(transformation))
			return Response.status(Response.Status.BAD_REQUEST).build();

		reportXmlTransformer.setXslt(transformation);
		return Response.ok().build();
	}

	@GET
	@Path("/transformation/{storage}/{storageId}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateReportTransformation() {
		String transformation = reportXmlTransformer.getXslt();
		if (StringUtils.isEmpty(transformation))
			return Response.noContent().build();

		Map<String, String> map = new HashMap<>(1);
		map.put("transformation", transformation);
		return Response.ok(map).build();
	}

	public static void setTestTool(TestTool testTool) {
		TestToolApi.testTool = testTool;
	}

	public static void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		TestToolApi.reportXmlTransformer = reportXmlTransformer;
	}
}
