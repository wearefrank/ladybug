/*
   Copyright 2021-2026 WeAreFrank!

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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

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
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.filter.View;

import org.wearefrank.ladybug.web.common.Constants;
import org.wearefrank.ladybug.web.common.HttpBadRequestException;
import org.wearefrank.ladybug.web.common.HttpInternalServerErrorException;
import org.wearefrank.ladybug.web.common.TestToolApiImpl;

@Path("/" + Constants.LADYBUG_API_PATH + "/testtool")
public class TestToolApi extends ApiBase {
	@Autowired
	private @Setter TestToolApiImpl delegate;

	/**
	 * @return Response containing test tool data.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInfo() {
		Map<String, Object> info = delegate.getTestToolInfo();
		return Response.ok(info).build();
	}

	@GET
	@Path("/reset")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resetInfo() {
		Map<String, Object> info = delegate.resetInfo();
		return Response.ok(info).build();
	}


	/**
	 * Change settings of the testtool.
	 *
	 * @param map New settings that can contain (generatorEnabled, regexFilter)
	 * @return The response after changing the settings.
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateInfo(Map<String, String> map) {
		try {
			delegate.updateInfo(map);
			return Response.ok().build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
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
		try {
			Report result = delegate.getReportsInProgress(index);
			return Response.ok(result).build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
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
		try {
			delegate.deleteReportInProgress(index);
			return Response.ok().build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
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
		return Response.ok(delegate.getReportsInProgressWarningThreshold()).build();
	}

	/**
	 * Change the default transformation.
	 *
	 * @param map Map containing key "transformation"
	 * @return The response after changing the transformation.
	 */
	@POST
	@Path("/transformation")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateReportTransformation(Map<String, String> map) {
		try {
			delegate.updateReportTransformation(map);
			return Response.ok().build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	/**
	 * @param defaultTransformation Boolean to check if we need to use the default transformation
	 * @return Response containing the current default transformation of the test tool.
	 */
	@GET
	@Path("/transformation/{defaultTransformation}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReportTransformation(@PathParam("defaultTransformation") boolean defaultTransformation) {
		Map<String, String> result = delegate.getReportTransformation(defaultTransformation);
		if (result == null) {
			return Response.noContent().build();
		} else {
			return Response.ok(result).build();
		}
	}

	/**
	 * @return The configured views
	 */
	@GET
	@Path("/views")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getViewsResponse() {
		return Response.ok(delegate.getViewsResponse()).build();
	}

	@PUT
	@Path("/node-link-strategy")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changeNodeLinkStrategy(@QueryParam("nodeLinkStrategy") String nodeLinkStrategy, @QueryParam("viewName") String viewName) {
		for (View view : delegate.getViews()) {
			if (viewName.equals(view.getName())) {
				setSessionAttr(view.getName() + ".NodeLinkStrategy", nodeLinkStrategy);
				break;
			}
		}

		return Response.ok().build();
	}

	@GET
	@Path("/stub-strategies")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getPossibleStubStrategies() {
		return Response.ok(delegate.getPossibleStubStrategies()).build();
	}

	@GET
	@Path("/version")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getVersion() {
		return Response.ok(delegate.getVersion()).build();
	}
}
