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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wearefrank.ladybug.TestTool;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

@Component
public class TracingApiImpl {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


	@Autowired
	private @Setter TestTool testTool;

	public void processSpans(String contentType, byte[] data) throws InvalidProtocolBufferException {
		List<ResourceSpans> resourceSpans = parseData(contentType, data);

		for (ResourceSpans resourceSpan : resourceSpans) {
			for (ScopeSpans scopeSpan : resourceSpan.getScopeSpansList()) {
				List<Span> trace = scopeSpan.getSpansList();

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
		}
	}

	public List<ResourceSpans> parseData(String contentType, byte[] data) throws InvalidProtocolBufferException {
		ExportTraceServiceRequest request = null;

		if (contentType != null && contentType.startsWith("application/x-protobuf")) {
			request = ExportTraceServiceRequest.parseFrom(data);
		} else if (contentType != null && contentType.startsWith("application/json")) {
			ExportTraceServiceRequest.Builder builder = ExportTraceServiceRequest.newBuilder();
			// OTLP/JSON receivers must ignore unknown fields, so that senders can use a newer version of the protocol
			JsonFormat.parser().ignoringUnknownFields().merge(otlpJsonToProtobufJson(data), builder);
			request = builder.build();
		}

		return request.getResourceSpansList();
	}

	/**
	 * OTLP/JSON deviates from the standard Protobuf JSON mapping by hex encoding trace and span ids instead of base64
	 * encoding them, see https://opentelemetry.io/docs/specs/otlp/#json-protobuf-encoding. Convert the ids of the spans
	 * and their links to base64, so that the result can be parsed with {@link JsonFormat}.
	 */
	private String otlpJsonToProtobufJson(byte[] data) throws InvalidProtocolBufferException {
		try {
			JsonElement json = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
			if (json.isJsonObject()) {
				for (JsonObject resourceSpans : objectsInArray(json.getAsJsonObject(), "resourceSpans")) {
					for (JsonObject scopeSpans : objectsInArray(resourceSpans, "scopeSpans")) {
						for (JsonObject span : objectsInArray(scopeSpans, "spans")) {
							hexToBase64(span, "traceId", "spanId", "parentSpanId");
							for (JsonObject link : objectsInArray(span, "links")) {
								hexToBase64(link, "traceId", "spanId");
							}
						}
					}
				}
			}
			return json.toString();
		} catch (JsonParseException | DecoderException e) {
			throw new InvalidProtocolBufferException("Invalid OTLP/JSON: " + e.getMessage());
		}
	}

	private List<JsonObject> objectsInArray(JsonObject parent, String arrayName) {
		List<JsonObject> result = new ArrayList<>();
		JsonElement array = parent.get(arrayName);
		if (array != null && array.isJsonArray()) {
			for (JsonElement element : array.getAsJsonArray()) {
				if (element.isJsonObject()) {
					result.add(element.getAsJsonObject());
				}
			}
		}
		return result;
	}

	private void hexToBase64(JsonObject object, String... fieldNames) throws DecoderException {
		for (String fieldName : fieldNames) {
			JsonElement value = object.get(fieldName);
			if (value != null && value.isJsonPrimitive()) {
				byte[] id = Hex.decodeHex(value.getAsString());
				object.addProperty(fieldName, Base64.getEncoder().encodeToString(id));
			}
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

	public String getInfo() {
		return "You've found the correct URL for OTEL to POST to :-)";
	}
}
