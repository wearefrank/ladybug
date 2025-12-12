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

import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wearefrank.ladybug.Span;
import org.wearefrank.ladybug.web.common.CollectorApiImpl;

import lombok.Setter;

@RestController
@RequestMapping("/collector")
@RolesAllowed("IbisWebService")
public class CollectorApi {
	@Autowired
	private @Setter CollectorApiImpl delegate;

	@PostMapping(value = "/")
	public ResponseEntity<Void> collectSpans(Span[] trace) {
		delegate.processSpans(trace);
		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> collectSpansJson(Span[] trace) {
		delegate.processSpans(trace);
		return ResponseEntity.ok().build();
	}

}