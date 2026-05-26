/*
   Copyright 2025, 2026 WeAreFrank!

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
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import jakarta.annotation.security.RolesAllowed;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.wearefrank.ladybug.web.common.TracingApiImpl;

@RestController
@RequestMapping("/traces")
@RolesAllowed("IbisWebService")
public class TracingApi {
	@Autowired
	private @Setter TracingApiImpl delegate;

	@PostMapping(consumes = {"application/x-protobuf", MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> receiveSpans(@RequestHeader("Content-Type") String contentType, @RequestBody byte[] data) throws InvalidProtocolBufferException {
		ExportTraceServiceRequest request;

		if (contentType != null && contentType.startsWith("application/x-protobuf")) {
			request = ExportTraceServiceRequest.parseFrom(data);
		} else if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
			String json = new String(data);
			ExportTraceServiceRequest.Builder builder = ExportTraceServiceRequest.newBuilder();
			JsonFormat.parser().merge(json, builder);
			request = builder.build();
		} else {
			ExportTraceServiceResponse response = ExportTraceServiceResponse.newBuilder().build();
			return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
					.contentType(MediaType.APPLICATION_JSON)
					.body(JsonFormat.printer().print(response));
		}

		for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
			for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
				delegate.processSpans(scopeSpans.getSpansList());
			}
		}

		ExportTraceServiceResponse response = ExportTraceServiceResponse.newBuilder().build();
		if (contentType.startsWith("application/x-protobuf")) {
			return ResponseEntity.ok()
					.contentType(org.springframework.http.MediaType.parseMediaType("application/x-protobuf"))
					.body(response.toByteArray());
		} else {
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(JsonFormat.printer().print(response));
		}
	}
}