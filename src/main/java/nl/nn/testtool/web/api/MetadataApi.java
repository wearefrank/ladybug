/*
   Copyright 2021 WeAreFrank!

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


import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/metadata")
public class MetadataApi extends ApiBase {

	/**
	 * Searches the storage metadata.
	 *
	 * @param storageParam Name of the storage to search.
	 * @param limit Maximum number of results to return.
	 * @param uriInfo Query parameters for search.
	 * @return Response containing fields [List[String]] and values [List[List[Object]]].
	 * @throws ApiException If an exception occurs during metadata search in storage.
	 */
	@GET
	@Path("/{storage}/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMetadataList(@PathParam("storage") String storageParam, @DefaultValue("-1") @QueryParam("limit") int limit ,
									@DefaultValue("") @QueryParam("sort") String sortParam , @DefaultValue("Descending") @QueryParam("order") String order,
									@Context UriInfo uriInfo) throws ApiException {
		// TODO: Sorting and filtering
		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

		// Make sure limit will not be included in searh parameters.
		if (params != null) {
			for (String p : new String[]{"limit", "sort", "order"})
				params.remove(p);
		}

		List<String> searchValues = new ArrayList<>();
		List<String> metadataNames = new ArrayList<>();
		Set<String> storedMetadataFields = getMetadataFields();
		int sort = -1;
		int index = 0;
		if (params == null || params.size() == 0) {
			// Add all metadata fields to be extracted without filtering.
			for(String field : storedMetadataFields) {
				if (field.equalsIgnoreCase(sortParam)) sort = index;
				index ++;
				metadataNames.add(field);
				searchValues.add(null);
			}
		} else {
			for (String param : params.keySet()) {
				// Extract search parameters for storage from the query parameters.
				if (param.equalsIgnoreCase(sortParam)) sort = index;
				index ++;

				List<String> values = params.get(param);
				metadataNames.add(param);

				if (values != null && values.size() > 0)
					searchValues.add(values.get(0));
				else
					searchValues.add(null);

			}
		}
		try {
			// Get storage, search for metadata, and return the results.
			Storage storage = getBean(storageParam);
			List<List<Object>> list;

			if (sort != -1) {
				// Sort based on one of the fields.
				list = storage.getMetadata(-1, metadataNames, searchValues, MetadataExtractor.VALUE_TYPE_OBJECT);

				// Get the class of the field to check if it implements Comparable.
				if (list.isEmpty()) return Response.noContent().build();
				Class clazz = list.get(0).get(sort).getClass();

				if (Comparable.class.isAssignableFrom(clazz)) {
					// Sort the metadata.
					Class<? extends Comparable> clazz2 = (Class<? extends Comparable>) clazz;
					int finalSort = sort;
					int finalOrder = order.equalsIgnoreCase("ascending") || order.equalsIgnoreCase("asc") ? 1 : -1;
					list.sort((o1, o2) -> clazz2.cast(o1.get(finalSort)).compareTo(clazz2.cast(o2.get(finalSort))) * finalOrder);
				}

				// Limit the output list and serialize into String.
				limit = limit > 0 ? Math.min(limit, list.size()) : list.size();
				ArrayList<List<Object>> outList = new ArrayList<>(limit);
				MetadataExtractor metadataExtractor = new MetadataExtractor();
				for (int i = 0; i < limit; i++) {
					List<Object> row = list.get(i);
					String[] stringRow = new String[row.size()];
					for (int j = 0; j < stringRow.length; j++) {
						stringRow[j] = metadataExtractor.fromObjectToString(metadataNames.get(j), row.get(j));
					}
					outList.add(Arrays.asList(stringRow));
				}
				list = outList;
			} else {
				// Return unsorted.
				list = storage.getMetadata(limit, metadataNames, searchValues, MetadataExtractor.VALUE_TYPE_STRING);
			}
			Map<String, Object> out = new HashMap<>(2);
			out.put("fields", metadataNames);
			out.put("values", list);
			return Response.ok().entity(out).build();
		} catch (StorageException e) {
			throw new ApiException("Exception during filtering metadata.", e);
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
		return new HashSet<String>(getBean("whiteBoxViewMetadataNames"));
	}
}
