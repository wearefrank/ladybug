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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice({"org.wearefrank.ladybug", "org.frankframework.ladybug"})
public class ExceptionHandlers {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public ExceptionHandlers() {
		// TODO: Decrease log level.
		log.error("Constructing org.wearefrank.ladybug.web.springmvc.api.ExceptionHandlers");
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<Object> handleAccessDeniedException(AuthorizationDeniedException e) {
		log.error("ExceptionHandlers.handleAccessDeniedException() captured exception", e);
		Map<String, String> json = new HashMap<>();
		json.put("status", HttpStatus.valueOf(403).getReasonPhrase());
		// Replace non ASCII characters, tabs, spaces and newlines.
		json.put("error", "Not allowed");

		ResponseEntity.BodyBuilder builder = ResponseEntity.status(403);
		builder.contentType(MediaType.APPLICATION_JSON);
		return builder.body(json);
	}
}
