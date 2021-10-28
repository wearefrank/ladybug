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

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.context.event.ContextRefreshedEvent;

public class ApiServlet extends CXFServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Static method that can be used to set the default mapping when creating this servlet programmatically instead of
	 * defining it in web.xml. The javadoc of {@link ApiServlet#getDefaultInitParameters()} shows an example usage for a
	 * Spring Boot application.
	 * 
	 * @return ...
	 */
	public static String getDefaultMapping() {
		return "/ladybug/api/*";
	}

	/**
	 * Static method that can be used to set the init parameters with the default values when creating this servlet
	 * programmatically instead of defining it in web.xml. This can for example be used in a Spring Boot application as
	 * follows:
	 * 
	 * <code>
	 * 	ServletRegistrationBean&lt;ApiServlet&gt; servletRegistrationBean =
	 * 			new ServletRegistrationBean&lt;ApiServlet&gt;(new ApiServlet(), ApiServlet.getDefaultMapping());
	 * 	servletRegistrationBean.setInitParameters(ApiServlet.getDefaultInitParameters());
	 * </code>
	 * 
	 * @return ...
	 */
	public static Map<String, String> getDefaultInitParameters() {
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
