package nl.nn.testtool.web.api;

import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.util.Upload;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.Export;
import nl.nn.testtool.util.ExportResult;
import nl.nn.testtool.util.LogUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.log4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
public class ReportApi extends ApiBase {
	private static final Logger logger = LogUtil.getLogger(ReportApi.class);

	@GET
	@Path("/report/{storage}/{storageId}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReport(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId) throws ApiException {
		try {
			Storage storage = getBean(storageParam);
			Report report = storage.getReport(storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND)
						.header("Access-Control-Allow-Origin", "*")
						.header("Access-Control-Allow-Credentials", "true")
						.header("Access-Control-Allow-Headers",
								"origin, content-type, accept, authorization")
						.header("Access-Control-Allow-Methods",
								"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();

			return Response.ok().entity(report)
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Credentials", "true")
					.header("Access-Control-Allow-Headers",
							"origin, content-type, accept, authorization")
					.header("Access-Control-Allow-Methods",
							"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
		} catch (StorageException e) {
			throw new ApiException("Exception while getting report [" + storageId + "] from storage [" + storageParam + "]", e);
		}
	}

	@DELETE
	@Path("/report/{storage}/{storageId}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Response deleteReport(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId) {
		Storage storage = getBean(storageParam);
		if (!(storage instanceof CrudStorage)) {
			String msg = "Given storage [" + storageParam + "] does not implement delete function.";
			logger.warn(msg);
			return Response.status(Response.Status.NOT_IMPLEMENTED).entity(msg)
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Credentials", "true")
					.header("Access-Control-Allow-Headers",
							"origin, content-type, accept, authorization")
					.header("Access-Control-Allow-Methods",
							"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
		}
		try {
			Report report = storage.getReport(storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND)
						.header("Access-Control-Allow-Origin", "*")
						.header("Access-Control-Allow-Credentials", "true")
						.header("Access-Control-Allow-Headers",
								"origin, content-type, accept, authorization")
						.header("Access-Control-Allow-Methods",
								"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
			((CrudStorage) storage).delete(report);
		} catch (StorageException e) {
			throw new ApiException("Exception while deleting a report.", e);
		}
		return Response.ok()
				.header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Credentials", "true")
				.header("Access-Control-Allow-Headers",
						"origin, content-type, accept, authorization")
				.header("Access-Control-Allow-Methods",
						"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
	}

	@POST
	@Path("/report/transformation/{storage}/{storageId}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateReportTransformation(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId, Map<String, String> map) {
		String transformation = map.get("transformation");
		if (StringUtils.isEmpty(transformation))
			return Response.status(Response.Status.BAD_REQUEST)
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Credentials", "true")
					.header("Access-Control-Allow-Headers",
							"origin, content-type, accept, authorization")
					.header("Access-Control-Allow-Methods",
							"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();

		try {
			Storage storage = getBean(storageParam);
			storage.getReport(storageId).setTransformation(transformation);
			return Response.ok()
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Credentials", "true")
					.header("Access-Control-Allow-Headers",
							"origin, content-type, accept, authorization")
					.header("Access-Control-Allow-Methods",
							"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
		} catch (StorageException e) {
			throw new ApiException("Exception while setting transformation for a report.", e);
		}
	}

	@GET
	@Path("/report/transformation/{storage}/{storageId}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReportTransformation(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId) {
		try {
			Storage storage = getBean(storageParam);
			String transformation = storage.getReport(storageId).getTransformation();
			Map<String, String> map = new HashMap<>(1);
			map.put("transformation", transformation);
			return Response.ok(map)
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Credentials", "true")
					.header("Access-Control-Allow-Headers",
							"origin, content-type, accept, authorization")
					.header("Access-Control-Allow-Methods",
							"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
		} catch (StorageException e) {
			throw new ApiException("Exception while setting transformation for a report.", e);
		}
	}

	@PUT
	@Path("/report/store/{storage}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response copyReport(@PathParam("storage") String storageParam, Map<String, List<Integer>> sources) {
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
				throw new ApiException("Exception while setting transformation for a report.", e);
			}
		}
		// TODO: Find a better error response code.
		if (exceptions.size() > 0)
			return Response.status(Response.Status.BAD_REQUEST).entity(exceptions)
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Credentials", "true")
					.header("Access-Control-Allow-Headers",
							"origin, content-type, accept, authorization")
					.header("Access-Control-Allow-Methods",
							"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
		return Response.ok()
				.header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Credentials", "true")
				.header("Access-Control-Allow-Headers",
						"origin, content-type, accept, authorization")
				.header("Access-Control-Allow-Methods",
						"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
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
			return Response.ok()
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Credentials", "true")
					.header("Access-Control-Allow-Headers",
							"origin, content-type, accept, authorization")
					.header("Access-Control-Allow-Methods",
							"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
		}
		return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage)
				.header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Credentials", "true")
				.header("Access-Control-Allow-Headers",
						"origin, content-type, accept, authorization")
				.header("Access-Control-Allow-Methods",
						"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
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
			return response
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Credentials", "true")
					.header("Access-Control-Allow-Headers",
							"origin, content-type, accept, authorization")
					.header("Access-Control-Allow-Methods",
							"GET, POST, PUT, DELETE, OPTIONS, HEAD").build();
		} catch (StorageException e) {
			throw new ApiException("Exception while requesting report from the storage.", e);
		}
	}
}
