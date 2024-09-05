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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.Setter;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.web.ApiServlet;

@Path("/" + ApiServlet.LADYBUG_API_PATH + "/metadata")
public class MetadataApi extends ApiBase {
	private @Setter @Inject @Autowired TestTool testTool;

	/**
	 * Searches the storage metadata.
	 *
	 * @param storageName Name of the storage to search.
	 * @param metadataNames The metadata names to return.
	 * @param limit Maximum number of results to return.
	 * @param filterHeaders The headers on which we filter.
	 * @param uriInfo Query parameters for search.
	 * @param filterParams The regex on which the report names will be filtered
	 * @return Response containing fields [List[String]] and values [List[List[Object]]].
	 * @throws ApiException If an exception occurs during metadata search in storage.
	 */
	@GET
	@Path("/{storage}/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMetadataList(@PathParam("storage") String storageName,
									@QueryParam("metadataNames") List<String> metadataNames,
									@DefaultValue("-1") @QueryParam("limit") int limit,
									@QueryParam("filterHeader") List<String> filterHeaders,
									@QueryParam("filter") List<String> filterParams ,
									@Context UriInfo uriInfo) {
		List<String> searchValues = new ArrayList<>();
		for(String field : metadataNames) {
			boolean changed = false;
			for (int filterHeaderIndex = 0; filterHeaderIndex < filterHeaders.size(); filterHeaderIndex++) {
				if (filterHeaders.get(filterHeaderIndex).equals(field)) {
					searchValues.add(filterParams.get(filterHeaderIndex));
					changed = true;
				}
			}
			if(!changed) {
				searchValues.add(null);
			}
		}
		try {

			// Get storage, search for metadata, and return the results.
			Storage storage = testTool.getStorage(storageName);
			List<List<Object>> records = storage.getMetadata(limit, metadataNames, searchValues, MetadataExtractor.VALUE_TYPE_GUI);
			List<LinkedHashMap<String, String>> metadata = new ArrayList<>();
			for (List<Object> record : records) {
				LinkedHashMap<String, String> metadataItem = new LinkedHashMap<>();
				metadataItem.put("storageId", record.get(0).toString());
				for (int i = 1; i < metadataNames.size(); i++) {
					String metadataValue = null;
					if (record.get(i) != null) {
						metadataValue = record.get(i).toString();
					}
					metadataItem.put(metadataNames.get(i), metadataValue);
				}
				metadata.add(metadataItem);
			}

			return Response.ok().entity(metadata).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not find metadata with limit " + limit + " and filter [" + filterParams + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}

	/**
	 * Returns the user help for each filter header.
	 *
	 * @param storageName - Name of the storage of the headers.
	 * @param metadataNames - the header names.
	 * @return The user help of each filter header.
	 */
	@GET
	@Path("/{storage}/userHelp")
	public Response getUserHelp(@PathParam("storage") String storageName, @QueryParam("metadataNames") List<String> metadataNames) {
		try {
			Map<String, String> userHelp = new LinkedHashMap<>();
			Storage storage = testTool.getStorage(storageName);
			for (String field : metadataNames) {
				userHelp.put(field, storage.getUserHelp(field));
			}

			return Response.ok().entity(userHelp).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not find user help - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}

	/**
	 * Gets the count of metadata records.
	 *
	 * @param storageName - the storage from which the metadata records reside.
	 * @return the metadata count.
	 */
	@GET
	@Path("/{storage}/count")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMetadataCount(@PathParam("storage") String storageName) {
		try {
			Storage storage = testTool.getStorage(storageName);
			return Response.ok().entity(storage.getSize()).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not find metadata count - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}
}
