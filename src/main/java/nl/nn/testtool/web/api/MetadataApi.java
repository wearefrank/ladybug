/*
   Copyright 2021-2022 WeAreFrank!

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
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Set;

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

import lombok.Setter;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.Storage;

@Path("/metadata")
public class MetadataApi extends ApiBase {
	private @Setter TestTool testTool;

	/**
	 * Searches the storage metadata.
	 *
	 * @param storageName Name of the storage to search.
	 * @param metadataNames The metadata names to return.
	 * @param limit Maximum number of results to return.
	 * @param uriInfo Query parameters for search.
	 * @param filterParam The regex on which the report names will be filtered
	 * @return Response containing fields [List[String]] and values [List[List[Object]]].
	 * @throws ApiException If an exception occurs during metadata search in storage.
	 */
	@GET
	@Path("/{storage}/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMetadataList(@PathParam("storage") String storageName,
									@QueryParam("metadataNames") ArrayList<String> metadataNames,
									@DefaultValue("-1") @QueryParam("limit") int limit ,
									@DefaultValue(".*") @QueryParam("filter") String filterParam ,
									@Context UriInfo uriInfo) {

		List<String> searchValues = new ArrayList<>();
		for(String field : metadataNames) {
			if ("name".equals(field)) {
				searchValues.add("(" + filterParam + ")");
			} else {
				searchValues.add(null);
			}
		}
		try {

			// Get storage, search for metadata, and return the results.
			Storage storage = testTool.getStorage(storageName);
			List<List<Object>> list = storage.getMetadata(limit, metadataNames, searchValues, MetadataExtractor.VALUE_TYPE_STRING);
			List<LinkedHashMap<String, String>> metadata = new ArrayList<>();
			for (List<Object> item : list) {
				LinkedHashMap<String, String> metadataItem = new LinkedHashMap<>();
				metadataItem.put("storageId", item.get(0).toString());
				for (int i = 1; i < metadataNames.size(); i++) {
					metadataItem.put(metadataNames.get(i), item.get(i).toString());
				}
				metadata.add(metadataItem);
			}

			return Response.ok().entity(metadata).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not find metadata with limit [" + limit + "] and filter [" + filterParam + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}

	/**
	 * @return A response containing list of metadata fields.
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMetadataInformation() {
		return Response.ok().entity(getMetadataFields()).build();
	}

	/**
	 * @return Set of strings for list of parameters in metadata.
	 */
	private Set<String> getMetadataFields() {
		return new HashSet<String>(testTool.getViews().getDefaultView().getMetadataNames());
	}
}
