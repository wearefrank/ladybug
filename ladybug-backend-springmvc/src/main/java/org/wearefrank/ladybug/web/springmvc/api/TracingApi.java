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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.wearefrank.ladybug.storage.StorageException;
import org.wearefrank.ladybug.web.common.TracingApiImpl;

import lombok.Setter;

@RestController
@RequestMapping("/traces")
@RolesAllowed("IbisWebService")
public class TracingApi {
	@Autowired
	private @Setter TracingApiImpl delegate;

	@PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, "application/x-protobuf"})
	public ResponseEntity<Void> receiveTrace(@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType, @RequestBody byte[] data)  throws InvalidProtocolBufferException, StorageException {
		ExportTraceServiceRequest request;

		if (contentType.startsWith("application/x-protobuf")) {
			request = ExportTraceServiceRequest.parseFrom(data);
		} else if (contentType.startsWith("application/json")) {
			String json = new String(data);
			ExportTraceServiceRequest.Builder builder = ExportTraceServiceRequest.newBuilder();
			JsonFormat.parser().merge(json, builder);
			request = builder.build();
		} else {
			return ResponseEntity.badRequest().build();
		}

		for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
			for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
				delegate.processSpans(scopeSpans.getSpansList());
			}
		}

		return ResponseEntity.ok().build();
	}
}