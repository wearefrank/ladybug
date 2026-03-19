/*
   Copyright 2025, 2026 WeAreFrank!

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

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Setter;
import org.wearefrank.ladybug.filter.View;
import org.wearefrank.ladybug.web.common.HttpBadRequestException;
import org.wearefrank.ladybug.web.common.HttpInternalServerErrorException;
import org.wearefrank.ladybug.web.common.TestToolApiImpl;

import org.wearefrank.ladybug.Report;

@RestController
@RequestMapping("/testtool")
@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
public class TestToolApi {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private @Setter TestToolApiImpl delegate;

	private @Autowired
	@Resource(name="observerRoles") List<String> observerRoles;

	private @Autowired
	@Resource(name="dataAdminRoles") List<String> dataAdminRoles;

	private @Autowired
	@Resource(name="testerRoles") List<String> testerRoles;

	/**
	 * @return Response containing test tool data.
	 */
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getInfo() {
		try {
			Map<String, Object> info = delegate.getTestToolInfo();
			info.put("role", getRole());
			return ResponseEntity.ok(info);
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body("Fake exception");
		}
	}

	private String getRole() {
		List<String> authoritiesList = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
				.stream().map((a) -> a.getAuthority()).collect(Collectors.toList());
		log.debug("TestToolApi.getRole() sees authorities [{}]", authoritiesList.stream().collect(Collectors.joining(", ")));
		if (authoritiesList.size() != 1) {
			log.error("Expected only one role in [{}]", authoritiesList.stream().collect(Collectors.joining(", ")));
			return TestToolApiImpl.NO_AUTHORIZATION;
		}
		String role = authoritiesToRoles(authoritiesList).iterator().next();
		// The injected role sets observerRoles, dataAdminRoles and testerRoles are assumed cumulative.
		if (observerRoles.contains(role)) {
			return TestToolApiImpl.OBSERVER;
		} else if (dataAdminRoles.contains(role)) {
			return TestToolApiImpl.DATA_ADMIN;
		} else if (testerRoles.contains(role)) {
			return TestToolApiImpl.TESTER;
		} else {
			return TestToolApiImpl.NO_AUTHORIZATION;
		}
	}

	private Set<String> authoritiesToRoles(List<String> authorities) {
		Set<String> result = new HashSet<>();
		for (String authority: authorities) {
			String role = authority;
			if (role.toUpperCase().startsWith("ROLE")) {
				role = role.substring(4);
			}
			if (role.startsWith("_")) {
				role = role.substring(1);
			}
			result.add(role);
		}
		return result;
	}

	// IbisObserver is permitted to revert the generatorEnabled state and the regex filter to factory
	// defaults. These factory defaults are considered secure. But IbisObserver is not permitted to change
	// the generatorEnabled state or the regex filter arbitrarily. The IbisDataAdmin controls which data is
	// stored by ladybug by default.
	@GetMapping(value = "/reset", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> resetInfo() {
		try {
			Map<String, Object> info = delegate.resetInfo();
			return ResponseEntity.ok(info);
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body("Fake exception");
		}
	}

	/**
	 * Change settings of the testtool.
	 *
	 * @param map New settings that can contain (generatorEnabled, regexFilter)
	 * @return The response after changing the settings.
	 */
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateInfo(@RequestBody Map<String, String> map) {
		try {
			delegate.updateInfo(map);
			return ResponseEntity.ok().build();
		} catch(HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch(HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
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

	// It is intended that IbisObserver has full control over the report transformation. The report transformation
	// is not a means to hide sensitive information. When a report is opened, all information is visible anyway.
	/**
	 * Change the default transformation.
	 *
	 * @param map Map containing key "transformation"
	 * @return The response after changing the transformation.
	 */
	@PostMapping(value = "/transformation", consumes = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
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

	@PostMapping(value = "/transformation/reset")
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> restoreDefaultXsltTransformation() {
		delegate.restoreDefaultXsltTransformation();
		return ResponseEntity.ok().build();
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
				// todo: nodeLinkStrategy
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
