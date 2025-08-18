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
package nl.nn.testtool.web.api;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import nl.nn.testtool.web.ApiServlet;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.inject.Inject;
import lombok.Setter;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.Storage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/metadata")
@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
public class MetadataApi extends ApiBase {
	private @Setter @Inject @Autowired TestTool testTool;

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
	@GetMapping(value = "/{storage}/", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getMetadataList(@PathVariable("storage") String storageName,
											 @RequestParam(name = "metadataNames") List<String> metadataNames,
											 @RequestParam(name="limit", defaultValue = "-1") int limit,
											 @RequestParam(name = "filterHeader", defaultValue = "") List<String> filterHeaders,
											 @RequestParam(name = "filter", defaultValue = "") List<String> filterParams) {
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

			return ResponseEntity.ok(metadata);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Could not find metadata with limit " + limit + " and filter [" + filterParams + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}

	/**
	 * Returns the user help for each filter header.
	 *
	 * @param storageName - Name of the storage of the headers.
	 * @param metadataNames - the header names.
	 * @return The user help of each filter header.
	 */
	@GetMapping(value = "/{storage}/userHelp")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getUserHelp(@PathVariable("storage") String storageName, @RequestParam(name = "metadataNames") List<String> metadataNames) {
		try {
			Map<String, String> userHelp = new LinkedHashMap<>();
			Storage storage = testTool.getStorage(storageName);
			for (String field : metadataNames) {
				userHelp.put(field, storage.getUserHelp(field));
			}

			return ResponseEntity.ok(userHelp);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Could not find user help - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}

	/**
	 * Gets the count of metadata records.
	 *
	 * @param storageName - the storage from which the metadata records reside.
	 * @return the metadata count.
	 */
	@GetMapping(value = "/{storage}/count", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getMetadataCount(@PathVariable("storage") String storageName) {
		try {
			Storage storage = testTool.getStorage(storageName);
			return ResponseEntity.ok(storage.getSize());
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Could not find metadata count - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}
}
