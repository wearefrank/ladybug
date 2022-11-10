/*
   Copyright 2021-2022 WeAreFrank!

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
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

/**
 * Filter requests based on their permission requirements.
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
@Provider
@PreMatching
public class ApiAuthorizationFilter implements ContainerRequestFilter {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private Map<Pattern, ConfigurationPart> configuration = new HashMap<>();
	private boolean initialWarningLogged = false;
	private boolean globalFilter = false;

	public ApiAuthorizationFilter() {
		// When security disabled:
		//   Whitelist all url's to allow all API resources (the filter() method will skip isUserInRole() check when the
		//   user isn't authenticated)
		// When security is enabled:
		//   Deny access to all API resources by default and for example prevent that when by accident only
		//   setDataAdminRoles() is called it allows access to resources that should be disallowed by a call to
		//   setTesterRoles() (making /runner/run/.* override /runner/.* for the run resources)
		setObserverRoles(null);
		setDataAdminRoles(null);
		setTesterRoles(null);
	}

	/**
	 * Init to be used when filter is applied for all resource of the applicatione (e.g. in a Quarkus application). This
	 * is not needed and not recommended when filter is applied to the ApiServlet resources only (e.g. when configured
	 * in CXF, see cxf-beans.xml in Ladybug project (ApiServlet extends CXFServlet))
	 */
	@PostConstruct
	public void initGlobalFilter() {
		globalFilter = true;
	}

	public void setObserverRoles(List<String> observerRoles) {
		log.info("Set observer roles");
		addConfigurationPart("GET/"  + ApiServlet.LADYBUG_API_PATH + "/testtool.*$", observerRoles);
		addConfigurationPart("POST/" + ApiServlet.LADYBUG_API_PATH + "/testtool/transformation[/]?$", observerRoles); // [/]? because frontend is currently not using a slash at the end
		addConfigurationPart("GET/"  + ApiServlet.LADYBUG_API_PATH + "/metadata/.*$", observerRoles);
		addConfigurationPart("GET/"  + ApiServlet.LADYBUG_API_PATH + "/report/.*$", observerRoles);
	}

	public void setDataAdminRoles(List<String> dataAdminRoles) {
		log.info("Set change report generator enabled roles");
		addConfigurationPart("POST/"   + ApiServlet.LADYBUG_API_PATH + "/testtool$", dataAdminRoles);
		addConfigurationPart("DELETE/" + ApiServlet.LADYBUG_API_PATH + "/in-progress/.*$", dataAdminRoles);
		addConfigurationPart("DELETE/" + ApiServlet.LADYBUG_API_PATH + "/report/.*$", dataAdminRoles);
		addConfigurationPart("PUT/"    + ApiServlet.LADYBUG_API_PATH + "/report/.*$", dataAdminRoles);
		addConfigurationPart("POST/"   + ApiServlet.LADYBUG_API_PATH + "/report/.*$", dataAdminRoles);
		addConfigurationPart("PUT/"    + ApiServlet.LADYBUG_API_PATH + "/runner/.*", dataAdminRoles);
		addConfigurationPart("POST/"   + ApiServlet.LADYBUG_API_PATH + "/runner/.*", dataAdminRoles);
	}

	/**
	 * Set tester roles, a role which is normally not available in production environments, hence actions for this role
	 * will be disabled in P
	 * 
	 * @param testerRoles
	 */
	public void setTesterRoles(List<String> testerRoles) {
		log.info("Set rerun roles");
		addConfigurationPart("POST/" + ApiServlet.LADYBUG_API_PATH + "/runner/run/.*", testerRoles);
	}

	public void setLadybugApiRoles(Map<String, List<String>> ladybugApiRoles) {
		log.info("Set Ladybug api roles");
		for (String path : ladybugApiRoles.keySet()) {
			addConfigurationPart(path, ladybugApiRoles.get(path));
		}
	}

	private void addConfigurationPart(String path, List<String> roles) {
		ConfigurationPart configurationPart = new ConfigurationPart(path, roles);
		log.debug("Add configuration part [" + configurationPart.toString() + "]");
		configuration.put(configurationPart.getPattern(), configurationPart);
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String path = requestContext.getUriInfo().getPath().toLowerCase();
		if (path.startsWith("/")) {
			// Remove extra / at the beginning when using Quarkus (and possible other setups)
			path = path.substring(1);
		}
		if (globalFilter && !path.contains(ApiServlet.LADYBUG_API_PATH)) {
			// Ignore everything but the Ladybug API using contains() instead of startsWith() to be on the safe side
			// and prevent misuse of this return (e.g. by prefixing the Ladybug API path with multiple slashes)
			return;
		}
		String method = requestContext.getMethod();
		boolean noRolesFound = true;
		ConfigurationPart mostSpecificConfigurationPart = null;
		// Find the matching path pattern with the biggest length
		for (Pattern pattern : configuration.keySet()) {
			ConfigurationPart configurationPart = configuration.get(pattern);
			boolean pathMatches = pattern.matcher(path).matches();
			boolean isMostSpecificConfigurationPartNull = mostSpecificConfigurationPart == null;
			boolean isConfigurationPartMoreSpecific = isMostSpecificConfigurationPartNull
					|| configurationPart.getSpecificity() > mostSpecificConfigurationPart.getSpecificity();
			boolean configurationPartContainsMethod =
					configuration.get(pattern).containsMethod(method);
			if (pathMatches && isConfigurationPartMoreSpecific && configurationPartContainsMethod) {
				mostSpecificConfigurationPart = configurationPart;
			}
		}
		if (mostSpecificConfigurationPart != null) {
			String reason = " allowed by " + mostSpecificConfigurationPart.toString();
			if (requestContext.getSecurityContext().getUserPrincipal() == null) {
				// The servlet container didn't authenticate the user (e.g. when running and developing locally). In
				// this case allow everyone
				if (!initialWarningLogged) {
					log.warn("Security has been disabled, this should only be the case when developing locally!");
					initialWarningLogged = true;
				}
				log(requestContext, method, path, true, reason + " with security disabled");
				return;
			} else {
				for (String role : mostSpecificConfigurationPart.getRoles()) {
					if (role != null) {
						noRolesFound = false;
						if (requestContext.getSecurityContext().isUserInRole(role)) {
							log(requestContext, method, path, true, reason);
							return;
						}
					}
				}
			}
		}
		String reason = " NOT ALLOWED becasue";
		if (mostSpecificConfigurationPart == null) {
			reason = reason + " no matching pattern found";
		} else {
			reason = reason + " user not in role for " + mostSpecificConfigurationPart;
			if (noRolesFound) {
				reason = reason + " (use ApiAuthorizationFilter.set*Roles())";
			}
		}
		log(requestContext, method, path, false, reason);
		// Return unauthorized.
		// Return string "Not allowed!" to be able to see the request is unauthorized when CXF returns OK (200) instead
		// of UNAUTHORIZED (401). From https://cxf.apache.org/docs/jax-rs-filters.html:
		//   At the moment it is not possible to override a response status code from a CXF interceptor running before
		//   JAXRSOutInterceptor, like CustomOutInterceptor above, which will be fixed.
		requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Not allowed!").build());
	}

	private void log(ContainerRequestContext requestContext, String method, String path, boolean allowed,
			String reason) {
		String user = "";
		if (requestContext.getSecurityContext().getUserPrincipal() != null) {
			user = requestContext.getSecurityContext().getUserPrincipal().getName();
		}
		log.debug("[" + method + "]" + path + "[" + user + "]" + reason);
	}

	/**
	 * This class is used to represent a piece of authorization configuration
	 */
	private static class ConfigurationPart {
		private @Getter Set<String> methods;
		private @Getter Pattern pattern;
		private @Getter int specificity;
		private @Getter List<String> roles;

		/**
		 * Hold a piece of authorization configuration
		 * 
		 * @param methodsAndPathPattern  A string explaining the possible requests, such as: "POST,PUT/path/to/endpoint"
		 * @param roles                 List of roles accepted for the given config.
		 */
		ConfigurationPart(String methodsAndPathPattern, List<String> roles) {
			int firstSlash = methodsAndPathPattern.indexOf('/');
			String[] methods = methodsAndPathPattern.substring(0, firstSlash).split(",");
			if (methods.length != 0 && !methods[0].equals("")) {
				for (int i = 0; i < methods.length; i++)
					methods[i] = methods[i].toLowerCase();
				this.methods = new HashSet<String>(Arrays.asList(methods));
			}

			String regex = methodsAndPathPattern.substring(firstSlash + 1);
			this.specificity = (int) regex.chars().filter(ch -> ch == '/').count();
			this.pattern = Pattern.compile(regex);
			this.roles = roles == null ? new ArrayList<>(0) : roles;
		}

		public boolean containsMethod(String method) {
			return methods == null || methods.contains(method.toLowerCase());
		}

		@Override
		public String toString() {
			return methods.toString() + pattern + roles.toString();
		}
	}
}