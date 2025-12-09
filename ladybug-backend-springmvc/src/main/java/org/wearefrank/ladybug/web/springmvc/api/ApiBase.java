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
package org.wearefrank.ladybug.web.springmvc.api;

import java.security.Principal;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.wearefrank.ladybug.SecurityContext;
import org.wearefrank.ladybug.web.common.HttpInternalServerErrorException;

public class ApiBase implements SecurityContext {
	private static HttpServletRequest getHttpServletRequest() {
		ServletRequestAttributes requestAttributes =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		return requestAttributes.getRequest();
	}

	protected void setSessionAttr(String key, Object value) {
		getHttpServletRequest().getSession().setAttribute(key, value);
	}

	protected <T> T getSessionAttr(String key) throws HttpInternalServerErrorException {
		return getSessionAttr(key, true);
	}

	protected <T> T getSessionAttr(String key, boolean throwException) throws HttpInternalServerErrorException {
		T object = (T) getHttpServletRequest().getSession().getAttribute(key);
		if (object == null && throwException)
			throw new HttpInternalServerErrorException("No session attribute with name [" + key + "] found.");
		return object;
	}

	@Override
	public Principal getUserPrincipal() {
		return getHttpServletRequest().getUserPrincipal();
	}

	@Override
	public boolean isUserInRoles(List<String> roles) {
		if (getUserPrincipal() == null) {
			// The servlet container didn't authenticate the user (not
			// configured in the web.xml or explicitly overwritten by the
			// servlet container (e.g. when running locally in WSAD)). In this
			// case allow everything.
			return true;
		}
		for (String role : roles) {
			if (getHttpServletRequest().isUserInRole(role))
				return true;
		}
		return false;
	}
}
