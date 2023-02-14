/*
   Copyright 2021-2023 WeAreFrank!

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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.context.event.ContextRefreshedEvent;

public class ApiServlet extends CXFServlet {
	private static final long serialVersionUID = 1L;
	public static final String LADYBUG_API_PATH = "ladybug-api";

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
		parameters.put("config-location", "ladybug/cxf-beans.xml");
		parameters.put("bus", "ladybug-api-bus");
		return parameters;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			doRequest("GET", request, response);
		} catch (IOException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			doRequest("POST", request, response);
		} catch (IOException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doRequest("PUT", request, response);
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doRequest("DELETE", request, response);
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doRequest("HEAD", request, response);
	}

	private void doRequest(String method, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null || pathInfo.equals("") || pathInfo.equals("/")) {
			response.getWriter().write("This is the root of the Ladybug API");
		} else if (!pathInfo.startsWith("/" + ApiServlet.LADYBUG_API_PATH + "/")
				&& !pathInfo.startsWith(ApiServlet.LADYBUG_API_PATH + "/")) {
			// The paths in the API classes are prefixed with /ladybug-api/ to prevent the paths to interfere with the
			// paths used by applications that use Ladybug as a library and don't use CXF / this servlet (that could
			// prefix all API paths using a servlet url mapping), e.g. when using Quarkus. Hence, dispatch the mapped
			// url for this servlet to /ladybug-api/ which will call this method again going to the "else" below to
			// have the parent (CXFServlet) handle the API classes. Before using Quarkus it was not necessary to
			// override the do* methods and dispatch the request.
			pathInfo = ApiServlet.LADYBUG_API_PATH + pathInfo;
			final String finalPathInof = pathInfo;
			HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(request) {
				@Override
				public String getPathInfo() {
					return finalPathInof;
				}
				@Override
				public String getRequestURI() {
					return finalPathInof;
				}
			};
			RequestDispatcher requestDispatcher = request.getRequestDispatcher(pathInfo);
			requestDispatcher.forward(requestWrapper, response);
		} else {
			if (method.equals("GET")) {
				super.doGet(request, response);
			} else if (method.equals("POST")) {
				super.doPost(request, response);
			} else if (method.equals("PUT")) {
				super.doPut(request, response);
			} else if (method.equals("DELETE")) {
				super.doDelete(request, response);
			} else if (method.equals("HEAD")) {
				super.doHead(request, response);
			}
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// This event listens to all Spring refresh events.
		// When adding new Spring contexts (with this as a parent) refresh events originating from other contexts will also trigger this method.
		// Since we never want to reinitialize this servlet, we can ignore the 'refresh' event completely!
	}
}
