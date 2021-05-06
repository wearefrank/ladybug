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

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AuthenticationInterceptor implements ContainerRequestFilter {
	private static Map<String, RequestProperties> rolesMap = new HashMap<>();
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String path = requestContext.getUriInfo().getPath().toLowerCase();

		if (rolesMap == null) return;
		String mostSpecificPath = null;

		// Find the matching path security configuration with the biggest length.
		// TODO: Optimize with trees? Overengineering?
		// TODO: Maybe regex instead? for paths like /path/to/{param}/action?....
		for (String securePath : rolesMap.keySet()) {
			boolean a = path.startsWith(securePath.toLowerCase());
			boolean b = mostSpecificPath == null;
			boolean c = b || mostSpecificPath.length() < securePath.length();
			boolean d = rolesMap.get(securePath).containsMethod(requestContext.getMethod());
			if (a && (b || c) && d)
				mostSpecificPath = securePath;
		}

		// No specific security configuration is given for the path
		if (mostSpecificPath == null) return;

		RequestProperties properties = rolesMap.get(mostSpecificPath);
		for (String role : properties.getRoles()) {
			// If user is in one of the roles, continue with the chain.
			if (requestContext.getSecurityContext().isUserInRole(role)) return;
		}

		// If security configuration is defined but the user is not in any of the roles, return unauthorized.
		requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
	}

	public static void setRolesMap(Map<String, List<String>> rolesMap) {
		logger.info("Setting the roles map.");
		AuthenticationInterceptor.rolesMap = new HashMap<>(rolesMap.size());

		for (String config : rolesMap.keySet()) {
			RequestProperties properties = new RequestProperties(config, rolesMap.get(config));
			AuthenticationInterceptor.rolesMap.put(properties.getPath(), properties);
			logger.debug("Setting the role [" + properties.toString() + "]");
		}
	}

	private static class RequestProperties {
		private @Getter String path;
		private @Getter List<String> roles;
		private Set<String>  methods;

		RequestProperties(String config, List<String> roles) {
			int firstSlash = config.indexOf('/');
			String[] methods = config.substring(0, firstSlash).split(",");
			if (methods.length != 0 && !methods[0].equals("")) {
				for (int i = 0; i < methods.length; i++)
					methods[i] = methods[i].toLowerCase();
				this.methods = new HashSet<String>(Arrays.asList(methods));
			}

			this.path = config.substring(firstSlash + 1);
			this.roles = roles;
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