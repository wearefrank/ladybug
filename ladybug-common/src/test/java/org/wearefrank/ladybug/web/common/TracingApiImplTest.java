package org.wearefrank.ladybug.web.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

public class TracingApiImplTest {
	private static final String TRACE_ID = "5b8efff798038103d269b633813fc60c";
	private static final String SPAN_ID = "eee19b7ec3c1b174";
	private static final String PARENT_SPAN_ID = "eee19b7ec3c1b173";
	private static final String LINKED_TRACE_ID = "5b8efff798038103d269b633813fc60d";
	private static final String LINKED_SPAN_ID = "eee19b7ec3c1b175";

	// Example of the OTLP/JSON encoding in https://opentelemetry.io/docs/specs/otlp/#json-protobuf-encoding, with
	// an unknown field, upper case hex and enum values that are integers
	private static final String OTLP_JSON = """
			{
			  "resourceSpans": [{
			    "resource": {"attributes": [{"key": "service.name", "value": {"stringValue": "my.service"}}]},
			    "scopeSpans": [{
			      "scope": {"name": "my.library", "version": "1.0.0"},
			      "spans": [{
			        "traceId": "%s",
			        "spanId": "%s",
			        "parentSpanId": "%s",
			        "name": "I'm a server span",
			        "startTimeUnixNano": "1544712660000000000",
			        "endTimeUnixNano": 1544712661000000000,
			        "kind": 2,
			        "links": [{"traceId": "%s", "spanId": "%s"}],
			        "someFieldFromANewerProtocolVersion": {"spanId": "not hex"}
			      }]
			    }]
			  }]
			}
			""".formatted(TRACE_ID, SPAN_ID, PARENT_SPAN_ID, LINKED_TRACE_ID.toUpperCase(), LINKED_SPAN_ID);

	@Test
	public void when_otlp_json_is_parsed_then_ids_are_hex_decoded() throws Exception {
		Span span = parseJson(OTLP_JSON).get(0).getScopeSpans(0).getSpans(0);
		assertEquals(TRACE_ID, hex(span.getTraceId()));
		assertEquals(SPAN_ID, hex(span.getSpanId()));
		assertEquals(PARENT_SPAN_ID, hex(span.getParentSpanId()));
		assertEquals(LINKED_TRACE_ID, hex(span.getLinks(0).getTraceId()));
		assertEquals(LINKED_SPAN_ID, hex(span.getLinks(0).getSpanId()));
		assertEquals(Span.SpanKind.SPAN_KIND_SERVER, span.getKind());
		assertEquals(1544712661000000000L, span.getEndTimeUnixNano());
	}

	@Test
	public void when_same_trace_is_sent_as_otlp_json_and_protobuf_then_result_is_equal() throws Exception {
		Span span = Span.newBuilder()
				.setTraceId(bytes(TRACE_ID))
				.setSpanId(bytes(SPAN_ID))
				.setParentSpanId(bytes(PARENT_SPAN_ID))
				.setName("I'm a server span")
				.setStartTimeUnixNano(1544712660000000000L)
				.setEndTimeUnixNano(1544712661000000000L)
				.setKind(Span.SpanKind.SPAN_KIND_SERVER)
				.addLinks(Span.Link.newBuilder().setTraceId(bytes(LINKED_TRACE_ID)).setSpanId(bytes(LINKED_SPAN_ID)))
				.build();
		ExportTraceServiceRequest request = ExportTraceServiceRequest.newBuilder()
				.addResourceSpans(ResourceSpans.newBuilder()
						.setResource(parseJson(OTLP_JSON).get(0).getResource())
						.addScopeSpans(ScopeSpans.newBuilder()
								.setScope(parseJson(OTLP_JSON).get(0).getScopeSpans(0).getScope())
								.addSpans(span)))
				.build();
		List<ResourceSpans> fromProtobuf = new TracingApiImpl().parseData("application/x-protobuf", request.toByteArray());
		assertEquals(fromProtobuf, parseJson(OTLP_JSON));
	}

	@Test
	public void when_id_is_not_hex_then_invalid_protocol_buffer_exception() {
		String json = OTLP_JSON.replace(SPAN_ID, "not hex");
		assertThrows(InvalidProtocolBufferException.class, () -> parseJson(json));
	}

	@Test
	public void when_json_is_malformed_then_invalid_protocol_buffer_exception() {
		assertThrows(InvalidProtocolBufferException.class, () -> parseJson("{\"resourceSpans\": ["));
	}

	private List<ResourceSpans> parseJson(String json) throws InvalidProtocolBufferException {
		return new TracingApiImpl().parseData("application/json", json.getBytes(StandardCharsets.UTF_8));
	}

	private static String hex(ByteString byteString) {
		return Hex.encodeHexString(byteString.toByteArray());
	}

	private static ByteString bytes(String hex) throws Exception {
		return ByteString.copyFrom(Hex.decodeHex(hex));
	}
}
