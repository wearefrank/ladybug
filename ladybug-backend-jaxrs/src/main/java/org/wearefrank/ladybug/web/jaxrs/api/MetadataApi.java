/*
   Copyright 2021-2025 WeAreFrank!

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
import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.Storage;

import org.wearefrank.ladybug.web.common.Constants;
import org.wearefrank.ladybug.web.common.HttpInternalServerErrorException;
import org.wearefrank.ladybug.web.common.MetadataApiImpl;
import org.wearefrank.ladybug.web.jaxrs.api.ApiBase;
import org.wearefrank.ladybug.web.jaxrs.api.ApiException;

@Path("/" + Constants.LADYBUG_API_PATH + "/metadata")
public class MetadataApi extends ApiBase {
	@Autowired
	@Setter private MetadataApiImpl delegate;

	/**
	 * Searches the storage metadata.
	 *
	 * @param storageName Name of the storage to search.
	 * @param metadataNames The metadata names to return.
	 * @param limit Maximum number of results to return.
	 * @param filterHeaders The headers on which we filter.
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
									@QueryParam("filter") List<String> filterParams) {
		try {
			List<LinkedHashMap<String, String>> metadata = delegate.getMetadataList(storageName, metadataNames, limit, filterHeaders, filterParams);
			return Response.ok().entity(metadata).build();
		} catch (HttpInternalServerErrorException e) {
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
			Map<String, String> userHelp = delegate.getUserHelp(storageName, metadataNames);
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
			int count = delegate.getMetadataCount(storageName);
			return Response.ok().entity(count).build();
		} catch (HttpInternalServerErrorException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not find metadata count - detailed error message - " + e + Arrays.toString(e.getStackTrace())).build();
		}
	}
}
