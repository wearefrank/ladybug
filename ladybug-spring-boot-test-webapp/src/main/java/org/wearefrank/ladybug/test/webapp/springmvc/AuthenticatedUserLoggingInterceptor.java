/*
   Copyright 2026 WeAreFrank!

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
package org.wearefrank.ladybug.test.webapp.springmvc;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs the authenticated user for every request handled by a controller, in the same "[METHOD]path[user]" format
 * used by org.wearefrank.ladybug.web.jaxrs.ApiAuthorizationFilter, so that CI can grep ladybug.log to report which
 * user a Cypress test actually logged in as.
 */
@Slf4j
public class AuthenticatedUserLoggingInterceptor implements HandlerInterceptor {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String user = authentication != null ? authentication.getName() : "";
		log.debug("[" + request.getMethod() + "]" + request.getRequestURI() + "[" + user + "]");
		return true;
	}
}
