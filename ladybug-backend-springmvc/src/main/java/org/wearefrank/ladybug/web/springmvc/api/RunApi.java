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

import java.lang.invoke.MethodHandles;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.Setter;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.SecurityContext;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.run.ReportRunner;
import org.wearefrank.ladybug.run.RunResult;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wearefrank.ladybug.web.common.HttpBadRequestException;
import org.wearefrank.ladybug.web.common.HttpInternalServerErrorException;
import org.wearefrank.ladybug.web.common.RunApiImpl;

import static org.wearefrank.ladybug.web.common.Util.fullMessage;

@RestController
@RequestMapping("/runner")
@RolesAllowed("IbisTester")
public class RunApi implements SecurityContext {
	private @Autowired RunApiImpl delegate;

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
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			String role = authority.getAuthority();
			if (role.startsWith("ROLE_")) {
				role = role.substring(5);
			}
			if (roles.contains(role)) {
				return true;
			}
		}
		return false;
	}

	@PostMapping(value = "/run/{storageName}/{storageId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> runReport(@PathVariable("storageName") String storageName, @PathVariable("storageId") int storageId) {
		try {
			Map<String, Object> result = delegate.runReport(storageName, storageId, this);
			return ResponseEntity.ok(result);
		} catch (HttpBadRequestException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (HttpInternalServerErrorException e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}
}
