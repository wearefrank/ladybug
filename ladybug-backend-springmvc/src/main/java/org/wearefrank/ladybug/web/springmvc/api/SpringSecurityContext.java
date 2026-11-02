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
package org.wearefrank.ladybug.web.springmvc.api;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wearefrank.ladybug.SecurityContext;

/**
 * {@link SecurityContext} implementation backed by Spring Security, shared by
 * {@link RunApi} and {@link TestToolApi}.
 */
@Slf4j
public class SpringSecurityContext implements SecurityContext {
	@Override
	public Principal getUserPrincipal() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	@Override
	public boolean isUserInRoles(List<String> roles) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return true;
		}
		List<String> grantedRoles = authentication.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.filter((s) -> s.startsWith("ROLE_"))
				.map((s) -> s.substring(5))
				.collect(Collectors.toList());
		log.debug("SpringSecurityContext.isUserInRoles() sees granted roles {}", grantedRoles);
		return grantedRoles.stream().anyMatch(roles::contains);
	}
}
