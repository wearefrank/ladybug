/*
   Copyright 2021-2022, 2024, 2025 WeAreFrank!

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
package nl.nn.testtool.web;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;

public class FrontendServlet extends AngularServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String FALLBACK_MESSAGE = ", will fall back to version agnostic approach";

	/**
	 * See documentation at {@link ApiServlet#getDefaultMapping()}
	 * 
	 * @return ...
	 */
	public static String getDefaultMapping() {
		return "/ladybug/*";
	}

	@Override
	public void init() throws ServletException {
		String artifactId = "ladybug-frontend";
		setArtifactId(artifactId);
		try {
			String resource = "/META-INF/maven/org.wearefrank/" + artifactId + "/pom.properties";
			InputStream inputStream = this.getClass().getResourceAsStream(resource);
			if (inputStream != null) {
				Properties properties = new Properties();
				properties.load(inputStream);
				String version = properties.getProperty("version");
				log.debug("Resolved " + artifactId + " version to: " + version);
				setVersion(version);
			} else {
				log.debug("Could not find " + resource + FALLBACK_MESSAGE);
			}
		} catch (IOException e) {
			log.debug("Could not read " + artifactId + " version from pom.properties" + FALLBACK_MESSAGE, e);
		}
		super.init();
	}

}
