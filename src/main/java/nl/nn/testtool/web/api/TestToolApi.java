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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.Setter;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.filter.Views;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.transform.ReportXmlTransformer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/testtool")
@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
public class TestToolApi extends ApiBase {
	private @Setter @Inject @Autowired TestTool testTool;
	private @Setter @Inject @Autowired MetadataExtractor metadataExtractor;
	private @Setter @Inject @Autowired ReportXmlTransformer reportXmlTransformer;
	private @Setter @Inject @Autowired Views views;
	private String defaultTransformation;

	@PostConstruct
	public void init() {
		defaultTransformation = reportXmlTransformer.getXslt();
	}

	public String getDefaultTransformation() {
		return defaultTransformation;
	}

	/**
	 * @return Response containing test tool data.
	 */
	@GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getInfo() {
		Map<String, Object> info = getTestToolInfo();
		return ResponseEntity.ok(info);
	}

	@GetMapping(value = "/reset", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> resetInfo() {
		testTool.reset();
		Map<String, Object> info = getTestToolInfo();
		return ResponseEntity.ok(info);
	}

	public Map<String, Object> getTestToolInfo() {
		Map<String, Object> map = new HashMap<>(4);
		map.put("generatorEnabled", testTool.isReportGeneratorEnabled());
		map.put("estMemory", testTool.getReportsInProgressEstimatedMemoryUsage());
		map.put("regexFilter", testTool.getRegexFilter());
		map.put("reportsInProgress", testTool.getNumberOfReportsInProgress());
		map.put("stubStrategies", testTool.getStubStrategies());
		return map;
	}

	/**
	 * Change settings of the testtool.
	 *
	 * @param map New settings that can contain (generatorEnabled, regexFilter)
	 * @return The response after changing the settings.
	 */
	@PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateInfo(Map<String, String> map) {
		if (map.isEmpty()) {
			return ResponseEntity.badRequest().body("No settings have been provided - detailed error message - The settings that have been provided are " + map);
		}

		if (map.size() > 2) {
			return ResponseEntity.badRequest().body("Too many settings have been provided - detailed error message - The settings that have been provided are " + map);
		}
		// TODO: Check user roles.
		String generatorEnabled = map.remove("generatorEnabled");
		String regexFilter = map.remove("regexFilter");

		if (StringUtils.isNotEmpty(generatorEnabled)) {
			testTool.setReportGeneratorEnabled("1".equalsIgnoreCase(generatorEnabled) || "true".equalsIgnoreCase(generatorEnabled));
			testTool.sendReportGeneratorStatusUpdate();
		}
		if (StringUtils.isNotEmpty(regexFilter))
			testTool.setRegexFilter(regexFilter);

		return ResponseEntity.ok().build();
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
		if (index == 0)
			return ResponseEntity.badRequest().body("No progresses have been queried [" + index + "] and/or are available [" + testTool.getNumberOfReportsInProgress() + "]");

		try {
			Report report = testTool.getReportInProgress(index - 1);
			return ResponseEntity.ok(report);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Could not find report in progress with index [" + index + "] - detailed error message - " + e + Arrays.toString(e.getStackTrace()));
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
		if (index == 0)
			return ResponseEntity.badRequest().body("No progresses have been queried [" + index + "] or are available [" + testTool.getNumberOfReportsInProgress() + "]");

		try {
			testTool.removeReportInProgress(index - 1);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Could not find report in progress with index [" + index + "] :: " + e + Arrays.toString(e.getStackTrace()));
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
		return ResponseEntity.ok(testTool.getReportsInProgressThreshold());
	}

	/**
	 * Change the default transformation.
	 *
	 * @param map Map containing key "transformation"
	 * @return The response after changing the transformation.
	 */
	@PostMapping(value = "/transformation", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateReportTransformation(Map<String, String> map) {
		String transformation = map.get("transformation");
		if (StringUtils.isEmpty(transformation)) {
			return ResponseEntity.badRequest().body("No transformation has been provided");
		}
		String errorMessage = reportXmlTransformer.updateXslt(transformation);
		if (errorMessage != null) {
			// Without "- detailed error message -" the message is not shown in the toaster
			return ResponseEntity.internalServerError().body(errorMessage + " - detailed error message - No detailed error message available");
		}
		return ResponseEntity.ok().build();
	}

	/**
	 * @param defaultTransformation Boolean to check if we need to use the default transformation
	 * @return Response containing the current default transformation of the test tool.
	 */
	@GetMapping(value = "/transformation/{defaultTransformation}", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getReportTransformation(@PathVariable("defaultTransformation") boolean defaultTransformation) {
		String transformation;

		if (defaultTransformation) {
			transformation = getDefaultTransformation();
		} else {
			transformation = reportXmlTransformer.getXslt();
		}

		if (StringUtils.isEmpty(transformation))
			return ResponseEntity.noContent().build();

		Map<String, String> map = new HashMap<>(1);
		map.put("transformation", transformation);
		return ResponseEntity.ok(map);
	}

	/**
	 * @return The configured views
	 */
	@GetMapping(value = "/views", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getViewsResponse() {
		Map<String, Map<String, Object>> response = new LinkedHashMap<>();
		for (View view : views) {
			Map<String, Object> map = new HashMap<>();
			map.put("name", view.getName());
			map.put("storageName", view.getDebugStorage().getName());
			map.put("defaultView", view == views.getDefaultView());
			map.put("metadataNames", view.getMetadataNames());
			map.put("metadataLabels", view.getMetadataLabels());
			map.put("crudStorage", view.getDebugStorage() instanceof CrudStorage);
			map.put("nodeLinkStrategy", view.getNodeLinkStrategy());
			map.put("hasCheckpointMatchers", view.hasCheckpointMatchers());
			Map<String, String> metadataTypes = new HashMap<>();
			for (String metadataName : view.getMetadataNames()) {
				metadataTypes.put(metadataName, metadataExtractor.getType(metadataName));
			}
			map.put("metadataTypes", metadataTypes);
			response.put(view.getName(), map);
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping(value = "/node-link-strategy", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> changeNodeLinkStrategy(@RequestParam(name = "nodeLinkStrategy") String nodeLinkStrategy, @RequestParam(name = "viewName") String viewName) {
		for (View view : views) {
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
		var strategies = testTool.getStubStrategies();
		return ResponseEntity.ok(strategies);
	}

	@GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public ResponseEntity<?> getVersion() {
		return ResponseEntity.ok(testTool.getVersion());
	}
}
