package nl.nn.testtool.web.api;

import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.transform.ReportXmlTransformer;
import org.apache.commons.lang.StringUtils;

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
public class TestToolApi extends ApiBase {
	private static ReportXmlTransformer reportXmlTransformer;

	/**
	 * @return Response containing test tool data.
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInfo() {
		TestTool testTool = getBean("testTool");
		HashMap<String, Object> map = new HashMap<>(4);
		map.put("generatorEnabled", testTool.isReportGeneratorEnabled());
		map.put("estMemory", testTool.getReportsInProgressEstimatedMemoryUsage());
		map.put("regexFilter", testTool.getRegexFilter());
		map.put("reportsInProgress", testTool.getNumberOfReportsInProgress());
		map.put("stubStrategies", testTool.getStubStrategies());
		return Response.ok(map).build();
	}

	/**
	 * Change settings of the testtool.
	 *
	 * @param map New settings that can contain (generatorEnabled, regexFilter)
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response setInfo(Map<String, String> map) {
		TestTool testTool = getBean("testTool");
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

	/**
	 * Returns the reports in progress.
	 *
	 * @param count Maximum number of reports to return.
	 * @return Response containing a list of reports.
	 */
	@GET
	@Path("/in-progress/{count}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReportsInProgress(@PathParam("count") long count) {
		TestTool testTool = getBean("testTool");
		count = Math.min(count, testTool.getNumberOfReportsInProgress());
		if (count == 0)
			return Response.noContent().build();

		ArrayList<Report> reports = new ArrayList<>(((Number) count).intValue());
		for (int i = 0; i < count; i++)
			reports.add(testTool.getReportInProgress(i));

		return Response.ok(reports).build();
	}

	/**
	 * Change the default transformation.
	 *
	 * @param map Map containing key "transformation"
	 */
	@POST
	@Path("/transformation/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateReportTransformation(Map<String, String> map) {
		String transformation = map.get("transformation");
		if (StringUtils.isEmpty(transformation))
			return Response.status(Response.Status.BAD_REQUEST).build();

		getReportXmlTransformer().setXslt(transformation);
		return Response.ok().build();
	}

	/**
	 * @return Response containing the current default transformation of the test tool.
	 */
	@GET
	@Path("/transformation")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateReportTransformation() {
		String transformation = getReportXmlTransformer().getXslt();
		if (StringUtils.isEmpty(transformation))
			return Response.noContent().build();

		Map<String, String> map = new HashMap<>(1);
		map.put("transformation", transformation);
		return Response.ok(map).build();
	}

	/**
	 * @return The bean named reportXmlTransformer
	 */
	public ReportXmlTransformer getReportXmlTransformer() {
		if (reportXmlTransformer == null)
			reportXmlTransformer = getBean("reportXmlTransformer");

		return reportXmlTransformer;
	}
}
