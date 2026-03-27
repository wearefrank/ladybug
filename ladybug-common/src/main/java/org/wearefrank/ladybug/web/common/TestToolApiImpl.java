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
package org.wearefrank.ladybug.web.common;

import jakarta.inject.Inject;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.MetadataExtractor;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.filter.View;
import org.wearefrank.ladybug.filter.Views;
import org.wearefrank.ladybug.storage.CrudStorage;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TestToolApiImpl implements InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String KEY_TRANSFORMATION = "transformation";

	private @Setter
	@Inject
	@Autowired TestTool testTool;
	private @Setter @Inject @Autowired MetadataExtractor metadataExtractor;
	private @Setter @Inject @Autowired ReportXmlTransformer reportXmlTransformer;
	private @Setter @Inject @Autowired Views views;

	@Value("ladybug.ui.test.mode:DEFAULT")
	private TestToolInfoResponse.UI_TEST_MODE uiTestMode;

	@Value("ladybug.backend.throws.fake.exceptions:false")
	private boolean ladybugBackendThrowsFakeExceptions;

	@Value("ladybug.backend.fake.exception.call.count:5")
	int ladybugBackendFakeExceptionCallCount;

	private int callCount = 0;

	@Override
	public void afterPropertiesSet() {
		logTestPropertiesItem("ladybug.ui.test.mode", uiTestMode);
		logTestPropertiesItem("ladybug.backend.throws.fake.exceptions", ladybugBackendThrowsFakeExceptions);
		logTestPropertiesItem("ladybug.backend.fake.exception.call.count", ladybugBackendFakeExceptionCallCount);
	}

	private void logTestPropertiesItem(String propertyName, Object value) {
		log.info("Using from test.properties or default value: [{}]=[{}]", propertyName, value);
	}

	public TestToolInfoResponse getTestToolInfo() throws HttpInternalServerErrorException {
		if (ladybugBackendThrowsFakeExceptions && callCount == ladybugBackendFakeExceptionCallCount) {
			callCount = 0;
			throw new HttpInternalServerErrorException("Fake error");
		}
		++callCount;
		TestToolInfoResponse result = new TestToolInfoResponse();
		result.setGeneratorEnabled(testTool.isReportGeneratorEnabled());
		result.setEstMemory(testTool.getReportsInProgressEstimatedMemoryUsage());
		result.setRegexFilter(testTool.getRegexFilter());
		result.setReportsInProgress(testTool.getNumberOfReportsInProgress());
		result.setStubStrategies(testTool.getStubStrategies());
		result.setTransformation("");
		String transformation = reportXmlTransformer.getXslt();
		if (StringUtils.isEmpty(transformation)) {
			log.error("reportXmlTransformer.getXslt() should hold non-empty XSLT transformation");
		} else {
			result.setTransformation(transformation);
		}
		result.setUiTestMode(uiTestMode);
		return result;
	}

	public TestToolInfoResponse resetInfo() throws HttpInternalServerErrorException {
		testTool.reset();
		testTool.sendReportGeneratorStatusUpdate();
		reportXmlTransformer.restoreDefaultXslt();
		// Roles are added by the callers because retrieving them is specific to
		// JAX-RS or Spring MVC authorization.
		return getTestToolInfo();
	}

	public void updateInfo(Map<String, String> map) throws HttpBadRequestException, HttpInternalServerErrorException {
		if (map.isEmpty()) {
			throw new HttpBadRequestException("No settings have been provided - detailed error message - The settings that have been provided are " + map.toString());
		}

		if (map.size() > 3) {
			throw new HttpBadRequestException("Too many settings have been provided - detailed error message - The settings that have been provided are " + map.toString());
		}
		String generatorEnabled = map.remove("generatorEnabled");
		String regexFilter = map.remove("regexFilter");
		String transformation = map.remove(KEY_TRANSFORMATION);

		if (StringUtils.isNotEmpty(generatorEnabled)) {
			testTool.setReportGeneratorEnabled("1".equalsIgnoreCase(generatorEnabled) || "true".equalsIgnoreCase(generatorEnabled));
			testTool.sendReportGeneratorStatusUpdate();
		}
		if (StringUtils.isNotEmpty(regexFilter)) {
			testTool.setRegexFilter(regexFilter);
		}
		if (transformation != null) {
			this.updateReportTransformation(transformation);
		}
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
		String transformation = map.get(KEY_TRANSFORMATION);
		updateReportTransformation(transformation);
	}

	private void updateReportTransformation(String transformation) throws HttpBadRequestException, HttpInternalServerErrorException {
		if (StringUtils.isBlank(transformation)) {
			throw new HttpBadRequestException("It is not allowed to clear the report transformation");
		}
		transformation = transformation.replace("\r\n", "\n");
		String errorMessage = reportXmlTransformer.updateXslt(transformation);
		if (errorMessage != null) {
			// Without "- detailed error message -" the message is not shown in the toaster
			throw new HttpInternalServerErrorException(errorMessage + " - detailed error message - No detailed error message available");
		}
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
