package nl.nn.testtool.api;

import nl.nn.testtool.Report;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.LogUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/*
	TODO: PUT Upload a
	TODO: GET Download a report
 */


@Path("/ladybug")
public class ReportApi {
	private static Logger logger = LogUtil.getLogger(ReportApi.class);
	private static Map<String, Storage> storages;

	@GET
	@Path("/report/{storage}/{storageId}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReport(@PathParam("storage") String storage, @PathParam("storageId") int storageId) throws ApiException {
		try {
			Report report = getStorage(storage).getReport(storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND).build();

			return Response.ok().entity(report).build();
		} catch (StorageException e) {
			throw new ApiException("Exception while getting report [" + storageId + "] from storage [" + storage + "]", e);
		}
	}

	@DELETE
	@Path("/report/{storage}/{storageId}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Response deleteReport(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId) {
		Storage storage = getStorage(storageParam);
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
	@Path("/transformation/{storage}/{storageId}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateReportTransformation(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId, Map<String, String> map) {
		String transformation = map.get("transformation");
		if (StringUtils.isEmpty(transformation))
			return Response.status(Response.Status.BAD_REQUEST).build();

		try {
			Storage storage = getStorage(storageParam);
			storage.getReport(storageId).setTransformation(transformation);
			return Response.ok().build();
		} catch (StorageException e) {
			throw new ApiException("Exception while setting transformation for a report.", e);
		}
	}

	@GET
	@Path("/transformation/{storage}/{storageId}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateReportTransformation(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId) {
		Storage storage = getStorage(storageParam);
		try {
			String transformation = storage.getReport(storageId).getTransformation();
			if (StringUtils.isEmpty(transformation))
				return Response.noContent().build();

			Map<String, String> map = new HashMap<>(1);
			map.put("transformation", transformation);
			return Response.ok(map).build();
		} catch (StorageException e) {
			throw new ApiException("Exception while setting transformation for a report.", e);
		}
	}

	public void setStorages(Map<String, Storage> map) {
		storages = map;
	}


	private static Storage getStorage(String storage) throws ApiException {
		if (!storages.containsKey(storage))
			throw new ApiException("Given storage [" + storage + "] was not found.");
		return storages.get(storage);
	}
}
