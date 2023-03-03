/*
   Copyright 2021-2023 WeAreFrank!

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

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;

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
	 * @param filterHeader The header on which we filter.
	 * @param uriInfo Query parameters for search.
	 * @param filterParam The regex on which the report names will be filtered
	 * @return Response containing fields [List[String]] and values [List[List[Object]]].
	 * @throws ApiException If an exception occurs during metadata search in storage.
	 */
	@GET
	@Path("/{storage}/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMetadataList(@PathParam("storage") String storageName,
									@QueryParam("metadataNames") List<String> metadataNames,
									@DefaultValue("-1") @QueryParam("limit") int limit,
									@DefaultValue("") @QueryParam("filterHeader") String filterHeader,
									@DefaultValue("(.*)") @QueryParam("filter") String filterParam ,
									@Context UriInfo uriInfo) {

		List<String> searchValues = new ArrayList<>();
		for(String field : metadataNames) {
			if (filterHeader.equals(field)) {
				searchValues.add(filterParam);
			} else {
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
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not find metadata with limit [" + limit + "] and filter [" + filterParam + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
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
