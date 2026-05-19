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
import io.opentelemetry.proto.trace.v1.Span;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.wearefrank.ladybug.SpanBuffer;
import org.wearefrank.ladybug.web.common.TracingApiImpl;

import lombok.Setter;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/traces")
@RolesAllowed("IbisWebService")
public class TracingApi {
	private SpanBuffer spanBuffer;

	@Autowired
	private @Setter TracingApiImpl delegate;

	@PostConstruct
	public void init() {
		spanBuffer = new SpanBuffer(delegate);
	}

	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, "application/x-protobuf"})
	public ResponseEntity<Void> receiveTrace(@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType, @RequestBody byte[] data) throws InvalidProtocolBufferException {
		ExportTraceServiceRequest request;

		if (contentType.startsWith("application/x-protobuf")) {
			request = ExportTraceServiceRequest.parseFrom(data);
		} else if (contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
			String json = new String(data, StandardCharsets.UTF_8);
			ExportTraceServiceRequest.Builder builder =
					ExportTraceServiceRequest.newBuilder();
			JsonFormat.parser().merge(json, builder);
			request = builder.build();
		} else {
			return ResponseEntity.badRequest().build();
		}

		for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
			for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
				for (Span span : scopeSpans.getSpansList()) {
					spanBuffer.addSpan(span);
				}
			}
		}

		return ResponseEntity.ok().build();
	}

}