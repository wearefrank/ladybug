package nl.nn.testtool.web.api;


import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.LogUtil;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/")
@Service
public class MetadataApi extends ApiBase {
	private static final Logger logger = LogUtil.getLogger(MetadataApi.class);
	public static Set<String> metadataFields;

	@GET
	@Path("/metadata/{storage}/")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMetadataList(@PathParam("storage") String storageParam, @DefaultValue("-1") @QueryParam("limit") int limit, @Context UriInfo uriInfo) throws ApiException {
		// TODO: Sorting and filtering
		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		if (params != null)
			params.remove("limit");

		List<String> searchValues = new ArrayList<>();
		List<String> metadataNames = new ArrayList<>();
		if (params == null || params.size() == 0) {
			for(String field : getMetadataFields()) {
				metadataNames.add(field);
				searchValues.add(null);
			}
		} else {
			for (String param : params.keySet()) {
				if (!getMetadataFields().contains(param))
					continue;

				List<String> values = params.get(param);
				metadataNames.add(param);

				if (values != null && values.size() > 0)
					searchValues.add(values.get(0));
				else
					searchValues.add(null);

			}
		}
		try {
			Storage storage = getBean(storageParam);
			List<List<Object>> list = storage.getMetadata(limit, metadataNames, searchValues, MetadataExtractor.VALUE_TYPE_STRING);
			Map<String, Object> out = new HashMap<>(2);
			out.put("fields", metadataNames);
			out.put("values", list);
			return Response.ok().entity(out).build();
		} catch (StorageException e) {
			throw new ApiException("Exception during filtering metadata.", e);
		}
	}

	@GET
	@Path("/metadata")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMetadataInformation() {
		return Response.ok().entity(getMetadataFields()).build();
	}

	@GET
	@Path("/metadata/{storage}/{lastmodified}")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLatestMetadata(@PathParam("storage") String storageParam, @PathParam("lastmodified") long lastModified) {
		// Todo: implement this function's logic in storage interface.
		return Response.status(Response.Status.NOT_IMPLEMENTED).build();
	}

	private Set<String> getMetadataFields() {
		if (metadataFields == null)
			metadataFields = getBean("metadataFields");
		return metadataFields;
	}
}
