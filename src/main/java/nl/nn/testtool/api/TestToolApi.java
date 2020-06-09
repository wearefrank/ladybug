//package nl.nn.testtool.api;
//
//import nl.nn.testtool.Report;
//import nl.nn.testtool.TestTool;
//import nl.nn.testtool.util.LogUtil;
//import org.apache.commons.lang.StringUtils;
//import org.apache.log4j.Logger;
//
//import javax.annotation.security.RolesAllowed;
//import javax.ws.rs.GET;
//import javax.ws.rs.POST;
//import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
//import javax.ws.rs.Produces;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//import java.util.ArrayList;
//import java.util.HashMap;
//
///*
// - POST report filter
// */
//@Path("/testtool")
//public class TestToolApi {
//	private static TestTool testTool;
//	private static final Logger logger = LogUtil.getLogger(TestToolApi.class);
//
//
//	@GET
//	@Path("/")
//	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
//	@Produces(MediaType.APPLICATION_JSON)
//	public Response getInfo() {
//		HashMap<String, Object> map = new HashMap<>(4);
//		map.put("generatorEnabled", testTool.isReportGeneratorEnabled());
//		map.put("estMemory", testTool.getReportsInProgressEstimatedMemoryUsage());
//		map.put("regexFilter", testTool.getRegexFilter());
//		map.put("reportsInProgress", testTool.getNumberOfReportsInProgress());
//		return Response.ok(map).build();
//	}
//
//	@GET
//	@Path("/in-progress/{count}")
//	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
//	@Produces(MediaType.APPLICATION_JSON)
//	public Response getReportsInProgress(@PathParam("count") long count) {
//		count = Math.min(count, testTool.getNumberOfReportsInProgress());
//		if (count == 0)
//			return Response.noContent().build();
//
//		ArrayList<Report> reports = new ArrayList<>(((Number) count).intValue());
//		for (int i = 0; i < count; i++)
//			reports.add(testTool.getReportInProgress(i));
//
//		return Response.ok(reports).build();
//	}
//
//	@POST
//	@Path("/enable-generation/{enabled}")
//	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
//	public Response enableGeneration(@PathParam("enabled") String enabled) {
//		if (StringUtils.isEmpty(enabled))
//			return Response.status(Response.Status.BAD_REQUEST).build();
//
//		testTool.setReportGeneratorEnabled("1".equalsIgnoreCase(enabled) || "true".equalsIgnoreCase(enabled));
//		return Response.ok().build();
//	}
//
//	public static void setTestTool(TestTool testTool) {
//		TestToolApi.testTool = testTool;
//	}
//}
