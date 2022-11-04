/*
   Copyright 2022 WeAreFrank!

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Redirect frontend from /ladybug/api/ to /ladybug-api/ when for example running on Quarkus. When the frontend is
 * adjusted to try both /ladybug/api/ and /ladybug-api/ this isn't needed anymore
 * 
 * @author Jaco de Groot
 */
public class RedirectServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doRequest("GET", request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doRequest("PUT", request, response);
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
		String pathInfo = "/" + ApiServlet.LADYBUG_API_PATH + request.getPathInfo();
		String queryString = request.getQueryString();
		if (queryString != null) {
			pathInfo = pathInfo + "?" + queryString;
		}
		response.setHeader("Location", response.encodeRedirectURL(pathInfo));
		response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
	}

}
