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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;

@Component
public class CollectorApiImpl {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private @Setter TestTool testTool;

	public void processSpans(ArrayList<Span> trace) {
		ArrayList<String> parentIds = new ArrayList<>();
		for (Span span: trace) {
			String parentId = byteStringToHex(span.getParentSpanId());
			if (!parentId.isEmpty() && !parentIds.contains(parentId)) {
				parentIds.add(parentId);
			}
		}

		ArrayList<String> endpoints = new ArrayList<>();
		for (Span span: trace) {

			String parentId = byteStringToHex(span.getParentSpanId());

			if (parentId.isEmpty()) {
				testTool.startpoint(byteStringToHex(span.getTraceId()), null, span.getName(), toHashMap(span).toString());
				endpoints.add(span.getName());
			} else {
				if (parentIds.contains(parentId)) {
					testTool.startpoint(byteStringToHex(span.getTraceId()), null, span.getName(), toHashMap(span).toString());
					testTool.infopoint(byteStringToHex(span.getTraceId()), null, span.getName(), span.getKind());
					endpoints.add(span.getName());
				} else {
					testTool.infopoint(byteStringToHex(span.getTraceId()), null, span.getName(), toHashMap(span).toString());
				}
			}
		}
		for (int i = endpoints.size() - 1; i >= 0; i--) {
			testTool.endpoint(byteStringToHex(trace.get(0).getTraceId()), null, endpoints.get(i), "Endpoint");
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