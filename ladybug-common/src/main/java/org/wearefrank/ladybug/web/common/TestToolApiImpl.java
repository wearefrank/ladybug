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
package org.wearefrank.ladybug.web.common;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.filter.View;
import org.wearefrank.ladybug.filter.Views;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TestToolApiImpl {
	private @Setter
	@Inject
	@Autowired TestTool testTool;
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

	public Map<String, Object> getTestToolInfo() {
		Map<String, Object> map = new HashMap<>(4);
		map.put("generatorEnabled", testTool.isReportGeneratorEnabled());
		map.put("estMemory", testTool.getReportsInProgressEstimatedMemoryUsage());
		map.put("regexFilter", testTool.getRegexFilter());
		map.put("reportsInProgress", testTool.getNumberOfReportsInProgress());
		map.put("stubStrategies", testTool.getStubStrategies());
		return map;
	}

	public Map<String, Object> resetInfo() {
		testTool.reset();
		return getTestToolInfo();
	}

	public void updateInfo(Map<String, String> map) throws HttpBadRequestException {
		if (map.isEmpty()) {
			throw new HttpBadRequestException("No settings have been provided - detailed error message - The settings that have been provided are " + map.toString());
		}

		if (map.size() > 2) {
			throw new HttpBadRequestException("Too many settings have been provided - detailed error message - The settings that have been provided are " + map.toString());
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
	}

	public Report getReportsInProgress(int index) throws HttpBadRequestException {
		if (index == 0)
			throw new HttpBadRequestException("No progresses have been queried [" + index + "] and/or are available [" + testTool.getNumberOfReportsInProgress() + "]");

		try {
			return testTool.getReportInProgress(index - 1);
		} catch (Exception e) {
			throw new HttpBadRequestException(e);
		}
	}

	public void deleteReportInProgress(int index) throws HttpBadRequestException {
		if (index == 0)
			throw new HttpBadRequestException("No progresses have been queried [" + index + "] or are available [" + testTool.getNumberOfReportsInProgress() + "]");

		try {
			testTool.removeReportInProgress(index - 1);
		} catch (Exception e) {
			throw new HttpBadRequestException("Could not find report in progress with index [" + index + "] :: " + e + Arrays.toString(e.getStackTrace()), e);
		}
	}

	public int getReportsInProgressWarningThreshold() {
		return testTool.getReportsInProgressThreshold();
	}

	public void updateReportTransformation(Map<String, String> map) throws HttpBadRequestException, HttpInternalServerErrorException {
		String transformation = map.get("transformation");
		if (StringUtils.isEmpty(transformation)) {
			throw new HttpBadRequestException("No transformation has been provided");
		}
		String errorMessage = reportXmlTransformer.updateXslt(transformation);
		if (errorMessage != null) {
			// Without "- detailed error message -" the message is not shown in the toaster
			throw new HttpInternalServerErrorException(errorMessage + " - detailed error message - No detailed error message available");
		}
	}

	public Map<String, String> getReportTransformation(boolean defaultTransformation) {
		String transformation;

		if (defaultTransformation) {
			transformation = getDefaultTransformation();
		} else {
			transformation = reportXmlTransformer.getXslt();
		}

		if (StringUtils.isEmpty(transformation))
			return null;

		Map<String, String> map = new HashMap<>(1);
		map.put("transformation", transformation);
		return map;
	}

	public Map<String, Map<String, Object>> getViewsResponse() {
		// Starting from CXF 3.2.0 the setViews() will not be called by Spring when the name of this method is
		// getViews() instead of getViewsResponse() (with CXF 3.1.18 this was not the case) (maybe Spring's
		// ExtendedBeanInfo isn't used anymore with newer CXF versions)
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
		return response;
	}

	// TODO: Implement changeNodeLinkStrategy here.
	public List<View> getViews() {
		return views;
	}

	public List<String> getPossibleStubStrategies() {
		return testTool.getStubStrategies();
	}

	public String getVersion() {
		return testTool.getVersion();
	}
}
