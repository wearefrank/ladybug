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

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

/*
	Reads and holds properties that are only to test Ladybug. During normal use the default
	values applied in the fields of inner class TestProperties should apply.

	The read classpath resource "test.properties" is optional. In fact it is only provided
	in test project ladybug-test-webapp.

	We chose not to use @PropertyResource although it allows for optional properties
	files via attribute "ignoreResourceNotFound". We would need @Value annotations
	to inject the optional properties, giving limited control over the default values.
	This custom implementation also allows us to control the log messages produced when
	reading properties file "test.properties". Another drawback of this approach would
	be that the properties of "test.properties" would be added to the Environment,
	theoretically allowing them to affect other parts of Ladybug or the Frank!Framework.
*/
@Configuration
public class TestPropertiesConfiguration {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String LADYBUG_UI_TEST_MODE = "ladybug.ui.test.mode";
	private static final String LADYBUG_BACKEND_THROW_FAKE_EXCEPTIONS = "ladybug.backend.throws.fake.exceptions";
	private static final String LADYBUG_BACKEND_FAKE_EXCEPTION_CALL_COUNT = "ladybug.backend.fake.exception.call.count";
	public static enum UI_TEST_MODE {
		DEFAULT,
		DONT_BLOCK_BACKEND;
	}
	public static final class TestProperties {
		private @Getter @Setter UI_TEST_MODE uiTestMode = UI_TEST_MODE.DEFAULT;
		private @Getter @Setter boolean ladybugBackendThrowsFakeExceptions = false;
		private @Getter @Setter int ladybugBackendFakeExceptionCallCount=5;
	}

	@Bean
	public TestProperties testProperties() {
		TestProperties result = new TestProperties();
		File file = null;
		try {
			file = ResourceUtils.getFile("classpath:test.properties");
		} catch(FileNotFoundException e) {
			log.info("No classpath resource test.properties available. Ladybug will behave normally");
			return result;
		}
		Properties props = new Properties();
		try {
			InputStream fis = new FileInputStream(file);
			props.load(fis);
		} catch(IOException e) {
			log.error("Cannot read properties from existing classpath resource test.properties, ignoring test.properties");
			return result;
		}
		String uiTestModeStr = props.getProperty(LADYBUG_UI_TEST_MODE);
		if (uiTestModeStr != null) {
			try {
				UI_TEST_MODE uiTestMode = UI_TEST_MODE.valueOf(uiTestModeStr.toUpperCase());
				result.setUiTestMode(uiTestMode);
			} catch(IllegalArgumentException e) {
				logErrorUnknownValue(LADYBUG_UI_TEST_MODE, uiTestModeStr);
			}
		}
		String ladybugBackendThrowsFakeExceptions = props.getProperty(LADYBUG_BACKEND_THROW_FAKE_EXCEPTIONS);
		if (ladybugBackendThrowsFakeExceptions != null) {
			if (new HashSet<String>(Arrays.asList("true", "false")).contains(ladybugBackendThrowsFakeExceptions)) {
				result.setLadybugBackendThrowsFakeExceptions(Boolean.parseBoolean(ladybugBackendThrowsFakeExceptions));
			} else {
				;logErrorUnknownValue(LADYBUG_BACKEND_THROW_FAKE_EXCEPTIONS, ladybugBackendThrowsFakeExceptions);
			}
		}
		String ladybugBackendFakeExceptionCallCount = props.getProperty(LADYBUG_BACKEND_FAKE_EXCEPTION_CALL_COUNT);
		if (ladybugBackendFakeExceptionCallCount != null) {
			try {
				result.setLadybugBackendFakeExceptionCallCount(Integer.parseInt(ladybugBackendFakeExceptionCallCount));
			} catch (NumberFormatException e) {
				logErrorUnknownValue(LADYBUG_BACKEND_FAKE_EXCEPTION_CALL_COUNT, ladybugBackendFakeExceptionCallCount);
			}
		}
		logTestPropertiesItem(LADYBUG_UI_TEST_MODE, result.getUiTestMode());
		logTestPropertiesItem(LADYBUG_BACKEND_THROW_FAKE_EXCEPTIONS, result.isLadybugBackendThrowsFakeExceptions());
		logTestPropertiesItem(LADYBUG_BACKEND_FAKE_EXCEPTION_CALL_COUNT, result.getLadybugBackendFakeExceptionCallCount());
		return result;
	}

	private void logErrorUnknownValue(String propName, String propValue) {
		log.error("Error parsing properties file 'test.properties': Unknown value for property '[{}]': [{}]", propName, propValue);
	}

	private void logTestPropertiesItem(String propertyName, Object value) {
		log.warn("Using from test.properties: [{}]=[{}]", propertyName, value);
	}
}
