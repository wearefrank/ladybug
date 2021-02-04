package nl.nn.testtool.web.api;

import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.util.Upload;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.Export;
import nl.nn.testtool.util.ExportResult;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Path("/")
public class ReportApi extends ApiBase {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@GET
	@Path("/report/{storage}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReport(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId) throws ApiException {
		try {
			Storage storage = getBean(storageParam);
			Report report = storage.getReport(storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND).build();

			return Response.ok().entity(report).build();
		} catch (StorageException e) {
			throw new ApiException("Exception while getting report [" + storageId + "] from storage [" + storageParam + "]", e);
		}
	}

	@DELETE
	@Path("/report/{storage}/{storageId}")
	public Response deleteReport(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId) {
		Storage storage = getBean(storageParam);
		if (!(storage instanceof CrudStorage)) {
			String msg = "Given storage [" + storageParam + "] does not implement delete function.";
			logger.warn(msg);
			return Response.status(Response.Status.NOT_IMPLEMENTED).entity(msg).build();
		}
		try {
			Report report = storage.getReport(storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND).build();
			((CrudStorage) storage).delete(report);
		} catch (StorageException e) {
			throw new ApiException("Exception while deleting a report.", e);
		}
		return Response.ok().build();
	}

	@POST
	@Path("/report/transformation/{storage}/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateReportTransformation(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId, Map<String, String> map) {
		String transformation = map.get("transformation");
		if (StringUtils.isEmpty(transformation))
			return Response.status(Response.Status.BAD_REQUEST).build();

		try {
			Storage storage = getBean(storageParam);
			storage.getReport(storageId).setTransformation(transformation);
			return Response.ok().build();
		} catch (StorageException e) {
			throw new ApiException("Exception while setting transformation for a report.", e);
		}
	}

	@GET
	@Path("/report/transformation/{storage}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReportTransformation(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId) {
		try {
			Storage storage = getBean(storageParam);
			String transformation = storage.getReport(storageId).getTransformation();
			Map<String, String> map = new HashMap<>(1);
			map.put("transformation", transformation);
			return Response.ok(map).build();
		} catch (StorageException e) {
			throw new ApiException("Exception while setting transformation for a report.", e);
		}
	}

	@PUT
	@Path("/report/store/{storage}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response copyReport(@PathParam("storage") String storageParam, Map<String, List<Integer>> sources, @HeaderParam("Access-Control-Allow-Headers") String requestHeader) {
		Storage target = getBean(storageParam);
		Map<String, String> exceptions = new HashMap<>();
		for (String src : sources.keySet()) {
			try {
				Storage srcStorage = getBean(src);

				for (int storageId : sources.get(src)) {
					try {
						((CrudStorage) target).store(srcStorage.getReport(storageId));
					} catch (StorageException storageException) {
						exceptions.put(src + "_" + storageId, storageException.getMessage());
						logger.error("Could not copy the report. #Exceptions for request: " + exceptions, storageException);
					}
				}
			} catch (ApiException e) {
				exceptions.put(src, e.getMessage());
//				throw new ApiException("Exception while setting transformation for a report.", e);
				return Response.serverError().build();
			}
		}
		// TODO: Find a better error response code.
		if (exceptions.size() > 0)
			return Response.status(Response.Status.BAD_REQUEST).entity(exceptions).build();
		return Response.ok().build();
	}

	@POST
	@Path("/report/upload/{storage}")
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@PathParam("storage") String storageParam, @Multipart("file") Attachment attachment) {
		Storage storage = getBean(storageParam);
		if (!(storage instanceof CrudStorage)) {
			throw new ApiException("Given storage is not a Crud Storage. Therefore no reports can be added externally.");
		}
		CrudStorage crudStorage = (CrudStorage) storage;

		String filename = attachment.getContentDisposition().getParameter("filename");
		InputStream in = attachment.getObject(InputStream.class);
		String errorMessage = Upload.upload(filename, in, crudStorage, logger);
		if (StringUtils.isEmpty(errorMessage)) {
			return Response.ok().build();
		}
		return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
	}

	@GET
	@Path("/report/download/{storage}/{exportReport}/{exportReportXml}")
	@Produces("application/octet-stream")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response downloadFile(@PathParam("storage") String storageParam, @PathParam("exportReport") String exportReportParam,
								 @PathParam("exportReportXml") String exportReportXmlParam, @QueryParam("id") List<Integer> storageIds) {
		Storage storage = getBean(storageParam);
		if (storageIds == null || storageIds.isEmpty())
			return Response.status(Response.Status.BAD_REQUEST).build();
		boolean exportReport = exportReportParam.equalsIgnoreCase("true") || exportReportParam.equals("1");
		boolean exportReportXml = exportReportXmlParam.equalsIgnoreCase("true") || exportReportXmlParam.equals("1");
		try {
			ExportResult export;
			if (storageIds.size() == 1) {
				Report report = storage.getReport(storageIds.get(0));
				export = Export.export(report, exportReport, exportReportXml);
			} else {
				export = Export.export(storage, storageIds, exportReport, exportReportXml);
			}
			Response.ResponseBuilder response = Response.ok(export.getTempFile(), MediaType.APPLICATION_OCTET_STREAM);
			response.header("Content-Disposition", "attachment; filename=" + export.getSuggestedFilename());
			return response.build();
		} catch (StorageException e) {
			throw new ApiException("Exception while requesting report from the storage.", e);
		}
	}

	@PUT
	@Path("/report/move/{storage}/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePath(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId, Map<String, String> map) {
		CrudStorage storage = getBean(storageParam);
		String path = map.get("path");
		String action = map.get("action");
		if (StringUtils.isEmpty(action) || StringUtils.isEmpty(path))
			return Response.status(Response.Status.BAD_REQUEST).entity("[action] and [path] is required as request body.").build();

		try {
			Report original = storage.getReport(storageId);
			if ("copy".equalsIgnoreCase(action)) {
				Report clone = (Report) original.clone();
				clone.setPath(path);
				storage.store(clone);
			} else if ("move".equalsIgnoreCase(action)) {
				original.setPath(path);
				storage.update(original);
			} else {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity("Action parameter can only be either [copy] or [move]").build();
			}

			return Response.ok().build();
		} catch (StorageException e) {
			throw new ApiException("Storage exception with storage id [" + storageId + "] in storage [" + storageParam + "]", e);
		} catch (CloneNotSupportedException e) {
			throw new ApiException("Cloning exception for report with storage id [" + storageId + "] in storage [" + storageParam + "]", e);
		}
	}

	@POST
	@Path("/report/move/{storage}/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cloneReport(@QueryParam("storage") String storageParam, @QueryParam("storageId") int storageId, Map<String, String> map) {
		CrudStorage storage = getBean(storageParam);
		Report original = null;
		try {
			original = storage.getReport(storageId);
			String previousMessage = original.getInputCheckpoint().getMessage();
			boolean force = "true".equalsIgnoreCase(map.getOrDefault("force", "false"))
					|| "1".equalsIgnoreCase(map.getOrDefault("force", "false"));
			original.getInputCheckpoint().setMessage(map.get("message"));
			if (!original.getInputCheckpoint().containsVariables() && !force) {
				original.getInputCheckpoint().setMessage(previousMessage);
				return Response.status(Response.Status.BAD_REQUEST).entity("No variables found in input message; press again to confirm").build();
			}
		} catch (StorageException e) {
			e.printStackTrace();
			return Response.status(Response.Status.BAD_REQUEST).entity("Report could not be found.").build();
		}

		Scanner scanner = new Scanner(map.get("csv"));
		String firstLine = null;
		boolean originalSet = false;
		ArrayList<String> exceptions = new ArrayList<>();
		while (scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();
			if (StringUtils.isNotEmpty(nextLine) && !nextLine.startsWith("#"))
				continue;
			if (firstLine == null) {
				firstLine = nextLine;
			} else {
				try {
					if (originalSet) {
						Report clone = (Report) original.clone();
						clone.setVariableCsvWithoutException(firstLine + "\n" + nextLine);
						storage.store(clone);
					} else {
						originalSet = true;
						original.setVariableCsvWithoutException(firstLine + "\n" + nextLine);
						storage.update(original);
					}
				} catch (CloneNotSupportedException | StorageException e) {
					exceptions.add(e.getMessage() + " Caused by: " + e.getCause() + " For line: [" + nextLine + "]");
				}
			}
		}
		scanner.close();
		return Response.ok().entity(exceptions).build();
	}
}
