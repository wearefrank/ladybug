//package nl.nn.testtool.api;
//
//
//import nl.nn.testtool.MetadataExtractor;
//import nl.nn.testtool.storage.Storage;
//import nl.nn.testtool.storage.StorageException;
//import nl.nn.testtool.util.LogUtil;
//import org.apache.log4j.Logger;
//
//import javax.annotation.security.RolesAllowed;
//import javax.ws.rs.DefaultValue;
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
//import javax.ws.rs.Produces;
//import javax.ws.rs.QueryParam;
//import javax.ws.rs.core.Context;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.MultivaluedMap;
//import javax.ws.rs.core.Response;
//import javax.ws.rs.core.UriInfo;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//@Path("/metadata")
//public class MetadataApi {
//	private static final Logger logger = LogUtil.getLogger(MetadataApi.class);
//	private static Map<String, Storage> storages;
//	public static Map<String, String> metadataFields;
//
//	@GET
//	@Path("/{storage}/")
//	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
//	@Produces(MediaType.APPLICATION_JSON)
//	public Response getMetadataList(@PathParam("storage") String storageParam, @DefaultValue("-1") @QueryParam("max") int max, @Context UriInfo uriInfo) throws ApiException {
//		// TODO: Sorting and filtering
//		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
//		List<String> searchValues = new ArrayList<>();
//		List<String> metadataNames = new ArrayList<>();
//		for (String param : params.keySet()) {
//			if (!metadataFields.containsKey(param.toLowerCase()))
//				continue;
//
//			List<String> values = params.get(param);
//			metadataNames.add(param);
//
//			if (values != null && values.size() > 0)
//				searchValues.add(values.get(0));
//			else
//				searchValues.add(null);
//
//		}
//		try {
//			List<List<Object>> out = getStorage(storageParam).getMetadata(max, metadataNames, searchValues, MetadataExtractor.VALUE_TYPE_STRING);
//			return Response.ok().entity(out).build();
//		} catch (StorageException e) {
//			throw new ApiException("Exception during filtering metadata.", e);
//		}
//	}
//
//	@GET
//	@Path("/")
//	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
//	@Produces(MediaType.APPLICATION_JSON)
//	public Response getMetadataInformation() {
//		return Response.ok().entity(metadataFields).build();
//	}
//
//	@GET
//	@Path("/{storage}/{lastmodified}")
//	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
//	@Produces(MediaType.APPLICATION_JSON)
//	public Response getLatestMetadata(@PathParam("storage") String storageParam, @PathParam("lastmodified") long lastModified) {
//		// Todo: implement this function's logic in storage interface.
//		return Response.status(Response.Status.NOT_IMPLEMENTED).build();
//	}
//
//	public void setStorages(Map<String, Storage> map) {
//		storages = map;
//	}
//
//	public void setMetadataFields(Map<String, String> map) {
//		metadataFields = map;
//	}
//
//	private static Storage getStorage(String storage) throws ApiException {
//		if (!storages.containsKey(storage))
//			throw new ApiException("Given storage [" + storage + "] was not found.");
//		return storages.get(storage);
//	}
//}
