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
package nl.nn.testtool.web.interceptors;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

/**
 * AuthenticationInterceptor filters the requests based on their permission requirements.
 *
 * Permission requirements can be set with set*Roles() methods,
 * where keys are strings explaining possible requests, such as "POST,PUT/path/to/\w+/endpoint".
 *
 * The first part of the string is a list of methods as CSV (Comma Separated Values),
 * and the latter part (starting with '/') is a regex string used to match requested paths.
 *
 * If multiple regex strings match a path, the one with the most '/' (forward slashes) will be considered the most
 * specific string, and therefore be applied for filtering.
 *
 * Method part can be left empty in order to apply it for all methods, such as "/path/to/endpoint".
 */
public class AuthenticationInterceptor implements ContainerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private Map<Pattern, RequestProperties> rolesMap = new HashMap<>();

	public void setChangeReportGeneratorEnabledRoles(List<String> changeReportGeneratorEnabledRoles) {
		logger.info("Setting change report generator enabled roles");
		setRoles("POST/testtool$", changeReportGeneratorEnabledRoles);
	}

	public void setRerunRoles(List<String> rerunRoles) {
		logger.info("Setting rerun roles");
		setRoles("POST/runner/run/.*", rerunRoles);
	}

	public void setLadybugApiRoles(Map<String, List<String>> ladybugApiRoles) {
		logger.info("Setting Ladybug api roles");
		for (String path : ladybugApiRoles.keySet()) {
			setRoles(path, ladybugApiRoles.get(path));
		}
	}

	private void setRoles(String path, List<String> roles) {
		RequestProperties requestProperties = new RequestProperties(path, roles);
		logger.debug("Setting roles [" + requestProperties.toString() + "]");
		rolesMap.put(requestProperties.getPath(), requestProperties);
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if (requestContext.getSecurityContext().getUserPrincipal() == null) {
			// The servlet container didn't authenticate the user (e.g. when running and developing locally). In this
			// case allow everything
			return;
		} else if (rolesMap.size() == 0) {
			// When user is authenticated and rolesMap is empty deny everything by default to prevent security issues
			// because of misconfiguration (e.g. no set methods of this bean not being (auto)wired while the developer
			// expected them to be (auto)wired)
		} else {
			// Find the matching path security configuration with the biggest length.
			String path = requestContext.getUriInfo().getPath().toLowerCase();
			RequestProperties properties = null;
			for (Pattern securePath : rolesMap.keySet()) {
				RequestProperties requestProperties = rolesMap.get(securePath);
				boolean pathMatches = securePath.matcher(path).matches();
				boolean isPropertiesNull = properties == null;
				boolean isReqPropMoreSpecific = isPropertiesNull || properties.getSpecificity() < requestProperties.getSpecificity();
				boolean requestPropertiesContainsMethod = rolesMap.get(securePath).containsMethod(requestContext.getMethod());
				if (pathMatches && (isPropertiesNull || isReqPropMoreSpecific) && requestPropertiesContainsMethod)
					properties = requestProperties;
			}
			// No specific security configuration is given for the path
			if (properties == null) return;
			for (String role : properties.getRoles()) {
				// If user is in one of the roles, continue with the chain.
				if (requestContext.getSecurityContext().isUserInRole(role)) return;
			}
		}
		// If security configuration is defined but the user is not in any of the roles, return unauthorized.
		requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
	}

	/**
	 * This class is used to represent authentication configurations for the interceptor.
	 */
	private static class RequestProperties {
		private @Getter Pattern path;
		private @Getter int specificity;
		private @Getter List<String> roles;
		private Set<String>  methods;

		/**
		 * Sets the path, roles and methods from the given config and list of roles.
		 * @param config A string explaining the possible requests, such as: "POST,PUT/path/to/endpoint"
		 * @param roles List of roles accepted for the given config.
		 */
		RequestProperties(String config, List<String> roles) {
			int firstSlash = config.indexOf('/');
			String[] methods = config.substring(0, firstSlash).split(",");
			if (methods.length != 0 && !methods[0].equals("")) {
				for (int i = 0; i < methods.length; i++)
					methods[i] = methods[i].toLowerCase();
				this.methods = new HashSet<String>(Arrays.asList(methods));
			}

			String regex = config.substring(firstSlash + 1);
			this.specificity = (int) regex.chars().filter(ch -> ch == '/').count();
			this.path = Pattern.compile(regex);
			this.roles = roles == null ? new ArrayList<>(0) : roles;
		}

		public boolean containsMethod(String method) {
			return methods == null || methods.contains(method.toLowerCase());
		}

		@Override
		public String toString() {
			return "Path: \"" + path + "\", Roles: " + roles.toString();
		}
	}
}