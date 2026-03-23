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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Properties;

/*
	The ladybug frontend by default hides actions that the user is not allowed to do.
	This way, you cannot use the UI to test whether the backend blocks unauthorized use.
	This Spring configuration allows testers to work around this. See classpath
	file test.properties (src/main/resources/test.properties)
*/
@Configuration
public class TestPropertiesConfiguration {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static enum UI_TEST_MODE {
		DEFAULT,
		DONT_BLOCK_BACKEND;
	}
	public static final class TestProperties {
		private @Getter @Setter UI_TEST_MODE uiTestMode = UI_TEST_MODE.DEFAULT;
	}

	@Autowired
	private Environment env;

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
			log.error("Cannot read properties from existing classpath resource test.properties");
			return result;
		}
		String uiTestModeStr = props.getProperty("ladybug.ui.test.mode");
		if (uiTestModeStr != null) {
			try {
				UI_TEST_MODE uiTestMode = UI_TEST_MODE.valueOf(uiTestModeStr.toUpperCase());
				result.setUiTestMode(uiTestMode);
			} catch(IllegalArgumentException e) {
				log.error("Error parsing properties file 'test.properties': Unknown value for property 'ladybug.ui.test.mode' [{}]", uiTestModeStr);
			}
		}
		return result;
	}
}
