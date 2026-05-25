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

import com.google.protobuf.ByteString;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.opentelemetry.proto.trace.v1.Span;
import org.wearefrank.ladybug.TestTool;
import org.wearefrank.ladybug.storage.StorageException;

import java.lang.invoke.MethodHandles;
import java.util.*;

@Component
public class TracingApiImpl {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private @Setter TestTool testTool;

	public void processSpans(List<Span> spans) {
		for (Span span : spans) {
			String traceId = byteStringToHex(span.getTraceId());
			String spanId = byteStringToHex(span.getSpanId());
			String parentSpanId = span.getParentSpanId().isEmpty() ? "" : byteStringToHex(span.getParentSpanId());
			long startTime = span.getStartTimeUnixNano();

			testTool.startpoint(traceId, null, span.getName(), toHashMap(span), spanId, parentSpanId, startTime);
			testTool.endpoint(traceId, null, span.getName(), "Endpoint", spanId, parentSpanId, startTime);
			testTool.close(traceId);
		}
	}

	public HashMap<String, String> toHashMap(Span span) {
		HashMap<String, String> map = new HashMap<>();

		span.getAllFields().forEach((descriptor, value) -> {
			if (value instanceof ByteString) {
				map.put(descriptor.getName(), byteStringToHex((ByteString) value));
			} else {
				map.put(descriptor.getName(), value.toString());
			}
		});

		return map;
	}

	public String byteStringToHex(ByteString byteString) {
		return Hex.encodeHexString(byteString.toByteArray());
	}
}