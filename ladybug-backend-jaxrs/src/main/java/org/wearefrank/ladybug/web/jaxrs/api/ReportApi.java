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

import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.extensions.CustomReportAction;
import org.wearefrank.ladybug.filter.Views;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;
import org.wearefrank.ladybug.util.ExportResult;
import org.wearefrank.ladybug.web.common.Constants;
import org.wearefrank.ladybug.web.common.HttpBadRequestException;
import org.wearefrank.ladybug.web.common.HttpInternalServerErrorException;
import org.wearefrank.ladybug.web.common.HttpNotFoundException;
import org.wearefrank.ladybug.web.common.HttpNotImplementedException;
import org.wearefrank.ladybug.web.common.ReportApiImpl;
import org.wearefrank.ladybug.web.common.ReportUpdateRequest;

@Path("/" + Constants.LADYBUG_API_PATH + "/report")
public class ReportApi extends ApiBase {
	@Autowired
	private @Setter ReportApiImpl delegate;

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Setter @Inject @Autowired TestTool testTool;
	private @Setter @Inject @Autowired ReportXmlTransformer reportXmlTransformer;
	private @Setter @Inject @Autowired Views views;
	private @Setter @Inject @Autowired Optional<CustomReportAction> customReportAction;

	/**
	 * Returns the report details for the given storage and id.
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @param xml True if Xml of the report needs to be returned.
	 * @param globalTransformer True if reportXmlTransformer should be set for the report.
	 * @return A response containing serialized Report object.
	 */
	@GET
	@Path("/{storage}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReport(@PathParam("storage") String storageName,
							  @PathParam("storageId") int storageId,
							  @QueryParam("xml") @DefaultValue("false") boolean xml,
							  @QueryParam("globalTransformer") @DefaultValue("false") boolean globalTransformer) {
		try {
			Map<String, Object> result = delegate.getReport(storageName, storageId, xml, globalTransformer);
			return Response.ok(result).build();
		} catch(HttpNotFoundException e) {
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	/**
	 * Get a list of uids for the checkpoints of a specific report. Currently only implementing getting the uids of the
	 * checkpoints to show or hide according to a specific view (trying to apply the best practice that path params are
	 * used to identify a specific resource or resources, while query parameters are used to sort/filter those resources
	 * (see https://stackoverflow.com/questions/30967822/when-do-i-use-path-params-vs-query-params-in-a-restful-api)
	 * 
	 * @param storageName ...
	 * @param storageId   ...
	 * @param viewName    name of the view that determines which checkpoints to show/return and which to hide/exclude
	 * @param invert      when true return the checkpoints to hide and exclude the checkpoint to show
	 * @return            ...
	 */
	@GET
	@Path("/{storage}/{storageId}/checkpoints/uids")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCheckpointUids(	@PathParam("storage") String storageName,
										@PathParam("storageId") int storageId,
										@QueryParam("view") String viewName,
										@QueryParam("invert") boolean invert
										) {
		try {
			List<String> result = delegate.getCheckpointUids(storageName, storageId, viewName, invert);
			return Response.ok(result).build();
		}
		catch(HttpNotFoundException e) {
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	/**
	 * Returns the reports for the given storage and ids.
	 *
	 * @param storageName Name of the storage.
	 * @param storageIds Storage id of the report.
	 * @param xml True if Xml of the report needs to be returned.
	 * @param globalTransformer True if reportXmlTransformer should be set for the report.
	 * @return A response containing serialized Report object.
	 */
	@GET
	@Path("/{storage}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReports(@PathParam("storage") String storageName,
							   @QueryParam("storageIds") List<Integer> storageIds,
							   @QueryParam("xml") @DefaultValue("false") boolean xml,
							   @QueryParam("globalTransformer") @DefaultValue("false") boolean globalTransformer) {
		try {
			Map<Integer, Map<String, Object>> result = delegate.getReports(storageName, storageIds, xml, globalTransformer);
			return Response.ok(result).build();
		} catch(HttpNotFoundException e) {
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	/**
	 * Deletes the report.
	 *
	 * @param storageName Name of the storage.
	 * @param storageIds  Storage id's of the reports to delete
	 * @return "Ok" if deleted properly, "Not implemented" if storage does not support deletion, "Not found" if report does not exist.
	 */
	@DELETE
	@Path("/{storage}")
	public Response deleteReport(@PathParam("storage") String storageName, @QueryParam("storageIds") List<Integer> storageIds) {
		try {
			delegate.deleteReport(storageName, storageIds);
		} catch(HttpNotImplementedException e) {
			return Response.status(Response.Status.NOT_IMPLEMENTED).entity(e.getMessage()).build();
		} catch(HttpNotFoundException e) {
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		return Response.ok().build();
	}

	@DELETE
	@Path("/all/{storage}")
	public Response deleteAllReports(@PathParam("storage") String storageName) {
		try {
			delegate.deleteAllReports(storageName);
			return Response.ok().build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	/**
	 * Get the n latest reports in the storage.
	 *
	 * @param viewName Name of the storage.
	 * @param number Number of latest reports to retrieve.
	 * @return the n latest reports.
	 */
	@GET
	@Path("/latest/{viewName}/{numberOfReports}")
	public Response getLatestReports(@PathParam("viewName") String viewName, @PathParam("numberOfReports") int number) {
		try {
			List<Report> result = delegate.getLatestReports(viewName, number);
			return Response.ok(result).build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	/**
	 * Update the report with the given values..
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @param req Bean with field updates.
	 * @return The updated report.
	 */
	@POST
	@Path("/{storage}/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateReport(@PathParam("storage") String storageName, @PathParam("storageId") int storageId, ReportUpdateRequest req) {
		try {
			Map<String, Serializable> result = delegate.updateReport(storageName, storageId, req);
			return Response.ok(result).build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch(HttpNotFoundException e) {
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	/**
	 * Return transformation of a report.
	 *
	 * @param storageName Name of the storage.
	 * @param storageId Storage id of the report.
	 * @return Response containing a map containing transformation.
	 */
	@GET
	@Path("/transformation/{storage}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReportTransformation(@PathParam("storage") String storageName, @PathParam("storageId") int storageId) {
		try {
			Map<String, String> result = delegate.getReportTransformation(storageName, storageId);
			return Response.ok(result).build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	/**
	 * Copy the reports from the given storages and ids to the given target storage.
	 *
	 * @param storageName Name of the target storage.
	 * @param sources Map [String, Integer] where keys are storage names and integers are storage ids for the reports to be copied.
	 * @return The copied report.
	 */
	@PUT
	@Path("/store/{storage}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response copyReport(@PathParam("storage") String storageName, Map<String, List<Integer>> sources) {
		try {
			List<Report> result = delegate.copyReport(storageName, sources);
			return Response.ok(result).build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	/**
	 * Upload the given report to storage.
	 *
	 * @param storageName Name of the target storage.
	 * @param attachment Attachment containing report.
	 * @return The response of uploading a file.
	 */
	@POST
	@Path("/upload/{storage}")
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@PathParam("storage") String storageName, @Multipart("file") Attachment attachment) {
		try {
			delegate.uploadFile(storageName, () -> {
				String filename = attachment.getContentDisposition().getParameter("filename");
				InputStream in = attachment.getObject(InputStream.class);
				return new ReportApiImpl.AttachmentBeingRead(filename, in);
			});
			return Response.ok().build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	/**
	 * Uploads the given report to in-memory storage it, parses it and then returns the report in json format.
	 *
	 * @param attachment Attachment containing report.
	 * @return List of serialized report objects.
	 */
	@POST
	@Path("/upload")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response getFileReport(@Multipart("file") Attachment attachment) {
		try {
			List<Report> result = delegate.getFileReport(() -> {
				String filename = attachment.getContentDisposition().getParameter("filename");
				InputStream in = attachment.getObject(InputStream.class);
				return new ReportApiImpl.AttachmentBeingRead(filename, in);
			});
			return Response.ok(result).build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	/**
	 * Download the given reports.
	 *
	 * @param storageName Name of the storage.
	 * @param exportReportParam "true" or "1" to save the serialized version of report.
	 * @param exportReportXmlParam "true" or "1" to save Xml version of report.
	 * @param storageIds List of storage ids to download.
	 * @return The response when downloading a file.
	 */
	@GET
	@Path("/download/{storage}/{exportReport}/{exportReportXml}")
	@Produces("application/octet-stream")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response downloadFile(@PathParam("storage") String storageName, @PathParam("exportReport") String exportReportParam,
								 @PathParam("exportReportXml") String exportReportXmlParam, @QueryParam("id") List<Integer> storageIds) {
		try {
			ExportResult result = delegate.downloadFile(storageName, exportReportParam, exportReportXmlParam, storageIds);
			Response.ResponseBuilder response = Response.ok(result.getTempFile(), MediaType.APPLICATION_OCTET_STREAM);
			response.header("Content-Disposition", "attachment; filename=" + result.getSuggestedFilename());
			return response.build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	/**
	 * Copy or move report files in the same storage to different paths.
	 *
	 * @param storageName Name of the storage.
	 * @param storageIds Storage ids of the reports to be moved.
	 * @param map Map containing "path" and "action". Actions could be "copy" or "move".
	 * @return The response of updating the Path.
	 */
	@PUT
	@Path("/move/{storage}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePath(@PathParam("storage") String storageName, @QueryParam("storageIds") List<Integer> storageIds, Map<String, String> map) {
		try {
			delegate.updatePath(storageName, storageIds, map);
			return Response.ok().build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	/**
	 * Cloning the reports with the given parameters.
	 *
	 * @param storageName Storage id of the report to be cloned.
	 * @param storageId Name of the target storage.
	 * @param map Map containing csv for cloning.
	 * @return The response of cloning the report.
	 */
	@POST
	@Path("/move/{storageName}/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cloneReport(@PathParam("storageName") String storageName, @PathParam("storageId") int storageId, Map<String, String> map) {
		try {
			List<String> result = delegate.cloneReport(storageName, storageId, map);
			return Response.ok().entity(result).build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("warningsAndErrors/{storage}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getWarningsAndErrors(
			@PathParam("storage") String storageName
	) {
		return Response
				.status(Response.Status.OK)
				.entity(delegate.getWarningsAndErrors(storageName))
				.build();
	}

	@POST
	@Path("/customreportaction")
	public Response processCustomReportAction(@QueryParam("storage") String storageName, List<Integer> reportIds) {
		try {
			Map<String, String> result = delegate.processCustomReportAction(storageName, reportIds);
			return Response.ok(result).build();
		} catch(HttpBadRequestException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch(HttpNotFoundException e) {
			return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch(HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("/variables")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fetchVariables() {
		return Response.ok(delegate.fetchVariables()).build();
	}
}
