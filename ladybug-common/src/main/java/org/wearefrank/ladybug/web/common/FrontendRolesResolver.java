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
package org.wearefrank.ladybug.web.common;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/*
 * Spring MVC and JAX-RS authorize access to the Ladybug backend.
 * This bean provides values that represent the access rights
 * to the frontend. The frontend uses these value to prevent the user
 * from trying unauthorized actions. This improves error handling,
 * because it becomes less important that the frontend provides clear
 * error messages when authorization is not granted.
 *
 * The roles that exist in the backend are not static because they are
 * injected from Spring configuration files (XML). For this reason
 * it is not wise to return them. When a user is in one of the
 * Spring-configured tester roles, we report to the frontend that the
 * user is tester. When the user is in one of the Spring configured
 * observer roles, we report to the frontend that the user is
 * observer. And similar for dataAdmin. These do not exclude each other.
 * The frontend should deal with multiple user roles. It should check
 * whether the user is in some role to decide whether to provide a
 * feature.
 */
@Component
public class FrontendRolesResolver implements InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static final String TESTER = "tester";
	public static final String ADMIN = "admin";
	public static final String OBSERVER = "observer";

	@Inject @Resource(name="observerRoles") List<String> observerRoles;

	@Inject @Resource(name="dataAdminRoles") List<String> dataAdminRoles;

	@Inject @Resource(name="testerRoles") List<String> testerRoles;

	@Override
	public void afterPropertiesSet() {
		log.info("Frontend role [{}] when backend role one of [{}]", OBSERVER, observerRoles.stream().collect(Collectors.joining(", ")));
		log.info("Frontend role [{}] when backend role one of [{}]", ADMIN, dataAdminRoles.stream().collect(Collectors.joining(", ")));
		log.info("Frontend role [{}] when backend role one of [{}]", TESTER, testerRoles.stream().collect(Collectors.joining(", ")));
	}

	public List<String> getFrontendRoles(String userRole) {
		if (userRole == null) {
			log.warn("FrontendRolesResolver.getFrontendRoles(String) gets null role. Frontend will work as if every role granted");
			return Arrays.asList(OBSERVER, ADMIN, TESTER);
		}
		return getFrontendRoles(backendRolesList -> backendRolesList.contains(userRole));
	}

	public List<String> getFrontendRoles(Predicate<List<String>> userInRolePredicate) {
		// For JAX-RS authorization, which is applied with ladybug-test-webapp,
		// the userInRolePredicate accesses ApiBase.isUserInRoles(). That method
		// grants every role when there is no user principal. For this reason,
		// the frontend of ladybug-test-webapp will work as if all roles are
		// granted when authorization is not tested (default web.xmo for which
		// the security stuff is commented out).
		List<String> result = new ArrayList<>();
		if (userInRolePredicate.test(observerRoles)) {
			result.add(OBSERVER);
		}
		if (userInRolePredicate.test(dataAdminRoles)) {
			result.add(ADMIN);
		}
		if (userInRolePredicate.test(testerRoles)) {
			result.add(TESTER);
		}
		log.debug("Have the following frontend roles: [{}]", result.stream().collect(Collectors.joining(", ")));
		return result;
	}
}
