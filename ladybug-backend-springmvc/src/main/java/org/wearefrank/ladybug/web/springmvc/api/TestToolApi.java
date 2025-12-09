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

import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import lombok.Setter;
import org.wearefrank.ladybug.filter.View;
import org.wearefrank.ladybug.web.common.HttpBadRequestException;
import org.wearefrank.ladybug.web.common.HttpInternalServerErrorException;
import org.wearefrank.ladybug.web.common.TestToolApiImpl;

import org.wearefrank.ladybug.Report;

@RestController
@RequestMapping("/testtool")
@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
public class TestToolApi extends ApiBase {
	@Autowired
	private @Setter TestToolApiImpl delegate;

	/**
	 * @return Response containing test tool data.
	 */
	@GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getInfo() {
		Map<String, Object> info = delegate.getTestToolInfo();
		return ResponseEntity.ok(info);
	}

	@GetMapping(value = "/reset", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> resetInfo() {
		Map<String, Object> info = delegate.resetInfo();
		return ResponseEntity.ok(info);
	}

	/**
	 * Change settings of the testtool.
	 *
	 * @param map New settings that can contain (generatorEnabled, regexFilter)
	 * @return The response after changing the settings.
	 */
	@PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateInfo(@RequestBody Map<String, String> map) {
		try {
			delegate.updateInfo(map);
			return ResponseEntity.ok().build();
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	/**
	 * Returns the specified report in progress.
	 *
	 * @param index Index of the report to return
	 * @return Response containing the specified report, if present.
	 */
	@GetMapping(value = "/in-progress/{index}", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getReportsInProgress(@PathVariable("index") int index) {
		try {
			Report result = delegate.getReportsInProgress(index);
			return ResponseEntity.ok(result);
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	/**
	 * Removes the report in progress
	 *
	 * @param index Index of the report to be deleted
	 * @return Response confirming the delete, if report is present
	 */
	@DeleteMapping(value = "/in-progress/{index}")
	public ResponseEntity<?> deleteReportInProgress(@PathVariable("index") int index) {
		try {
			delegate.deleteReportInProgress(index);
			return ResponseEntity.ok().build();
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	/**
	 * Gets the report in progress warning threshold time
	 *
	 * @return Response containing the time it will take before ladybug shows a warning that a report is still in progress
	 */
	@GetMapping(value = "/in-progress/threshold-time", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getReportsInProgressWarningThreshold() {
		return ResponseEntity.ok(delegate.getReportsInProgressWarningThreshold());
	}

	/**
	 * Change the default transformation.
	 *
	 * @param map Map containing key "transformation"
	 * @return The response after changing the transformation.
	 */
	@PostMapping(value = "/transformation", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateReportTransformation(@RequestBody Map<String, String> map) {
		try {
			delegate.updateReportTransformation(map);
			return ResponseEntity.ok().build();
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	/**
	 * @param defaultTransformation Boolean to check if we need to use the default transformation
	 * @return Response containing the current default transformation of the test tool.
	 */
	@GetMapping(value = "/transformation/{defaultTransformation}", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getReportTransformation(@PathVariable("defaultTransformation") boolean defaultTransformation) {
		Map<String, String> result = delegate.getReportTransformation(defaultTransformation);
		if (result == null) {
			return ResponseEntity.noContent().build();
		} else {
			return ResponseEntity.ok(result);
		}
	}

	/**
	 * @return The configured views
	 */
	@GetMapping(value = "/views", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getViewsResponse() {
		return ResponseEntity.ok(delegate.getViewsResponse());
	}

	@PutMapping(value = "/node-link-strategy", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> changeNodeLinkStrategy(@RequestParam(name = "nodeLinkStrategy") String nodeLinkStrategy, @RequestParam(name = "viewName") String viewName) {
		for (View view : delegate.getViews()) {
			if (viewName.equals(view.getName())) {
				setSessionAttr(view.getName() + ".NodeLinkStrategy", nodeLinkStrategy);
				break;
			}
		}

		return ResponseEntity.ok().build();
	}

	@GetMapping(value = "/stub-strategies", consumes = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getPossibleStubStrategies() {
		return ResponseEntity.ok(delegate.getPossibleStubStrategies());
	}

	@GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getVersion() {
		return ResponseEntity.ok(delegate.getVersion());
	}
}
