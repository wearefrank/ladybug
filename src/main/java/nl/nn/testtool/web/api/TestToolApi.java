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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Setter;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.filter.Views;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.web.ApiServlet;

@Path("/" + ApiServlet.LADYBUG_API_PATH + "/testtool")
public class TestToolApi extends ApiBase {
    private @Setter
    @Inject
    @Autowired TestTool testTool;
    private @Setter
    @Inject
    @Autowired MetadataExtractor metadataExtractor;
    private @Setter
    @Inject
    @Autowired ReportXmlTransformer reportXmlTransformer;
    private @Setter
    @Inject
    @Autowired Views views;
    private String defaultTransformation;

    @PostConstruct
    public void init() {
        defaultTransformation = reportXmlTransformer.getXslt();
    }

    public String getDefaultTransformation() {
        return defaultTransformation;
    }

    /**
     * @return Response containing test tool data.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfo() {
        Map<String, Object> info = getTestToolInfo();
        return Response.ok(info).build();
    }

    @GET
    @Path("/reset")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetInfo() {
        testTool.reset();
        Map<String, Object> info = getTestToolInfo();
        return Response.ok(info).build();
    }

    public Map<String, Object> getTestToolInfo() {
        Map<String, Object> map = new HashMap<>(4);
        map.put("generatorEnabled", testTool.isReportGeneratorEnabled());
        map.put("estMemory", testTool.getReportsInProgressEstimatedMemoryUsage());
        map.put("regexFilter", testTool.getRegexFilter());
        map.put("reportsInProgress", testTool.getNumberOfReportsInProgress());
        map.put("stubStrategies", testTool.getStubStrategies());
        return map;
    }

    /**
     * Change settings of the testtool.
     *
     * @param map New settings that can contain (generatorEnabled, regexFilter)
     * @return The response after changing the settings.
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateInfo(Map<String, String> map) {
        if (map.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("No settings have been provided - detailed error message - The settings that have been provided are " + map).build();
        }

        if (map.size() > 2) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Too many settings have been provided - detailed error message - The settings that have been provided are " + map).build();
        }
        // TODO: Check user roles.
        String generatorEnabled = map.remove("generatorEnabled");
        String regexFilter = map.remove("regexFilter");

        if (StringUtils.isNotEmpty(generatorEnabled)) {
            testTool.setReportGeneratorEnabled("1".equalsIgnoreCase(generatorEnabled) || "true".equalsIgnoreCase(generatorEnabled));
            testTool.sendReportGeneratorStatusUpdate();
        }
        if (StringUtils.isNotEmpty(regexFilter))
            testTool.setRegexFilter(regexFilter);

        return Response.ok().build();
    }

    /**
     * Returns the specified report in progress.
     *
     * @param index Index of the report to return
     * @return Response containing the specified report, if present.
     */
    @GET
    @Path("/in-progress/{index}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReportsInProgress(@PathParam("index") int index) {
        if (index == 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("No progresses have been queried [" + index + "] and/or are available [" + testTool.getNumberOfReportsInProgress() + "]").build();

        try {
            Report report = testTool.getReportInProgress(index - 1);
            return Response.ok(report).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not find report in progress with index [" + index + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
        }
    }

    /**
     * Removes the report in progress
     *
     * @param index Index of the report to be deleted
     * @return Response confirming the delete, if report is present
     */
    @DELETE
    @Path("/in-progress/{index}")
    public Response deleteReportInProgress(@PathParam("index") int index) {
        if (index == 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("No progresses have been queried [" + index + "] or are available [" + testTool.getNumberOfReportsInProgress() + "]").build();

        try {
            testTool.removeReportInProgress(index - 1);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not find report in progress with index [" + index + "] :: " + e + Arrays.toString(e.getStackTrace())).build();
        }
    }

    /**
     * Gets the report in progress warning threshold time
     *
     * @return Response containing the time it will take before ladybug shows a warning that a report is still in progress
     */
    @GET
    @Path("/in-progress/threshold-time")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReportsInProgressWarningThreshold() {
        return Response.ok(testTool.getReportsInProgressThreshold()).build();
    }

    /**
     * Change the default transformation.
     *
     * @param map Map containing key "transformation"
     * @return The response after changing the transformation.
     */
    @POST
    @Path("/transformation/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateReportTransformation(Map<String, String> map) {
        String transformation = map.get("transformation");
        if (StringUtils.isEmpty(transformation))
            return Response.status(Response.Status.BAD_REQUEST).entity("No transformation has been provided").build();

        reportXmlTransformer.setXslt(transformation);
        return Response.ok().build();
    }

    /**
     * @param defaultTransformation Boolean to check if we need to use the default transformation
     * @return Response containing the current default transformation of the test tool.
     */
    @GET
    @Path("/transformation/{defaultTransformation}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReportTransformation(@PathParam("defaultTransformation") boolean defaultTransformation) {
        String transformation;

        if (defaultTransformation) {
            transformation = getDefaultTransformation();
        } else {
            transformation = reportXmlTransformer.getXslt();
        }

        if (StringUtils.isEmpty(transformation))
            return Response.noContent().build();

        Map<String, String> map = new HashMap<>(1);
        map.put("transformation", transformation);
        return Response.ok(map).build();
    }

    /**
     * @return The configured views
     */
    @GET
    @Path("/views/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getViewsResponse() {
        // Starting from CXF 3.2.0 the setViews() will not be called by Spring when the name of this method is
        // getViews() instead of getViewsResponse() (with CXF 3.1.18 this was not the case) (maybe Spring's
        // ExtendedBeanInfo isn't used anymore with newer CXF versions)
        Map<String, Map<String, Object>> response = new LinkedHashMap<>();
        for (View view : views) {
            response.put(view.getName(), view.toHashMap(view == views.getDefaultView()));
        }
        return Response.ok(response).build();
    }

    @PUT
    @Path("/node-link-strategy")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeNodeLinkStrategy(@QueryParam("nodeLinkStrategy") String nodeLinkStrategy, @QueryParam("viewName") String viewName) {
        for (View view : views) {
            if (viewName.equals(view.getName())) {
                setSessionAttr(view.getName() + ".NodeLinkStrategy", nodeLinkStrategy);
                break;
            }
        }

        return Response.ok().build();
    }

}
