/*
   Copyright 2021 WeAreFrank!

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

import org.apache.cxf.transport.servlet.CXFServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class ApiServlet extends CXFServlet {
	Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static Map<String, String> getInitParameters() {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("config-location", "LadybugWebContext.xml");
		parameters.put("bus", "ladybug-api-bus");
		return parameters;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// This event listens to all Spring refresh events.
		// When adding new Spring contexts (with this as a parent) refresh events originating from other contexts will also trigger this method.
		// Since we never want to reinitialize this servlet, we can ignore the 'refresh' event completely!
	}
}
