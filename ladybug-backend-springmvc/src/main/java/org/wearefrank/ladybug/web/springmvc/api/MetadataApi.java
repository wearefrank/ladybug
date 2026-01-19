/*
   Copyright 2025 WeAreFrank!

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
package org.wearefrank.ladybug.web.springmvc.api;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.wearefrank.ladybug.web.common.HttpInternalServerErrorException;
import org.wearefrank.ladybug.web.common.MetadataApiImpl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/metadata")
@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
public class MetadataApi {
	@Autowired
	private @Setter MetadataApiImpl delegate;

	@GetMapping(value = "/{viewName}", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getMetadataList(@PathVariable("viewName") String viewName,
											 @RequestParam(name = "metadataNames") List<String> metadataNames,
											 @RequestParam(name="limit", defaultValue = "-1") int limit,
											 @RequestParam(name = "filterHeader", defaultValue = "") List<String> filterHeaders,
											 @RequestParam(name = "filter", defaultValue = "") List<String> filterParams) {
		try {
			List<LinkedHashMap<String, String>> metadata = delegate.getMetadataList(viewName, metadataNames, limit, filterHeaders, filterParams);
			return ResponseEntity.ok(metadata);
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body("Could not find metadata with limit " + limit + " and filter [" + filterParams + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}

	/**
	 * Returns the user help for each filter header.
	 *
	 * @param viewName - Name of the storage of the headers.
	 * @param metadataNames - the header names.
	 * @return The user help of each filter header.
	 */
	@GetMapping(value = "/{viewName}/userHelp")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getUserHelp(@PathVariable("viewName") String viewName, @RequestParam(name = "metadataNames") List<String> metadataNames) {
		try {
			Map<String, String> userHelp = delegate.getUserHelp(viewName, metadataNames);
			return ResponseEntity.ok(userHelp);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Could not find user help - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}

	@GetMapping(value = "/{viewName}/count", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getMetadataCount(@PathVariable("viewName") String viewName) {
		try {
			int count = delegate.getMetadataCount(viewName);
			return ResponseEntity.ok(count);
		} catch (HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body("Could not find metadata count - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
		}
	}
}
