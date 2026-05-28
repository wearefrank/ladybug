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
package org.wearefrank.ladybug.web.common;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.trace.v1.Span;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.TestTool;
import org.apache.commons.codec.binary.Hex;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class TracingApiImpl {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private @Setter TestTool testTool;

	public void processSpans(List<Span> trace) {
		ArrayList<ByteString> parentIds = new ArrayList<>();
		for (Span span: trace) {
			if (!span.getParentSpanId().isEmpty() && !parentIds.contains(span.getParentSpanId())) {
				parentIds.add(span.getParentSpanId());
			}
		}
		ArrayList<String> endpoints = new ArrayList<>();
		for (int i = trace.size() - 1; i >= 0; i--) {
			if (trace.get(i).getParentSpanId().isEmpty()) {
				testTool.startpoint(byteStringToHex(trace.get(i).getTraceId()), null, trace.get(i).getName(), toHashMap(trace.get(i)).toString());
				endpoints.add(trace.get(i).getName());
			} else {
				if (parentIds.contains(trace.get(i).getSpanId())) {
					testTool.startpoint(byteStringToHex(trace.get(i).getTraceId()), null, trace.get(i).getName(), toHashMap(trace.get(i)).toString());
					endpoints.add(trace.get(i).getName());
				} else {
					testTool.infopoint(byteStringToHex(trace.get(i).getTraceId()), null, trace.get(i).getName(), toHashMap(trace.get(i)).toString());
				}
			}
		}
		for (int i = endpoints.size() - 1; i >= 0; i--) {
			testTool.endpoint(byteStringToHex(trace.get(0).getTraceId()), null, endpoints.get(i), "Endpoint");
		}
	}

	public String byteStringToHex(ByteString byteString) {
		return Hex.encodeHexString(byteString.toByteArray());
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
}
